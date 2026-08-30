package com.codeintel.infrastructure.acquisition;

import com.codeintel.application.acquisition.AcquisitionValidationException;

public final class AcquisitionSafetyException extends AcquisitionValidationException {
    public AcquisitionSafetyException(String message) {
        super(message);
    }

    public AcquisitionSafetyException(String message, Throwable cause) {
        super(message, cause);
    }
}
