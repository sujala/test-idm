package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.User;

/**
 * Serves as an abstraction for retrieving a the RequestContext.
 */
public interface RequestContextHolder {

    //TODO: Push this to RequestContext
    /**
     * Gets the End User (user being acted upon) from the Request Context. Returns
     * null if no user is associated with that id.
     *
     * @return
     */
    EndUser getTargetEndUser(String userId);

    //TODO: Push this to RequestContext
    /**
     * Gets the End User (user being acted upon) from the Request Context. Throws a
     * NotFoundException if no end user is associated with that id
     *
     * @return
     */
    EndUser getAndCheckTargetEndUser(String userId);

    //TODO: Push this to RequestContext
    /**
     * Gets the User (user being acted upon) from the Request Context. Returns
     * null if no user is associated with that id.
     *
     * @return
     */
    User getTargetUser(String userId);

    //TODO: Push this to RequestContext
    /**
     * Gets the User (user being acted upon) from the Request Context. Throws a
     * NotFoundException if no end user is associated with that id
     *
     * @return
     */
    User checkAndGetTargetUser(String userId);

    /**
     * Return the request context. Must not return null.
     * @return
     */
    RequestContext getRequestContext();

    /**
     * Return the authentication context. Must not return null.
     */
    AuthenticationContext getAuthenticationContext();

}
