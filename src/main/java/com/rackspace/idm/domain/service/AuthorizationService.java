package com.rackspace.idm.domain.service;

import org.apache.commons.configuration.Configuration;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.Entity;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;

public interface AuthorizationService {
    
	/**
	 * determines if client token is authorized
	 * 
	 * @param token  - token to check if authorized to access resource
	 * @param object - the entity we are trying to access.
	 * @param authorizedRoles  - the role ids that client must have to access the object. null allowed.
	 */
	void authorize(String token, Entity object, String... authorizedRoles);

    boolean authorizeRacker(ScopeAccess scopeAccess);
    boolean authorizeRackspaceClient(ScopeAccess scopeAccess);
    boolean authorizeCustomerUser(ScopeAccess scopeAccess, String customerId);
    boolean authorizeUser(ScopeAccess scopeAccess, String customerId, String username);
    boolean authorizeAsRequestorOrOwner(ScopeAccess targetScopeAccess, ScopeAccess requestingScopeAccess);
    boolean authorizeCustomerIdm(ScopeAccess scopeAccess);
    boolean authorizeCloudServiceAdmin(ScopeAccess scopeAccess);
    boolean authorizeCloudIdentityAdmin(ScopeAccess scopeAccess);
    boolean authorizeCloudUserAdmin(ScopeAccess scopeAccess);
    boolean authorizeCloudUser(ScopeAccess scopeAccess);
    boolean authorizeIdmSuperAdminOrRackspaceClient(ScopeAccess scopeAccess);
    boolean authorizeIdmSuperAdmin(ScopeAccess scopeAccess);

    void checkAuthAndHandleFailure(boolean authorized, ScopeAccess token);

    boolean hasDefaultUserRole(User user);
    boolean hasUserAdminRole(User user);
    boolean hasIdentityAdminRole(User user);
    boolean hasServiceAdminRole(User user);

    void verifyIdmSuperAdminAccess(String authToken);
    void verifyServiceAdminLevelAccess(ScopeAccess authScopeAccess);
    void verifyRackerOrIdentityAdminAccess(ScopeAccess authScopeAccess);
    void verifyIdentityAdminLevelAccess(ScopeAccess authScopeAccess);
    void verifyUserAdminLevelAccess(ScopeAccess authScopeAccess);
    void verifyUserLevelAccess(ScopeAccess authScopeAccess);
    void verifySelf(User requester, User requestedUser);
    void verifyTokenHasTenantAccess(String tenantId, ScopeAccess authScopeAccess);
    void verifyDomain(User retrievedUser, User caller);
	public void setScopeAccessDao(ScopeAccessDao accessDao);
	public void setApplicationDao(ApplicationDao applicationDao);
	public void setTenantDao(TenantDao tenantDao);
	public void setConfig(Configuration config);
}
