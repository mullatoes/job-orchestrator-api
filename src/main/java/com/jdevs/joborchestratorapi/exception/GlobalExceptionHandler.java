package com.jdevs.joborchestratorapi.exception;

import com.jdevs.joborchestratorapi.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleJobNotFoundException(JobNotFoundException ex) {
        log.warn("Job lookup failed. reason={}", ex.getMessage());

        return ResponseEntity.
                status(HttpStatus.NOT_FOUND)
                .body(
                        ApiResponse.error(ex.getMessage(),
                                null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Request validation failed. errors={}", errorMessage);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMessage, null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid request. reason={}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected system error occurred", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred", null));
    }

    @ExceptionHandler(InvalidJobStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidJobStateException(InvalidJobStateException ex) {
        log.warn("Invalid job state operation. reason={}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), null));
    }
}
