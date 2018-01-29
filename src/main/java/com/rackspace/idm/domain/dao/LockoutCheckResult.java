package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.User;
import org.joda.time.DateTime;

/**
 * Basic lockout check object. User is guaranteed to be populated _only_ be populated if
 */
public class LockoutCheckResult {
    private boolean userLoaded;
    private DateTime lockoutExpiration;
    private User user;

    public LockoutCheckResult(boolean userLoaded, DateTime lockoutExpiration, User user) {
        this.userLoaded = userLoaded;
        this.lockoutExpiration = lockoutExpiration;
        this.user = user;
    }

    /**
     * Whether the specified user is deemed to be locked out.
     *
     * @return
     */
    public boolean isUserLockedOut() {
        return lockoutExpiration != null;
    }

    /**
     * Whether the user was loaded from backend. If so, then the {@link #getUser()} will return the backend User if
     * it exists.
     *
     * This does not guarantee the getUser will return a non-null value. If the user was loaded from the backend,
     * but the user didn't exist in the backend, this service
     * would return true, while {@link #getUser()} would return null.
     *
     * @return
     */
    public boolean isUserLoaded() {
        return userLoaded;
    }

    public User getUser() {
        return user;
    }

    /**
     * If user is locked out, this returns the expiration time of the lockout.
     *
     * @return
     */
    public DateTime getLockoutExpiration() {
        return lockoutExpiration;
    }
}
