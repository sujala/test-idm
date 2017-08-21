package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class UnsupportedMediaTypeException extends IdmException {
    public UnsupportedMediaTypeException() {
        super();
    }

    public UnsupportedMediaTypeException(String message) {
        super(message);
    }

    public UnsupportedMediaTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedMediaTypeException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
