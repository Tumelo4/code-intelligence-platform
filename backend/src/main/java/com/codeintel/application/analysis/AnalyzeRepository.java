package com.codeintel.application.analysis;

import com.codeintel.application.ports.outbound.AcquisitionRecordStore;
import com.codeintel.application.ports.outbound.RepositoryInventoryStore;
import com.codeintel.application.ports.outbound.StaticAnalysisStore;
import com.codeintel.application.ports.outbound.StaticAnalyzerPort;
import com.codeintel.domain.analysis.StaticAnalysisResult;
import com.codeintel.domain.repository.RepositoryId;
import java.time.Clock;

public final class AnalyzeRepository {
    private final AcquisitionRecordStore acquisitions;
    private final RepositoryInventoryStore inventories;
    private final StaticAnalyzerPort analyzer;
    private final StaticAnalysisStore analyses;
    private final Clock clock;

    public AnalyzeRepository(AcquisitionRecordStore acquisitions, RepositoryInventoryStore inventories,
            StaticAnalyzerPort analyzer, StaticAnalysisStore analyses, Clock clock) {
        this.acquisitions = acquisitions;
        this.inventories = inventories;
        this.analyzer = analyzer;
        this.analyses = analyses;
        this.clock = clock;
    }

    public StaticAnalysisResult execute(RepositoryId repositoryId) {
        var acquisition = acquisitions.findLatest(repositoryId)
                .orElseThrow(() -> new AnalysisNotFoundException("repository acquisition not found"));
        var inventory = inventories.findLatest(repositoryId)
                .orElseThrow(() -> new AnalysisNotFoundException("repository inventory not found"));
        if (!inventory.acquisitionRevision().equals(acquisition.revision())) {
            throw new AnalysisValidationException("inventory does not match latest acquisition revision");
        }
        var report = analyzer.analyze(acquisition.immutableOriginal(),
                inventory.report().paths().sourceRoots(), acquisition.revision());
        var result = new StaticAnalysisResult(repositoryId, acquisition.revision(), report, clock.instant());
        analyses.save(result);
        return result;
    }
}
