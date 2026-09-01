package com.codeintel.domain.analysis;

import java.util.List;

public record JavaFileMetrics(String file, int loc, int classCount, int methodCount,
        int dependencyCount, List<JavaClassMetrics> classes) {
    public JavaFileMetrics {
        if (file == null || file.isBlank() || file.startsWith("/") || loc < 1 || classCount < 0
                || methodCount < 0 || dependencyCount < 0) {
            throw new IllegalArgumentException("invalid file metrics");
        }
        classes = List.copyOf(classes);
    }
}
