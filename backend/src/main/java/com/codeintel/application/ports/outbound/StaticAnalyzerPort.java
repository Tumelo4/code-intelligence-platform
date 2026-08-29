package com.codeintel.application.ports.outbound;

import com.codeintel.domain.repository.RepositoryRevision;

public interface StaticAnalyzerPort {
    void analyze(RepositoryRevision revision);
}
