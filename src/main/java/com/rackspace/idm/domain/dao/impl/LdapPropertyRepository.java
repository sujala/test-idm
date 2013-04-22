package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.PropertyDao;
import com.rackspace.idm.domain.entity.Property;
import com.unboundid.ldap.sdk.Filter;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/29/12
 * Time: 1:18 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class LdapPropertyRepository extends LdapGenericRepository<Property> implements PropertyDao {

    public String getBaseDn(){
        return PROPERTY_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_PROPERTY;
    }

    @Override
    public Property getProperty(String name) {
        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_PROPERTY)
                .addEqualAttribute(LdapRepository.ATTR_NAME, name).build();
        return getObject(searchFilter);
    }
}
