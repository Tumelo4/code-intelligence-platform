package com.codeintel.infrastructure.persistence;

import com.codeintel.domain.acquisition.AcquiredRepository;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.repository.RepositoryId;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JdbcAcquisitionRecordStoreTest {
    @Test
    void persistsExactRevisionAndSeparatedPaths() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        JdbcAcquisitionRecordStore store = new JdbcAcquisitionRecordStore(jdbc);
        Instant acquiredAt = Instant.parse("2026-08-30T00:00:00Z");
        AcquiredRepository acquired = new AcquiredRepository(new RepositoryId(UUID.randomUUID()),
                new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40)),
                "main", Path.of("/safe/original"), Path.of("/safe/working"), 2, acquiredAt);

        store.save(acquired);

        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(anyString(), arguments.capture());
        assertThat(arguments.getValue()).containsExactly(acquired.repositoryId().value(),
                "GIT_COMMIT", "a".repeat(40), "main", "/safe/original", "/safe/working", 2,
                Timestamp.from(acquiredAt));
    }
}
