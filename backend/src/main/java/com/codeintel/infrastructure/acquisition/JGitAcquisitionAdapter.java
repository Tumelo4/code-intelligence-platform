package com.codeintel.infrastructure.acquisition;

import com.codeintel.application.ports.outbound.GitAcquisitionPort;
import com.codeintel.domain.acquisition.AcquiredRepository;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.acquisition.GitRemoteAcquisitionSource;
import com.codeintel.domain.acquisition.RepositoryAcquisitionRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

public final class JGitAcquisitionAdapter implements GitAcquisitionPort {
    private final Path workspaceRoot;
    private final AcquisitionLimits limits;
    private final AcquisitionNetworkController networkController;
    private final GitCredentialProvider credentialProvider;
    private final Clock clock;

    public JGitAcquisitionAdapter(Path workspaceRoot, AcquisitionLimits limits,
            AcquisitionNetworkController networkController,
            GitCredentialProvider credentialProvider,
            Clock clock) {
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
        this.limits = limits;
        this.networkController = networkController;
        this.credentialProvider = credentialProvider;
        this.clock = clock;
    }

    @Override
    public AcquiredRepository acquire(RepositoryAcquisitionRequest request) {
        if (!(request.source() instanceof GitRemoteAcquisitionSource source)) {
            throw new AcquisitionSafetyException("JGit adapter requires a Git remote source");
        }
        Path acquisitionRoot = workspaceRoot.resolve(request.repositoryId().value() + "-" + UUID.randomUUID())
                .normalize();
        if (!acquisitionRoot.startsWith(workspaceRoot)) {
            throw new AcquisitionSafetyException("acquisition workspace escaped configured root");
        }
        Path metadata = acquisitionRoot.resolve("acquisition.git");
        Path original = acquisitionRoot.resolve("original");
        Path working = acquisitionRoot.resolve("working");
        try {
            Files.createDirectories(acquisitionRoot);
            cloneBare(source, metadata);
            MaterializedTree tree = materializeExactRevision(metadata, request.requestedRevision(), original);
            deleteTree(metadata);
            copyTree(original, working);
            makeReadOnly(original);
            return new AcquiredRepository(request.repositoryId(),
                    new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, tree.commitSha()),
                    request.requestedRevision(), original, working, tree.skippedSubmodules(), clock.instant());
        } catch (AcquisitionSafetyException exception) {
            deleteTreeQuietly(acquisitionRoot);
            throw exception;
        } catch (Exception exception) {
            deleteTreeQuietly(acquisitionRoot);
            throw new AcquisitionSafetyException("Git acquisition failed safely", exception);
        }
    }

    private void cloneBare(GitRemoteAcquisitionSource source, Path metadata) throws Exception {
        try (AcquisitionNetworkController.NetworkLease ignored = networkController.openFor(source);
                GitCredentialProvider.CredentialLease credentials = credentialProvider.openFor(source)) {
            CloneCommand clone = Git.cloneRepository()
                    .setURI(source.remoteUri().toASCIIString())
                    .setDirectory(metadata.toFile())
                    .setBare(true)
                    .setCloneSubmodules(false);
            if (credentials.credentials() != null) {
                clone.setCredentialsProvider(credentials.credentials());
            }
            try (Git ignoredGit = clone.call()) {
                // Network and credentials exist only for this bare object transfer.
            }
        }
    }

    private MaterializedTree materializeExactRevision(
            Path metadata, String requestedRevision, Path destination) throws Exception {
        Files.createDirectories(destination);
        try (Git git = Git.open(metadata.toFile())) {
            Repository repository = git.getRepository();
            ObjectId resolved = resolve(repository, requestedRevision);
            try (RevWalk walk = new RevWalk(repository)) {
                RevCommit commit = walk.parseCommit(resolved);
                int files = 0;
                int submodules = 0;
                long totalBytes = 0;
                Set<Path> targets = new java.util.HashSet<>();
                try (TreeWalk tree = new TreeWalk(repository)) {
                    tree.addTree(commit.getTree());
                    tree.setRecursive(true);
                    while (tree.next()) {
                        FileMode mode = tree.getFileMode(0);
                        if (FileMode.GITLINK.equals(mode)) {
                            submodules++;
                            continue;
                        }
                        if (FileMode.SYMLINK.equals(mode)) {
                            throw new AcquisitionSafetyException("Git tree contains a symbolic link");
                        }
                        if (mode.getObjectType() != Constants.OBJ_BLOB || ++files > limits.maximumFiles()) {
                            throw new AcquisitionSafetyException("Git tree type or file count is unsafe");
                        }
                        Path target = safeTarget(destination, tree.getPathString());
                        if (!targets.add(target)) {
                            throw new AcquisitionSafetyException("Git tree contains duplicate target");
                        }
                        Files.createDirectories(target.getParent());
                        ObjectLoader loader = repository.open(tree.getObjectId(0), Constants.OBJ_BLOB);
                        if (loader.getSize() > limits.maximumFileBytes()) {
                            throw new AcquisitionSafetyException("Git blob exceeds file-size limit");
                        }
                        totalBytes = Math.addExact(totalBytes, loader.getSize());
                        if (totalBytes > limits.maximumExpandedBytes()) {
                            throw new AcquisitionSafetyException("Git tree exceeds expanded-size limit");
                        }
                        try (OutputStream output = Files.newOutputStream(target,
                                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                            loader.copyTo(output);
                        }
                    }
                }
                return new MaterializedTree(commit.getId().name(), submodules);
            }
        }
    }

    private static ObjectId resolve(Repository repository, String requestedRevision) throws IOException {
        for (String candidate : new String[]{requestedRevision, "refs/heads/" + requestedRevision,
                "refs/tags/" + requestedRevision, "refs/remotes/origin/" + requestedRevision}) {
            ObjectId resolved = repository.resolve(candidate + "^{commit}");
            if (resolved != null) {
                return resolved;
            }
        }
        throw new AcquisitionSafetyException("requested Git revision was not found");
    }

    private static Path safeTarget(Path root, String repositoryPath) {
        if (repositoryPath.isBlank() || repositoryPath.indexOf('\\') >= 0) {
            throw new AcquisitionSafetyException("Git tree path is unsafe");
        }
        Path relative = Path.of(repositoryPath);
        Path target = root.resolve(relative).normalize();
        if (relative.isAbsolute() || relative.normalize().startsWith("..") || !target.startsWith(root)) {
            throw new AcquisitionSafetyException("Git tree path escapes destination");
        }
        return target;
    }

    private static void copyTree(Path source, Path destination) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path target = destination.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(path, target);
                }
            }
        }
    }

    private static void makeReadOnly(Path root) throws IOException {
        Set<PosixFilePermission> filePermissions = EnumSet.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ,
                PosixFilePermission.OTHERS_READ);
        Set<PosixFilePermission> directoryPermissions = EnumSet.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE);
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                try {
                    Files.setPosixFilePermissions(path,
                            Files.isDirectory(path) ? directoryPermissions : filePermissions);
                } catch (UnsupportedOperationException exception) {
                    if (!path.toFile().setWritable(false, false)) {
                        throw new IOException("could not make original read-only", exception);
                    }
                }
            }
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void deleteTreeQuietly(Path root) {
        try {
            deleteTree(root);
        } catch (IOException ignored) {
            // Preserve the acquisition failure; cleanup can be retried by workspace maintenance.
        }
    }

    private record MaterializedTree(String commitSha, int skippedSubmodules) {
    }
}
