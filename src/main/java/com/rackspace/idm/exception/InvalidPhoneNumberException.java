package com.rackspace.idm.exception;

/**
 * Thrown when an invalid phone number is provided.
 */
public class InvalidPhoneNumberException extends IdmException {
    public InvalidPhoneNumberException() {
    }

    public InvalidPhoneNumberException(String message) {
        super(message);
    }

    public InvalidPhoneNumberException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPhoneNumberException(Throwable cause) {
        super(cause);
    }
}