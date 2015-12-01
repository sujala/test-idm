package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class NotFoundException extends IdmException {
    public NotFoundException() {
        super();
    }

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    public NotFoundException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
