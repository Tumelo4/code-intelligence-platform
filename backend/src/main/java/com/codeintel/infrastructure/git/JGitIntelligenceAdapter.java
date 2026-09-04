package com.codeintel.infrastructure.git;

import com.codeintel.application.ports.outbound.GitAnalysisPort;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.git.AuthorContribution;
import com.codeintel.domain.git.ChangeCoupling;
import com.codeintel.domain.git.FileHistory;
import com.codeintel.domain.git.GitCommit;
import com.codeintel.domain.git.GitIntelligenceReport;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public final class JGitIntelligenceAdapter implements GitAnalysisPort {
    private static final Comparator<GitCommit> COMMIT_ORDER = Comparator
            .comparing(GitCommit::authoredAt).reversed().thenComparing(GitCommit::sha);
    private final GitIntelligenceLimits limits;

    public JGitIntelligenceAdapter(GitIntelligenceLimits limits) { this.limits = limits; }

    @Override
    public GitIntelligenceReport analyze(Path immutableOriginal, AcquisitionRevision revision) {
        if (revision.kind() != AcquisitionRevision.Kind.GIT_COMMIT) {
            throw new GitIntelligenceSafetyException("Git intelligence requires a Git commit acquisition");
        }
        try {
            Path original = immutableOriginal.toRealPath(LinkOption.NOFOLLOW_LINKS);
            Path history = original.resolveSibling("history.git");
            if (!Files.isDirectory(original, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(history)
                    || !Files.isDirectory(history, LinkOption.NOFOLLOW_LINKS)
                    || !history.toRealPath(LinkOption.NOFOLLOW_LINKS).getParent().equals(original.getParent())) {
                throw new GitIntelligenceSafetyException("immutable Git history is unavailable or unsafe");
            }
            try (Repository repository = new FileRepositoryBuilder().setGitDir(history.toFile())
                    .setBare().setMustExist(true).build()) {
                return analyze(repository, revision.value());
            }
        } catch (GitIntelligenceSafetyException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GitIntelligenceSafetyException("Git history analysis failed safely", exception);
        }
    }

    private GitIntelligenceReport analyze(Repository repository, String revision) throws Exception {
        ObjectId objectId = repository.resolve(revision + "^{commit}");
        if (objectId == null || !objectId.name().equals(revision)) {
            throw new GitIntelligenceSafetyException("exact acquired commit is missing");
        }
        List<CommitChange> changes = new ArrayList<>();
        Map<String, String> canonicalNames = new HashMap<>();
        boolean truncated = false;
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit head = walk.parseCommit(objectId);
            walk.markStart(head);
            int count = 0;
            for (RevCommit commit : walk) {
                if (count++ == limits.maximumCommits()) {
                    truncated = true;
                    break;
                }
                changes.add(readCommit(repository, walk, commit, canonicalNames));
            }
        }
        changes.sort(Comparator.comparing((CommitChange change) -> change.commit().authoredAt()).reversed()
                .thenComparing(change -> change.commit().sha()));
        return aggregate(changes, truncated);
    }

    private CommitChange readCommit(Repository repository, RevWalk walk, RevCommit commit,
            Map<String, String> canonicalNames) throws Exception {
        RevCommit parent = commit.getParentCount() == 0 ? null : walk.parseCommit(commit.getParent(0));
        List<FileChange> files = new ArrayList<>();
        BoundedOutputStream patchBytes = new BoundedOutputStream(limits.maximumDiffBytes());
        try (ObjectReader reader = repository.newObjectReader();
                DiffFormatter formatter = new DiffFormatter(patchBytes)) {
            formatter.setRepository(repository);
            formatter.setReader(reader, repository.getConfig());
            formatter.setDiffComparator(RawTextComparator.DEFAULT);
            formatter.setDetectRenames(true);
            List<DiffEntry> entries = formatter.scan(tree(reader, parent), tree(reader, commit));
            if (entries.size() > limits.maximumFilesPerCommit()) {
                throw new GitIntelligenceSafetyException("commit exceeds changed-file limit");
            }
            for (DiffEntry entry : entries) {
                formatter.format(entry);
                int added = 0;
                int deleted = 0;
                for (var edit : formatter.toFileHeader(entry).toEditList()) {
                    added = Math.addExact(added, edit.getEndB() - edit.getBeginB());
                    deleted = Math.addExact(deleted, edit.getEndA() - edit.getBeginA());
                }
                files.add(new FileChange(path(entry, canonicalNames), added, deleted));
            }
        }
        files.sort(Comparator.comparing(FileChange::file));
        String authorId = authorId(commit);
        GitCommit value = new GitCommit(commit.name(), Instant.ofEpochSecond(commit.getAuthorIdent().getWhenAsInstant().getEpochSecond()),
                authorId, subject(commit.getShortMessage()), files.stream().map(FileChange::file).distinct().toList());
        return new CommitChange(value, files);
    }

    private static AbstractTreeIterator tree(ObjectReader reader, RevCommit commit) throws IOException {
        if (commit == null) return new EmptyTreeIterator();
        CanonicalTreeParser parser = new CanonicalTreeParser();
        parser.reset(reader, commit.getTree());
        return parser;
    }

    private GitIntelligenceReport aggregate(List<CommitChange> changes, boolean truncated) {
        Map<String, MutableFile> files = new HashMap<>();
        Map<FilePair, Integer> pairs = new HashMap<>();
        for (CommitChange change : changes) {
            Set<String> changed = new HashSet<>();
            for (FileChange file : change.files()) {
                files.computeIfAbsent(file.file(), ignored -> new MutableFile()).add(
                        change.commit().authoredAt(), change.commit().authorId(), file.added(), file.deleted());
                changed.add(file.file());
            }
            List<String> ordered = changed.stream().sorted().toList();
            for (int first = 0; first < ordered.size(); first++) {
                for (int second = first + 1; second < ordered.size(); second++) {
                    pairs.merge(new FilePair(ordered.get(first), ordered.get(second)), 1, Integer::sum);
                }
            }
        }
        List<FileHistory> histories = files.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue().freeze(entry.getKey())).toList();
        Map<String, Integer> commitCounts = new HashMap<>();
        histories.forEach(history -> commitCounts.put(history.file(), history.commitCount()));
        List<ChangeCoupling> couplings = pairs.entrySet().stream()
                .filter(entry -> entry.getValue() >= limits.minimumCouplingSupport())
                .map(entry -> coupling(entry.getKey(), entry.getValue(), commitCounts))
                .filter(coupling -> coupling.strength() >= limits.minimumCouplingStrength())
                .sorted(Comparator.comparingInt(ChangeCoupling::cochangeCount).reversed()
                        .thenComparing(ChangeCoupling::firstFile).thenComparing(ChangeCoupling::secondFile))
                .limit(limits.maximumCouplingPairs()).toList();
        return new GitIntelligenceReport(changes.stream().map(CommitChange::commit).sorted(COMMIT_ORDER).toList(),
                histories, couplings, truncated);
    }

    private static ChangeCoupling coupling(FilePair pair, int count, Map<String, Integer> totals) {
        int first = totals.get(pair.first());
        int second = totals.get(pair.second());
        return new ChangeCoupling(pair.first(), pair.second(), count, first, second,
                (double) count / Math.min(first, second));
    }

    private static String path(DiffEntry entry, Map<String, String> canonicalNames) {
        String raw = entry.getChangeType() == DiffEntry.ChangeType.DELETE
                ? entry.getOldPath() : entry.getNewPath();
        String value = canonicalNames.getOrDefault(raw, raw);
        if (entry.getChangeType() == DiffEntry.ChangeType.RENAME) {
            canonicalNames.put(safePath(entry.getOldPath()), value);
        }
        return safePath(value);
    }

    private static String safePath(String value) {
        if (value == null || value.equals(DiffEntry.DEV_NULL) || value.startsWith("/")
                || value.contains("\\") || value.equals("..") || value.startsWith("../")
                || value.endsWith("/..") || value.contains("/../")) {
            throw new GitIntelligenceSafetyException("Git history contains an unsafe path");
        }
        return value;
    }

    private static String authorId(RevCommit commit) throws Exception {
        String normalized = commit.getAuthorIdent().getName().strip().toLowerCase(Locale.ROOT) + "\n"
                + commit.getAuthorIdent().getEmailAddress().strip().toLowerCase(Locale.ROOT);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(normalized.getBytes(StandardCharsets.UTF_8)));
    }

    private static String subject(String value) {
        String normalized = value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').strip();
        return normalized.length() <= 256 ? normalized : normalized.substring(0, 256);
    }

    private record FileChange(String file, int added, int deleted) { }
    private record CommitChange(GitCommit commit, List<FileChange> files) { }
    private record FilePair(String first, String second) { }

    private static final class BoundedOutputStream extends OutputStream {
        private final long maximumBytes;
        private long bytes;
        BoundedOutputStream(long maximumBytes) { this.maximumBytes = maximumBytes; }
        @Override public void write(int value) throws IOException { add(1); }
        @Override public void write(byte[] values, int offset, int length) throws IOException { add(length); }
        private void add(int count) throws IOException {
            try {
                bytes = Math.addExact(bytes, count);
            } catch (ArithmeticException exception) {
                throw new IOException("diff byte count overflow", exception);
            }
            if (bytes > maximumBytes) throw new IOException("commit exceeds diff-byte limit");
        }
    }

    private static final class MutableFile {
        private int commits;
        private int added;
        private int deleted;
        private Instant first;
        private Instant last;
        private final Map<String, MutableAuthor> authors = new LinkedHashMap<>();
        void add(Instant changedAt, String author, int additions, int deletions) {
            commits++;
            added = Math.addExact(added, additions);
            deleted = Math.addExact(deleted, deletions);
            first = first == null || changedAt.isBefore(first) ? changedAt : first;
            last = last == null || changedAt.isAfter(last) ? changedAt : last;
            authors.computeIfAbsent(author, ignored -> new MutableAuthor()).add(additions, deletions);
        }
        FileHistory freeze(String file) {
            List<AuthorContribution> values = authors.entrySet().stream()
                    .map(entry -> entry.getValue().freeze(entry.getKey()))
                    .sorted(Comparator.comparingInt(AuthorContribution::commits).reversed()
                            .thenComparing(AuthorContribution::authorId)).toList();
            return new FileHistory(file, commits, added, deleted, first, last, values);
        }
    }

    private static final class MutableAuthor {
        private int commits;
        private int added;
        private int deleted;
        void add(int additions, int deletions) {
            commits++;
            added = Math.addExact(added, additions);
            deleted = Math.addExact(deleted, deletions);
        }
        AuthorContribution freeze(String author) { return new AuthorContribution(author, commits, added, deleted); }
    }
}
