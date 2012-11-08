package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.entity.ServiceApi;
import org.springframework.stereotype.Component;


/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/22/12
 * Time: 4:43 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class LdapServiceApiRepository extends LdapGenericRepository<ServiceApi> {
    public String getBaseDn(){
        return BASEURL_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_BASEURL;
    }
}
