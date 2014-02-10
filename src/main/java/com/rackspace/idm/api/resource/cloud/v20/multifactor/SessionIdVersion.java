package com.rackspace.idm.api.resource.cloud.v20.multifactor;

public interface SessionIdVersion {
    public static final int PREFIX_LENGTH = 5;

    /**
     * Return the prefix added to the plaintext to uniquely identify this version. MUST return a string of exactly prefix length.
     *
     * @return
     */
    String getPrefix();

    /**
     * Convert the plaintext to a sessionid object for this version
     * @param plaintext
     * @return
     */
    SessionId getSessionId(String plaintext);

    String getPlaintext(SessionId session);

}
