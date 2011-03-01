package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class PasswordValidationException extends IdmException {
    public PasswordValidationException() {
        super();
    }

    public PasswordValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PasswordValidationException(String message) {
        super(message);
    }

    public PasswordValidationException(Throwable cause) {
        super(cause);
    }
}