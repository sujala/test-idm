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
}
