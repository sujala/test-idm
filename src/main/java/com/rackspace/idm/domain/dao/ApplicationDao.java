package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.*;

import java.util.List;

public interface ApplicationDao {

    void addClient(Application client);

    ClientAuthenticationResult authenticate(String clientId, String clientSecret);

    void deleteApplication(Application client);

    List<Application> getAllApplications();

    Application getApplicationByClientId(String clientId);

    Application getApplicationByName(String clientName);

    Application getApplicationByCustomerIdAndClientId(String customerId, String clientId);
    
    Application getApplicationByScope(String scope);

    Applications getClientsByCustomerId(String customerId, int offset, int limit);

    Applications getAllApplications(List<FilterParam> filters, int offset, int limit);
    
    void updateApplication(Application client);
    
    List<Application> getAvailableScopes();
    
    List<Application> getOpenStackServices();

    void softDeleteApplication(Application application);

    void unSoftDeleteApplication(Application application);

    Application getSoftDeletedClientByName(String clientName);

    Application getSoftDeletedApplicationById(String id);
}
