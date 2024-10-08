package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.DeleteNullValues;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.DaoGetEntityType;
import com.rackspace.idm.domain.dao.GenericDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.*;
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
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

public class LdapGenericRepository<T extends UniqueId> extends LdapRepository implements GenericDao<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapGenericRepository.class);

    public static final String ERROR_GETTING_OBJECT = "Error getting object";
    public static final int PAGE_SIZE = 1000;

    public static final String USE_VLV_SSS_OPTIMIZATION_PROP_NAME = "feature.optimize.vlv.sss.usage.enabled";
    public static final boolean USE_VLV_SSS_OPTIMIZATION_DEFAULT_VALUE = false;
    public static final String ATTRIBUTE_DXENTRYCOUNT = "dxentrycount";

    final private Class<T> entityType = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    @Autowired
    protected LdapPaginatorSearcher<T> paginator;

    @Autowired
    private RequestContextHolder requestContextHolder;

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

        if (optimizeUseOfVlvSss()) {
            try {
                return getUnpagedUnsortedObjects(searchFilter,dn, scope);
            } catch (LDAPSearchException ldapEx) {
                if (ldapEx.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
                    //fallback to the paging mechanism to retrieve results. This is not an error and is expected when result size is
                    // greater than threshold.
                    getLogger().info(String.format("Server reported search size exceeded threshold. Switched to paged results for '%s' with searchFilter '%s'", entityType.toString(), searchFilter));
                }
                else {
                    //the pre-refactor code returned a fresh context with no results when received an error, so just do the same... May
                    //need to revisit this...
                    String loggerMsg = String.format("Encountered error searching for %s - %s using unsorted/paged method. Falling back to original code.", entityType.toString(), searchFilter);
                    getLogger().error(loggerMsg);
                }
                return new LdapPagingIterator<T>(this, searchFilter, dn, scope, identityConfig.getReloadableConfig().getMaxDirectoryPageSize());
            }
        } else {
            return new LdapPagingIterator<T>(this, searchFilter, dn, scope, identityConfig.getReloadableConfig().getMaxDirectoryPageSize());
        }
    }

    private boolean optimizeUseOfVlvSss() {
        return config.getBoolean(USE_VLV_SSS_OPTIMIZATION_PROP_NAME, USE_VLV_SSS_OPTIMIZATION_DEFAULT_VALUE);
    }

    protected List<T> getUnpagedUnsortedObjects(Filter searchFilter, String dn, SearchScope scope) throws LDAPSearchException {
        return getUnpagedUnsortedObjects(searchFilter, dn, scope, identityConfig.getReloadableConfig().getMaxDirectoryPageSize());
    }

    protected List<T> getUnpagedUnsortedObjects(Filter searchFilter, String dn, SearchScope scope, int maxResult) throws LDAPSearchException {
        if(maxResult > identityConfig.getReloadableConfig().getMaxDirectoryPageSize()) {
            getLogger().debug("Aborting search request due to requested max results of {} exceeding maximum limit of {}", maxResult, identityConfig.getReloadableConfig().getMaxDirectoryPageSize());
            throw new IllegalArgumentException("Max results must not exceed " + identityConfig.getReloadableConfig().getMaxDirectoryPageSize());
        }
        getLogger().debug(String.format("Getting all %s unpaged and unsorted objects", entityType));

        SearchResult searchResult;
        SearchRequest searchRequest = new SearchRequest(dn, scope, searchFilter);
        List<T> objects = new ArrayList<T>();
        searchRequest.setSizeLimit(maxResult);
        searchResult = getAppInterface().search(searchRequest);

        if (searchResult.getEntryCount() > 0) {
            objects = processSearchResult(searchResult.getSearchEntries());
        }
        return objects;
    }

    @Override
    public PaginatorContext<T> getObjectsPaged(Filter searchFilter, String dn, SearchScope scope, int offset, int limit) {
        getLogger().debug("Getting " + entityType.toString() + " paged");

        SearchRequest searchRequest = new SearchRequest(dn, scope, searchFilter, getSearchAttributes());
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

    protected List<T> processSearchResult(List<SearchResultEntry> searchResultList) {
        List<T> objects = new ArrayList<T>();
        for (SearchResultEntry entry : searchResultList) {
            getLogger().debug("Getting % entry", entityType.toString());

            T entity = null;
            try {
                entity = (T) LDAPPersister.getInstance(getEntityTypeFromEntry(entry)).decode(entry);
                doPostEncode(entity);
                objects.add(entity);
            } catch (LDAPPersistException e) {
                String dn = entry != null ? entry.getDN() : null;
                String loggerMsg = String.format("Error converting entity with DN '%s' to class '%s'", dn, entityType.toString());
                getLogger().error(loggerMsg, e);
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
            addMetadata(object);
            final LDAPPersister<T> persister = (LDAPPersister<T>) LDAPPersister.getInstance(object.getClass());
            doPreEncode(object);

            //this is an approximation of the date this occurred
            LDAPResult result = persister.add(object, getAppInterface(), dn);
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

    private void addMetadata(T object) {
        try {
            if (object instanceof Metadata) {
                HashSet<String> metadata = new HashSet<String>();
                metadata.add("modifiersId: rsId=" + getCallerId());
                metadata.add("requestId: " + MDC.get(Audit.GUUID));
                ((Metadata)object).setMetadata(metadata);
            } else {
                String errMsg = String.format("Object is not an instance of Metadata: %s", entityType.getClass());
                getLogger().warn(errMsg);
            }
        } catch (Exception ex) {
            getLogger().warn("An error was encountered adding metadata to entry being created/modificed. Skipping adding the metadata.", ex);
        }
    }

    private String getCallerId() {
        if (requestContextHolder != null &&
                requestContextHolder.getRequestContext() != null &&
                requestContextHolder.getRequestContext().getSecurityContext() != null &&
                requestContextHolder.getRequestContext().getSecurityContext().getCallerToken() != null) {
            ScopeAccess callerToken = requestContextHolder.getRequestContext().getSecurityContext().getCallerToken();
            return ((BaseUserToken) callerToken).getIssuedToUserId();
        } else {
            return "not-available";
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
    public String addOrganizationalUnit(String partialDnString, String ou) {
        SearchResultEntry entry = getLdapOrganizationalUnit(partialDnString, ou);
        if (entry == null) {
            Audit audit = Audit.log(String.format("Adding organizational unit: %s", ou));
            List<Attribute> attributes = new ArrayList<Attribute>();
            attributes.add(new Attribute(ATTR_OBJECT_CLASS, OBJECTCLASS_ORGANIZATIONAL_UNIT));
            attributes.add(new Attribute(ATTR_OU, ou));
            Attribute[] attributeArray = attributes.toArray(new Attribute[0]);
            String dn = new LdapDnBuilder(partialDnString).addAttribute(ATTR_OU, ou).build();
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

    protected SearchResultEntry getLdapContainer(String dn, String containerName) {
        Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_RACKSPACE_CONTAINER)
                .addEqualAttribute(ATTR_NAME, containerName).build();
        return getSingleEntry(dn, SearchScope.ONE, filter);
    }

    private SearchResultEntry getLdapOrganizationalUnit(String dn, String ouName) {
        Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_ORGANIZATIONAL_UNIT)
                .addEqualAttribute(ATTR_OU, ouName).build();
        return getSingleEntry(dn, SearchScope.ONE, filter);
    }

    public T getObject(DN dn) {
        Validate.notNull(dn);

        String loggerMsg = String.format("Doing search for %s", entityType.toString());
        getLogger().debug(loggerMsg);

        T object;
        try {
            SearchResultEntry entry = getAppInterface().getEntry(dn.toString(), getSearchAttributes());

            if (entry == null) {
                return null;
            }

            object = (T) LDAPPersister.getInstance(getEntityTypeFromEntry(entry)).decode(entry);
            doPostEncode(object);
        } catch (LDAPException e) {
            getLogger().error(ERROR_GETTING_OBJECT, e);
            throw new IllegalStateException(e);
        }

        getLogger().debug("Found - {}", object);
        return object;
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
    public T getObject(Filter searchFilter, String dn, SearchScope scope, String... searchAttributes) {
        String loggerMsg = String.format("Doing search for %s", entityType.toString());
        getLogger().debug(loggerMsg);

        T object;
        try {
            object = getSingleObject(dn, scope, searchFilter, searchAttributes);
        } catch (LDAPPersistException e) {
            getLogger().error(ERROR_GETTING_OBJECT, e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Found - {}", object);

        return object;
    }

    @Override
    public int countObjects(Filter searchFilter) {
        return countObjects(searchFilter, getBaseDn(), SearchScope.SUB);
    }

    @Override
    public int countObjects(Filter searchFilter, String dn) {
        return countObjects(searchFilter, dn, SearchScope.SUB);
    }

    @Override
    public int countObjects(Filter searchFilter, String dn, SearchScope scope) {
        String loggerMsg = String.format("Doing search to count %s", entityType.toString());
        getLogger().debug(loggerMsg);

        SearchResult result = this.search(dn, scope, searchFilter, ATTRIBUTE_DXENTRYCOUNT);

        String entryCountString = "0";
        /*
         Add null check logic. See https://rackspace.slack.com/archives/G6BFMR000/p1486162245003267 for discussion. Seems
         in some cases when 0 should be returned, no results are returned at all which results in a NPE without the
         check
         */
        if (result != null && result.getSearchEntries() != null && result.getSearchEntries().size() > 0) {
            entryCountString = result.getSearchEntries().get(0).getAttribute(ATTRIBUTE_DXENTRYCOUNT).getValues()[0];
        }
        getLogger().debug("Found - {} entries", entryCountString);

        try {
            return Integer.valueOf(entryCountString);
        } catch(NumberFormatException e) {
            throw new IllegalStateException(e);
        }
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
            addMetadata(object);
            doPreEncode(object);
            List<Modification> mods = getModificationsForAttributes(object, true);
            audit.modify(mods);
            applyModifictations(object, mods);
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
            addMetadata(object);
            doPreEncode(object);
            List<Modification> mods = new ArrayList<>();
            mods.addAll(getModifications(object, false));
            mods.addAll(getModifications(object, true));
            audit.modify(mods);
            applyModifictations(object, mods);
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
    private List<Modification> getModificationsForAttributes(T object, boolean deleteNullAttributes, String... attributes) throws LDAPPersistException {
        LDAPPersister<T> persister = (LDAPPersister<T>) LDAPPersister.getInstance(object.getClass());
        return persister.getModifications(object, deleteNullAttributes, attributes);
    }

    private void applyModifictations(T object, List<Modification> mods) throws LDAPPersistException {
        if (mods.size() > 0) {
            try {
                if (object instanceof Metadata) {
                    List<Modification> allMods = new ArrayList<>();
                    allMods.addAll(mods);
                    allMods.addAll(getObjectClassModification(object, mods));
                    final ModifyRequest modifyRequest = new ModifyRequest(object.getUniqueId(), allMods);
                    LDAPResult ldapResult = getAppInterface().modify(modifyRequest);
                } else {
                    final ModifyRequest modifyRequest = new ModifyRequest(object.getUniqueId(), mods);
                    LDAPResult ldapResult = getAppInterface().modify(modifyRequest);
                }
            }
            catch (LDAPException le) {
                throw new LDAPPersistException(le);
            }
        }
    }

    private List<Modification> getModifications(T object, boolean deleteNullAttributes) throws LDAPPersistException {
        String[] attributes = getLDAPFieldAttributes(object, deleteNullAttributes);
        /*
        This method must only call the getModificationsForAttributes(object, deleteNullAttributes, attributes); if
        there is at least one attribute because the purpose of the getLDAPFieldAttributes(object, deleteNullAttributes)
        method is to explicitly limit which attributes should be updated. If that method determines that no attributes
        should be updated, then none should be. The getModificationsForAttributes will analyze ALL attributes if
        no attributes are provided.
         */
        if (attributes.length == 0) {
            return new ArrayList<>();
        } else {
            return getModificationsForAttributes(object, deleteNullAttributes, attributes);
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

    private List<Modification> getObjectClassModification(T object, List<Modification> mods) {
        List<Modification> result = new ArrayList<>();
        Set<String> objectClass = new HashSet<>();
        for (Field field : getDeclaredFields(object.getClass())) {
            for (Annotation annotation : field.getAnnotations()) {
                if (annotation.annotationType() == LDAPField.class) {
                    LDAPField ldapField = (LDAPField) annotation;
                    objectClass.addAll(Arrays.asList(ldapField.objectClass()));
                }
            }
        }
        //TODO: allow adding and removing auxiliary objectClasses baded on mods and readOnlyEntry
        result.add(new Modification(ModificationType.REPLACE, ATTR_OBJECT_CLASS, objectClass.toArray(new String[objectClass.size()])));
        return result;
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

    private T getSingleObject(String dn, SearchScope scope, Filter searchFilter, String... searchAttributes) throws LDAPPersistException {
        SearchResultEntry entry;
        if (searchAttributes != null && searchAttributes.length > 0) {
            entry = this.getSingleEntry(dn, scope, searchFilter, searchAttributes);
        } else {
            entry = this.getSingleEntry(dn, scope, searchFilter, getSearchAttributes());
        }
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

