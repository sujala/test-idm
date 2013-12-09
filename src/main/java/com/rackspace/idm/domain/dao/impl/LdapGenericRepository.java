package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.DeleteNullValues;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.DaoGetEntityType;
import com.rackspace.idm.domain.dao.GenericDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.Auditable;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.StalePasswordException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    public static final int PAGE_SIZE = 1000;

    final private Class<T> entityType = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    @Autowired
    private LdapPaginatorRepository<T> paginator;

    @Override
    public Iterable<T> getObjects(Filter searchFilter) {
        return getObjects(searchFilter, getBaseDn(), SearchScope.SUB);
    }

    @Override
    public Iterable<T> getObjects(Filter searchFilter, String dn) {
        return getObjects(searchFilter, dn, SearchScope.SUB);
    }

    @Override
    public Iterable<T> getObjects(Filter searchFilter, String dn, SearchScope scope) {
        getLogger().debug("Getting all " + entityType.toString());

        return new LdapPagingIterator<T>(this, searchFilter, dn, scope);
    }

    @Override
    public PaginatorContext<T> getObjectsPaged(Filter searchFilter, String dn, SearchScope scope, int offset, int limit) {
        getLogger().debug("Getting " + entityType.toString() + " paged");

        SearchRequest searchRequest = new SearchRequest(dn, scope, searchFilter);
        PaginatorContext<T> context = paginator.createSearchRequest(getSortAttribute(), searchRequest, offset, limit);

        List<T> objects = new ArrayList<T>();
        SearchResult searchResult;

        try {
            searchResult = getAppInterface().search(searchRequest);
        } catch (LDAPSearchException ldapEx) {
            String loggerMsg = String.format("Error searching for %s - %s", entityType.toString(), searchFilter);
            getLogger().error(loggerMsg);
            return context;
        }

        paginator.createPage(searchResult, context);
        if (searchResult.getEntryCount() > 0) {
            objects = processSearchResult(context.getSearchResultEntryList());
        }

        context.setValueList(objects);

        return context;
    }

    @Override
    public PaginatorContext<T> getObjectsPaged(Filter searchFilter, int offset, int limit) {
        return getObjectsPaged(searchFilter, getBaseDn(), SearchScope.SUB, offset, limit);
    }

    private List<T> processSearchResult(List<SearchResultEntry> searchResultList) {
        List<T> objects = new ArrayList<T>();
        for (SearchResultEntry entry : searchResultList) {
            getLogger().debug("Getting % entry", entityType.toString());

            T entity = null;
            try {
                entity = (T) LDAPPersister.getInstance(getEntityTypeFromEntry(entry)).decode(entry);
                doPostEncode(entity);
                objects.add(entity);
            } catch (LDAPPersistException e) {
                String loggerMsg = String.format("Error converting entity for %s - {}", entityType.toString());
                getLogger().error(loggerMsg);
            }
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
            final LDAPPersister<T> persister = (LDAPPersister<T>) LDAPPersister.getInstance(object.getClass());
            doPreEncode(object);
            persister.add(object, getAppInterface(), dn);
            audit.succeed();
            getLogger().info("Added: {}", object);
        } catch (final LDAPException e) {
            getLogger().error("Error adding object", e);
            audit.fail(e.getMessage());
            switch (e.getResultCode().intValue()){
                case 68:
                    throw new DuplicateException(e.getMessage());
                default:
                    throw new IllegalStateException(e);
            }
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

    @Override
    public void doPreEncode(T object) {
    }

    @Override
    public void doPostEncode(T object) {
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
    public T getObject(Filter searchFilter, SearchScope scope) {
        return getObject(searchFilter, getBaseDn(), scope);
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
    public void updateObjectAsIs(T object) {
        if (object == null || StringUtils.isBlank(object.getUniqueId())) {
            getLogger().error(ERROR_GETTING_OBJECT);
            getLogger().info("Invalid parameter.");
            throw new IllegalArgumentException("Missing argument on update");
        }
        String loggerMsg = String.format("Updating object %s with id %s", entityType.toString(), object.getUniqueId());
        getLogger().debug(loggerMsg);
        Audit audit = Audit.log((Auditable)object).modify();

        try {
            doPreEncode(object);
            applyModificationsForAttributes(object, true);
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating {} - {}", object, ldapEx);
            audit.fail("Error updating");
            throwIfStalePassword(ldapEx, audit);
            throw new IllegalStateException(ldapEx.getMessage(), ldapEx);
        }
        audit.succeed();
        getLogger().info("Updated - {}", object);
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
            doPreEncode(object);
            applyModifications(object, false);
            applyModifications(object, true);

        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating {} - {}", object, ldapEx);
            audit.fail("Error updating");
            throwIfStalePassword(ldapEx, audit);
            throw new IllegalStateException(ldapEx.getMessage(), ldapEx);
        }
        audit.succeed();
        getLogger().info("Updated - {}", object);
    }

    /*
     * Ldap has a constrain violation that does not allow a user entity to update a password
     * to one of its last previous passwords. Updating to the current
     * password, ldap message is 'Password matches previous password' else if updating to a
     * previous password the message is 'Password match in history'.
     */
    void throwIfStalePassword(LDAPException ldapEx, Audit audit) {
        String[] stalePasswordMsg = config.getStringArray("stalePasswordMsg");
        if (ResultCode.CONSTRAINT_VIOLATION.equals(ldapEx.getResultCode())){
            for(String msg : stalePasswordMsg){
                if(msg.equals(ldapEx.getMessage())){
                    audit.fail(msg);
                    throw new StalePasswordException(msg);
                }
            }
        }
    }

    /**
     * Applies the changes for the specified attributes on the passed in object. If no attributes are passed, will operate on all attributes.
     * If a specified attribute has a Null/blank value, it will be removed.
     *
     * @param object
     * @param attributes
     * @throws LDAPPersistException
     */
    private void applyModificationsForAttributes(T object, boolean deleteNullAttributes, String... attributes) throws LDAPPersistException {
        Audit audit = Audit.log((Auditable)object).modify();
        LDAPPersister<T> persister = (LDAPPersister<T>) LDAPPersister.getInstance(object.getClass());

        List<Modification> mods = persister.getModifications(object, deleteNullAttributes, attributes);
        audit.modify(mods);
        if (mods.size() > 0) {
            persister.modify(object, getAppInterface(), null, deleteNullAttributes, attributes);
        }
    }

    private void applyModifications(T object, boolean deleteNullAttributes) throws LDAPPersistException {
        String[] attributes = getLDAPFieldAttributes(object, deleteNullAttributes);
        /*
        This method must only call the applyModificationsForAttributes(object, deleteNullAttributes, attributes); if
        there is at least one attribute because the purpose of the getLDAPFieldAttributes(object, deleteNullAttributes)
        method is to explicitly limit which attributes should be updated. If that method determines that no attributes
        should be updated, then none should be. The applyModificationsForAttributes will analyze ALL attributes if
        no attributes are provided.
         */
        if (attributes.length > 0) {
            applyModificationsForAttributes(object, deleteNullAttributes, attributes);
        }
    }

    /**
     * Applies logic to determine whether or not to include a particular field for update.
     *
     *
     * 1. If deleteNullAttributes = true, a given attribute is only returned if the property is annotated
     * with @DeleteNullValues AND the property is not a string with a null/whitespace value
     *
     * 2. If deleteNullAttributes = false, a given attribute is only returned if the property is NOT annotated
     * with @DeleteNullValues AND the property is not a string with a null/whitespace value
     *
     * @param object
     * @param deleteNullAttributes
     * @return
     */
    private String[] getLDAPFieldAttributes(T object, boolean deleteNullAttributes) {
        List<String> attributes = new ArrayList<String>();
        for (Field field : getDeclaredFields(object.getClass())) {
            boolean hasDeleteNullValueAnnotation = false;
            String attribute = null;
            for (Annotation annotation : field.getAnnotations()) {
                if (annotation.annotationType() == DeleteNullValues.class) {
                    hasDeleteNullValueAnnotation = true;
                }
                if (annotation.annotationType() == LDAPField.class) {
                    LDAPField ldapField = (LDAPField) annotation;
                    attribute = ldapField.attribute();
                }
            }
            if (deleteNullAttributes) {
                if (hasDeleteNullValueAnnotation) {
                    if(shouldAddAttribute(object, field)){
                        attributes.add(attribute);
                    }
                }
            } else {
                if (!hasDeleteNullValueAnnotation) {
                    if (shouldAddAttribute(object, field)){
                        attributes.add(attribute);
                    }
                }
            }
        }
        return attributes.toArray(new String[attributes.size()]);
    }

    private List<Field> getDeclaredFields(Class<?> type) {
        List<Field> result = new ArrayList<Field>();
        Collections.addAll(result, type.getDeclaredFields());

        if (type.getSuperclass() != null) {
            result.addAll(getDeclaredFields(type.getSuperclass()));
        }

        return result;
    }

    /**
     * Fields coming into this method come from the object using reflection.
     * So, the getProperty should always find the attribute. Blank strings are
     * a constrain violation in the directory for an update hence why the fields
     * need to be ignored. If it fails to get the property the method returns true
     * keeping old functionality which makes it save to ignore any exceptions.
     */
    private boolean shouldAddAttribute(T object, Field field) {
        try {
            if (field.getType() != String.class) {
                return true;
            } else {
                String value = BeanUtils.getProperty(object, field.getName());
                return StringUtils.isNotBlank(value);
            }
        } catch (Exception e) {
            return true;
        }
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
    public void softDeleteObject(T object) {
        getLogger().info("SoftDeleting object - {}", object);
        try {
            String oldRdn = object.getUniqueId();
            if(oldRdn == null) {
                getLogger().error("Error soft deleting object");
                throw new IllegalStateException();
            }

            List<String> tokens = Arrays.asList(oldRdn.split(","));

            String newRdn = tokens.get(0);

            getAppInterface().modifyDN(oldRdn, newRdn, true, getSoftDeletedBaseDn());
        } catch (LDAPException e) {
            getLogger().error("Error soft deleting object", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        getLogger().info("SoftDeleted object - {}", object);
    }

    @Override
    public void unSoftDeleteObject(T object) {
        getLogger().info("UnSoftDeleting object - {}", object);
        try {
            String oldRdn = object.getUniqueId();
            if(oldRdn == null) {
                getLogger().error("Error soft deleting object");
                throw new IllegalStateException();
            }

            List<String> tokens = Arrays.asList(oldRdn.split(","));

            String newRdn = tokens.get(0);

            getAppInterface().modifyDN(oldRdn, newRdn, true, getBaseDn());
        } catch (LDAPException e) {
            getLogger().error("Error soft deleting object", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
        getLogger().info("UnSoftDeleted object - {}", object);
    }

    @Override
    public String[] getSearchAttributes() {
        return ATTR_DEFAULT_SEARCH_ATTRIBUTES;
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
        SearchResultEntry entry = this.getSingleEntry(dn, scope, searchFilter, getSearchAttributes());
        if (entry == null) {
            return null;
        }

        T object = (T) LDAPPersister.getInstance(getEntityTypeFromEntry(entry)).decode(entry);
        doPostEncode(object);
        return object;
    }

    private Class getEntityTypeFromEntry(SearchResultEntry entry) {
        Class type = entityType;
        if(this instanceof DaoGetEntityType) {
            type = ((DaoGetEntityType) this).getEntityType(entry);
        }
        return type;
    }

    @Override
    public String getBaseDn(){
        throw new NotImplementedException();
    }

    @Override
    public String getSoftDeletedBaseDn() {
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

