package com.codeintel.application.analysis;

public class AnalysisValidationException extends RuntimeException {
    public AnalysisValidationException(String message) { super(message); }
    public AnalysisValidationException(String message, Throwable cause) { super(message, cause); }
}
