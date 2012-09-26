package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.converter.cloudv20.DomainConverterCloudV20;
import com.rackspace.idm.domain.entity.Domains;
import com.rackspace.idm.domain.entity.ScopeAccess;
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

public class CloudIdentityAdminAccessibility extends CloudUserAccessibility {

    public CloudIdentityAdminAccessibility(TenantService tenantService, DomainService domainService, AuthorizationService authorizationService, UserService userService, ScopeAccess callerScopeAccess) {
        super(tenantService, domainService, authorizationService, userService, callerScopeAccess);
    }

    public Domains getAccessibleDomainsByScopeAccess(ScopeAccess userScopeAccess) {
        if (isIdentityAdmin(callerScopeAccess) || isServiceAdmin(callerScopeAccess)) {
            Domains domains = getAccessibleDomainsByScopeAccessForUser(userScopeAccess);
            return domains;
        }else {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
    }

}
