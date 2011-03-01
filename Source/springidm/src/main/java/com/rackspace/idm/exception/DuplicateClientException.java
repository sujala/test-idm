package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class DuplicateClientException extends IdmException {
    public DuplicateClientException() {
        super();
    }

    public DuplicateClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateClientException(String message) {
        super(message);
    }

    public DuplicateClientException(Throwable cause) {
        super(cause);
    }
}