package com.rackspace.idm.exception;

public class UnrecognizedAuthenticationMethodException extends ErrorCodeIdmException {
    public UnrecognizedAuthenticationMethodException(String errorCode) {
        super(errorCode);
    }

    public UnrecognizedAuthenticationMethodException(String errorCode, String message) {
        super(errorCode, message);
    }

    public UnrecognizedAuthenticationMethodException(String errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public UnrecognizedAuthenticationMethodException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
