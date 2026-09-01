package com.codeintel.domain.analysis;

import java.util.List;

public record AnalysisReport(List<JavaFileMetrics> files, List<AnalysisFinding> findings) {
    public AnalysisReport {
        files = List.copyOf(files);
        findings = List.copyOf(findings);
    }
}
