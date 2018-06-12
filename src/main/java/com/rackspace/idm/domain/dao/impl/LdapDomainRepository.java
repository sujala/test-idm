package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.exception.DuplicateException;
import com.unboundid.ldap.sdk.Filter;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/6/12
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
@LDAPComponent
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
            return null;
        }
        return getObject(searchByIdFilter(domainId));
    }

    @Override
    public boolean domainExistsWithName(String name) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        return countObjects(searchByNameFilter(name)) >= 1;
    }

    @Override
    public boolean domainExistsWithNameAndNotId(String name, String id) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(id)) {
            return false;
        }
        return countObjects(searchByNameAndNotIdFilter(name, id)) >= 1;
    }

    @Override
    public void updateDomain(Domain domain) {
        updateObject(domain);
    }

    @Override
    public void updateDomainAsIs(Domain domain) {
        updateObjectAsIs(domain);
    }

    @Override
    public void deleteDomain(String domainId) {
        validatePassedId(domainId);
        deleteObject(searchByIdFilter(domainId));
    }

    @Override
    public Iterable<Domain> getDomainsForTenant(List<Tenant> tenants) {
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

    @Override
    public Iterable<Domain> getRCNDomains(String rcn) {
        return getObjects(searchByRcnFilter(rcn));
    }

    @Override
    public PaginatorContext<Domain> getRCNDomainsPaged(String rcn, Integer marker, Integer limit) {
        return getObjectsPaged(searchByRcnFilter(rcn), marker, limit);
    }

    @Override
    public Iterable<Domain> findDomainsWithRcn(String rcn) {
        return getObjects(searchByRcnFilter(rcn));
    }

    Filter searchByIdFilter(String id) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, id)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_DOMAIN).build();
    }

    Filter searchByNameFilter(String name) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_NAME, name)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_DOMAIN).build();
    }

    Filter searchByRcnFilter(String rcn) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_RS_RACKSPACE_CUSTOMER_NUMBER, rcn)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_DOMAIN).build();
    }

    Filter searchByNameAndNotIdFilter(String name, String id) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_NAME, name)
                .addNotEqualAttribute(ATTR_ID, id)
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

    public String getSortAttribute() {
        return ATTR_ID;
    }
}
