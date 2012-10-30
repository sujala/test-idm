package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.GenericDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.Auditable;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;

import javax.annotation.PostConstruct;
import javax.validation.groups.Default;
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
    public static final String NULL_OR_EMPTY_ID_PARAMETER = "Null or Empty Id parameter";
    public static final String NULL_OR_EMPTY_NAME_PARAMETER = "Null or Empty name parameter";
    public static final String ERROR_GETTING_OBJECT = "Error getting object";

    @Override
    public List<T> getObjects() {
        getLogger().debug("Getting all" + getGenericType().toString());

        List<T> objects = new ArrayList<T>();
        SearchResult searchResult = null;

        Filter searchFilter = new LdapSearchBuilder().addEqualAttribute(ATTR_OBJECT_CLASS, getObjectClass()).build();

        try {
            searchResult = getAppInterface().search(getBaseDn(), SearchScope.ONE, searchFilter);
            getLogger().info("Got" + getGenericType().toString());
        } catch (LDAPSearchException ldapEx) {
            String loggerMsg = String.format("Error searching for %s - {}",getGenericType().toString(),ldapEx);
            getLogger().error(loggerMsg);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                objects.add(getEntry(entry));
            }
        }

        return objects;
    }

    @Override
    public void addObject(T object) {
        if (object == null) {
            getLogger().error(ERROR_GETTING_OBJECT);
            throw new IllegalArgumentException(ERROR_GETTING_OBJECT);
        }
        getLogger().info("Adding object: {}", object);
        Audit audit = Audit.log((Auditable)object).add();
        try {
            final LDAPPersister<T> persister = LDAPPersister.getInstance(getGenericType());
            persister.add(object, getAppInterface(), getBaseDn());
            audit.succeed();
            getLogger().info("Added: {}", object);
        } catch (final LDAPException e) {
            getLogger().error("Error adding object", e);
            audit.fail(e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    @Override
    public T getObject(String objectId, Filter searchFilter) {
        getLogger().debug("Doing search for " + objectId);
        if (StringUtils.isBlank(objectId)) {
            getLogger().error(NULL_OR_EMPTY_ID_PARAMETER);
            getLogger().info("Invalid parameter.");
            throw new IllegalArgumentException("Invalid parameter.");
        }

        T object;
        try {
            object = getSingleObject(searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error(ERROR_GETTING_OBJECT, e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Found - {}", object);

        return object;
    }

    @Override
    public void updateObject(T object) {
        if (object == null || StringUtils.isBlank(getUniqueId(object))) {
            getLogger().error(ERROR_GETTING_OBJECT);
            getLogger().info("Invalid parameter.");
            throw new IllegalArgumentException("Missing argument on update");
        }
        Audit audit = Audit.log((Auditable)object).modify();

        try {
            LDAPPersister<T> persister = LDAPPersister.getInstance(getGenericType());
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
    public void deleteObject(String objectId, Filter searchFilter) {
        if (StringUtils.isBlank(objectId)) {
            getLogger().error(NULL_OR_EMPTY_ID_PARAMETER);
            throw new IllegalArgumentException(
                "Null or Empty id parameter.");
        }
        T object = getObject(objectId, searchFilter);
        getLogger().debug("Deleting: {}", object);
        final String dn = object.getUniqueId();
        final Audit audit = Audit.log((Auditable)object).delete();
        deleteEntryAndSubtree(dn, audit);
        audit.succeed();
        getLogger().debug("Deleted: {}", object);
    }

    private T getSingleObject(Filter searchFilter) throws LDAPPersistException {
        SearchResultEntry entry = this.getSingleEntry(getBaseDn(), SearchScope.ONE, searchFilter, getSearchAttributes());
        if (entry == null) {
            return null;
        }
        T object = (T)LDAPPersister.getInstance(getGenericType()).decode(entry);
        return object;
    }

    @Override
    public String getBaseDn(){
        throw new NotImplementedException();
    }

    @Override
    public String[] getSearchAttributes(){
        throw new NotImplementedException();
    }

    @Override
    public String getObjectClass(){
        throw new NotImplementedException();
    }

    @Override
    public String getUniqueId(T object){
        throw new NotImplementedException();
    }

    @Override
    public T getEntry(SearchResultEntry entry){
        throw new NotImplementedException();
    }

    @Override
    public Class getGenericType(){
        throw new NotImplementedException();
    }

}

