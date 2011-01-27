package com.rackspace.idm.exceptions;

@SuppressWarnings("serial")
public class BaseUrlConflictException extends IdmException {
    public BaseUrlConflictException() {
        super();
    }

    public BaseUrlConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public BaseUrlConflictException(String message) {
        super(message);
    }

    public BaseUrlConflictException(Throwable cause) {
        super(cause);
    }
}
