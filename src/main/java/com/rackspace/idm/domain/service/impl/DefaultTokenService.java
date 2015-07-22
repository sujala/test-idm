package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultTokenService implements TokenService {
    private final Logger logger = LoggerFactory.getLogger(DefaultTokenService.class);

    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private ApplicationService clientService;
    @Autowired
    private Configuration config;
    @Autowired
    private ScopeAccessService scopeAccessService;
    @Autowired
    private UserService userService;
    @Autowired
	private TenantService tenantService;

    {
        logger.info("Instantiating DefaultTokenService");
    }

    @Override
    public ScopeAccess getAccessTokenByAuthHeader(final String authHeader) {
        return this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
    }

    public ScopeAccess getAccessTokenByToken(String token) {
        return this.scopeAccessService.getScopeAccessByAccessToken(token);
    }

	@Override
	public boolean doesTokenHaveApplicationRole(String token,String applicationId, String roleId) {
		ScopeAccess scopeAccess = this.scopeAccessService
				.loadScopeAccessByAccessToken(token);

		List<TenantRole> roles = tenantService.getTenantRolesForScopeAccess(scopeAccess);
		for (TenantRole role : roles) {
			if (role.getRoleRsId().equals(roleId)
					&& role.getClientId().equals(applicationId)) {
				return true;
			}
		}

		return false;
	}

    @Override
    public void revokeAccessToken(String tokenStringRequestingDelete,
        String tokenToDelete) {
        logger.debug("Deleting Token {}", tokenToDelete);
        ScopeAccess scopeAccessToDelete = this.scopeAccessService.getScopeAccessByAccessToken(tokenToDelete);

        if (scopeAccessToDelete == null) {
            final String error = "No entry found for token " + tokenToDelete;
            logger.warn(error);
            throw new NotFoundException(error);
        }

        ScopeAccess scopeAccessRequestor = this.scopeAccessService
            .getScopeAccessByAccessToken(tokenStringRequestingDelete);

        if (scopeAccessRequestor == null) {
            final String error = "No entry found for token "
                + tokenStringRequestingDelete;
            logger.warn(error);
            throw new IllegalStateException(error);
        }

        final boolean isGoodAsIdm = authorizationService
            .authorizeCustomerIdm(scopeAccessRequestor);
        // Only CustomerIdm Client and Client that got token or the user of
        // the token are authorized to revoke token
        final boolean isAuthorized = isGoodAsIdm
            || authorizationService.authorizeAsRequestorOrOwner(
                scopeAccessToDelete, scopeAccessRequestor);

        if (!isAuthorized) {
            String errMsg;
            errMsg = String.format(
                "Requesting token %s not authorized to revoke token %s.",
                tokenStringRequestingDelete, tokenToDelete);
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }

        scopeAccessToDelete.setAccessTokenExpired();
        this.scopeAccessService.updateScopeAccess(scopeAccessToDelete);

        logger.debug("Deleted Token {}", tokenToDelete);
    }

    
    @Override
    public void revokeAllTokensForClient(final String clientId) {
        logger.debug("Deleting all access tokens for client {}.", clientId);
        this.scopeAccessService.expireAllTokensForClient(clientId);
        logger.debug("Deleted all access tokens for client {}.", clientId);
    }

    @Override
    public void revokeAllTokensForUser(final String username) throws IOException, JAXBException {
        logger.debug("Deleting all access tokens for user {}.", username);
        this.scopeAccessService.expireAllTokensForUser(username);
        logger.debug("Deleted all access tokens for user {}.", username);
    }

    @Override
    public void setClientService(ApplicationService clientService) {
        this.clientService = clientService;
    }

    @Override
    public void setAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public void setConfig(Configuration config) {
        this.config = config;
    }

    @Override
    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    @Override
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }
}
