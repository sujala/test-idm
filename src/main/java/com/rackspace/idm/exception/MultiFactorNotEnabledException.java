package com.rackspace.idm.exception;

public class MultiFactorNotEnabledException extends IdmException {
    public MultiFactorNotEnabledException() {
    }

    public MultiFactorNotEnabledException(String message) {
        super(message);
    }

    public MultiFactorNotEnabledException(Throwable cause) {
        super(cause);
    }

    public MultiFactorNotEnabledException(String message, Throwable cause) {
        super(message, cause);
    }
}
