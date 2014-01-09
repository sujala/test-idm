package com.rackspace.idm.multifactor.providers.exceptions;

import com.rackspace.idm.exception.IdmException;

/**
 */
public class CreateUserException extends IdmException {
    public CreateUserException() {
    }

    public CreateUserException(String message) {
        super(message);
    }

    public CreateUserException(Throwable cause) {
        super(cause);
    }

    public CreateUserException(String message, Throwable cause) {
        super(message, cause);
    }
}