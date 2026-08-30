package com.codeintel.application.repository;

public class RepositoryConnectionValidationException extends RuntimeException {
    public RepositoryConnectionValidationException(String message) {
        super(message);
    }

    public RepositoryConnectionValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
