package com.rackspace.idm.domain.dao;


import com.rackspace.idm.domain.entity.PaginatorContext;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/26/12
 * Time: 3:22 PM
 * To change this template use File | Settings | File Templates.
 */
public interface GenericDao<T> {
    void addObject(T object);
    void addObject(String dn, T object);

    /**
     * Retrieve a single object by provided DN. Return null if no object was found.
     *
     * @param dn
     * @throws IllegalArgumentException If supplied dn is null.
     *
     * @return
     */
    T getObject(DN dn);

    T getObject(Filter searchFilter);
    T getObject(Filter searchFilter, SearchScope scope);
    T getObject(Filter searchFilter, String dn);
    T getObject(Filter searchFilter, String dn, SearchScope scope, String... searchAttributes);
    Iterable<T> getObjects(Filter searchFilter);
    Iterable<T> getObjects(Filter searchFilter, String dn);
    Iterable<T> getObjects(Filter searchFilter, String dn, SearchScope scope);
    PaginatorContext<T> getObjectsPaged(Filter searchFilter, String dn, SearchScope scope, int offset, int limit);
    PaginatorContext<T> getObjectsPaged(Filter searchFilter, int offset, int limit);
    int countObjects(Filter searchFilter);
    int countObjects(Filter searchFilter, String dn);
    int countObjects(Filter searchFilter, String dn, SearchScope scope);
    void updateObject(T object);

    /**
     * Updates the object in LDAP. The passed in object should fully reflect the state that should be saved in LDAP. Properties that are null will be removed from
     * LDAP.
     *
     * @param object
     */
    void updateObjectAsIs(T object);

    /**
     * Deletes the single object found by executing the search filter. The children of the object are also deleted.
     *
     * @throws com.rackspace.idm.exception.NotFoundException If no object is found by executing the search filter
     * @throws IllegalStateException If multiple objects are found by executing the search filter
     *
     * @param searchFilter
     */
    void deleteObject(Filter searchFilter);

    String[] getSearchAttributes();
    String getBaseDn();
    String getLdapEntityClass();
    String getNextId();
    String addLdapContainer(String dnString, String containerName);
    String addOrganizationalUnit(String partialDnString, String ou);
    void doPreEncode(T object);

    /**
     * After retrieving and decoding the ldap entry into the appropriate object, this method is called to do any required
     * post processing of the result prior to returning
     *
     * @param object
     */
    void doPostEncode(T object);

    /**
     * Deletes the single object. If the object does not exist, no error is thrown.
     *
     * @param object
     */
    void deleteObject(T object);
    String getSortAttribute();
}
