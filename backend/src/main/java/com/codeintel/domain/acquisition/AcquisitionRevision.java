package com.codeintel.domain.acquisition;

public record AcquisitionRevision(Kind kind, String value) {
    public AcquisitionRevision {
        if (kind == null || value == null || !switch (kind) {
            case GIT_COMMIT -> value.matches("[0-9a-f]{40}");
            case ARCHIVE_SHA256, LOCAL_SNAPSHOT_SHA256 -> value.matches("[0-9a-f]{64}");
        }) {
            throw new IllegalArgumentException("acquisition revision does not match its kind");
        }
    }

    public enum Kind {
        GIT_COMMIT,
        ARCHIVE_SHA256,
        LOCAL_SNAPSHOT_SHA256
    }
}
