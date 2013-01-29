package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.openstack.docs.identity.api.v2.EndpointList;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.VersionForService;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/25/12
 * Time: 2:37 PM
 * To change this template use File | Settings | File Templates.
 */

public class CloudUserAccessibility {

    protected Configuration config;

    public static final String NOT_AUTHORIZED = "Not Authorized";

    protected TenantService tenantService;

    private DomainService domainService;

    protected AuthorizationService authorizationService;

    protected UserService userService;

    private ObjectFactory objFactory;

    protected User caller;

    public CloudUserAccessibility() {
    }

    public CloudUserAccessibility(TenantService tenantService, DomainService domainService,
                                  AuthorizationService authorizationService, UserService userService,
                                  Configuration config, ObjectFactory objFactory, ScopeAccess scopeAccess) {
        this.tenantService = tenantService;
        this.domainService = domainService;
        this.authorizationService = authorizationService;
        this.userService = userService;
        this.objFactory = objFactory;
        this.config = config;
        this.caller = userService.getUserByScopeAccess(scopeAccess);
    }

    public Domains getAccessibleDomainsForUser(User user) {
        Domains domains = new Domains();
        List<Tenant> tenants = tenantService.getTenantsForUserByTenantRoles(user);
        if (tenants == null || tenants.size() == 0) {
            return domains;
        }
        List<Domain> listDomains = domainService.getDomainsForTenants(tenants);
        for (Domain domain : listDomains) {
            domains.getDomain().add(domain);
        }
        return domains;
    }

    public Domains getAccessibleDomainsByUser(User user) {
        Domains domains;
        if (hasAccess(user)) {
            domains = getAccessibleDomainsForUser(user);
        } else {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
        return domains;
    }

    public Domains addUserDomainToDomains(User user, Domains domains) {
        Domain domain = domainService.getDomain(user.getDomainId());
        if (domain == null) {
            return domains;
        }
        domains.getDomain().add(domain);
        return domains;
    }

    public Domains removeDuplicateDomains(Domains domains) {
        if(domains == null){
            return new Domains();
        }

        HashMap<String, Domain> map = new HashMap<String, Domain>();
        for (Domain domain : domains.getDomain()) {
            if (!map.containsKey(domain.getDomainId())) {
                map.put(domain.getDomainId(), domain);
            }
        }
        Domains result = new Domains();
        result.getDomain().addAll(map.values());
        return result;
    }

    public List<OpenstackEndpoint> getAccessibleDomainEndpoints(List<OpenstackEndpoint> endpoints, List<Tenant> tenants, User user) {
        if(hasAccess(user)) {
            List<OpenstackEndpoint> openstackEndpoints = new ArrayList<OpenstackEndpoint>();
            if (endpoints.isEmpty() || tenants.isEmpty()) {
                return openstackEndpoints;
            }
            for (OpenstackEndpoint openstackEndpoint : endpoints) {
                String tenantId = openstackEndpoint.getTenantId();
                for (Tenant tenant : tenants) {
                    if (tenantId.equals(tenant.getTenantId())) {
                        openstackEndpoints.add(openstackEndpoint);
                    }
                }

            }
            return openstackEndpoints;
        } else {
            throw new ForbiddenException(NOT_AUTHORIZED);
        }
    }

    public EndpointList convertPopulateEndpointList(List<OpenstackEndpoint> endpoints) {
        EndpointList list = objFactory.createEndpointList();
        List<TenantRole> tenantRolesForUser = tenantService.getTenantRolesForUser(caller);
        Boolean isIdentityAdmin = userContainsRole(tenantRolesForUser, config.getString("cloudAuth.adminRole"));
        Boolean isServiceAdmin = userContainsRole(tenantRolesForUser, config.getString("cloudAuth.serviceAdminRole"));

        if (endpoints == null || endpoints.size() == 0) {
            return list;
        }

        for (OpenstackEndpoint point : endpoints) {
            for (CloudBaseUrl baseUrl : point.getBaseUrls()) {
                VersionForService version = new VersionForService();
                version.setId(baseUrl.getVersionId());
                version.setInfo(baseUrl.getVersionInfo());
                version.setList(baseUrl.getVersionList());

                Endpoint endpoint = objFactory.createEndpoint();
                if (isIdentityAdmin || isServiceAdmin) {
                    endpoint.setAdminURL(baseUrl.getAdminUrl());
                }
                if (baseUrl.getBaseUrlId() != null) {
                    endpoint.setId(baseUrl.getBaseUrlId());
                }
                endpoint.setInternalURL(baseUrl.getInternalUrl());
                endpoint.setName(baseUrl.getServiceName());
                endpoint.setPublicURL(baseUrl.getPublicUrl());
                endpoint.setRegion(baseUrl.getRegion());
                endpoint.setType(baseUrl.getOpenstackType());
                endpoint.setTenantId(point.getTenantId());
                if (!StringUtils.isBlank(version.getId())) {
                    endpoint.setVersion(version);
                }
                list.getEndpoint().add(endpoint);
            }
        }
        return list;
    }

    public boolean hasAccess(User user) {
        return false;
    }

    boolean userContainsRole(List<TenantRole> tenantRoleList, String role) {
        boolean hasRole = false;
        for (TenantRole tenantRole : tenantRoleList) {
            String name = tenantRole.getName();
            if (name.equals(role)) {
                hasRole = true;
            }
        }
        return hasRole;
    }


}
