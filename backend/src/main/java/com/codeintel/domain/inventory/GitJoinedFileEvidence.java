package com.codeintel.domain.inventory;

import com.codeintel.domain.git.FileHistory;

/** Current-revision file evidence enriched with available Git history. */
public record GitJoinedFileEvidence(FileEvidence evidence, FileHistory history,
        int couplingCount, double strongestCoupling) {
    public GitJoinedFileEvidence {
        if (evidence == null || (history != null && !evidence.file().equals(history.file()))
                || couplingCount < 0 || !Double.isFinite(strongestCoupling)
                || strongestCoupling < 0 || strongestCoupling > 1) {
            throw new IllegalArgumentException("joined file evidence is invalid");
        }
    }
}
