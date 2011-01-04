package com.rackspace.idm.exceptions;

@SuppressWarnings("serial")
public class CustomerConflictException extends IdmException {
    public CustomerConflictException() {
        super();
    }

    public CustomerConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public CustomerConflictException(String message) {
        super(message);
    }

    public CustomerConflictException(Throwable cause) {
        super(cause);
    }
}
