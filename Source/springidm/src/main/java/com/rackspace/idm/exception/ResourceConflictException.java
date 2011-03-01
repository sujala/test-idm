package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class ResourceConflictException extends IdmException {
    public ResourceConflictException() {
        super();
    }

    public ResourceConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceConflictException(String message) {
        super(message);
    }

    public ResourceConflictException(Throwable cause) {
        super(cause);
    }
}
