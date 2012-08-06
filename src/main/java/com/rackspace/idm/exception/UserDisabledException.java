package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class UserDisabledException extends IdmException {
    public UserDisabledException() {
        super();
    }

    public UserDisabledException(String message) {
        super(message);
    }
}
