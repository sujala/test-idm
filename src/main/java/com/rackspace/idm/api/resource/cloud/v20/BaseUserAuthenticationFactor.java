package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseUserAuthenticationFactor implements UserAuthenticationFactor {

    @Autowired
    protected ScopeAccessService scopeAccessService;

    @Autowired
    protected IdentityConfig identityConfig;

    @Override
    public AuthResponseTuple createScopeAccessForUserAuthenticationResult(UserAuthenticationResult userAuthenticationResult) {

        UserScopeAccess sa;

        // If the token is going to be scoped we create it directly, otherwise we'll go through the usual call
        if (StringUtils.isNotBlank(userAuthenticationResult.getScope())) {
            int expirationSeconds = identityConfig.getScopedTokenExpirationSeconds();
            sa = (UserScopeAccess)scopeAccessService.addScopedScopeAccess(userAuthenticationResult.getUser(),
                    identityConfig.getCloudAuthClientId(),
                    userAuthenticationResult.getAuthenticatedBy(),
                    expirationSeconds,
                    userAuthenticationResult.getScope());

        }  else {
            sa = scopeAccessService.getValidUserScopeAccessForClientId((User) userAuthenticationResult.getUser(),
                    identityConfig.getCloudAuthClientId(),
                    userAuthenticationResult.getAuthenticatedBy());
        }

        AuthResponseTuple authResponseTuple = new AuthResponseTuple();
        authResponseTuple.setUser((User)userAuthenticationResult.getUser());
        authResponseTuple.setUserScopeAccess(sa);

        return authResponseTuple;
    }

    protected void validateUserAuthenticationResult(final UserAuthenticationResult result) {
        if (!result.isAuthenticated()) {
            String errorMessage = String.format("Unable to authenticate user with credentials provided.");
            throw new NotAuthenticatedException(errorMessage);
        }
    }
}
