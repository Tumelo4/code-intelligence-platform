package com.codeintel.presentation;

import com.codeintel.application.repository.RepositoryConnectionNotFoundException;
import com.codeintel.application.repository.RepositoryConnectionValidationException;
import com.codeintel.application.acquisition.AcquisitionNotFoundException;
import com.codeintel.application.acquisition.AcquisitionValidationException;
import com.codeintel.application.inventory.InventoryNotFoundException;
import com.codeintel.application.inventory.InventoryValidationException;
import com.codeintel.application.analysis.AnalysisNotFoundException;
import com.codeintel.application.analysis.AnalysisValidationException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RepositoryConnectionExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse invalidInput(IllegalArgumentException exception) {
        return new ErrorResponse("invalid_repository_connection", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(RepositoryConnectionValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ErrorResponse accessDenied(RepositoryConnectionValidationException exception) {
        return new ErrorResponse("repository_access_not_validated", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(RepositoryConnectionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse notFound(RepositoryConnectionNotFoundException exception) {
        return new ErrorResponse("repository_connection_not_found", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(AcquisitionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse acquisitionNotFound(AcquisitionNotFoundException exception) {
        return new ErrorResponse("repository_acquisition_not_found", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(AcquisitionValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ErrorResponse acquisitionRejected(AcquisitionValidationException exception) {
        return new ErrorResponse("repository_acquisition_rejected", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(InventoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse inventoryNotFound(InventoryNotFoundException exception) {
        return new ErrorResponse("repository_inventory_not_found", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(InventoryValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ErrorResponse inventoryRejected(InventoryValidationException exception) {
        return new ErrorResponse("repository_inventory_rejected", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(AnalysisNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse analysisNotFound(AnalysisNotFoundException exception) {
        return new ErrorResponse("static_analysis_not_found", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(AnalysisValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ErrorResponse analysisRejected(AnalysisValidationException exception) {
        return new ErrorResponse("static_analysis_rejected", exception.getMessage(), Instant.now());
    }

    record ErrorResponse(String code, String message, Instant timestamp) {
    }
}
