package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class IdmException extends RuntimeException {
    public IdmException() {
        super();
    }

    public IdmException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdmException(String message) {
        super(message);
    }

    public IdmException(Throwable cause) {
        super(cause);
    }
}
