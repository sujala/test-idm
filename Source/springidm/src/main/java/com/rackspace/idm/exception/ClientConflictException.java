package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class ClientConflictException extends IdmException {
    public ClientConflictException() {
        super();
    }

    public ClientConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientConflictException(String message) {
        super(message);
    }

    public ClientConflictException(Throwable cause) {
        super(cause);
    }
}
