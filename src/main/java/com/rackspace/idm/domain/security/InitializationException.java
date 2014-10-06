package com.rackspace.idm.domain.security;

import com.rackspace.idm.exception.IdmException;

public class InitializationException extends IdmException {
    public InitializationException() {
    }

    public InitializationException(String message) {
        super(message);
    }

    public InitializationException(Throwable cause) {
        super(cause);
    }

    public InitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
