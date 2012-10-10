package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.Domains;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.ForbiddenException;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/25/12
 * Time: 3:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class CloudUserAdminAccessibility extends CloudUserAccessibility {

    public CloudUserAdminAccessibility(TenantService tenantService, DomainService domainService, AuthorizationService authorizationService, UserService userService, ScopeAccess callerScopeAccess) {
        super(tenantService, domainService, authorizationService, userService, callerScopeAccess);
    }

    public boolean hasAccess(ScopeAccess userScopeAccess){
        User caller = userService.getUserByScopeAccess(callerScopeAccess);
        User user = userService.getUserByScopeAccess(userScopeAccess);
        if(caller.getId().equals(user.getId())){
            return true;
        }
        Boolean isDefaultUser = authorizationService.authorizeCloudUser(userScopeAccess);
        if(isDefaultUser && caller.getDomainId().equals(user.getDomainId())){
            return true;
        }
        return false;
    }
}
