package com.codeintel.domain.scoring;

import java.util.List;

public record ScoringReport(int healthScore, List<FileHotspot> hotspots,
        List<FindingPriority> priorities) {
    public ScoringReport {
        if (healthScore < 0 || healthScore > 100 || hotspots == null || priorities == null) {
            throw new IllegalArgumentException("scoring report fields are invalid");
        }
        hotspots = List.copyOf(hotspots);
        priorities = List.copyOf(priorities);
    }
}
