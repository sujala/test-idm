package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.Users;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/6/12
 * Time: 3:38 PM
 * To change this template use File | Settings | File Templates.
 */
public interface DomainService {

    void addDomain(Domain domain);
    Domain getDomain(String domainId);
    void updateDomain(Domain domain);
    void deleteDomain(String domainId);
    void addTenantToDomain(String tenantId, String domainId);
    void removeTenantFromDomain(String tenantId, String domainId);
    Users getUsersByDomainId(String domainId);
    Users getUsersByDomainId(String domainId, boolean enabled);
    Domain checkAndGetDomain(String domainId);
    String createNewDomain(String domainId);
}
