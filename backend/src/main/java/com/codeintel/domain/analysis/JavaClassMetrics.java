package com.codeintel.domain.analysis;

import java.util.List;

public record JavaClassMetrics(String name, SourceRange range, int loc, int fieldCount,
        List<JavaMethodMetrics> methods) {
    public JavaClassMetrics {
        if (name == null || name.isBlank() || range == null || loc < 1 || fieldCount < 0) {
            throw new IllegalArgumentException("invalid class metrics");
        }
        methods = List.copyOf(methods);
    }
}
