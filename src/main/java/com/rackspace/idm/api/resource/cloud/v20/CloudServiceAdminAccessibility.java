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

public class CloudServiceAdminAccessibility extends CloudUserAccessibility {

    public CloudServiceAdminAccessibility(TenantService tenantService, DomainService domainService, AuthorizationService authorizationService, UserService userService, ScopeAccess callerScopeAccess) {
        super(tenantService, domainService, authorizationService, userService, callerScopeAccess);
    }

    public boolean hasAccess(ScopeAccess scopeAccess){
        Boolean isIdentityAdmin = authorizationService.authorizeCloudIdentityAdmin(callerScopeAccess);
        Boolean isServiceAdmin = authorizationService.authorizeCloudServiceAdmin(callerScopeAccess);
        if(isIdentityAdmin || isServiceAdmin){
            return true;
        }
        return false;
    }

}
