package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.converter.cloudv20.DomainConverterCloudV20;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/25/12
 * Time: 2:37 PM
 * To change this template use File | Settings | File Templates.
 */

public class CloudUserAccessibility {

    public static final String NOT_AUTHORIZED = "Not authorized.";

    public ScopeAccess callerScopeAccess;

    private TenantService tenantService;

    private DomainService domainService;

    private DomainConverterCloudV20 domainConverterCloudV20;

    private AuthorizationService authorizationService;

    private UserService userService;

    public CloudUserAccessibility(TenantService tenantService, DomainService domainService,
                                  AuthorizationService authorizationService, UserService userService,
                                  ScopeAccess callerScopeAccess) {
        this.tenantService = tenantService;
        this.domainService = domainService;
        this.authorizationService = authorizationService;
        this.userService = userService;
        this.callerScopeAccess = callerScopeAccess;
    }

    public Domains getAccessibleDomainsByScopeAccessForUser(ScopeAccess scopeAccessByAccessToken) {
        List<Tenant> tenants;
        tenants = tenantService.getTenantsForScopeAccessByTenantRoles(scopeAccessByAccessToken);
        Domains domains = new Domains();
        List<Domain> listDomains = domainService.getDomainsForTenants(tenants);
        for (Domain domain : listDomains) {
            domains.getDomain().add(domain);
        }
        return domains;
    }

    public Domains getAccessibleDomainsByScopeAccess(ScopeAccess scopeAccess){
        return null;
    }

    public Domains addUserDomainToDomains(User user, Domains domains){
        Domain domain = domainService.getDomain(user.getDomainId());
        domains.getDomain().add(domain);
        return domains;
    }

    public Domains removeDuplicateDomains(Domains domains){
        Domains noDup = new Domains();
        for(Domain domain : domains.getDomain()) {
            if(!noDup.getDomain().contains(domain)){
                noDup.getDomain().add(domain);
            }
        }
        return noDup;
    }

    public User getCallerByScopeAccess(ScopeAccess scopeAccess){
        return this.userService.getUserByScopeAccess(scopeAccess);
    }

    public Boolean isDefaultUser(ScopeAccess userScope){
        return this.authorizationService.authorizeCloudUser(userScope);
    }

    public Boolean isUserAdmin(ScopeAccess userScope){
        return this.authorizationService.authorizeCloudUserAdmin(userScope);
    }

    public Boolean isIdentityAdmin(ScopeAccess userScope){
        return this.authorizationService.authorizeCloudIdentityAdmin(userScope);
    }

    public Boolean isServiceAdmin(ScopeAccess userScope){
        return this.authorizationService.authorizeCloudServiceAdmin(userScope);
    }

    public void setScopeAccess(ScopeAccess scopeAccess) {
        this.callerScopeAccess = scopeAccess;
    }
}
