package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface ApplicationService {

    void add(Application client);

    void delete(String clientId);

    Application loadApplication(String applicationId);
    
    Application getById(String clientId);

    Application getByName(String clientName);

    Iterable<Application> getByType(String type);

    Application checkAndGetApplication(String applicationId);

    Applications getClientServices(Application client);

    Application getApplicationByScopeAccess(ScopeAccess scopeAccess);
    
    ClientSecret resetClientSecret(Application client);

    void save(Application client);
    
    void updateClient(Application client);

    void addClientRole(ClientRole role);

    void addClientRole(ClientRole role, String roleId);
    
    void deleteClientRole(ClientRole role);
    
    void updateClientRole(ClientRole role);
    
    Iterable<ClientRole> getClientRolesByClientId(String clientId);
    
    ClientRole getClientRoleByClientIdAndRoleName(String clientId, String roleName);
    
    ClientRole getClientRoleById(String id);
    
    PaginatorContext<ClientRole> getAvailableClientRolesPaged(int offset, int limit, int maxWeightAvailable);

    PaginatorContext<ClientRole> getAvailableClientRolesPaged(String applicationId, int offset, int limit, int maxWeightAvailable);

    Iterable<Application> getOpenStackServices();

    ClientRole getUserIdentityRole(EndUser user);
}
