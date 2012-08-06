package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class CustomerConflictException extends IdmException {
    public CustomerConflictException() {
        super();
    }

    public CustomerConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
