package com.codeintel.domain.acquisition;

import com.codeintel.domain.repository.RepositoryId;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public record AcquiredRepository(
        RepositoryId repositoryId,
        AcquisitionRevision revision,
        String requestedRevision,
        Path immutableOriginal,
        Path workingCopy,
        int skippedSubmodules,
        Instant acquiredAt) {
    public AcquiredRepository {
        Objects.requireNonNull(repositoryId);
        Objects.requireNonNull(revision);
        Objects.requireNonNull(requestedRevision);
        Objects.requireNonNull(immutableOriginal);
        Objects.requireNonNull(workingCopy);
        Objects.requireNonNull(acquiredAt);
        if (immutableOriginal.equals(workingCopy) || skippedSubmodules < 0) {
            throw new IllegalArgumentException("acquisition copies or submodule count are invalid");
        }
    }
}
