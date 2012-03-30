package com.rackspace.idm.api.converter;

import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;
import org.springframework.stereotype.Component;

@Component
public class TenantRoleConverter {


    /*
         com.rackspace.idm.domain.entity.TenantRole
     */

    public TenantRole fromClientRole(ClientRole clientRole, String userId, String tenantId){
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName(clientRole.getName());
        tenantRole.setClientId(clientRole.getClientId());
        tenantRole.setRoleRsId(clientRole.getId());
        tenantRole.setUserId(userId);
        tenantRole.setTenantIds(new String[]{tenantId});
        return tenantRole;

    }

}
