package com.codeintel.infrastructure.acquisition;

import com.codeintel.application.ports.outbound.LocalAcquisitionPort;
import com.codeintel.domain.acquisition.AcquiredRepository;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.acquisition.LocalDirectoryAcquisitionSource;
import com.codeintel.domain.acquisition.RepositoryAcquisitionRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Comparator;
import java.util.HexFormat;

public final class LocalSnapshotAcquisitionAdapter implements LocalAcquisitionPort {
    private final Path workspaceRoot;
    private final AcquisitionLimits limits;
    private final Clock clock;

    public LocalSnapshotAcquisitionAdapter(Path workspaceRoot, AcquisitionLimits limits, Clock clock) {
        this.workspaceRoot = workspaceRoot;
        this.limits = limits;
        this.clock = clock;
    }

    @Override
    public AcquiredRepository acquire(RepositoryAcquisitionRequest request) {
        if (!(request.source() instanceof LocalDirectoryAcquisitionSource source)) {
            throw new AcquisitionSafetyException("local adapter requires a directory source");
        }
        Path root = null;
        try {
            Path sourceRoot = source.directory().toRealPath();
            if (!Files.isDirectory(sourceRoot, LinkOption.NOFOLLOW_LINKS)) {
                throw new AcquisitionSafetyException("local source is not a directory");
            }
            root = AcquisitionWorkspaceSupport.createRoot(workspaceRoot, request.repositoryId().value());
            Path original = root.resolve("original");
            Path working = root.resolve("working");
            Files.createDirectory(original);
            String digest = snapshot(sourceRoot, original);
            AcquisitionWorkspaceSupport.copyTree(original, working);
            AcquisitionWorkspaceSupport.makeReadOnly(original);
            return new AcquiredRepository(request.repositoryId(),
                    new AcquisitionRevision(AcquisitionRevision.Kind.LOCAL_SNAPSHOT_SHA256, digest),
                    request.requestedRevision(), original, working, 0, clock.instant());
        } catch (AcquisitionSafetyException exception) {
            if (root != null) AcquisitionWorkspaceSupport.deleteTreeQuietly(root);
            throw exception;
        } catch (IOException exception) {
            if (root != null) AcquisitionWorkspaceSupport.deleteTreeQuietly(root);
            throw new AcquisitionSafetyException("local snapshot failed safely", exception);
        }
    }

    private String snapshot(Path sourceRoot, Path destination) throws IOException {
        MessageDigest digest = sha256();
        int files = 0;
        long totalBytes = 0;
        try (var paths = Files.walk(sourceRoot)) {
            for (Path source : paths.sorted(Comparator.comparing(Path::toString)).toList()) {
                Path relative = sourceRoot.relativize(source);
                if (relative.getNameCount() > 0 && relative.getName(0).toString().equals(".git")) {
                    continue;
                }
                if (Files.isSymbolicLink(source)) {
                    throw new AcquisitionSafetyException("local source contains a symbolic link");
                }
                Path target = destination.resolve(relative).normalize();
                if (!target.startsWith(destination)) {
                    throw new AcquisitionSafetyException("local source path escapes snapshot");
                }
                if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createDirectories(target);
                    continue;
                }
                if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)
                        || ++files > limits.maximumFiles()) {
                    throw new AcquisitionSafetyException("local source type or file count is unsafe");
                }
                long size = Files.size(source);
                if (size > limits.maximumFileBytes()
                        || (totalBytes = Math.addExact(totalBytes, size)) > limits.maximumExpandedBytes()) {
                    throw new AcquisitionSafetyException("local source exceeds size limits");
                }
                digest.update(relative.toString().replace('\\', '/').getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                Files.createDirectories(target.getParent());
                try (InputStream input = Files.newInputStream(source);
                        OutputStream output = Files.newOutputStream(target,
                                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) >= 0) {
                        digest.update(buffer, 0, count);
                        output.write(buffer, 0, count);
                    }
                }
                digest.update((byte) 0xff);
            }
        } catch (ArithmeticException exception) {
            throw new AcquisitionSafetyException("local source exceeds size limits", exception);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
