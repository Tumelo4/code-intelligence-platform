package com.codeintel.infrastructure.repository;

import com.codeintel.domain.repository.ZipUploadConnection;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class StagedZipUploadAccessProbe implements ZipUploadAccessProbe {
    private final Path stagingRoot;

    public StagedZipUploadAccessProbe(Path stagingRoot) {
        this.stagingRoot = stagingRoot.toAbsolutePath().normalize();
    }

    @Override
    public boolean isAvailable(ZipUploadConnection upload) {
        Path candidate = stagingRoot.resolve(upload.contentSha256() + ".zip").normalize();
        if (!candidate.startsWith(stagingRoot) || Files.isSymbolicLink(candidate)
                || !Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        try {
            Path realRoot = stagingRoot.toRealPath();
            Path realFile = candidate.toRealPath();
            return realFile.startsWith(realRoot)
                    && Files.size(realFile) == upload.sizeBytes()
                    && digest(realFile).equals(upload.contentSha256());
        } catch (IOException exception) {
            return false;
        }
    }

    private static String digest(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, count);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
