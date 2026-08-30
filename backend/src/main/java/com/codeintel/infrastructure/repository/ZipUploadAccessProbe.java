package com.codeintel.infrastructure.repository;

import com.codeintel.domain.repository.ZipUploadConnection;

@FunctionalInterface
public interface ZipUploadAccessProbe {
    boolean isAvailable(ZipUploadConnection upload);
}
