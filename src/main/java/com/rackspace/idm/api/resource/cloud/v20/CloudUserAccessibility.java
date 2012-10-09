package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.converter.cloudv20.DomainConverterCloudV20;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;

import java.util.ArrayList;
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

    private AuthorizationService authorizationService;

    private UserService userService;

    private EndpointService endpointService;

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
        Domains domains = new Domains();
        List<Tenant> tenants = tenantService.getTenantsForScopeAccessByTenantRoles(scopeAccessByAccessToken);
        if(tenants == null || tenants.size() == 0){
            return domains;
        }
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
        if(domain == null){
            return domains;
        }
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

    public List<OpenstackEndpoint> getAccessibleDomainEndpoints(List<OpenstackEndpoint> endpoints, List<Tenant> tenants) {
        List<OpenstackEndpoint> openstackEndpoints = new ArrayList<OpenstackEndpoint>();
        if(endpoints.isEmpty() || tenants.isEmpty()){
            return openstackEndpoints;
        }
        for(OpenstackEndpoint openstackEndpoint : endpoints){
            String tenantId = openstackEndpoint.getTenantId();
            for(Tenant tenant : tenants){
                if(tenantId.equals(tenant.getTenantId())){
                    openstackEndpoints.add(openstackEndpoint);
                }
            }

        }

        return openstackEndpoints;
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
