package com.jdevs.joborchestratorapi.exception;

public class InvalidJobStateException extends RuntimeException {
    public InvalidJobStateException(String message) {
        super(message);
    }
}
