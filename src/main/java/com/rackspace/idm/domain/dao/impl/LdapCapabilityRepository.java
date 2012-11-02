package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.entity.Capability;
import com.rackspace.idm.domain.entity.Question;
import org.springframework.stereotype.Component;



/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/22/12
 * Time: 4:43 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class LdapCapabilityRepository  extends LdapGenericRepository<Capability> {
    public String getBaseDn(){
        return CAPABILITY_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_CAPABILITY;
    }

    public String getNextCapabilityId() {
        return getNextId(NEXT_CAPABILITY_ID);
    }
}
