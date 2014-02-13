package com.rackspace.idm.exception;

public class MultiFactorSessionIdFormatException extends IdmException {
    public MultiFactorSessionIdFormatException() {
    }

    public MultiFactorSessionIdFormatException(String message) {
        super(message);
    }

    public MultiFactorSessionIdFormatException(Throwable cause) {
        super(cause);
    }

    public MultiFactorSessionIdFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
