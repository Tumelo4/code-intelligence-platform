package com.codeintel.infrastructure.acquisition;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.acquisition.LocalDirectoryAcquisitionSource;
import com.codeintel.domain.acquisition.RepositoryAcquisitionRequest;
import com.codeintel.domain.repository.RepositoryId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalSnapshotAcquisitionAdapterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void snapshotsDeterministicallyWithoutGitMetadata() throws Exception {
        Path source = Files.createDirectory(temporaryDirectory.resolve("source"));
        Files.writeString(source.resolve("README.md"), "local content");
        Files.createDirectory(source.resolve(".git"));
        Files.writeString(source.resolve(".git/config"), "credential = unavailable");
        LocalSnapshotAcquisitionAdapter adapter = adapter();
        RepositoryAcquisitionRequest request = new RepositoryAcquisitionRequest(
                new RepositoryId(UUID.randomUUID()), new LocalDirectoryAcquisitionSource(source),
                "snapshot");

        var first = adapter.acquire(request);
        var second = adapter.acquire(request);

        assertThat(first.revision().kind()).isEqualTo(AcquisitionRevision.Kind.LOCAL_SNAPSHOT_SHA256);
        assertThat(first.revision()).isEqualTo(second.revision());
        assertThat(Files.exists(first.immutableOriginal().resolve(".git"))).isFalse();
        assertThat(Files.readString(first.workingCopy().resolve("README.md"))).isEqualTo("local content");
    }

    @Test
    void rejectsLocalSymlink() throws Exception {
        Path source = Files.createDirectory(temporaryDirectory.resolve("unsafe"));
        Path outside = Files.writeString(temporaryDirectory.resolve("outside"), "data");
        Files.createSymbolicLink(source.resolve("link"), outside);
        RepositoryAcquisitionRequest request = new RepositoryAcquisitionRequest(
                new RepositoryId(UUID.randomUUID()), new LocalDirectoryAcquisitionSource(source),
                "snapshot");

        assertThatThrownBy(() -> adapter().acquire(request))
                .isInstanceOf(AcquisitionSafetyException.class)
                .hasMessageContaining("symbolic link");
    }

    private LocalSnapshotAcquisitionAdapter adapter() {
        return new LocalSnapshotAcquisitionAdapter(temporaryDirectory.resolve("workspace"),
                new AcquisitionLimits(100, 100_000, 10_000),
                Clock.fixed(Instant.parse("2026-08-30T00:00:00Z"), ZoneOffset.UTC));
    }
}
