package com.rackspace.idm.exceptions;

@SuppressWarnings("serial")
public class PermissionConflictException extends IdmException {
    public PermissionConflictException() {
        super();
    }

    public PermissionConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public PermissionConflictException(String message) {
        super(message);
    }

    public PermissionConflictException(Throwable cause) {
        super(cause);
    }
}
