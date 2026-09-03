package com.codeintel.domain.git;

import java.time.Instant;
import java.util.List;

public record FileHistory(String file, int commitCount, int linesAdded, int linesDeleted,
        Instant firstChangedAt, Instant lastChangedAt, List<AuthorContribution> authors) {
    public FileHistory {
        if (invalidRelativeFile(file)
                || commitCount < 1 || linesAdded < 0 || linesDeleted < 0
                || firstChangedAt == null || lastChangedAt == null || firstChangedAt.isAfter(lastChangedAt)) {
            throw new IllegalArgumentException("file history fields are invalid");
        }
        authors = List.copyOf(authors);
    }

    private static boolean invalidRelativeFile(String file) {
        return file == null || file.isBlank() || file.startsWith("/") || file.contains("\\")
                || file.equals("..") || file.startsWith("../") || file.endsWith("/..")
                || file.contains("/../");
    }
}
