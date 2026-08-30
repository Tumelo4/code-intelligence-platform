package com.codeintel.infrastructure.acquisition;

public record AcquisitionLimits(int maximumFiles, long maximumExpandedBytes, long maximumFileBytes) {
    public AcquisitionLimits {
        if (maximumFiles <= 0 || maximumExpandedBytes <= 0 || maximumFileBytes <= 0
                || maximumFileBytes > maximumExpandedBytes) {
            throw new IllegalArgumentException("acquisition limits are invalid");
        }
    }
}
