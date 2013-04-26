package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.Property;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/30/12
 * Time: 2:16 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PropertyDao {
    Property getProperty(String name);
}
