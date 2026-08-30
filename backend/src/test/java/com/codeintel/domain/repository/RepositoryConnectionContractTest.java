package com.codeintel.domain.repository;

import java.net.URI;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryConnectionContractTest {
    @Test
    void githubAppConnectionContainsNoCredential() {
        GitHubAppConnection connection = new GitHubAppConnection(
                42, new GitHubRepository("openai", "example"));

        assertThat(connection.safeLocator()).isEqualTo("github.com/openai/example");
        assertThat(GitHubAppConnection.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly("installationId", "repository");
    }

    @Test
    void publicGitUrlRejectsEmbeddedCredentialsAndInsecureTransport() {
        assertThatThrownBy(() -> new PublicGitConnection(
                URI.create("https://token@github.com/openai/example.git")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PublicGitConnection(
                URI.create("http://github.com/openai/example.git")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zipContractRequiresSafeNameSizeAndDigest() {
        ZipUploadConnection upload = new ZipUploadConnection(
                "example.zip", 100, "a".repeat(64));

        assertThat(upload.safeLocator()).isEqualTo("example.zip#sha256=" + "a".repeat(64));
        assertThatThrownBy(() -> new ZipUploadConnection("../example.zip", 100, "a".repeat(64)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void localDevelopmentContractRequiresAbsolutePath() {
        assertThatThrownBy(() -> new LocalDevelopmentConnection(Path.of("relative")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
