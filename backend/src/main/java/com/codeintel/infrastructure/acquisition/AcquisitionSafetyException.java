package com.codeintel.infrastructure.acquisition;

public final class AcquisitionSafetyException extends RuntimeException {
    public AcquisitionSafetyException(String message) {
        super(message);
    }

    public AcquisitionSafetyException(String message, Throwable cause) {
        super(message, cause);
    }
}
