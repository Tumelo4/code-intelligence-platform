package com.codeintel.infrastructure.analysis;

public final class StaticAnalysisSafetyException extends RuntimeException {
    public StaticAnalysisSafetyException(String message) { super(message); }
    public StaticAnalysisSafetyException(String message, Throwable cause) { super(message, cause); }
}
