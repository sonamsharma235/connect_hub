package com.connecthub.room.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(String message) {
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
        String message = exception.getReason() == null ? "Request failed" : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiError(message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException exception) {
        String details = exception.getMostSpecificCause() == null ? "" : exception.getMostSpecificCause().getMessage();
        String lower = details == null ? "" : details.toLowerCase();

        if (lower.contains("room_code") || lower.contains("roomcode") || lower.contains("roomcode_unique") || lower.contains("roomcode_idx")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("Room id already exists"));
        }
        if (lower.contains("rooms") && lower.contains("name")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("Room name already exists"));
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError("Room already exists"));
    }
}
