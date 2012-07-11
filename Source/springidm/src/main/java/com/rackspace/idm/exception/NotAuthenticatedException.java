package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class NotAuthenticatedException extends IdmException {
    public NotAuthenticatedException() {
        super();
    }

    public NotAuthenticatedException(String message) {
        super(message);
    }
}
