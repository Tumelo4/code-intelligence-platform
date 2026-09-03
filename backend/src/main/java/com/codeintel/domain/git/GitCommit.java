package com.codeintel.domain.git;

import java.time.Instant;
import java.util.List;

public record GitCommit(String sha, Instant authoredAt, String authorId, String subject,
        List<String> changedFiles) {
    public GitCommit {
        if (sha == null || !sha.matches("[0-9a-f]{40}") || authoredAt == null
                || authorId == null || authorId.isBlank() || subject == null) {
            throw new IllegalArgumentException("git commit fields are invalid");
        }
        changedFiles = List.copyOf(changedFiles);
        if (changedFiles.stream().anyMatch(GitCommit::invalidRelativeFile)) {
            throw new IllegalArgumentException("changed files must be repository relative");
        }
    }

    private static boolean invalidRelativeFile(String file) {
        return file == null || file.isBlank() || file.startsWith("/") || file.contains("\\")
                || file.equals("..") || file.startsWith("../") || file.endsWith("/..")
                || file.contains("/../");
    }
}
