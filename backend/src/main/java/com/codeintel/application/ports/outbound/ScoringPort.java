package com.codeintel.application.ports.outbound;

import com.codeintel.domain.analysis.StaticAnalysisResult;
import com.codeintel.domain.git.GitIntelligenceResult;
import com.codeintel.domain.scoring.ScoringReport;

public interface ScoringPort {
    ScoringReport score(StaticAnalysisResult analysis, GitIntelligenceResult gitIntelligence);
}
