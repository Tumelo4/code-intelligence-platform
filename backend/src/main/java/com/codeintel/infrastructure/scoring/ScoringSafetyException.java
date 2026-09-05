package com.codeintel.infrastructure.scoring;

import com.codeintel.application.ports.outbound.ScoringPortException;

public final class ScoringSafetyException extends ScoringPortException {
    public ScoringSafetyException(String message) { super(message); }
}
