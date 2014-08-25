package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface ApplicationService {

    void add(Application client);

    ClientAuthenticationResult authenticate(String clientId, String clientSecret);

    void delete(String clientId);

    Applications getAllApplications(List<FilterParam> filters, int offset, int limit);

    Applications getByCustomerId(String customerId, int offset, int limit);

    PaginatorContext<Application> getAllApplicationsPaged(List<FilterParam> filters, int offset, int limit);

    PaginatorContext<Application> getByCustomerIdPaged(String customerId, int offset, int limit);

    Application loadApplication(String applicationId);
    
    Application getById(String clientId);

    Application getByName(String clientName);

    Iterable<Application> getByType(String type);

    Application checkAndGetApplication(String applicationId);

    Application getClient(String customerId, String clientId);

    Applications getClientServices(Application client);

    Application getApplicationByScopeAccess(ScopeAccess scopeAccess);
    
    ClientSecret resetClientSecret(Application client);

    void save(Application client);
    
    void updateClient(Application client);

    Application getClientByScope(String scope);
    
    void addClientRole(ClientRole role);

    void addClientRole(ClientRole role, String roleId);
    
    void deleteClientRole(ClientRole role);
    
    void updateClientRole(ClientRole role);
    
    Iterable<ClientRole> getClientRolesByClientId(String clientId);
    
    ClientRole getClientRoleByClientIdAndRoleName(String clientId, String roleName);
    
    ClientRole getClientRoleById(String id);
    
    Iterable<ClientRole> getAllClientRoles();

    PaginatorContext<ClientRole> getAvailableClientRolesPaged(int offset, int limit, int maxWeightAvailable);

    PaginatorContext<ClientRole> getAvailableClientRolesPaged(String applicationId, int offset, int limit, int maxWeightAvailable);

    PaginatorContext<ClientRole> getClientRolesPaged(String applicationId, String roleName, int offset, int limit);

    Iterable<Application> getOpenStackServices();

    void softDeleteApplication(Application application);

    ClientRole getUserIdentityRole(User user);
}
