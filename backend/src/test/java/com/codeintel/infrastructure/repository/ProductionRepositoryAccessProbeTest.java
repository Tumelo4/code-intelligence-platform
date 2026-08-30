package com.codeintel.infrastructure.repository;

import com.codeintel.domain.repository.GitHubRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionRepositoryAccessProbeTest {
    @Test
    void githubAppExchangesSignedJwtAndKeepsInstallationTokenInternal() throws Exception {
        var keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        List<Map<String, String>> observedHeaders = new ArrayList<>();
        OutboundHttpClient client = (method, uri, headers, body) -> {
            observedHeaders.add(headers);
            if (uri.getPath().contains("access_tokens")) {
                return new OutboundHttpClient.Response(201, Map.of(), "{\"token\":\"installation-token-value\"}");
            }
            return new OutboundHttpClient.Response(200, Map.of(), "{}");
        };
        GitHubAppApiAccessProbe probe = new GitHubAppApiAccessProbe(
                123, keyPair.getPrivate(), URI.create("https://api.github.com"), client,
                new ObjectMapper(), Clock.fixed(Instant.parse("2026-08-30T00:00:00Z"), ZoneOffset.UTC));

        assertThat(probe.canRead(42, new GitHubRepository("owner", "repo"))).isTrue();
        assertThat(observedHeaders.get(0).get("Authorization")).startsWith("Bearer eyJ");
        assertThat(observedHeaders.get(1).get("Authorization"))
                .isEqualTo("Bearer installation-token-value");
    }

    @Test
    void publicGitProbeRestrictsEgressAndRequiresSmartHttpAdvertisement() {
        OutboundHttpClient client = (method, uri, headers, body) ->
                new OutboundHttpClient.Response(200,
                        Map.of("Content-Type", List.of("application/x-git-upload-pack-advertisement")), "");
        AllowlistedPublicGitAccessProbe probe = new AllowlistedPublicGitAccessProbe(
                Set.of("github.com"), client);

        assertThat(probe.canRead(URI.create("https://github.com/owner/repo.git"))).isTrue();
        assertThat(probe.canRead(URI.create("https://example.com/owner/repo.git"))).isFalse();
    }
}
