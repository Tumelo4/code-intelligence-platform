package com.codeintel.domain.git;

public record AuthorContribution(String authorId, int commits, int linesAdded, int linesDeleted) {
    public AuthorContribution {
        if (authorId == null || !authorId.matches("[0-9a-f]{64}")
                || commits < 1 || linesAdded < 0 || linesDeleted < 0) {
            throw new IllegalArgumentException("author contribution fields are invalid");
        }
    }
}
