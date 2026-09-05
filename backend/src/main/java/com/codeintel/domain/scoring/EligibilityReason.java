package com.codeintel.domain.scoring;

public enum EligibilityReason {
    LOW_CONFIDENCE,
    LOW_PRIORITY,
    MISSING_FILE_METRICS,
    TRUNCATED_HISTORY,
    UNSUPPORTED_FINDING_TYPE
}
