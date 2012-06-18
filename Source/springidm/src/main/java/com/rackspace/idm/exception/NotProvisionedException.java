package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class NotProvisionedException extends IdmException {
    public NotProvisionedException() {
        super();
    }

    public NotProvisionedException(String message) {
        super(message);
    }

}
