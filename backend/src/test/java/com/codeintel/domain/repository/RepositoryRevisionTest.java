package com.codeintel.domain.repository;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryRevisionTest {
    private final RepositoryId repositoryId = new RepositoryId(UUID.randomUUID());

    @Test
    void rejectsNonShaRevision() {
        assertThatThrownBy(() -> new RepositoryRevision(repositoryId, "main"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
