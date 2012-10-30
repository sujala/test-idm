package com.rackspace.idm.domain.dao;


import com.rackspace.idm.domain.entity.Question;
import com.rackspace.idm.domain.entity.Questions;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchResultEntry;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/26/12
 * Time: 3:22 PM
 * To change this template use File | Settings | File Templates.
 */
public interface GenericDao<T> {
    List<T> getObjects();
    void addObject(T object);
    T getObject(String objectId, Filter searchFilter);
    void updateObject(T object);
    void deleteObject(String objectId, Filter searchFilter);
    String getBaseDn();
    String[] getSearchAttributes();
    String getObjectClass();
    String getUniqueId(T object);
    T getEntry(SearchResultEntry entry);
    Class getGenericType();
}
