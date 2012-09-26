package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.converter.cloudv20.DomainConverterCloudV20;
import com.rackspace.idm.domain.entity.Domains;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.ForbiddenException;
import org.springframework.stereotype.Component;

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

    public Domains getAccessibleDomainsByScopeAccess(ScopeAccess userScopeAccess) {
        Domains domains;
        User caller = getCallerByScopeAccess(callerScopeAccess);
        User user = getCallerByScopeAccess(userScopeAccess);
        //User Admin
        if (caller.getId().equals(user.getId()) || (isDefaultUser(userScopeAccess) && caller.getDomainId().equals(user.getDomainId()))) {
            domains = getAccessibleDomainsByScopeAccessForUser(userScopeAccess);
        } else {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
        return domains;
    }

}
