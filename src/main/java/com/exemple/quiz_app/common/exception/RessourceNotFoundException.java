package com.exemple.quiz_app.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RessourceNotFoundException extends RuntimeException {

    private String resourceName;
    private String fieldName;
    private Object fieldValue;

    public RessourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s non trouve avec %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public RessourceNotFoundException(String message) {
        super(message);
    }

    public RessourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}