package com.codeintel.infrastructure.acquisition;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.acquisition.GitRemoteAcquisitionSource;
import com.codeintel.domain.acquisition.RepositoryAcquisitionRequest;
import com.codeintel.domain.repository.RepositoryId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class JGitAcquisitionAdapterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void acquiresExactCommitWithoutHooksCheckoutMetadataOrPersistentAccess() throws Exception {
        Path seed = temporaryDirectory.resolve("seed");
        String exactSha;
        try (Git git = Git.init().setDirectory(seed.toFile()).call()) {
            Files.writeString(seed.resolve("README.md"), "safe content");
            git.add().addFilepattern("README.md").call();
            exactSha = git.commit().setMessage("initial").setAuthor("Test", "test@example.com")
                    .setCommitter("Test", "test@example.com").call().getId().name();
        }
        AtomicBoolean networkOpen = new AtomicBoolean();
        AtomicBoolean networkClosed = new AtomicBoolean();
        AcquisitionNetworkController network = source -> {
            networkOpen.set(true);
            return () -> networkClosed.set(true);
        };
        AtomicBoolean credentialsClosed = new AtomicBoolean();
        GitCredentialProvider credentials = source -> new GitCredentialProvider.CredentialLease() {
            public org.eclipse.jgit.transport.CredentialsProvider credentials() {
                return null;
            }

            public void close() {
                credentialsClosed.set(true);
            }
        };
        JGitAcquisitionAdapter adapter = new JGitAcquisitionAdapter(
                temporaryDirectory.resolve("workspace"), new AcquisitionLimits(100, 100_000, 10_000),
                network, credentials, Clock.fixed(Instant.parse("2026-08-30T00:00:00Z"), ZoneOffset.UTC));
        RepositoryAcquisitionRequest request = new RepositoryAcquisitionRequest(
                new RepositoryId(UUID.randomUUID()),
                GitRemoteAcquisitionSource.publicRemote(seed.toUri()), exactSha);

        var acquired = adapter.acquire(request);

        assertThat(acquired.revision()).isEqualTo(
                new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, exactSha));
        assertThat(Files.readString(acquired.immutableOriginal().resolve("README.md")))
                .isEqualTo("safe content");
        assertThat(Files.exists(acquired.immutableOriginal().resolve(".git"))).isFalse();
        assertThat(Files.exists(acquired.workingCopy().resolve(".git"))).isFalse();
        assertThat(Files.isDirectory(acquired.immutableOriginal().resolveSibling("history.git"))).isTrue();
        assertThat(Files.getPosixFilePermissions(
                acquired.immutableOriginal().resolveSibling("history.git").resolve("config")))
                .doesNotContain(java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                        java.nio.file.attribute.PosixFilePermission.GROUP_WRITE,
                        java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE);
        assertThat(Files.getPosixFilePermissions(acquired.immutableOriginal().resolve("README.md")))
                .doesNotContain(java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                        java.nio.file.attribute.PosixFilePermission.GROUP_WRITE,
                        java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE);
        assertThat(Files.getPosixFilePermissions(acquired.workingCopy().resolve("README.md")))
                .contains(java.nio.file.attribute.PosixFilePermission.OWNER_WRITE);
        assertThat(networkOpen).isTrue();
        assertThat(networkClosed).isTrue();
        assertThat(credentialsClosed).isTrue();
    }
}
