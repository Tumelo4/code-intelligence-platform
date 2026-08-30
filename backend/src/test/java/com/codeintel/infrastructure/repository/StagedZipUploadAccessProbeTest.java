package com.codeintel.infrastructure.repository;

import com.codeintel.domain.repository.ZipUploadConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class StagedZipUploadAccessProbeTest {
    @TempDir
    Path stagingRoot;

    @Test
    void acceptsOnlyStagedFileMatchingSizeAndDigest() throws Exception {
        byte[] content = "safe archive bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        Files.write(stagingRoot.resolve(sha + ".zip"), content);
        StagedZipUploadAccessProbe probe = new StagedZipUploadAccessProbe(stagingRoot);

        assertThat(probe.isAvailable(new ZipUploadConnection("repo.zip", content.length, sha))).isTrue();
        assertThat(probe.isAvailable(new ZipUploadConnection("repo.zip", content.length + 1, sha))).isFalse();
    }
}
