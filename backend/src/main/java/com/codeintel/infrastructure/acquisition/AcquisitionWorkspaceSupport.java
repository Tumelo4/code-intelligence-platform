package com.codeintel.infrastructure.acquisition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

final class AcquisitionWorkspaceSupport {
    private AcquisitionWorkspaceSupport() {
    }

    static Path createRoot(Path workspaceRoot, java.util.UUID repositoryId) throws IOException {
        Path normalizedRoot = workspaceRoot.toAbsolutePath().normalize();
        Files.createDirectories(normalizedRoot);
        Path acquisitionRoot = normalizedRoot.resolve(repositoryId + "-" + java.util.UUID.randomUUID())
                .normalize();
        if (!acquisitionRoot.startsWith(normalizedRoot)) {
            throw new AcquisitionSafetyException("acquisition workspace escaped configured root");
        }
        Files.createDirectory(acquisitionRoot);
        return acquisitionRoot;
    }

    static void copyTree(Path source, Path destination) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path target = destination.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(path, target);
                }
            }
        }
    }

    static void makeReadOnly(Path root) throws IOException {
        Set<PosixFilePermission> files = EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ);
        Set<PosixFilePermission> directories = EnumSet.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_EXECUTE);
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                try {
                    Files.setPosixFilePermissions(path, Files.isDirectory(path) ? directories : files);
                } catch (UnsupportedOperationException exception) {
                    if (!path.toFile().setWritable(false, false)) {
                        throw new IOException("could not make original read-only", exception);
                    }
                }
            }
        }
    }

    static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    static void deleteTreeQuietly(Path root) {
        try {
            deleteTree(root);
        } catch (IOException ignored) {
            // Workspace maintenance can retry cleanup after the original failure is reported.
        }
    }
}
