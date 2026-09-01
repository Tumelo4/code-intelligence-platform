package com.codeintel.infrastructure.persistence;

import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.inventory.InventoryReport;
import com.codeintel.domain.inventory.RepositoryInventory;
import com.codeintel.domain.inventory.RepositoryPathInventory;
import com.codeintel.domain.repository.RepositoryId;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JdbcRepositoryInventoryStoreTest {
    @Test
    void persistsRevisionLinkedInventoryAsStructuredJson() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        var store = new JdbcRepositoryInventoryStore(jdbc, new ObjectMapper().findAndRegisterModules());
        var paths = new RepositoryPathInventory(List.of("src/main/java"), List.of(),
                List.of("pom.xml"), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());
        var report = new InventoryReport(List.of("JAVA"), List.of("MAVEN"), paths, List.of(), 2);
        Instant time = Instant.parse("2026-09-01T00:00:00Z");
        var inventory = new RepositoryInventory(new RepositoryId(UUID.randomUUID()),
                new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40)),
                report, time);

        store.save(inventory);

        ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(anyString(), arguments.capture());
        assertThat(arguments.getValue()[0]).isEqualTo(inventory.repositoryId().value());
        assertThat(arguments.getValue()[1]).isEqualTo("GIT_COMMIT");
        assertThat(arguments.getValue()[2]).isEqualTo("a".repeat(40));
        assertThat((String) arguments.getValue()[3]).contains("\"languages\":[\"JAVA\"]")
                .doesNotContain("/tmp/", "immutableOriginal", "workingCopy");
        assertThat(arguments.getValue()[4]).isEqualTo(Timestamp.from(time));
    }
}
