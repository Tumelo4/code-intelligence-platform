package com.codeintel.infrastructure.analysis;

import com.codeintel.application.analysis.AnalysisValidationException;

public final class StaticAnalysisSafetyException extends AnalysisValidationException {
    public StaticAnalysisSafetyException(String message) { super(message); }
    public StaticAnalysisSafetyException(String message, Throwable cause) { super(message, cause); }
}
