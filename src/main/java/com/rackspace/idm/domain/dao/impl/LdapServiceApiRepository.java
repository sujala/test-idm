package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.ServiceApiDao;
import com.rackspace.idm.domain.entity.ServiceApi;
import com.unboundid.ldap.sdk.Filter;
import org.springframework.stereotype.Component;


/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/22/12
 * Time: 4:43 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class LdapServiceApiRepository extends LdapGenericRepository<ServiceApi> implements ServiceApiDao {
    public String getBaseDn(){
        return BASEURL_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_BASEURL;
    }

    public String getSortAttribute() {
        return ATTR_ID;
    }

    @Override
    public Iterable<ServiceApi> getServiceApis() {
        return getObjects(createServiceApiFilter());
    }

    private Filter createServiceApiFilter() {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_BASEURL)
                .addPresenceAttribute(ATTR_VERSION_ID)
                .addPresenceAttribute(ATTR_OPENSTACK_TYPE).build();
    }
}

