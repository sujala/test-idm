package com.rackspace.idm.exception;

public class UnrecognizedAuthenticationMethodException extends ErrorCodeIdmException {
    public UnrecognizedAuthenticationMethodException(String message) {
        super(message);
    }

    public UnrecognizedAuthenticationMethodException(String errorCode, String message) {
        super(errorCode, message);
    }

    public UnrecognizedAuthenticationMethodException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
