package com.codeintel.infrastructure.git;

import com.codeintel.application.git.GitIntelligenceValidationException;

public final class GitIntelligenceSafetyException extends GitIntelligenceValidationException {
    public GitIntelligenceSafetyException(String message) { super(message); }
    public GitIntelligenceSafetyException(String message, Throwable cause) { super(message, cause); }
}
