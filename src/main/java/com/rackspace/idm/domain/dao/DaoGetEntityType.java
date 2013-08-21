package com.rackspace.idm.domain.dao;

import com.unboundid.ldap.sdk.SearchResultEntry;

/**
 * Created with IntelliJ IDEA.
 * User: rmlynch
 * Date: 8/16/13
 * Time: 3:28 PM
 * To change this template use File | Settings | File Templates.
 */
public interface DaoGetEntityType {
    Class getEntityType(SearchResultEntry entry);
}
