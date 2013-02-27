package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.exception.NotFoundException;
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
    private ScopeAccessService scopeAccessService;

    @Autowired
    private TenantService tenantService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    AuthResponseTuple authenticate(AuthenticationRequest authenticationRequest) {
        if (StringUtils.isBlank(authenticationRequest.getToken().getId())) {
            throw new BadRequestException("Invalid Token Id");
        }
        if (StringUtils.isBlank(authenticationRequest.getTenantId()) && StringUtils.isBlank(authenticationRequest.getTenantName())) {
            throw new BadRequestException("Invalid request. Specify tenantId or tenantName.");
        }

        AuthResponseTuple authResponseTuple = new AuthResponseTuple();
        ScopeAccess sa = scopeAccessService.getScopeAccessByAccessToken(authenticationRequest.getToken().getId());

        // Check for impersonated token
        if (sa instanceof ImpersonatedScopeAccess) {
            authResponseTuple.setImpersonatedScopeAccess((ImpersonatedScopeAccess) sa);
            // Check Expiration of impersonated token
            if (sa.isAccessTokenExpired(new DateTime())) {
                throw new NotAuthorizedException("Token not authenticated");
            }
            // Swap token out and Log
            String newToken = ((ImpersonatedScopeAccess) sa).getImpersonatingToken();
            logger.info("Impersonating token {} with token {} ", authenticationRequest.getToken(), newToken);
            sa = scopeAccessService.getScopeAccessByAccessToken(newToken);
        }
        if (!(sa instanceof UserScopeAccess) || sa.isAccessTokenExpired(new DateTime())) {
            String errMsg = "Token not authenticated";
            logger.warn(errMsg);
            throw new NotAuthenticatedException(errMsg);
        }
        authResponseTuple.setUserScopeAccess((UserScopeAccess) sa);

        authResponseTuple.setUser(getUserByIdForAuthentication(authResponseTuple.getUserScopeAccess().getUserRsId()));

        scopeAccessService.updateExpiredUserScopeAccess(authResponseTuple.getUser(), sa.getClientId());

        if (!StringUtils.isBlank(authenticationRequest.getTenantName()) && !tenantService.hasTenantAccess(authResponseTuple.getUser(), authenticationRequest.getTenantName())) {
            String errMsg = "Token doesn't belong to Tenant with Id/Name: '" + authenticationRequest.getTenantName() + "'";
            logger.warn(errMsg);
            throw new NotAuthenticatedException(errMsg);
        }
        if (!StringUtils.isBlank(authenticationRequest.getTenantId()) && !tenantService.hasTenantAccess(authResponseTuple.getUser(), authenticationRequest.getTenantId())) {
            String errMsg = "Token doesn't belong to Tenant with Id/Name: '" + authenticationRequest.getTenantId() + "'";
            logger.warn(errMsg);
            throw new NotAuthenticatedException(errMsg);
        }

        return authResponseTuple;
    }

    User getUserByIdForAuthentication(String id) {
        User user = null;

        try {
            user = userService.checkAndGetUserById(id);
        } catch (NotFoundException e) {
            String errorMessage = String.format("Unable to authenticate user with credentials provided.");
            logger.warn(errorMessage);
            throw new NotAuthenticatedException(errorMessage, e);
        }
        return user;
    }
}
