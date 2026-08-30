package com.codeintel.domain.acquisition;

import java.nio.file.Path;
import java.util.Objects;

public record ZipArchiveAcquisitionSource(Path archive, String contentSha256)
        implements AcquisitionSource {
    public ZipArchiveAcquisitionSource {
        Objects.requireNonNull(archive, "archive must not be null");
        if (!archive.isAbsolute()) {
            throw new IllegalArgumentException("archive path must be absolute");
        }
        archive = archive.normalize();
        if (contentSha256 == null || !contentSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("archive SHA-256 is invalid");
        }
    }
}
