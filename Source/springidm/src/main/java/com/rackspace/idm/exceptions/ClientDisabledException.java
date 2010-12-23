package com.rackspace.idm.exceptions;

@SuppressWarnings("serial")
public class ClientDisabledException extends IdmException {
    public ClientDisabledException() {
        super();
    }

    public ClientDisabledException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientDisabledException(String message) {
        super(message);
    }

    public ClientDisabledException(Throwable cause) {
        super(cause);
    }
}
