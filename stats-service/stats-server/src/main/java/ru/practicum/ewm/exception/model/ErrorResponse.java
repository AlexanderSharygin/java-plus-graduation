package ru.practicum.ewm.exception.model;

import lombok.Data;

@Data
public class ErrorResponse {
    String error;

    String status;
    String description;

    public ErrorResponse(String error, String description, String status) {
        this.error = error;
        this.description = description;
        this.status = status;
    }
}