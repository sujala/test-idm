package com.rackspace.idm.exception;

/**
 */
public class SaveOrUpdateException extends IdmException {
    public SaveOrUpdateException() {
    }

    public SaveOrUpdateException(String message) {
        super(message);
    }

    public SaveOrUpdateException(Throwable cause) {
        super(cause);
    }

    public SaveOrUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
