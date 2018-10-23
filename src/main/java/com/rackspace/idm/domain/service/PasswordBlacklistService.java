package com.rackspace.idm.domain.service;

public interface PasswordBlacklistService {

    /**
     * Checks the password blacklist for the given password. A password
     * is considered to be in the blacklist when the password is in the
     * blacklist and the number of times the password has been
     * publicly compromised is greater than the provided count.
     *
     * @param password
     * @return
     */
    boolean isPasswordInBlacklist(String password);
}
