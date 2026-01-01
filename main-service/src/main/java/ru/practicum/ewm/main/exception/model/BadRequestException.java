package ru.practicum.ewm.main.exception.model;

public class BadRequestException extends RuntimeException {
    private final String parameter;

    public BadRequestException(String parameter) {
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }

}
