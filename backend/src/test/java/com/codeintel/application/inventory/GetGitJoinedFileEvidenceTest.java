package com.codeintel.application.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codeintel.application.git.GetGitIntelligence;
import com.codeintel.application.ports.outbound.GitIntelligenceStore;
import com.codeintel.application.ports.outbound.RepositoryInventoryStore;
import com.codeintel.domain.acquisition.AcquisitionRevision;
import com.codeintel.domain.git.AuthorContribution;
import com.codeintel.domain.git.ChangeCoupling;
import com.codeintel.domain.git.FileHistory;
import com.codeintel.domain.git.GitIntelligenceReport;
import com.codeintel.domain.git.GitIntelligenceResult;
import com.codeintel.domain.inventory.FileEvidence;
import com.codeintel.domain.inventory.InventoryReport;
import com.codeintel.domain.inventory.RepositoryInventory;
import com.codeintel.domain.inventory.RepositoryPathInventory;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetGitJoinedFileEvidenceTest {
    private static final Instant TIME = Instant.parse("2026-09-01T00:00:00Z");

    @Test
    void joinsCurrentFilesAcrossLanguagesAndLeavesMissingHistoryExplicit() {
        var id = new RepositoryId(UUID.randomUUID());
        var revision = new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "a".repeat(40));
        var files = List.of(new FileEvidence("src/main.py", "PYTHON", 12, 2,
                        FileEvidence.Status.BASIC),
                new FileEvidence("src/web.ts", "TYPESCRIPT", 20, 3, FileEvidence.Status.BASIC),
                new FileEvidence("src/image.png", "UNKNOWN", 10, 0, FileEvidence.Status.BINARY));
        var inventory = inventory(id, revision, files);
        var python = new FileHistory("src/main.py", 2, 6, 2, TIME, TIME,
                List.of(new AuthorContribution("b".repeat(64), 2, 6, 2)));
        var coupling = new ChangeCoupling("src/main.py", "src/web.ts", 2, 2, 2, 1.0);
        var git = new GitIntelligenceResult(id, revision,
                new GitIntelligenceReport(List.of(), List.of(python), List.of(coupling), true), TIME);

        var result = join(inventory, git).execute(id);

        assertThat(result.files()).extracting(file -> file.evidence().file())
                .containsExactly("src/image.png", "src/main.py", "src/web.ts");
        assertThat(result.files().get(1).history()).isEqualTo(python);
        assertThat(result.files().get(1).couplingCount()).isEqualTo(1);
        assertThat(result.files().get(1).strongestCoupling()).isEqualTo(1.0);
        assertThat(result.files().get(2).history()).isNull();
        assertThat(result.historyTruncated()).isTrue();
    }

    @Test
    void refusesToJoinDifferentExactRevisions() {
        var id = new RepositoryId(UUID.randomUUID());
        var inventory = inventory(id, new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT,
                "a".repeat(40)), List.of());
        var git = new GitIntelligenceResult(id,
                new AcquisitionRevision(AcquisitionRevision.Kind.GIT_COMMIT, "b".repeat(40)),
                new GitIntelligenceReport(List.of(), List.of(), List.of(), false), TIME);

        assertThatThrownBy(() -> join(inventory, git).execute(id))
                .isInstanceOf(InventoryValidationException.class)
                .hasMessageContaining("revisions differ");
    }

    private static GetGitJoinedFileEvidence join(RepositoryInventory inventory,
            GitIntelligenceResult git) {
        var inventoryStore = mock(RepositoryInventoryStore.class);
        var gitStore = mock(GitIntelligenceStore.class);
        when(inventoryStore.findLatest(inventory.repositoryId())).thenReturn(Optional.of(inventory));
        when(gitStore.findLatest(git.repositoryId())).thenReturn(Optional.of(git));
        return new GetGitJoinedFileEvidence(new GetRepositoryInventory(inventoryStore),
                new GetGitIntelligence(gitStore));
    }

    private static RepositoryInventory inventory(RepositoryId id, AcquisitionRevision revision,
            List<FileEvidence> files) {
        var paths = new RepositoryPathInventory(List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        return new RepositoryInventory(id, revision,
                new InventoryReport(List.of(), List.of(), paths, List.of(), files.size(), files), TIME);
    }
}
