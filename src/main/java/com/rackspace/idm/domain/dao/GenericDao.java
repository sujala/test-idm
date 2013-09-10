package com.rackspace.idm.domain.dao;


import com.rackspace.idm.domain.entity.PaginatorContext;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;

import java.util.List;

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
    T getObject(Filter searchFilter);
    T getObject(Filter searchFilter, SearchScope scope);
    T getObject(Filter searchFilter, String dn);
    T getObject(Filter searchFilter, String dn, SearchScope scope);
    Iterable<T> getObjects(Filter searchFilter);
    Iterable<T> getObjects(Filter searchFilter, String dn);
    Iterable<T> getObjects(Filter searchFilter, String dn, SearchScope scope);
    PaginatorContext<T> getObjectsPaged(Filter searchFilter, String dn, SearchScope scope, int offset, int limit);
    PaginatorContext<T> getObjectsPaged(Filter searchFilter, int offset, int limit);
    void updateObject(T object);

    /**
     * Deletes the single object found by executing the search filter. The children of the object are also deleted.
     *
     * @throws com.rackspace.idm.exception.NotFoundException If no object is found by executing the search filter
     * @throws IllegalStateException If multiple objects are found by executing the search filter
     *
     * @param searchFilter
     */
    void deleteObject(Filter searchFilter);

    void softDeleteObject(T object);
    void unSoftDeleteObject(T object);
    String[] getSearchAttributes();
    String getBaseDn();
    String getSoftDeletedBaseDn();
    String getLdapEntityClass();
    String getNextId();
    String addLdapContainer(String dnString, String containerName);
    void doPreEncode(T object);
    void doPostEncode(T object);

    /**
     * Deletes the single object. If the object does not exist, no error is thrown.
     *
     * @param object
     */
    void deleteObject(T object);
    String getSortAttribute();
}
