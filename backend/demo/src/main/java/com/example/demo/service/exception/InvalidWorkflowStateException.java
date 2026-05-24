package com.example.demo.service.exception;

public class InvalidWorkflowStateException extends RuntimeException {

    public InvalidWorkflowStateException(String message) {
        super(message);
    }
}
