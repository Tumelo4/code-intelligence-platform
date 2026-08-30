package com.codeintel.domain.acquisition;

public sealed interface AcquisitionSource permits GitRemoteAcquisitionSource, ZipArchiveAcquisitionSource,
        LocalDirectoryAcquisitionSource {
}
