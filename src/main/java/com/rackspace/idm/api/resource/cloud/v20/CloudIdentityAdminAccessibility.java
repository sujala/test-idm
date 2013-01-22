package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.ObjectFactory;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/25/12
 * Time: 3:03 PM
 * To change this template use File | Settings | File Templates.
 */

public class CloudIdentityAdminAccessibility extends CloudUserAccessibility {

    public CloudIdentityAdminAccessibility() {
        super();
    }

    public CloudIdentityAdminAccessibility(TenantService tenantService, DomainService domainService,
                                           AuthorizationService authorizationService, UserService userService,
                                           Configuration config, ObjectFactory objFactory, ScopeAccess callerScopeAccess) {
        super(tenantService, domainService, authorizationService, userService, config, objFactory,callerScopeAccess);
    }

    public boolean hasAccess(User user){
        List<TenantRole> tenantRolesForUser = tenantService.getTenantRolesForUser(caller);
        Boolean isIdentityAdmin = userContainsRole(tenantRolesForUser, config.getString("cloudAuth.adminRole"));
        Boolean isServiceAdmin = userContainsRole(tenantRolesForUser, config.getString("cloudAuth.serviceAdminRole"));

        if(isIdentityAdmin || isServiceAdmin){
            return true;
        }
        return false;
    }
}
