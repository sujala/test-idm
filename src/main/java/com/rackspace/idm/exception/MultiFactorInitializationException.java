package com.rackspace.idm.exception;

public class MultiFactorInitializationException extends IdmException {
    public MultiFactorInitializationException() {
    }

    public MultiFactorInitializationException(String message) {
        super(message);
    }

    public MultiFactorInitializationException(Throwable cause) {
        super(cause);
    }

    public MultiFactorInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
