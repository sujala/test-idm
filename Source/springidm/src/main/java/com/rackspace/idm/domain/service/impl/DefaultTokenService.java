package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.FilterParam.FilterParamName;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.InputValidator;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DefaultTokenService implements TokenService {
    private final AuthorizationService authorizationService;
    private final ApplicationService clientService;
    private final Configuration config;
    final private Logger logger = LoggerFactory
        .getLogger(DefaultTokenService.class);
    private final ScopeAccessService scopeAccessService;
    private final UserDao userDao;
	private final TenantService tenantService;

    {
        logger.info("Instantiating DefaultTokenService");
    }

    public DefaultTokenService(
        final ApplicationService clientService,
        final AuthorizationService authorizationService,
        final Configuration config, final InputValidator inputValidator,
        final ScopeAccessService scopeAccessService,
        final UserDao userDao, final TenantService tenantService) {
    	this.userDao = userDao;
        this.clientService = clientService;
        this.authorizationService = authorizationService;
        this.config = config;
        this.scopeAccessService = scopeAccessService;
        this.tenantService = tenantService;
    }

    
    @Override
    public ScopeAccess getAccessTokenByAuthHeader(final String authHeader) {
        return this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
    }

    public ScopeAccess getAccessTokenByToken(String token) {
        return this.scopeAccessService.getScopeAccessByAccessToken(token);
    }
    
	@Override
	public boolean doesTokenHaveAccessToApplication(String token,
			String applicationId) {
		ScopeAccess scopeAccessToken = this.scopeAccessService
				.loadScopeAccessByAccessToken(token);
		return this.scopeAccessService.doesAccessTokenHaveService(
				scopeAccessToken, applicationId);
	}

	@Override
	public boolean doesTokenHaveApplicationRole(String token,String applicationId, String roleId) {
		ScopeAccess scopeAccess = this.scopeAccessService
				.loadScopeAccessByAccessToken(token);

		List<TenantRole> roles = tenantService
				.getTenantRolesForScopeAccess(scopeAccess);
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
        ScopeAccess scopeAccessToDelete = this.scopeAccessService
            .getScopeAccessByAccessToken(tokenToDelete);

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

        if (scopeAccessToDelete instanceof HasAccessToken) {
            ((HasAccessToken) scopeAccessToDelete).setAccessTokenExpired();
            this.scopeAccessService.updateScopeAccess(scopeAccessToDelete);
        }

        logger.debug("Deleted Token {}", tokenToDelete);
    }

    
    @Override
    public void revokeAllTokensForClient(final String clientId) {
        logger.debug("Deleting all access tokens for client {}.", clientId);
        this.scopeAccessService.expireAllTokensForClient(clientId);
        logger.debug("Deleted all access tokens for client {}.", clientId);
    }

    
    @Override
    public void revokeAllTokensForCustomer(final String customerId) {
        logger
            .debug("Revoking all access tokens for customer: {}.", customerId);
        final List<User> usersList = getAllUsersForCustomerId(customerId);
        for (final User user : usersList) {
            this.scopeAccessService.expireAllTokensForUser(user.getUsername());
        }

        final List<Application> clientsList = getAllClientsForCustomerId(customerId);
        for (final Application client : clientsList) {
            this.scopeAccessService.expireAllTokensForClient(client
                .getClientId());
        }

        logger.debug("Deleted all access tokens for customer {}.", customerId);
    }

    
    @Override
    public void revokeAllTokensForUser(final String username) {
        logger.debug("Deleting all access tokens for user {}.", username);
        this.scopeAccessService.expireAllTokensForUser(username);
        logger.debug("Deleted all access tokens for user {}.", username);
    }

    List<Application> getAllClientsForCustomerId(final String customerId) {
        logger.debug("Finding Clients from CustomerId: {}", customerId);
        final List<Application> clientsList = new ArrayList<Application>();
        int total = 1; // This gets overwritten, just needs to be greater than
        // offset right now.
        for (int offset = 0; offset < total; offset += getPagingLimit()) {
            final Applications clientsObj = clientService.getByCustomerId(
                customerId, offset, getPagingLimit());
            clientsList.addAll(clientsObj.getClients());
            total = clientsObj.getTotalRecords();
        }
        logger.debug("Found {} Client(s) from CustomerId: {}",
            clientsList.size(), customerId);
        return clientsList;
    }

    List<User> getAllUsersForCustomerId(final String customerId) {
    	FilterParam[] filters = new FilterParam[] { new FilterParam(FilterParamName.RCN, customerId)};
        logger.debug("Finding Users for CustomerId: {}", customerId);
        
        final List<User> usersList = new ArrayList<User>();
        int total = 1; // This gets overwritten, just needs to be greater than
        // offset right now.
        for (int offset = 0; offset < total; offset += getPagingLimit()) {
            final Users usersObj = userDao.getAllUsers(filters,offset, getPagingLimit());
            usersList.addAll(usersObj.getUsers());
            total = usersObj.getTotalRecords();
        }
        logger.debug("Found {} User(s) for CustomerId: {}", usersList.size(),
            customerId);
        return usersList;
    }

    int getPagingLimit() {
        return config.getInt("ldap.paging.limit.max");
    }
}
