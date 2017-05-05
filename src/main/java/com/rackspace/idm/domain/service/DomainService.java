package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;

import java.util.List;

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
    PaginatorContext<Domain> getDomains(int offset, int limit);
    void updateDomain(Domain domain);
    void deleteDomain(String domainId);
    void addTenantToDomain(String tenantId, String domainId);
    void removeTenantFromDomain(String tenantId, String domainId);
    Iterable<User> getUsersByDomainId(String domainId);
    Iterable<User> getUsersByDomainIdAndEnabledFlag(String domainId, boolean enabled);
    List<User> getDomainAdmins(String domainId);
    List<User> getDomainSuperAdmins(String domainId);
    List<User> getEnabledDomainAdmins(String domainId);
    Domain checkAndGetDomain(String domainId);
    String createNewDomain(String domainId);
    Iterable<Domain> getDomainsForTenants(List<Tenant> tenants);
    void expireAllTokenInDomain(String domainId);

    /**
     * Return all the domains that have the specified RCN. Will return an empty list if the specified RCN is empty.
     *
     * @param rcn
     * @return
     */
    Iterable<Domain> findDomainsWithRcn(String rcn);

    /**
     * Generates a UUID for a new domain
     */
    String getDomainUUID();
}
