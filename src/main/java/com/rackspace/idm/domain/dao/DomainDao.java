package com.rackspace.idm.domain.dao;


import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.Tenant;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/6/12
 * Time: 3:45 PM
 * To change this template use File | Settings | File Templates.
 */
public interface DomainDao {
    void addDomain(Domain domain);
    Domain getDomain(String domainId);
    boolean domainExistsWithName(String name);
    boolean domainExistsWithNameAndNotId(String name, String id);
    PaginatorContext<Domain> getAllDomainsPaged(int offset, int limit);
    void updateDomain(Domain domain);

    /**
     * Updates the backend with the provided domain object. Null values result in the attribute being removed.
     * @param domain
     */
    void updateDomainAsIs(Domain domain);
    void deleteDomain(String domainId);
    Iterable<Domain> getDomainsForTenant(List<Tenant> tenants);

    /**
     * Return all the domains that have the specified RCN
     * @param rcn
     * @return
     */
    Iterable<Domain> findDomainsWithRcn(String rcn);
}
