package com.exemple.quiz_app.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BadRequestException extends RuntimeException {

    private String message;
    private String code;

    public BadRequestException(String message) {
        super(message);
        this.message = message;
        this.code = "BAD_REQUEST";
    }

    public BadRequestException(String message, String code) {
        super(message);
        this.message = message;
        this.code = code;
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.code = "BAD_REQUEST";
    }
}