package com.codeintel.infrastructure.acquisition;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

public final class SafeZipExtractor {
    private final AcquisitionLimits limits;

    public SafeZipExtractor(AcquisitionLimits limits) {
        this.limits = limits;
    }

    public ExtractionResult extract(Path archive, Path destination) {
        Path root = destination.toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            int files = 0;
            long expandedBytes = 0;
            Set<Path> targets = new HashSet<>();
            try (ZipFile zip = ZipFile.builder().setPath(archive).get()) {
                var entries = zip.getEntries();
                while (entries.hasMoreElements()) {
                    ZipArchiveEntry entry = entries.nextElement();
                    Path target = safeTarget(root, entry);
                    if (!targets.add(target)) {
                        throw new AcquisitionSafetyException("archive contains duplicate path: " + entry.getName());
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(target);
                        continue;
                    }
                    if (++files > limits.maximumFiles()) {
                        throw new AcquisitionSafetyException("archive exceeds file-count limit");
                    }
                    Files.createDirectories(target.getParent());
                    try (InputStream input = zip.getInputStream(entry);
                            OutputStream output = Files.newOutputStream(target,
                                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                        long fileBytes = copyBounded(input, output, limits.maximumFileBytes());
                        expandedBytes = Math.addExact(expandedBytes, fileBytes);
                        if (expandedBytes > limits.maximumExpandedBytes()) {
                            throw new AcquisitionSafetyException("archive exceeds expanded-size limit");
                        }
                    }
                }
            }
            return new ExtractionResult(files, expandedBytes);
        } catch (AcquisitionSafetyException exception) {
            throw exception;
        } catch (IOException | ArithmeticException exception) {
            throw new AcquisitionSafetyException("archive extraction failed safely", exception);
        }
    }

    private static Path safeTarget(Path root, ZipArchiveEntry entry) {
        String name = entry.getName();
        if (name == null || name.isBlank() || name.indexOf('\\') >= 0 || entry.isUnixSymlink()) {
            throw new AcquisitionSafetyException("archive contains unsafe link or path");
        }
        Path relative = Path.of(name);
        if (relative.isAbsolute() || relative.normalize().startsWith("..")) {
            throw new AcquisitionSafetyException("archive path escapes extraction root: " + name);
        }
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || Files.isSymbolicLink(target)
                || (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(target))) {
            throw new AcquisitionSafetyException("archive target is unsafe: " + name);
        }
        return target;
    }

    private static long copyBounded(InputStream input, OutputStream output, long maximumBytes)
            throws IOException {
        byte[] buffer = new byte[8192];
        long total = 0;
        int count;
        while ((count = input.read(buffer)) >= 0) {
            total += count;
            if (total > maximumBytes) {
                throw new AcquisitionSafetyException("archive entry exceeds file-size limit");
            }
            output.write(buffer, 0, count);
        }
        return total;
    }

    public record ExtractionResult(int files, long expandedBytes) {
    }
}
