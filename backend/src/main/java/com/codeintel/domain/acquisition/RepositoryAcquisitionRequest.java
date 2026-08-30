package com.codeintel.domain.acquisition;

import com.codeintel.domain.repository.RepositoryId;
import java.util.Objects;

public record RepositoryAcquisitionRequest(
        RepositoryId repositoryId,
        AcquisitionSource source,
        String requestedRevision) {
    public RepositoryAcquisitionRequest {
        Objects.requireNonNull(repositoryId, "repositoryId must not be null");
        Objects.requireNonNull(source, "source must not be null");
        if (requestedRevision == null || requestedRevision.isBlank()
                || requestedRevision.length() > 255 || requestedRevision.contains("..")
                || requestedRevision.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("requested revision is invalid");
        }
    }
}
