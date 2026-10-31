package com.example.shortener.controllers;

import com.example.shortener.model.AlreadyMappedShortKeyException;
import com.example.shortener.model.ErrorObject;
import com.example.shortener.model.InvalidRequestException;
import com.example.shortener.model.InvalidUrlException;
import com.example.shortener.model.RedirectionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            InvalidRequestException.class,
            InvalidUrlException.class,
            AlreadyMappedShortKeyException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorObject handleBadRequest(Exception exception) {
        return new ErrorObject(exception.getMessage());
    }

    @ExceptionHandler(RedirectionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorObject handleNotFound(
            RedirectionNotFoundException exception) {

        return new ErrorObject(exception.getMessage());
    }
}