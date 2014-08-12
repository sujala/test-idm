package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.IdentityUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultRequestContextHolder implements RequestContextHolder {

    @Autowired
    private RequestContext requestContext;

    @Autowired
    private IdentityUserService identityUserService;

    @Override
    public void setImpersonated(boolean flag) {
        requestContext.setImpersonated(flag);
    }

    /**
     * This method returns 'true' when the request was made using an impersonated token.
     *
     * @return
     */
    @Override
    public boolean isImpersonated() {
        return requestContext.isImpersonated();
    }

    /**
     * Sets the End User (user being acted upon) in the Request Context
     */
    @Override
    public void setEndUser(EndUser user) {
        requestContext.setEndUser(user);
    }

    /**
     * Gets the End User (user being acted upon) from the Request Context. Returns
     * null if no user is associated with that id.
     *
     * @return
     */
    @Override
    public EndUser getEndUser(String userId) {
        EndUser endUser = requestContext.getEndUser();
        if (endUser == null) {
            endUser = identityUserService.getEndUserById(userId);
            requestContext.setEndUser(endUser);
        }
        return endUser;
    }

    /**
     * Gets the End User (user being acted upon) from the Request Context. Throws a
     * NotFoundException if no end user is associated with that id
     *
     * @return
     */
    @Override
    public EndUser checkAndGetEndUser(String userId) {
        EndUser endUser = requestContext.getEndUser();
        if (endUser == null) {
            endUser = identityUserService.checkAndGetEndUserById(userId);
            requestContext.setEndUser(endUser);
        }
        return endUser;
    }

    /**
     * Gets the User (user being acted upon) from the Request Context. Returns
     * null if no user is associated with that id.
     *
     * @return
     */
    @Override
    public User getUser(String userId) {
        return (User)getEndUser(userId);
    }

    /**
     * Gets the User (user being acted upon) from the Request Context. Throws a
     * NotFoundException if no end user is associated with that id
     *
     * @return
     */
    @Override
    public User checkAndGetUser(String userId) {
        return (User)checkAndGetEndUser(userId);
    }
}
