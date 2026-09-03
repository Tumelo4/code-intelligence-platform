package com.codeintel.domain.git;

public record AuthorContribution(String authorId, int commits, int linesAdded, int linesDeleted) {
    public AuthorContribution {
        if (authorId == null || authorId.isBlank() || commits < 0 || linesAdded < 0 || linesDeleted < 0) {
            throw new IllegalArgumentException("author contribution fields are invalid");
        }
    }
}
