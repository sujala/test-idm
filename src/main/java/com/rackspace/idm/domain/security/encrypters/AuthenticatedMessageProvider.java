package com.rackspace.idm.domain.security.encrypters;

public interface AuthenticatedMessageProvider {
    /**
     * Generate an authenticated and encrypted message for the provided bytes
     *
     * @param bytes
     * @return
     */
    byte[] encrypt(byte[] bytes);

    /**
     * Reverses the result of {@link #encrypt(byte[])} by retrieving the protected message contained with the provided bytes.
     * @param bytes
     * @return
     */
    byte[] decrypt(byte[] bytes);
}
