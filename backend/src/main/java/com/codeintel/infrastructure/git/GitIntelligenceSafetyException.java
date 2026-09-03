package com.codeintel.infrastructure.git;

public final class GitIntelligenceSafetyException extends RuntimeException {
    public GitIntelligenceSafetyException(String message) { super(message); }
    public GitIntelligenceSafetyException(String message, Throwable cause) { super(message, cause); }
}
