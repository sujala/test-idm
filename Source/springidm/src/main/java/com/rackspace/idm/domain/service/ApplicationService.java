
package com.rackspace.idm.domain.service;

import java.util.List;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.Applications;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.DefinedPermission;
import com.rackspace.idm.domain.entity.FilterParam;

public interface ApplicationService {

    void add(Application client);

    void addClientGroup(ClientGroup clientGroup);

    void addDefinedPermission(DefinedPermission permission);
    
    void addUserToClientGroup(String username, String customerId, String clientId, String groupName);

//    ClientAuthenticationResult authenticate(String clientId, String clientSecret);

    void delete(String clientId);

    void deleteClientGroup(String customerId, String clientId, String groupName);

    void deleteDefinedPermission(DefinedPermission permission);

    Applications getAllApplications(FilterParam[] filters, int offset, int limit);
    
    Applications getByCustomerId(String customerId, int offset, int limit);

    Application loadApplication(String applicationId);
    
    Application getById(String clientId);

    Application getByName(String clientName);

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
    
    void deleteClientRole(ClientRole role);
    
    void updateClientRole(ClientRole role);
    
    List<ClientRole> getClientRolesByClientId(String clientId);
    
    ClientRole getClientRoleByClientIdAndRoleName(String clientId, String roleName);
    
    ClientRole getClientRoleById(String id);
    
    List<ClientRole> getAllClientRoles(FilterParam[] filters);
    
    List<Application> getOpenStackServices();

    void softDeleteApplication(Application application);
}
