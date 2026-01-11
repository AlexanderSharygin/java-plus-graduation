package ru.practicum.ewm.exception.model;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class ErrorResponse {
    private String error;
    private String status;
    private String description;

    public ErrorResponse(String error, String description, String status) {
        this.error = error;
        this.description = description;
        this.status = status;
    }
}