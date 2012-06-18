package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class DuplicateClientException extends IdmException {
    public DuplicateClientException() {
        super();
    }

    public DuplicateClientException(String message) {
        super(message);
    }
}