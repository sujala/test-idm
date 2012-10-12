package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.ObjectFactory;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/25/12
 * Time: 3:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class CloudUserAdminAccessibility extends CloudUserAccessibility {

    public CloudUserAdminAccessibility(TenantService tenantService, DomainService domainService,
                                       AuthorizationService authorizationService, UserService userService,
                                       Configuration config, ObjectFactory objFactory, ScopeAccess callerScopeAccess) {
        super(tenantService, domainService, authorizationService, userService, config, objFactory, callerScopeAccess);
    }

    public boolean hasAccess(ScopeAccess scopeAccess){
        User caller = userService.getUserByScopeAccess(callerScopeAccess);
        User user = userService.getUserByScopeAccess(scopeAccess);
        if(caller.getId().equals(user.getId())){
            return true;
        }
        Boolean isDefaultUser = userContainsRole(scopeAccess, config.getString("cloudAuth.userRole"));
        if(isDefaultUser && caller.getDomainId().equals(user.getDomainId())){
            return true;
        }
        return false;
    }
}

