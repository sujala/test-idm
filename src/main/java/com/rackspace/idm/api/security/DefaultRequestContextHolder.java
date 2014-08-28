package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultRequestContextHolder implements RequestContextHolder {

    @Autowired
    private RequestContext requestContext;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    private static final String USER_ID = "UserId";
    private static final String USER = "User";
    private static final String TOKEN_STRING = "TokenString";
    private static final String SCOPE_ACCESS = "ScopeAccess";
    private static final String SPRING_CONTEXT_ERROR = "Spring request context error getting %s with %s = %s";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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

        // This check is to verify the Spring Scope "Request" is working properly. This should never
        // fail, but because it's obviously a security concern we should verify it.
        if (endUser != null && !endUser.getId().equalsIgnoreCase(userId)) {
            String errMsg = String.format(SPRING_CONTEXT_ERROR, USER, USER_ID, userId);
            logger.error(errMsg);
            throw new IllegalStateException(errMsg);
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

        // This check is to verify the Spring Scope "Request" is working properly. This should never
        // fail, but because it's obviously a security concern we should verify it.
        if (!endUser.getId().equalsIgnoreCase(userId)) {
            String errMsg = String.format(SPRING_CONTEXT_ERROR, USER, USER_ID, userId);
            logger.error(errMsg);
            throw new IllegalStateException(errMsg);
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

    /**
     * Gets the effective caller's ScopeAccess (token sent in X-AUTH header) from the Request Context.
     *
     * @param tokenString
     * @return
     */
    @Override
    public ScopeAccess getEffectiveCallerScopeAccess(String tokenString) {
        ScopeAccess scopeAccess = requestContext.getCallerScopeAccess();

        if (scopeAccess == null) {
            scopeAccess = scopeAccessService.getScopeAccessByAccessToken(tokenString);
            requestContext.setCallerScopeAccess(scopeAccess);
        }

        // This check is to verify the Spring Scope "Request" is working properly. This should never
        // fail, but because it's obviously a security concern we should verify it.
        if (scopeAccess != null && !scopeAccess.getAccessTokenString().equalsIgnoreCase(tokenString)) {
            String errMsg = String.format(SPRING_CONTEXT_ERROR, SCOPE_ACCESS, TOKEN_STRING, tokenString);
            logger.error(errMsg);
            throw new IllegalStateException(errMsg);
        }

        return scopeAccess;
    }

    /**
     * Sets the effective caller's ScopeAcess
     * @param scopeAccess
     */
    @Override
    public void setEffectiveCallerScopeAccess(ScopeAccess scopeAccess) {
        requestContext.setCallerScopeAccess(scopeAccess);
    }
}
