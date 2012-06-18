package com.rackspace.idm.exception;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/8/11
 * Time: 10:15 AM
 */
@SuppressWarnings("serial")
public class CloudAdminAuthorizationException extends IdmException {
    public CloudAdminAuthorizationException() {
        super();
    }

    public CloudAdminAuthorizationException(String message) {
        super(message);
    }
}