package com.codeintel.infrastructure.repository;

import com.codeintel.application.repository.RepositoryConnectionValidationException;

public final class RepositoryAccessDeniedException extends RepositoryConnectionValidationException {
    public RepositoryAccessDeniedException(String message) {
        super(message);
    }

    public RepositoryAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
