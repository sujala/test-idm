package com.rackspace.idm.exception;

public class MultiFactorDeniedException extends IdmException {
    public MultiFactorDeniedException() {
    }

    public MultiFactorDeniedException(String message) {
        super(message);
    }

    public MultiFactorDeniedException(Throwable cause) {
        super(cause);
    }

    public MultiFactorDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
