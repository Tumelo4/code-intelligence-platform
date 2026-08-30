package com.codeintel.infrastructure.persistence;

import com.codeintel.application.ports.outbound.RepositoryStore;
import com.codeintel.domain.repository.RepositoryId;
import com.codeintel.domain.repository.RepositoryConnection;
import com.codeintel.domain.repository.GitHubAppConnection;
import com.codeintel.domain.repository.GitHubRepository;
import com.codeintel.domain.repository.LocalDevelopmentConnection;
import com.codeintel.domain.repository.PublicGitConnection;
import com.codeintel.domain.repository.RepositorySourceType;
import com.codeintel.domain.repository.ValidatedRepositoryConnection;
import com.codeintel.domain.repository.ZipUploadConnection;
import java.net.URI;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRepositoryStore implements RepositoryStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcRepositoryStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(ValidatedRepositoryConnection connection, RepositoryConnection source) {
        if (connection.sourceType() != source.sourceType()) {
            throw new IllegalArgumentException("validated connection and source types must match");
        }
        SourceColumns columns = SourceColumns.from(source);
        jdbcTemplate.update("""
                INSERT INTO repository_connection
                    (repository_id, source_type, safe_locator, validated_at, repository_uri,
                     github_installation_id, github_owner, github_name, zip_filename,
                     zip_size, zip_sha256, local_path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, connection.repositoryId().value(), connection.sourceType().name(),
                connection.safeLocator(), Timestamp.from(connection.validatedAt()),
                columns.repositoryUri(), columns.installationId(), columns.githubOwner(),
                columns.githubName(), columns.zipFilename(), columns.zipSize(),
                columns.zipSha256(), columns.localPath());
    }

    @Override
    public Optional<ValidatedRepositoryConnection> find(RepositoryId repositoryId) {
        return jdbcTemplate.query("""
                        SELECT repository_id, source_type, safe_locator, validated_at
                        FROM repository_connection WHERE repository_id = ?
                        """, this::map, repositoryId.value()).stream().findFirst();
    }

    @Override
    public Optional<RepositoryConnection> findSource(RepositoryId repositoryId) {
        return jdbcTemplate.query("""
                        SELECT source_type, repository_uri, github_installation_id, github_owner,
                               github_name, zip_filename, zip_size, zip_sha256, local_path
                        FROM repository_connection
                        WHERE repository_id = ?
                          AND ((source_type = 'PUBLIC_GIT_URL' AND repository_uri IS NOT NULL)
                            OR (source_type = 'GITHUB_APP' AND github_installation_id IS NOT NULL
                                AND github_owner IS NOT NULL AND github_name IS NOT NULL)
                            OR (source_type = 'ZIP_UPLOAD' AND zip_filename IS NOT NULL
                                AND zip_size IS NOT NULL AND zip_sha256 IS NOT NULL)
                            OR (source_type = 'LOCAL_DEVELOPMENT_PATH' AND local_path IS NOT NULL))
                        """, this::mapSource, repositoryId.value()).stream().findFirst();
    }

    private ValidatedRepositoryConnection map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ValidatedRepositoryConnection(
                new RepositoryId(resultSet.getObject("repository_id", java.util.UUID.class)),
                RepositorySourceType.valueOf(resultSet.getString("source_type")),
                resultSet.getString("safe_locator"),
                resultSet.getTimestamp("validated_at").toInstant());
    }

    private RepositoryConnection mapSource(ResultSet resultSet, int rowNumber) throws SQLException {
        RepositorySourceType type = RepositorySourceType.valueOf(resultSet.getString("source_type"));
        return switch (type) {
            case GITHUB_APP -> new GitHubAppConnection(
                    resultSet.getLong("github_installation_id"),
                    new GitHubRepository(resultSet.getString("github_owner"),
                            resultSet.getString("github_name")));
            case PUBLIC_GIT_URL -> new PublicGitConnection(
                    URI.create(resultSet.getString("repository_uri")));
            case ZIP_UPLOAD -> new ZipUploadConnection(resultSet.getString("zip_filename"),
                    resultSet.getLong("zip_size"), resultSet.getString("zip_sha256"));
            case LOCAL_DEVELOPMENT_PATH -> new LocalDevelopmentConnection(
                    Path.of(resultSet.getString("local_path")));
        };
    }

    private record SourceColumns(
            String repositoryUri, Long installationId, String githubOwner, String githubName,
            String zipFilename, Long zipSize, String zipSha256, String localPath) {
        static SourceColumns from(RepositoryConnection source) {
            if (source instanceof GitHubAppConnection github) {
                return new SourceColumns(null, github.installationId(), github.repository().owner(),
                        github.repository().name(), null, null, null, null);
            }
            if (source instanceof PublicGitConnection publicGit) {
                return new SourceColumns(publicGit.repositoryUri().toASCIIString(), null, null,
                        null, null, null, null, null);
            }
            if (source instanceof ZipUploadConnection zip) {
                return new SourceColumns(null, null, null, null, zip.originalFilename(),
                        zip.sizeBytes(), zip.contentSha256(), null);
            }
            if (source instanceof LocalDevelopmentConnection local) {
                return new SourceColumns(null, null, null, null, null, null, null,
                        local.repositoryPath().toString());
            }
            throw new IllegalArgumentException("unsupported repository source");
        }
    }
}
