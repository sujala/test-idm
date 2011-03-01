package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class DuplicateUsernameException extends IdmException {
    public DuplicateUsernameException() {
        super();
    }

    public DuplicateUsernameException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateUsernameException(String message) {
        super(message);
    }

    public DuplicateUsernameException(Throwable cause) {
        super(cause);
    }
}