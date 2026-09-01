package com.codeintel.infrastructure.inventory;

import com.codeintel.application.inventory.InventoryValidationException;

public final class InventorySafetyException extends InventoryValidationException {
    public InventorySafetyException(String message) {
        super(message);
    }

    public InventorySafetyException(String message, Throwable cause) {
        super(message, cause);
    }
}
