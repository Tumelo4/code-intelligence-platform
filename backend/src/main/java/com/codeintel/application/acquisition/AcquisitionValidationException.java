package com.codeintel.application.acquisition;

public class AcquisitionValidationException extends RuntimeException {
    public AcquisitionValidationException(String message) {
        super(message);
    }

    public AcquisitionValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
