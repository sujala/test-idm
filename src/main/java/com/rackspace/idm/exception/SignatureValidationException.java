package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class SignatureValidationException extends IdmException {
    public SignatureValidationException() {
        super();
    }

    public SignatureValidationException(String message) {
        super(message);
    }

    public SignatureValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignatureValidationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }

    public SignatureValidationException(String message, String errorCode) {
        super(message, errorCode);
    }
}
