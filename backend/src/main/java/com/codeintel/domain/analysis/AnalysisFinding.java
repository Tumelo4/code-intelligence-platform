package com.codeintel.domain.analysis;

public record AnalysisFinding(String id, String title, FindingType type, String area, String severity,
        String confidence, String file, SourceRange range, String evidence, String observation,
        String whyItMatters, String recommendation, String effort, int priority) {
    public AnalysisFinding {
        if (id == null || id.isBlank() || title == null || type == null || area == null
                || severity == null || confidence == null || file == null || file.startsWith("/")
                || range == null || evidence == null || observation == null || whyItMatters == null
                || recommendation == null || effort == null || priority < 1) {
            throw new IllegalArgumentException("finding evidence contract is incomplete");
        }
    }
}
