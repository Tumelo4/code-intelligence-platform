package com.codeintel.infrastructure.acquisition;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.acquisition.RepositoryAcquisitionRequest;
import com.codeintel.domain.acquisition.ZipArchiveAcquisitionSource;
import com.codeintel.domain.repository.RepositoryId;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class ZipAcquisitionAdapterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void verifiesArchiveIdentityAndCreatesSeparatedCopies() throws Exception {
        Path archive = temporaryDirectory.resolve("staged.zip");
        try (OutputStream output = Files.newOutputStream(archive);
                ZipArchiveOutputStream zip = new ZipArchiveOutputStream(output)) {
            zip.putArchiveEntry(new ZipArchiveEntry("README.md"));
            zip.write("archive content".getBytes(StandardCharsets.UTF_8));
            zip.closeArchiveEntry();
        }
        String sha = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(archive)));
        RepositoryAcquisitionRequest request = new RepositoryAcquisitionRequest(
                new RepositoryId(UUID.randomUUID()), new ZipArchiveAcquisitionSource(archive, sha),
                "uploaded");
        ZipAcquisitionAdapter adapter = new ZipAcquisitionAdapter(
                temporaryDirectory.resolve("workspace"),
                new SafeZipExtractor(new AcquisitionLimits(10, 10_000, 1_000)),
                Clock.fixed(Instant.parse("2026-08-30T00:00:00Z"), ZoneOffset.UTC));

        var acquired = adapter.acquire(request);

        assertThat(acquired.revision()).isEqualTo(
                new AcquisitionRevision(AcquisitionRevision.Kind.ARCHIVE_SHA256, sha));
        assertThat(Files.readString(acquired.immutableOriginal().resolve("README.md")))
                .isEqualTo("archive content");
        assertThat(Files.readString(acquired.workingCopy().resolve("README.md")))
                .isEqualTo("archive content");
    }
}
