package com.codeintel.application.inventory;

import com.codeintel.application.git.GetGitIntelligence;
import com.codeintel.domain.git.ChangeCoupling;
import com.codeintel.domain.git.FileHistory;
import com.codeintel.domain.git.GitIntelligenceResult;
import com.codeintel.domain.inventory.FileEvidence;
import com.codeintel.domain.inventory.GitJoinedFileEvidence;
import com.codeintel.domain.inventory.RepositoryInventory;
import com.codeintel.domain.repository.RepositoryId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Joins two separately persisted analyses only when their acquisition revisions match. */
public final class GetGitJoinedFileEvidence {
    private final GetRepositoryInventory inventories;
    private final GetGitIntelligence git;

    public GetGitJoinedFileEvidence(GetRepositoryInventory inventories, GetGitIntelligence git) {
        this.inventories = inventories;
        this.git = git;
    }

    public Result execute(RepositoryId repositoryId) {
        RepositoryInventory inventory = inventories.execute(repositoryId);
        GitIntelligenceResult intelligence = git.execute(repositoryId);
        if (!inventory.acquisitionRevision().equals(intelligence.acquisitionRevision())) {
            throw new InventoryValidationException("inventory and Git intelligence revisions differ");
        }
        Map<String, FileHistory> histories = new HashMap<>();
        for (FileHistory history : intelligence.report().files()) {
            if (histories.putIfAbsent(history.file(), history) != null) {
                throw new InventoryValidationException("duplicate Git file history");
            }
        }
        Map<String, List<ChangeCoupling>> couplings = new HashMap<>();
        for (ChangeCoupling coupling : intelligence.report().couplings()) {
            couplings.computeIfAbsent(coupling.firstFile(), ignored -> new ArrayList<>()).add(coupling);
            couplings.computeIfAbsent(coupling.secondFile(), ignored -> new ArrayList<>()).add(coupling);
        }
        List<GitJoinedFileEvidence> files = inventory.report().fileEvidence().stream()
                .sorted(Comparator.comparing(FileEvidence::file))
                .map(evidence -> {
                    List<ChangeCoupling> attached = couplings.getOrDefault(evidence.file(), List.of());
                    double strongest = attached.stream().mapToDouble(ChangeCoupling::strength).max().orElse(0);
                    return new GitJoinedFileEvidence(evidence, histories.get(evidence.file()),
                            attached.size(), strongest);
                }).toList();
        return new Result(inventory, intelligence.report().historyTruncated(), files);
    }

    public record Result(RepositoryInventory inventory, boolean historyTruncated,
            List<GitJoinedFileEvidence> files) { }
}
