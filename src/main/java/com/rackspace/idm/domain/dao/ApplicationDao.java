package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.*;

public interface ApplicationDao {

    void addApplication(Application client);

    void deleteApplication(Application client);

    Iterable<Application> getAllApplications();

    Application getApplicationByClientId(String clientId);

    Application getApplicationByName(String clientName);

    Iterable<Application> getApplicationByType(String type);

    void updateApplication(Application client);
    
    Iterable<Application> getOpenStackServices();

    PaginatorContext<Application> getOpenStackServices(int offset, int limit);
}
