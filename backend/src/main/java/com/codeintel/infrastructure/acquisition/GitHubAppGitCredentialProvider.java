package com.codeintel.infrastructure.acquisition;

import com.codeintel.domain.acquisition.GitRemoteAcquisitionSource;
import com.codeintel.domain.repository.GitHubRepository;
import com.codeintel.infrastructure.repository.GitHubAppApiAccessProbe;
import java.util.Arrays;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public final class GitHubAppGitCredentialProvider implements GitCredentialProvider {
    private final GitHubAppApiAccessProbe tokenService;

    public GitHubAppGitCredentialProvider(GitHubAppApiAccessProbe tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public CredentialLease openFor(GitRemoteAcquisitionSource source) {
        if (source.gitHubInstallationId().isEmpty()) {
            return new NoCredentialsProvider().openFor(source);
        }
        GitHubRepository repository = parseGitHubRepository(source);
        String token = tokenService.issueInstallationToken(
                source.gitHubInstallationId().getAsLong(), repository);
        char[] tokenCharacters = token.toCharArray();
        UsernamePasswordCredentialsProvider provider =
                new UsernamePasswordCredentialsProvider("x-access-token", tokenCharacters);
        return new CredentialLease() {
            @Override
            public CredentialsProvider credentials() {
                return provider;
            }

            @Override
            public void close() {
                provider.clear();
                Arrays.fill(tokenCharacters, '\0');
            }
        };
    }

    private static GitHubRepository parseGitHubRepository(GitRemoteAcquisitionSource source) {
        if (!"github.com".equalsIgnoreCase(source.remoteUri().getHost())) {
            throw new AcquisitionSafetyException("GitHub App credentials may only target github.com");
        }
        String[] parts = source.remoteUri().getPath().replaceFirst("^/", "")
                .replaceFirst("\\.git$", "").split("/");
        if (parts.length != 2) {
            throw new AcquisitionSafetyException("GitHub repository URI is invalid");
        }
        return new GitHubRepository(parts[0], parts[1]);
    }
}
