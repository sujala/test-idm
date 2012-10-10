package com.rackspace.idm.domain.dao.impl;

import org.springframework.stereotype.Component;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/6/12
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class LdapDomainRepository extends LdapRepository implements DomainDao{

    public static final String NULL_OR_EMPTY_DOMAIN_ID_PARAMETER = "Null or Empty domainId parameter";
    public static final String ERROR_GETTING_DOMAIN_OBJECT = "Error getting domain object";
    public static final String PARENT_UNIQUE_ID_CANNOT_BE_BLANK = "ParentUniqueId cannot be blank";

    @Override
    public void addDomain(Domain domain) {
        if (domain == null) {
            String errmsg = "Null instance of Domain was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        getLogger().info("Adding Domain: {}", domain);
        Audit audit = Audit.log(domain).add();
        try {
            final LDAPPersister<Domain> persister = LDAPPersister.getInstance(Domain.class);
            persister.add(domain, getAppInterface(), DOMAIN_BASE_DN);
            audit.succeed();
            getLogger().info("Added Domain: {}", domain);
        } catch (final LDAPException e) {
            if (e.getResultCode() == ResultCode.ENTRY_ALREADY_EXISTS) {
                String errMsg = String.format("Domain %s already exists", domain.getDomainId());
                getLogger().warn(errMsg);
                throw new DuplicateException(errMsg, e);
            }
            getLogger().error("Error adding domain object", e);
            audit.fail(e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Domain getDomain(String domainId) {
        getLogger().debug("Doing search for domainId " + domainId);
        if (StringUtils.isBlank(domainId)) {
            getLogger().error(NULL_OR_EMPTY_DOMAIN_ID_PARAMETER);
            getLogger().info("Invalid domainId parameter.");
            return null;
        }

        Filter searchFilter = new LdapSearchBuilder()
            .addEqualAttribute(ATTR_ID, domainId)
            .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_DOMAIN).build();

        Domain domain = null;

        try {
            domain = getSingleDomain(searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error(ERROR_GETTING_DOMAIN_OBJECT, e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Found Domain - {}", domain);

        return domain;
    }

    @Override
    public void updateDomain(Domain domain) {
        if (domain == null || StringUtils.isBlank(domain.getUniqueId())) {
            String errmsg = "Null instance of Domain was passed";
            getLogger().error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
        getLogger().debug("Updating Domain: {}", domain);
        Audit audit = Audit.log(domain);
        try {
            final LDAPPersister<Domain> persister = LDAPPersister.getInstance(Domain.class);
            List<Modification> modifications = persister.getModifications(domain, true);
            audit.modify(modifications);
            if (modifications.size() > 0) {
                persister.modify(domain, getAppInterface(), null, true);
            }
            getLogger().debug("Updated Domain: {}", domain);
            audit.succeed();
        } catch (final LDAPException e) {
            getLogger().error("Error updating domain", e);
            audit.fail();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void deleteDomain(String domainId) {
        if (StringUtils.isBlank(domainId)) {
            getLogger().error(NULL_OR_EMPTY_DOMAIN_ID_PARAMETER);
            throw new IllegalArgumentException(
                "Null or Empty domainId parameter.");
        }
        Domain domain = getDomain(domainId);
        if (domain == null) {
            String errMsg = String.format("domain %s not found", domainId);
            getLogger().warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        getLogger().debug("Deleting Domain: {}", domain);
        final String dn = domain.getUniqueId();
        final Audit audit = Audit.log(domain).delete();
        deleteEntryAndSubtree(dn, audit);
        audit.succeed();
        getLogger().debug("Deleted Domain: {}", domain);
    }

    Domain getSingleDomain(Filter searchFilter)
        throws LDAPPersistException {
        SearchResultEntry entry = this.getSingleEntry(DOMAIN_BASE_DN, SearchScope.ONE, searchFilter, ATTR_DOMAIN_SEARCH_ATTRIBUTES);
        if (entry == null) {
            return null;
        }
        Domain domain = null;
        domain = LDAPPersister.getInstance(Domain.class).decode(entry);
        return domain;
    }
}
