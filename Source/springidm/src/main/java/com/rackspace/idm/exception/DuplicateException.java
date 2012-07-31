package com.rackspace.idm.exception;

import com.unboundid.ldap.sdk.LDAPException;

@SuppressWarnings("serial")
public class DuplicateException extends IdmException {
    public DuplicateException() {
        super();
    }

    public DuplicateException(String message) {
        super(message);
    }

    public DuplicateException(String message, Throwable cause) {
        super(message, cause);
    }
}
