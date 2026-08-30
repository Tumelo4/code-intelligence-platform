package com.codeintel.infrastructure.repository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AllowlistedPublicGitAccessProbe implements PublicGitAccessProbe {
    private final Set<String> allowedHosts;
    private final OutboundHttpClient httpClient;

    public AllowlistedPublicGitAccessProbe(Set<String> allowedHosts, OutboundHttpClient httpClient) {
        this.allowedHosts = allowedHosts.stream()
                .map(host -> host.toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
        this.httpClient = httpClient;
    }

    @Override
    public boolean canRead(URI repositoryUri) {
        if (!allowedHosts.contains(repositoryUri.getHost().toLowerCase(Locale.ROOT))) {
            return false;
        }
        OutboundHttpClient.Response response = httpClient.exchange(
                "GET", discoveryUri(repositoryUri),
                Map.of("Accept", "application/x-git-upload-pack-advertisement"), "");
        return response.statusCode() == 200
                && response.firstHeader("Content-Type").toLowerCase(Locale.ROOT)
                .startsWith("application/x-git-upload-pack-advertisement");
    }

    private static URI discoveryUri(URI repositoryUri) {
        String path = repositoryUri.getPath().endsWith("/")
                ? repositoryUri.getPath() + "info/refs"
                : repositoryUri.getPath() + "/info/refs";
        try {
            return new URI(repositoryUri.getScheme(), null, repositoryUri.getHost(), repositoryUri.getPort(),
                    path, "service=git-upload-pack", null);
        } catch (URISyntaxException exception) {
            throw new RepositoryAccessDeniedException("public Git discovery URI is invalid", exception);
        }
    }
}
