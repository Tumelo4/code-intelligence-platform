package com.codeintel.domain.acquisition;

import java.nio.file.Path;
import java.util.Objects;

public record LocalDirectoryAcquisitionSource(Path directory) implements AcquisitionSource {
    public LocalDirectoryAcquisitionSource {
        Objects.requireNonNull(directory, "directory must not be null");
        if (!directory.isAbsolute()) {
            throw new IllegalArgumentException("local source must be absolute");
        }
        directory = directory.normalize();
    }
}
