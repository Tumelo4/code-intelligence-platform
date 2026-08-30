package com.codeintel.presentation;

import com.codeintel.application.repository.ConnectRepository;
import com.codeintel.application.repository.GetRepositoryConnection;
import com.codeintel.domain.repository.GitHubAppConnection;
import com.codeintel.domain.repository.GitHubRepository;
import com.codeintel.domain.repository.LocalDevelopmentConnection;
import com.codeintel.domain.repository.PublicGitConnection;
import com.codeintel.domain.repository.RepositoryConnection;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.repository.RepositorySourceType;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;
import com.codeintel.domain.repository.ZipUploadConnection;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repository-connections")
public class RepositoryConnectionController {
    private final ConnectRepository connectRepository;
    private final GetRepositoryConnection getRepositoryConnection;

    public RepositoryConnectionController(
            ConnectRepository connectRepository,
            GetRepositoryConnection getRepositoryConnection) {
        this.connectRepository = connectRepository;
        this.getRepositoryConnection = getRepositoryConnection;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectionResponse connect(@RequestBody ConnectionRequest request) {
        return ConnectionResponse.from(connectRepository.execute(request.toDomain()));
    }

    @GetMapping("/{repositoryId}")
    public ConnectionResponse get(@PathVariable UUID repositoryId) {
        return ConnectionResponse.from(getRepositoryConnection.execute(new RepositoryId(repositoryId)));
    }

    public record ConnectionRequest(
            RepositorySourceType sourceType,
            Long installationId,
            String owner,
            String repository,
            URI repositoryUri,
            String originalFilename,
            Long sizeBytes,
            String contentSha256,
            Path repositoryPath) {
        RepositoryConnection toDomain() {
            if (sourceType == null) {
                throw new IllegalArgumentException("sourceType is required");
            }
            return switch (sourceType) {
                case GITHUB_APP -> new GitHubAppConnection(requirePositive(installationId),
                        new GitHubRepository(owner, repository));
                case PUBLIC_GIT_URL -> new PublicGitConnection(repositoryUri);
                case ZIP_UPLOAD -> new ZipUploadConnection(
                        originalFilename, requirePositive(sizeBytes), contentSha256);
                case LOCAL_DEVELOPMENT_PATH -> new LocalDevelopmentConnection(repositoryPath);
            };
        }

        private static long requirePositive(Long value) {
            if (value == null || value <= 0) {
                throw new IllegalArgumentException("required positive numeric field is missing");
            }
            return value;
        }
    }

    public record ConnectionResponse(
            UUID repositoryId,
            RepositorySourceType sourceType,
            String locator,
            Instant validatedAt) {
        static ConnectionResponse from(ValidatedRepositoryConnection connection) {
            return new ConnectionResponse(connection.repositoryId().value(), connection.sourceType(),
                    connection.safeLocator(), connection.validatedAt());
        }
    }
}
