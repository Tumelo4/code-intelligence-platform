package com.codeintel.infrastructure.repository;

import com.codeintel.domain.repository.GitHubRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Clock;
import java.util.Base64;
import java.util.Map;

public final class GitHubAppApiAccessProbe implements GitHubAppAccessProbe {
    private static final String ACCEPT = "application/vnd.github+json";
    private final long appId;
    private final PrivateKey privateKey;
    private final URI apiBase;
    private final OutboundHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public GitHubAppApiAccessProbe(long appId, PrivateKey privateKey, URI apiBase,
            OutboundHttpClient httpClient, ObjectMapper objectMapper, Clock clock) {
        this.appId = appId;
        this.privateKey = privateKey;
        this.apiBase = apiBase;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public boolean canRead(long installationId, GitHubRepository repository) {
        String token = installationToken(installationId, repository);
        OutboundHttpClient.Response response = httpClient.exchange("GET",
                apiBase.resolve("/repos/" + repository.owner() + "/" + repository.name()),
                Map.of("Accept", ACCEPT, "Authorization", "Bearer " + token,
                        "X-GitHub-Api-Version", "2022-11-28"), "");
        return response.statusCode() == 200;
    }

    private String installationToken(long installationId, GitHubRepository repository) {
        OutboundHttpClient.Response response = httpClient.exchange("POST",
                apiBase.resolve("/app/installations/" + installationId + "/access_tokens"),
                Map.of("Accept", ACCEPT, "Authorization", "Bearer " + signedAppJwt(),
                        "X-GitHub-Api-Version", "2022-11-28"),
                "{\"repositories\":[\"" + repository.name()
                        + "\"],\"permissions\":{\"contents\":\"read\"}}");
        if (response.statusCode() != 201) {
            throw new RepositoryAccessDeniedException("GitHub App installation authorization failed");
        }
        try {
            String token = objectMapper.readTree(response.body()).path("token").asText();
            if (token.isBlank()) {
                throw new RepositoryAccessDeniedException("GitHub App returned no installation token");
            }
            return token;
        } catch (JsonProcessingException exception) {
            throw new RepositoryAccessDeniedException("GitHub App token response was invalid", exception);
        }
    }

    private String signedAppJwt() {
        long now = clock.instant().getEpochSecond();
        String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("{\"iat\":" + (now - 60) + ",\"exp\":" + (now + 540)
                + ",\"iss\":\"" + appId + "\"}");
        String signingInput = header + "." + payload;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
            return signingInput + "." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(signature.sign());
        } catch (GeneralSecurityException exception) {
            throw new RepositoryAccessDeniedException("could not sign GitHub App request", exception);
        }
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
