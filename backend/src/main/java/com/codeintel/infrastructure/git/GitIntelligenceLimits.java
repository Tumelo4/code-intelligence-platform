package com.codeintel.infrastructure.git;

public record GitIntelligenceLimits(int maximumCommits, int maximumFilesPerCommit,
        long maximumDiffBytes, int minimumCouplingSupport, double minimumCouplingStrength,
        int maximumCouplingPairs) {
    public GitIntelligenceLimits {
        if (maximumCommits < 1 || maximumFilesPerCommit < 1 || maximumDiffBytes < 1
                || minimumCouplingSupport < 1 || !Double.isFinite(minimumCouplingStrength)
                || minimumCouplingStrength < 0 || minimumCouplingStrength > 1
                || maximumCouplingPairs < 1) {
            throw new IllegalArgumentException("Git intelligence limits must be positive and bounded");
        }
    }
}
