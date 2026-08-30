package com.codeintel.infrastructure.repository;

import com.codeintel.application.ports.outbound.RepositoryConnectionPort;
import com.codeintel.application.ports.outbound.RepositoryStore;
import com.codeintel.application.repository.ConnectRepository;
import com.codeintel.application.repository.GetRepositoryConnection;
import com.codeintel.application.repository.ValidateRepositoryConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConnectionConfiguration {
    @Bean
    OutboundHttpClient repositoryOutboundHttpClient(
            @Value("${repository.connection.http-timeout:10s}") Duration timeout) {
        return new JavaNetOutboundHttpClient(timeout);
    }

    @Bean
    PublicGitAccessProbe publicGitAccessProbe(
            @Value("${repository.connection.public-allowed-hosts:github.com,gitlab.com,bitbucket.org}") String hosts,
            OutboundHttpClient httpClient) {
        Set<String> allowedHosts = Arrays.stream(hosts.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toUnmodifiableSet());
        return new AllowlistedPublicGitAccessProbe(allowedHosts, httpClient);
    }

    @Bean
    GitHubAppAccessProbe gitHubAppAccessProbe(
            @Value("${repository.connection.github-app-id:0}") long appId,
            @Value("${repository.connection.github-private-key:}") String privateKey,
            @Value("${repository.connection.github-api:https://api.github.com}") URI apiBase,
            OutboundHttpClient httpClient,
            ObjectMapper objectMapper) {
        if (appId <= 0 || privateKey.isBlank()) {
            return (installationId, repository) -> false;
        }
        return new GitHubAppApiAccessProbe(appId, PemPrivateKeyLoader.loadPkcs8(privateKey),
                apiBase, httpClient, objectMapper, Clock.systemUTC());
    }

    @Bean
    ZipUploadAccessProbe zipUploadAccessProbe(
            @Value("${repository.connection.zip-staging-root:/tmp/code-intelligence-uploads}") Path stagingRoot) {
        return new StagedZipUploadAccessProbe(stagingRoot);
    }

    @Bean
    RepositoryConnectionPort repositoryConnectionPort(
            GitHubAppAccessProbe gitHubProbe,
            PublicGitAccessProbe publicGitProbe,
            ZipUploadAccessProbe zipProbe,
            @Value("${repository.connection.local-development-enabled:false}") boolean localEnabled) {
        return new RepositoryConnectionAdapter(gitHubProbe, publicGitProbe, zipProbe,
                UUID::randomUUID, Clock.systemUTC(), localEnabled);
    }

    @Bean
    ValidateRepositoryConnection validateRepositoryConnection(RepositoryConnectionPort connectionPort) {
        return new ValidateRepositoryConnection(connectionPort);
    }

    @Bean
    ConnectRepository connectRepository(
            RepositoryConnectionPort connectionPort, RepositoryStore repositoryStore) {
        return new ConnectRepository(connectionPort, repositoryStore);
    }

    @Bean
    GetRepositoryConnection getRepositoryConnection(RepositoryStore repositoryStore) {
        return new GetRepositoryConnection(repositoryStore);
    }
}
