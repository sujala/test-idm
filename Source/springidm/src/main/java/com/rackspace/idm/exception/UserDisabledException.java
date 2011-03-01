package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class UserDisabledException extends IdmException {
    public UserDisabledException() {
        super();
    }

    public UserDisabledException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserDisabledException(String message) {
        super(message);
    }

    public UserDisabledException(Throwable cause) {
        super(cause);
    }
}
