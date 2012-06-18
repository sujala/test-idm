package com.rackspace.idm.exception;

@SuppressWarnings("serial")
public class DuplicateClientGroupException extends IdmException {
    public DuplicateClientGroupException() {
        super();
    }

    public DuplicateClientGroupException(String message) {
        super(message);
    }
}