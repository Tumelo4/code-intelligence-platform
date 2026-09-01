package com.codeintel.domain.analysis;

public record JavaMethodMetrics(String name, SourceRange range, int loc, int cyclomaticComplexity,
        int nestingDepth, int parameterCount, int branchCount, int loopCount) {
    public JavaMethodMetrics {
        if (name == null || name.isBlank() || range == null || loc < 1 || cyclomaticComplexity < 1
                || nestingDepth < 0 || parameterCount < 0 || branchCount < 0 || loopCount < 0) {
            throw new IllegalArgumentException("invalid method metrics");
        }
    }
}
