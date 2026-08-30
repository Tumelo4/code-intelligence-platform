package com.codeintel.infrastructure.acquisition;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafeZipExtractorTest {
    @TempDir
    Path temporaryDirectory;

    private final SafeZipExtractor extractor = new SafeZipExtractor(
            new AcquisitionLimits(10, 100, 50));

    @Test
    void extractsBoundedRegularFiles() throws Exception {
        Path archive = zip("src/Main.java", "class Main {}".getBytes(StandardCharsets.UTF_8), false);
        Path destination = temporaryDirectory.resolve("original");

        var result = extractor.extract(archive, destination);

        assertThat(result.files()).isEqualTo(1);
        assertThat(Files.readString(destination.resolve("src/Main.java"))).isEqualTo("class Main {}");
    }

    @Test
    void rejectsTraversalAndSymlinkEntries() throws Exception {
        Path traversal = zip("../escape.txt", new byte[]{1}, false);
        assertThatThrownBy(() -> extractor.extract(traversal, temporaryDirectory.resolve("traversal")))
                .isInstanceOf(AcquisitionSafetyException.class).hasMessageContaining("escapes");

        Path symlink = zip("link", "target".getBytes(StandardCharsets.UTF_8), true);
        assertThatThrownBy(() -> extractor.extract(symlink, temporaryDirectory.resolve("symlink")))
                .isInstanceOf(AcquisitionSafetyException.class).hasMessageContaining("unsafe");
    }

    @Test
    void rejectsExcessiveEntryExpansion() throws Exception {
        Path archive = zip("large.txt", new byte[51], false);

        assertThatThrownBy(() -> extractor.extract(archive, temporaryDirectory.resolve("large")))
                .isInstanceOf(AcquisitionSafetyException.class).hasMessageContaining("file-size");
    }

    private Path zip(String name, byte[] content, boolean symlink) throws Exception {
        Path archive = temporaryDirectory.resolve(UUIDHolder.next() + ".zip");
        try (OutputStream file = Files.newOutputStream(archive);
                ZipArchiveOutputStream zip = new ZipArchiveOutputStream(file)) {
            ZipArchiveEntry entry = new ZipArchiveEntry(name);
            if (symlink) {
                entry.setUnixMode(UnixStat.LINK_FLAG | 0777);
            }
            zip.putArchiveEntry(entry);
            zip.write(content);
            zip.closeArchiveEntry();
        }
        return archive;
    }

    private static final class UUIDHolder {
        private static String next() {
            return java.util.UUID.randomUUID().toString();
        }
    }
}
