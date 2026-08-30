package com.codeintel.domain.repository;

public record ZipUploadConnection(String originalFilename, long sizeBytes, String contentSha256)
        implements RepositoryConnection {
    public ZipUploadConnection {
        if (originalFilename == null || originalFilename.isBlank()
                || originalFilename.contains("/") || originalFilename.contains("\\")
                || !originalFilename.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) {
            throw new IllegalArgumentException("ZIP filename is invalid");
        }
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("ZIP size must be positive");
        }
        if (contentSha256 == null || !contentSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("ZIP content SHA-256 is invalid");
        }
    }

    @Override
    public RepositorySourceType sourceType() {
        return RepositorySourceType.ZIP_UPLOAD;
    }

    @Override
    public String safeLocator() {
        return originalFilename + "#sha256=" + contentSha256;
    }
}
