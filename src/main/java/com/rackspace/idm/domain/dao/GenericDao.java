package com.rackspace.idm.domain.dao;


import com.rackspace.idm.api.resource.pagination.PaginatorContext;
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
    T getObject(Filter searchFilter, String dn);
    T getObject(Filter searchFilter, String dn, SearchScope scope);
    List<T> getObjects(Filter searchFilter);
    List<T> getObjects(Filter searchFilter, String dn);
    List<T> getObjects(Filter searchFilter, String dn, SearchScope scope);
    PaginatorContext<T> getObjectsPaged(Filter searchFilter, String dn, SearchScope scope, int offset, int limit);
    PaginatorContext<T> getObjectsPaged(Filter searchFilter, int offset, int limit);
    void updateObject(T object);
    void deleteObject(Filter searchFilter);
    void softDeleteObject(T object);
    void unSoftDeleteObject(T object);
    String getBaseDn();
    String getSoftDeletedBaseDn();
    String getLdapEntityClass();
    String getNextId();
    String addLdapContainer(String dnString, String containerName);
    void doPreEncode(T object);
    void doPostEncode(T object);

    void deleteObject(T object);
    String getSortAttribute();
}
