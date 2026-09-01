package com.codeintel.domain.analysis;

public record SourceRange(int startLine, int endLine) {
    public SourceRange {
        if (startLine < 1 || endLine < startLine) throw new IllegalArgumentException("invalid source range");
    }

    public int lineCount() {
        return endLine - startLine + 1;
    }
}
