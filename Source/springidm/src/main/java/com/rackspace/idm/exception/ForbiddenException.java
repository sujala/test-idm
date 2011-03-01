package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class ForbiddenException extends IdmException {
    public ForbiddenException() {
        super();
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(Throwable cause) {
        super(cause);
    }
}