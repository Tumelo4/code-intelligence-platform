package com.codeintel.infrastructure.inventory;

public record InventoryLimits(int maximumFiles, int maximumModules, long maximumPomBytes) {
    public InventoryLimits {
        if (maximumFiles < 1 || maximumModules < 1 || maximumPomBytes < 1) {
            throw new IllegalArgumentException("inventory limits must be positive");
        }
    }
}
