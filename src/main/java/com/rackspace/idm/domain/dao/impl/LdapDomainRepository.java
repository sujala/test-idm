package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.entity.PaginatorContext;
import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.exception.DuplicateException;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/6/12
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class LdapDomainRepository extends LdapGenericRepository<Domain> implements DomainDao {

    public static final String NULL_OR_EMPTY_DOMAIN_ID_PARAMETER = "Null or Empty domainId parameter";
    public static final String NULL_OR_EMPTY_TENANT_ID_PARAMETER = "Null or Empty tenantId parameter";

    @Override
    public void addDomain(Domain domain) {
        try {
            addObject(domain);
        }
        catch(DuplicateException ex){
            throw new DuplicateException(String.format("Domain %s already exists", domain.getDomainId()));
        }
    }

    @Override
    public Domain getDomain(String domainId) {
        if (StringUtils.isBlank(domainId)) {
            getLogger().error(NULL_OR_EMPTY_DOMAIN_ID_PARAMETER);
            return null;
        }
        return getObject(searchByIdFilter(domainId));
    }

    @Override
    public void updateDomain(Domain domain) {
        updateObject(domain);
    }

    @Override
    public void deleteDomain(String domainId) {
        validatePassedId(domainId);
        deleteObject(searchByIdFilter(domainId));
    }

    @Override
    public List<Domain> getDomainsForTenant(List<Tenant> tenants) {
        if (tenants == null || tenants.size() < 1) {
            getLogger().error(NULL_OR_EMPTY_TENANT_ID_PARAMETER);
            getLogger().info("Invalid tenantIds parameter.");
            return null;
        }
        List<Filter> filterList = new ArrayList<Filter>();
        for(Tenant tenant: tenants){
            filterList.add(Filter.createEqualityFilter(ATTR_TENANT_RS_ID,tenant.getTenantId()));
        }
        Filter orFilter = Filter.createORFilter(filterList);
        Filter andFilter = Filter.createANDFilter(Filter.createEqualityFilter(ATTR_OBJECT_CLASS,OBJECTCLASS_DOMAIN),orFilter);

        return getObjects(andFilter);
    }

    @Override
    public PaginatorContext<Domain> getAllDomainsPaged(int offset, int limit) {
        return getObjectsPaged(searchAllDomains(), offset, limit);
    }

    Filter searchByIdFilter(String id) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, id)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_DOMAIN).build();
    }

    Filter searchAllDomains() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_DOMAIN).build();
    }

    void validatePassedId(String id) {
        if (StringUtils.isBlank(id)) {
            getLogger().error(NULL_OR_EMPTY_DOMAIN_ID_PARAMETER);
            throw new IllegalArgumentException(
                    "Null or Empty domainId parameter.");
        }
    }

    public String getBaseDn(){
        return DOMAIN_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_DOMAIN;
    }

    public String getNextCapabilityId() {
        return getNextId(NEXT_DOMAIN_ID);
    }

    public String getSortAttribute() {
        return ATTR_ID;
    }
}
