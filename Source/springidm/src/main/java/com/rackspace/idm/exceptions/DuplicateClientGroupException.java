package com.rackspace.idm.exceptions;

@SuppressWarnings("serial")
public class DuplicateClientGroupException extends IdmException {
    public DuplicateClientGroupException() {
        super();
    }

    public DuplicateClientGroupException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateClientGroupException(String message) {
        super(message);
    }

    public DuplicateClientGroupException(Throwable cause) {
        super(cause);
    }
}