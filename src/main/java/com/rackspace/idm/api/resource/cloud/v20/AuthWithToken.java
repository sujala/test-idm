package com.rackspace.idm.api.resource.cloud.v20;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.*;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthWithToken {

    @Autowired
    private UserService userService;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RequestContextHolder requestContextHolder;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Trace
    AuthResponseTuple authenticate(AuthenticationRequest authenticationRequest) {
        if (StringUtils.isBlank(authenticationRequest.getToken().getId())) {
            throw new BadRequestException("Invalid Token Id");
        }
        if (StringUtils.isBlank(authenticationRequest.getTenantId()) && StringUtils.isBlank(authenticationRequest.getTenantName())) {
            throw new BadRequestException("Invalid request. Specify tenantId or tenantName.");
        }

        ScopeAccess sa = scopeAccessService.getScopeAccessByAccessToken(authenticationRequest.getToken().getId());

        /*
        TODO: Should consolidate/clean up auth w/ token validity logic.
         It's weird that an expired impersonation token will throw
         a NotAuthorizedException while an expired UserScopeAccess throws a NotAuthenticatedException. Should verify if
         response is different in either case, and if not, consolidate/cleanup the validity code.
         */

        //fail fast if null.
        if (sa == null) {
            String errMsg = "Token not authenticated";
            logger.warn(errMsg);
            throw new NotAuthenticatedException(errMsg);
        }

        //restricted (scoped) tokens can not be used for auth w/ token
        if (StringUtils.isNotBlank(sa.getScope())) {
            throw new ForbiddenException(GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN);
        }

        // Check for impersonated token
        ImpersonatedScopeAccess impersonatedScopeAccess = null;
        if (sa instanceof ImpersonatedScopeAccess) {
            impersonatedScopeAccess = (ImpersonatedScopeAccess) sa;
            // Check Expiration of impersonated token
            if (sa.isAccessTokenExpired(new DateTime())) {
                throw new NotAuthorizedException("Token not authenticated");
            }
            // Swap token out and Log
            String newToken = ((ImpersonatedScopeAccess) sa).getImpersonatingToken();
            logger.info("Impersonating token {} with token {} ", authenticationRequest.getToken(), newToken);
            sa = scopeAccessService.getScopeAccessByAccessToken(newToken);
        }

        //Perform final checks on effective token. If original was impersonation token then sa was replaced with a new token
        //so need to recheck
        if (!(sa instanceof UserScopeAccess) || sa.isAccessTokenExpired(new DateTime())) {
            String errMsg = "Token not authenticated";
            logger.warn(errMsg);
            throw new NotAuthenticatedException(errMsg);
        }
        UserScopeAccess usa = (UserScopeAccess) sa;
        EndUser user = getUserByIdForAuthentication(usa.getUserRsId());
        requestContextHolder.getAuthenticationContext().setUsername(user.getUsername());

        if(!(user instanceof FederatedUser)) {
            scopeAccessService.updateExpiredUserScopeAccess((User) user, sa.getClientId(), null);
        }

        return new AuthResponseTuple(user, usa, impersonatedScopeAccess);
    }

    EndUser getUserByIdForAuthentication(String id) {
        EndUser user;

        try {
            user = identityUserService.checkAndGetUserById(id);
        } catch (NotFoundException e) {
            String errorMessage = String.format("Unable to authenticate user with credentials provided.");
            logger.warn(errorMessage);
            throw new NotAuthenticatedException(errorMessage, e);
        }

        return user;
    }
}
