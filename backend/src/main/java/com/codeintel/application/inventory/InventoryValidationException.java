package com.codeintel.application.inventory;

public class InventoryValidationException extends RuntimeException {
    public InventoryValidationException(String message) {
        super(message);
    }

    public InventoryValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
