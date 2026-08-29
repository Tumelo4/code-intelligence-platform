package com.codeintel.domain.repository;

import java.util.Objects;
import java.util.UUID;

public record RepositoryId(UUID value) {
    public RepositoryId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
