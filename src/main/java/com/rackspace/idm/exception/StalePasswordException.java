package com.rackspace.idm.exception;

/**
 * User: john.eo
 * Date: 1/24/11
 * Time: 2:24 PM
 */
@SuppressWarnings("serial")
public class StalePasswordException extends IdmException {
    public StalePasswordException(String message) {
        super(message);
    }
}
