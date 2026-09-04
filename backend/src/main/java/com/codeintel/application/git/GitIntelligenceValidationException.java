package com.codeintel.application.git;

public class GitIntelligenceValidationException extends RuntimeException {
    public GitIntelligenceValidationException(String message) { super(message); }
    public GitIntelligenceValidationException(String message, Throwable cause) { super(message, cause); }
}
