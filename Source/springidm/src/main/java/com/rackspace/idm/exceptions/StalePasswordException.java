package com.rackspace.idm.exceptions;

/**
 * User: john.eo
 * Date: 1/24/11
 * Time: 2:24 PM
 */
public class StalePasswordException extends IdmException {
    public StalePasswordException(String message) {
        super(message);
    }
}
