package com.codeintel.application.analysis;

import com.codeintel.application.ports.outbound.StaticAnalysisStore;
import com.codeintel.domain.analysis.StaticAnalysisResult;
import com.codeintel.domain.repository.RepositoryId;

public final class GetStaticAnalysis {
    private final StaticAnalysisStore store;
    public GetStaticAnalysis(StaticAnalysisStore store) { this.store = store; }
    public StaticAnalysisResult execute(RepositoryId id) {
        return store.findLatest(id).orElseThrow(() -> new AnalysisNotFoundException("static analysis not found"));
    }
}
