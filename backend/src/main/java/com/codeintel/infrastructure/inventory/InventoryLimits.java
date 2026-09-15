package com.codeintel.infrastructure.inventory;

public record InventoryLimits(int maximumFiles, int maximumModules, long maximumPomBytes,
        long maximumEvidenceFileBytes) {
    public InventoryLimits {
        if (maximumFiles < 1 || maximumModules < 1 || maximumPomBytes < 1
                || maximumEvidenceFileBytes < 1) {
            throw new IllegalArgumentException("inventory limits must be positive");
        }
    }

    public InventoryLimits(int maximumFiles, int maximumModules, long maximumPomBytes) {
        this(maximumFiles, maximumModules, maximumPomBytes, 2_097_152);
    }
}
