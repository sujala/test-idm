package com.rackspace.idm.domain.dao;


import com.unboundid.ldap.sdk.Filter;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/26/12
 * Time: 3:22 PM
 * To change this template use File | Settings | File Templates.
 */
public interface GenericDao<T> {
    List<T> getObjects(Filter searchFilter);
    void addObject(T object);
    void addObject(T object, String dn);
    T getObject(Filter searchFilter);
    void updateObject(T object);
    void deleteObject(Filter searchFilter);
    String getBaseDn();
    String getLdapEntityClass();
    String getNextId();
    String addLdapContainer(String dnString, String containerName);
}
