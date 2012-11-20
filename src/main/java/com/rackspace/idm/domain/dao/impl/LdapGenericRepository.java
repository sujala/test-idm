package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.api.resource.pagination.DefaultPaginator;
import com.rackspace.idm.api.resource.pagination.PaginatorContext;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.GenericDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.Auditable;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/26/12
 * Time: 3:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class LdapGenericRepository<T extends UniqueId> extends LdapRepository implements GenericDao<T> {
    public static final String ERROR_GETTING_OBJECT = "Error getting object";

    final private Class<T> entityType = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    @Autowired
    private DefaultPaginator<T> paginator;

    @Override
    public List<T> getObjects(Filter searchFilter) {
        return getObjects(searchFilter, getBaseDn(), SearchScope.SUB);
    }

    @Override
    public List<T> getObjects(Filter searchFilter, String dn) {
        return getObjects(searchFilter, dn, SearchScope.SUB);
    }

    @Override
    public List<T> getObjects(Filter searchFilter, String dn, SearchScope scope) {
        getLogger().debug("Getting all " + entityType.toString());

        List<T> objects = new ArrayList<T>();
        SearchResult searchResult;

        try {
            searchResult = getAppInterface().search(dn, scope, searchFilter);
            getLogger().info("Got" + entityType.toString());
        } catch (LDAPSearchException ldapEx) {
            String loggerMsg = String.format("Error searching for %s - {}", entityType.toString());
            getLogger().error(loggerMsg);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            objects = processSearchResult(searchResult.getSearchEntries());
        }

        return objects;
    }

    @Override
    public PaginatorContext<T> getObjectsPaged(Filter searchFilter, int offset, int limit) {
        getLogger().debug("Getting " + entityType.toString() + " paged");

        SearchRequest searchRequest = new SearchRequest(getBaseDn(), SearchScope.SUB, searchFilter);
        PaginatorContext<T> context = paginator.createSearchRequest(getSortAttribute(), searchRequest, offset, limit);

        List<T> objects = new ArrayList<T>();
        SearchResult searchResult;

        try {
            searchResult = getAppInterface().search(searchRequest);
        } catch (LDAPSearchException ldapEx) {
            String loggerMsg = String.format("Error searching for %s - {}", entityType.toString());
            getLogger().error(loggerMsg);
            throw new IllegalStateException(ldapEx);
        }

        paginator.createPage(searchResult, context);
        if (searchResult.getEntryCount() > 0) {
            objects = processSearchResult(context.getSearchResultEntryList());
        }

        context.setValueList(objects);

        return context;
    }

    private List<T> processSearchResult(List<SearchResultEntry> searchResultList) {
        List<T> objects = new ArrayList<T>();
        for (SearchResultEntry entry : searchResultList) {
            getLogger().debug("Getting % entry", entityType.toString());

            T entity = null;
            try {
                entity = LDAPPersister.getInstance(entityType).decode(entry);
            } catch (LDAPPersistException e) {
                String loggerMsg = String.format("Error converting entity for %s - {}", entityType.toString());
                getLogger().error(loggerMsg);
            }
            objects.add(entity);
        }

        return objects;
    }

    @Override
    public void addObject(T object) {
        addObject(getBaseDn(), object);
    }

    @Override
    public void addObject(String dn, T object) {
        if (object == null) {
            getLogger().error(ERROR_GETTING_OBJECT);
            throw new IllegalArgumentException(ERROR_GETTING_OBJECT);
        }
        getLogger().info("Adding object: {}", object);
        Audit audit = Audit.log((Auditable)object).add();
        try {
            final LDAPPersister<T> persister = LDAPPersister.getInstance(entityType);
            persister.add(object, getAppInterface(), dn);
            audit.succeed();
            getLogger().info("Added: {}", object);
        } catch (final LDAPException e) {
            getLogger().error("Error adding object", e);
            audit.fail(e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String addLdapContainer(String partialDnString, String containerName) {
        SearchResultEntry entry = getLdapContainer(partialDnString, containerName);
        if (entry == null) {
            Audit audit = Audit.log(String.format("Adding container: %s", containerName));
            List<Attribute> attributes = new ArrayList<Attribute>();
            attributes.add(new Attribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACE_CONTAINER));
            attributes.add(new Attribute(ATTR_NAME, containerName));
            Attribute[] attributeArray = attributes.toArray(new Attribute[0]);
            String dn = new LdapDnBuilder(partialDnString).addAttribute(ATTR_NAME, containerName).build();
            try {
                getAppInterface().add(dn, attributeArray);
                audit.succeed();
                return dn;
            } catch (LDAPException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return entry.getDN();
    }

    private SearchResultEntry getLdapContainer(String dn, String containerName) {
        Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACE_CONTAINER)
                .addEqualAttribute(ATTR_NAME, containerName).build();
        return getSingleEntry(dn, SearchScope.ONE, filter);
    }

    @Override
    public T getObject(Filter searchFilter) {
        return getObject(searchFilter, getBaseDn());
    }

    @Override
    public T getObject(Filter searchFilter, String dn) {
        return getObject(searchFilter, dn, SearchScope.ONE);
    }

    @Override
    public T getObject(Filter searchFilter, String dn, SearchScope scope) {
        String loggerMsg = String.format("Doing search for %s", entityType.toString());
        getLogger().debug(loggerMsg);

        T object;
        try {
            object = getSingleObject(dn, scope, searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error(ERROR_GETTING_OBJECT, e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Found - {}", object);

        return object;
    }

    @Override
    public void updateObject(T object) {
        if (object == null || StringUtils.isBlank(object.getUniqueId())) {
            getLogger().error(ERROR_GETTING_OBJECT);
            getLogger().info("Invalid parameter.");
            throw new IllegalArgumentException("Missing argument on update");
        }
        String loggerMsg = String.format("Updating object %s with id %s", entityType.toString(), object.getUniqueId());
        getLogger().debug(loggerMsg);
        Audit audit = Audit.log((Auditable)object).modify();

        try {
            LDAPPersister<T> persister = LDAPPersister.getInstance(entityType);
            List<Modification> mods = persister.getModifications(object, true);
            audit.modify(mods);

            if (mods.size() > 0) {
                persister.modify(object, getAppInterface(), null, true);
            }
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating {} - {}", object, ldapEx);
            audit.fail("Error updating");
            throw new IllegalStateException(ldapEx.getMessage(), ldapEx);
        }
        audit.succeed();
        getLogger().info("Updated - {}", object);
    }

    @Override
    public void deleteObject(Filter searchFilter) {
        String loggerMsg = String.format("Deleting object %s", entityType.toString());
        getLogger().debug(loggerMsg);

        T object = getObject(searchFilter);

        if (object == null) {
            String errMsg = String.format("Object %s not found", searchFilter.toNormalizedString());
            getLogger().warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        getLogger().debug("Deleting: {}", object);
        final String dn = object.getUniqueId();
        final Audit audit = Audit.log((Auditable)object).delete();
        deleteEntryAndSubtree(dn, audit);
        audit.succeed();
        getLogger().debug("Deleted: {}", object);
    }

    @Override
    public void deleteObject(T object) {
        String loggerMsg = String.format("Deleting object %s", object.getUniqueId());
        getLogger().debug(loggerMsg);

        getLogger().debug("Deleting: {}", object);
        final String dn = object.getUniqueId();
        final Audit audit = Audit.log((Auditable)object).delete();
        deleteEntryAndSubtree(dn, audit);
        audit.succeed();
        getLogger().debug("Deleted: {}", object);
    }

    private T getSingleObject(String dn, SearchScope scope, Filter searchFilter) throws LDAPPersistException {
        SearchResultEntry entry = this.getSingleEntry(dn, scope, searchFilter);
        if (entry == null) {
            return null;
        }
        return LDAPPersister.getInstance(entityType).decode(entry);
    }

    @Override
    public String getBaseDn(){
        throw new NotImplementedException();
    }

    @Override
    public String getLdapEntityClass(){
        throw new NotImplementedException();
    }

    @Override
    public String getNextId() {
        throw new NotImplementedException();
    }

    @Override
    public String getSortAttribute() {
        throw new NotImplementedException();
    }
}

