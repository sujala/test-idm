package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class NotAuthorizedException extends IdmException {
    public NotAuthorizedException() {
        super();
    }

    public NotAuthorizedException(String message) {
        super(message);
    }

    public NotAuthorizedException(String message, Throwable clause) {
        super(message, clause);
    }
}
