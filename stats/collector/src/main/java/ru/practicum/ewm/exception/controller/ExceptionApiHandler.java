package ru.practicum.ewm.exception.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.ewm.exception.model.ErrorResponse;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestControllerAdvice
@Slf4j
public class ExceptionApiHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingServletRequestParameterException(final Throwable e) {
        log.warn("MissingServletRequestParameterException. Message: {}, StackTrace: {}", e.getMessage(), e.getStackTrace());
        return new ErrorResponse(e.getMessage(), "Validation error", BAD_REQUEST.toString());
    }


    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleOtherExceptions(final Throwable e) {
        log.warn("Unknown error. Message: {}, StackTrace: {}", e.getMessage(), e.getStackTrace());

        return new ErrorResponse(e.getMessage(), "Unknown error", HttpStatus.INTERNAL_SERVER_ERROR.toString());
    }

    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleMIllegalArgumentException(final Throwable e) {
        log.warn("IllegalArgumentException. Message: {}, StackTrace: {}", e.getMessage(), e.getStackTrace());
        return new ErrorResponse(e.getMessage(), "Wrong evetn type", BAD_REQUEST.toString());
    }

}
