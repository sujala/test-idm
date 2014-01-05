package com.rackspace.idm.multifactor.providers.exceptions;

import com.rackspace.idm.exception.IdmException;

/**
 */
public class DeleteUserException extends IdmException {
    public DeleteUserException() {
    }

    public DeleteUserException(String message) {
        super(message);
    }

    public DeleteUserException(Throwable cause) {
        super(cause);
    }

    public DeleteUserException(String message, Throwable cause) {
        super(message, cause);
    }
}