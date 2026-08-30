package com.codeintel.infrastructure.acquisition;

import com.codeintel.application.ports.outbound.ArchiveAcquisitionPort;
import com.codeintel.domain.acquisition.AcquiredRepository;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.acquisition.RepositoryAcquisitionRequest;
import com.codeintel.domain.acquisition.ZipArchiveAcquisitionSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;

public final class ZipAcquisitionAdapter implements ArchiveAcquisitionPort {
    private final Path workspaceRoot;
    private final SafeZipExtractor extractor;
    private final Clock clock;

    public ZipAcquisitionAdapter(Path workspaceRoot, SafeZipExtractor extractor, Clock clock) {
        this.workspaceRoot = workspaceRoot;
        this.extractor = extractor;
        this.clock = clock;
    }

    @Override
    public AcquiredRepository acquire(RepositoryAcquisitionRequest request) {
        if (!(request.source() instanceof ZipArchiveAcquisitionSource source)) {
            throw new AcquisitionSafetyException("ZIP adapter requires an archive source");
        }
        Path root = null;
        try {
            Path archive = source.archive().toRealPath();
            if (Files.isSymbolicLink(source.archive())
                    || !Files.isRegularFile(archive, LinkOption.NOFOLLOW_LINKS)
                    || !sha256(archive).equals(source.contentSha256())) {
                throw new AcquisitionSafetyException("staged archive identity is invalid");
            }
            root = AcquisitionWorkspaceSupport.createRoot(workspaceRoot, request.repositoryId().value());
            Path original = root.resolve("original");
            Path working = root.resolve("working");
            extractor.extract(archive, original);
            AcquisitionWorkspaceSupport.copyTree(original, working);
            AcquisitionWorkspaceSupport.makeReadOnly(original);
            return new AcquiredRepository(request.repositoryId(),
                    new AcquisitionRevision(AcquisitionRevision.Kind.ARCHIVE_SHA256,
                            source.contentSha256()), request.requestedRevision(), original, working, 0,
                    clock.instant());
        } catch (AcquisitionSafetyException exception) {
            if (root != null) AcquisitionWorkspaceSupport.deleteTreeQuietly(root);
            throw exception;
        } catch (IOException exception) {
            if (root != null) AcquisitionWorkspaceSupport.deleteTreeQuietly(root);
            throw new AcquisitionSafetyException("ZIP acquisition failed safely", exception);
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) >= 0) digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
