package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class DuplicateUsernameException extends IdmException {
    public DuplicateUsernameException() {
        super();
    }

    public DuplicateUsernameException(String message) {
        super(message);
    }

    public DuplicateUsernameException(String message, String errorCode) {
        super(message, errorCode);
    }
}