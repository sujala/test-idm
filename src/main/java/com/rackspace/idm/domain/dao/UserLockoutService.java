package com.rackspace.idm.domain.dao;

public interface UserLockoutService {

    /**
     * Performs a lockout check on a user to determine whether the user is currently considered locked out.
     *
     * If the check requires loading the user, it will return the loaded user as well (if exists). Callers can check
     * whether the user was loaded via {@link LockoutCheckResult#isUserLoaded()}. If this method returns `true`, but
     * {@link LockoutCheckResult#getUser()} returns null, it means the user does not exist. If the method returns `false`,
     * no determination can be made on whether or not the user exists in the backend.
     *
     * @param username
     * @return
     */
    LockoutCheckResult performLockoutCheck(String username);
}
