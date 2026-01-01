package ru.practicum.ewm.exception.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.ewm.exception.model.BadRequestException;
import ru.practicum.ewm.exception.model.ErrorResponse;

import java.util.Arrays;

@RestControllerAdvice
@Slf4j
public class ExceptionHandler {
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ErrorResponse handleAll(final Exception e) {
        log.error(Arrays.toString(e.getStackTrace()));

        return new ErrorResponse(e.getMessage(), "Something went wrong",
                HttpStatus.INTERNAL_SERVER_ERROR.toString());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIncorrectParameterException(final BadRequestException e) {
        log.warn(Arrays.toString(e.getStackTrace()));

        return new ErrorResponse(e.getParameter(), "Bad request", HttpStatus.BAD_REQUEST.toString());
    }
}
