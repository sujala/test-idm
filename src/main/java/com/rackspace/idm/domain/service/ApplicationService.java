
package com.rackspace.idm.domain.service;

import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface ApplicationService {

    void add(Application client);

    void addClientGroup(ClientGroup clientGroup);

    void addDefinedPermission(DefinedPermission permission);
    
    void addUserToClientGroup(String username, String customerId, String clientId, String groupName);

//    ClientAuthenticationResult authenticate(String clientId, String clientSecret);

    void delete(String clientId);

    void deleteClientGroup(String customerId, String clientId, String groupName);

    void deleteDefinedPermission(DefinedPermission permission);

    Applications getAllApplications(List<FilterParam> filters, int offset, int limit);
    
    Applications getByCustomerId(String customerId, int offset, int limit);

    Application loadApplication(String applicationId);
    
    Application getById(String clientId);

    Application getByName(String clientName);

    Application checkAndGetApplication(String applicationId);

    Application getClient(String customerId, String clientId);

    ClientGroup getClientGroup(String customerId, String clientId,
        String groupName);

    List<ClientGroup> getClientGroupsByClientId(String clientId);

    List<ClientGroup> getClientGroupsForUser(String username);

    List<ClientGroup> getClientGroupsForUserByClientIdAndType(String username, String clientId, String type);

    Applications getClientServices(Application client);
    
    DefinedPermission getDefinedPermissionByClientIdAndPermissionId(String clientId,
        String permissionId);
    
    DefinedPermission checkAndGetPermission(String customerId, String clientId, String permissionId);

    List<DefinedPermission> getDefinedPermissionsByClient(Application client);

    boolean isUserMemberOfClientGroup(String username, ClientGroup group);

    void removeUserFromClientGroup(String username, ClientGroup clientGroup);

    ClientSecret resetClientSecret(Application client);

    void save(Application client);
    
    void updateClient(Application client);

    void updateClientGroup(ClientGroup group);

    void updateDefinedPermission(DefinedPermission permission);

    Application getClientByScope(String scope);
    
    List<Application> getAvailableScopes();
    
    void addClientRole(ClientRole role);

    void addClientRole(ClientRole role, String roleId);
    
    void deleteClientRole(ClientRole role);
    
    void updateClientRole(ClientRole role);
    
    List<ClientRole> getClientRolesByClientId(String clientId);
    
    ClientRole getClientRoleByClientIdAndRoleName(String clientId, String roleName);
    
    ClientRole getClientRoleById(String id);
    
    List<ClientRole> getAllClientRoles();

    PaginatorContext<ClientRole> getClientRolesPaged(int offset, int limit);

    PaginatorContext<ClientRole> getClientRolesPaged(String applicationId, int offset, int limit);

    PaginatorContext<ClientRole> getClientRolesPaged(String applicationId, String roleName, int offset, int limit);
    
    List<Application> getOpenStackServices();

    void softDeleteApplication(Application application);

	void setScopeAccessDao(ScopeAccessDao scopeAccessDao);

	void setApplicationDao(ApplicationDao applicationDao);

	void setCustomerDao(CustomerDao customerDao);

	void setUserDao(UserDao userDao);

	void setTenantDao(TenantDao tenantDao);

    void setApplicationRoleDao(ApplicationRoleDao applicationRoleDao);

    ClientRole getUserIdentityRole(User user, String applicationId, List<String> roleNames);

    void setTenantRoleDao(TenantRoleDao tenantRoleDao);
}
