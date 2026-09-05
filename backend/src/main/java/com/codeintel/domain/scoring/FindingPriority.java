package com.codeintel.domain.scoring;

import java.util.Comparator;
import java.util.List;

public record FindingPriority(String findingId, String file, int score, int rank,
        boolean eligible, List<EligibilityReason> reasons) {
    public FindingPriority {
        if (findingId == null || findingId.isBlank() || file == null || file.isBlank()
                || file.startsWith("/") || score < 0 || score > 100 || rank < 1 || reasons == null) {
            throw new IllegalArgumentException("finding priority fields are invalid");
        }
        reasons = reasons.stream().sorted(Comparator.comparing(Enum::name)).distinct().toList();
        if (eligible != reasons.isEmpty()) {
            throw new IllegalArgumentException("eligibility and reasons are inconsistent");
        }
    }
}
