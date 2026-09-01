package com.codeintel.infrastructure.analysis;

public record AnalysisThresholds(int longMethodLoc, int largeClassLoc, int largeClassMethods,
        int highComplexity, int deepNesting, int manyParameters, int godClassLoc,
        int godClassMethods, int godClassFields, int duplicateStatements, int maximumFiles,
        long maximumFileBytes) {
    public static AnalysisThresholds defaults() {
        return new AnalysisThresholds(60, 500, 30, 10, 4, 5, 800, 40, 15, 6, 100000, 2097152);
    }
}
