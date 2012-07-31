package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class NotAuthenticatedException extends IdmException {
    public NotAuthenticatedException() {
        super();
    }

    public NotAuthenticatedException(String message) {
        super(message);
    }

    public NotAuthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
