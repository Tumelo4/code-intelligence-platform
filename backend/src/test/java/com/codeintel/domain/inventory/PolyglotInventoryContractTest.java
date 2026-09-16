package com.codeintel.domain.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolyglotInventoryContractTest {
    @Test
    void oldStoredReportWithoutFileEvidenceRemainsReadable() throws Exception {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        var paths = new RepositoryPathInventory(List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        var old = new InventoryReport(List.of("JAVA"), List.of(), paths, List.of(), 1);
        String json = mapper.writeValueAsString(old).replace(",\"fileEvidence\":[]", "");

        var restored = mapper.readValue(json, InventoryReport.class);

        assertThat(restored.fileEvidence()).isEmpty();
        assertThat(restored.languages()).containsExactly("JAVA");
    }

    @Test
    void rejectsUnsafeFileIdentityAndExcludedFilesWithLineCounts() {
        assertThatThrownBy(() -> new FileEvidence("../outside", "UNKNOWN", 1, 1,
                FileEvidence.Status.BASIC)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FileEvidence("vendor/lib.py", "PYTHON", 10, 2,
                FileEvidence.Status.VENDORED)).isInstanceOf(IllegalArgumentException.class);
    }
}
