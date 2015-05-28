package com.rackspace.idm.domain.security;

/**
 * A basic set of methods to control functioning of AE Tokens
 */
public interface AEControlMethods {

    /**
     * Reload the AE keys used for encryption/decryption from where ever they are stored
     */
    void reloadKeys();
}
