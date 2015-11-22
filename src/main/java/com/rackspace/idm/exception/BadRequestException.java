package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class BadRequestException extends IdmException {
    public BadRequestException() {
        super();
    }

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(String message, String errorCode) {
        super(message, errorCode);
    }

    public BadRequestException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
