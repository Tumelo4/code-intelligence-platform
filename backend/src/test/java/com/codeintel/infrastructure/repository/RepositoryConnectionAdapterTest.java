package com.codeintel.infrastructure.repository;

import com.codeintel.domain.repository.GitHubAppConnection;
import com.codeintel.domain.repository.GitHubRepository;
import com.codeintel.domain.repository.LocalDevelopmentConnection;
import com.codeintel.domain.repository.PublicGitConnection;
import com.codeintel.domain.repository.RepositorySourceType;
import com.codeintel.domain.repository.ZipUploadConnection;
import java.io.IOException;
import java.net.URI;
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

class RepositoryConnectionAdapterTest {
    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final Instant NOW = Instant.parse("2026-08-29T12:00:00Z");

    @TempDir
    Path temporaryDirectory;

    @Test
    void validatesGithubInstallationWithoutReturningCredentials() {
        RepositoryConnectionAdapter adapter = adapter(
                (installation, repository) -> installation == 42 && repository.fullName().equals("owner/repo"),
                uri -> false, zip -> false);

        var result = adapter.validate(new GitHubAppConnection(
                42, new GitHubRepository("owner", "repo")));

        assertThat(result.repositoryId().value()).isEqualTo(ID);
        assertThat(result.sourceType()).isEqualTo(RepositorySourceType.GITHUB_APP);
        assertThat(result.safeLocator()).isEqualTo("github.com/owner/repo");
        assertThat(result.validatedAt()).isEqualTo(NOW);
    }

    @Test
    void failsClosedWhenPublicAccessCannotBeValidated() {
        RepositoryConnectionAdapter adapter = adapter(
                (installation, repository) -> false, uri -> false, zip -> false);

        assertThatThrownBy(() -> adapter.validate(new PublicGitConnection(
                URI.create("https://github.com/owner/repo.git"))))
                .isInstanceOf(RepositoryAccessDeniedException.class)
                .hasMessageContaining("PUBLIC_GIT_URL");
    }

    @Test
    void validatesAvailableZipByManifest() {
        RepositoryConnectionAdapter adapter = adapter(
                (installation, repository) -> false, uri -> false,
                zip -> zip.contentSha256().equals("b".repeat(64)));

        var result = adapter.validate(new ZipUploadConnection("repo.zip", 50, "b".repeat(64)));

        assertThat(result.sourceType()).isEqualTo(RepositorySourceType.ZIP_UPLOAD);
    }

    @Test
    void validatesRealLocalGitDirectory() throws IOException {
        Path repository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        Files.createDirectory(repository.resolve(".git"));
        RepositoryConnectionAdapter adapter = adapter(
                (installation, githubRepository) -> false, uri -> false, zip -> false);

        var result = adapter.validate(new LocalDevelopmentConnection(repository));

        assertThat(result.sourceType()).isEqualTo(RepositorySourceType.LOCAL_DEVELOPMENT_PATH);
        assertThat(result.safeLocator()).isEqualTo(repository.toRealPath().toString());
    }

    @Test
    void blocksLocalRepositoryWhenDevelopmentModeIsDisabled() throws IOException {
        Path repository = Files.createDirectory(temporaryDirectory.resolve("repository"));
        Files.createDirectory(repository.resolve(".git"));
        RepositoryConnectionAdapter adapter = new RepositoryConnectionAdapter(
                (installation, githubRepository) -> false, uri -> false, zip -> false,
                () -> ID, Clock.fixed(NOW, ZoneOffset.UTC), false);

        assertThatThrownBy(() -> adapter.validate(new LocalDevelopmentConnection(repository)))
                .isInstanceOf(RepositoryAccessDeniedException.class)
                .hasMessageContaining("disabled");
    }

    private RepositoryConnectionAdapter adapter(
            GitHubAppAccessProbe github,
            PublicGitAccessProbe publicGit,
            ZipUploadAccessProbe zip) {
        return new RepositoryConnectionAdapter(
                github, publicGit, zip, () -> ID, Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
