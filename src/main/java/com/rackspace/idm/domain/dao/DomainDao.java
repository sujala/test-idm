package com.rackspace.idm.domain.dao;


import com.rackspace.idm.domain.entity.Domain;
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
    void updateDomain(Domain domain);
    void deleteDomain(String domainId);
    List<Domain> getDomainsForTenant(List<Tenant> tenants);
}
