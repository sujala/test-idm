package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.ForbiddenException;
import gherkin.lexer.i18n.EN;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.openstack.docs.identity.api.v2.EndpointList;
import org.openstack.docs.identity.api.v2.ObjectFactory;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;


/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/11/12
 * Time: 1:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class CloudUserAccessibilityTest {

    private CloudUserAccessibility cloudUserAccessibility;

    private CloudUserAccessibility spy;

    private Configuration config;

    public ScopeAccess scopeAccess;

    protected TenantService tenantService;

    private DomainService domainService;

    protected AuthorizationService authorizationService;

    protected UserService userService;

    private ObjectFactory objFactory;

    @Before
    public void setUp() {
        scopeAccess = mock(ScopeAccess.class);
        tenantService = mock(TenantService.class);
        domainService = mock(DomainService.class);
        authorizationService = mock(AuthorizationService.class);
        userService = mock(UserService.class);
        objFactory = mock(ObjectFactory.class);
        config = mock(Configuration.class);

        cloudUserAccessibility = new CloudUserAccessibility(tenantService, domainService, authorizationService, userService, config, objFactory, scopeAccess);
        spy = spy(cloudUserAccessibility);
    }

    @Test
    public void getAccessibleDomainsByScopeAccessForUser_noTenants_returnsEmptyList() throws Exception {
        List<Tenant> tenantList = new ArrayList<Tenant>();

        when(tenantService.getTenantsForScopeAccessByTenantRoles(scopeAccess)).thenReturn(tenantList);

        Domains domains = cloudUserAccessibility.getAccessibleDomainsByScopeAccessForUser(scopeAccess);
        assertThat("Domains", domains.getDomain().size(), equalTo(0));
    }

    @Test
    public void getAccessibleDomainsByScopeAccessForUser_noDomains_returnsEmptyList() throws Exception {
        List<Tenant> tenantList = new ArrayList<Tenant>();
        Tenant tenant = new Tenant();
        tenantList.add(tenant);

        when(tenantService.getTenantsForScopeAccessByTenantRoles(scopeAccess)).thenReturn(tenantList);

        Domains domains = cloudUserAccessibility.getAccessibleDomainsByScopeAccessForUser(scopeAccess);
        assertThat("Domains", domains.getDomain().size(), equalTo(0));
    }

    @Test
    public void getAccessibleDomainsByScopeAccessForUser_tenantBelongsToDomain() throws Exception {
        List<Tenant> tenantList = new ArrayList<Tenant>();
        List<Domain> listDomains = new ArrayList<Domain>();
        Domain domain = new Domain();
        domain.setDomainId("1");
        domain.setName("someName");
        Tenant tenant = new Tenant();
        tenantList.add(tenant);
        listDomains.add(domain);

        when(domainService.getDomainsForTenants(anyList())).thenReturn(listDomains);
        when(tenantService.getTenantsForScopeAccessByTenantRoles(scopeAccess)).thenReturn(tenantList);

        Domains domains = cloudUserAccessibility.getAccessibleDomainsByScopeAccessForUser(scopeAccess);
        assertThat("Domains", domains.getDomain().size(), equalTo(1));
        assertThat("Domains", domains.getDomain().get(0).getDomainId(), equalTo("1"));
    }

    @Test(expected = ForbiddenException.class)
    public void getAccessibleDomainsByScopeAccess_notAuthorized() {
        cloudUserAccessibility.getAccessibleDomainsByScopeAccess(scopeAccess);
    }

    @Test
    public void getAccessibleDomainsByScopeAccess_returnsDomains() {
        List<Tenant> tenantList = new ArrayList<Tenant>();
        List<Domain> listDomains = new ArrayList<Domain>();
        Domain domain = new Domain();
        domain.setDomainId("1");
        domain.setName("someName");
        Tenant tenant = new Tenant();
        tenantList.add(tenant);
        listDomains.add(domain);

        when(domainService.getDomainsForTenants(anyList())).thenReturn(listDomains);
        when(tenantService.getTenantsForScopeAccessByTenantRoles(scopeAccess)).thenReturn(tenantList);
        when(spy.hasAccess(scopeAccess)).thenReturn(true);

        Domains domains = spy.getAccessibleDomainsByScopeAccess(scopeAccess);
        assertThat("Domains", domains.getDomain().size(), equalTo(1));
        assertThat("Domains", domains.getDomain().get(0).getDomainId(), equalTo("1"));
    }

    @Test
    public void addUserDomainToDomains_domainsIsNull_returnsEmptyList() {
        Domains domains = new Domains();
        Domain domain = new Domain();
        domain.setDomainId("1");
        User user = new User();

        when(domainService.getDomain(anyString())).thenReturn(domain);
        Domains newDomains = cloudUserAccessibility.addUserDomainToDomains(user, domains);

        assertThat("Domains", newDomains.getDomain().get(0).getDomainId(), equalTo("1"));
    }

    @Test
    public void removeDuplicateDomains_returnListWithOutDup() {
        Domains domains = new Domains();
        Domain domain = new Domain();
        domain.setDomainId("1");
        domains.getDomain().add(domain);
        domains.getDomain().add(domain);
        Domains noDup = cloudUserAccessibility.removeDuplicateDomains(domains);
        assertThat("No Duplicates", noDup.getDomain().size(), equalTo(1));
    }

    @Test(expected = ForbiddenException.class)
    public void getAccessibleDomainEndpoints_notAuthorized() {
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        List<Tenant> tenants = new ArrayList<Tenant>();
        cloudUserAccessibility.getAccessibleDomainEndpoints(endpoints, tenants, scopeAccess);
    }

    @Test
    public void getAccessibleDomainEndpoints_returnsList() throws Exception {
        List<Tenant> tenants = new ArrayList<Tenant>();
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();

        OpenstackEndpoint endpoint = new OpenstackEndpoint();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        Tenant tenant = new Tenant();

        tenant.setTenantId("1");
        tenants.add(tenant);
        cloudBaseUrl.setPublicUrl("http://publicURL");
        cloudBaseUrl.setBaseUrlId(1);
        cloudBaseUrlList.add(cloudBaseUrl);
        endpoint.setBaseUrls(cloudBaseUrlList);
        endpoint.setTenantId("1");
        endpoints.add(endpoint);

        when(spy.hasAccess(scopeAccess)).thenReturn(true);

        List<OpenstackEndpoint> openstackEndpoints = spy.getAccessibleDomainEndpoints(endpoints, tenants, scopeAccess);
        assertThat("Endpoints", openstackEndpoints.get(0).getTenantId(), equalTo("1"));
    }

    @Test
    public void getAccessibleDomainEndpoints_emptyList() throws Exception {
        List<Tenant> tenants = new ArrayList<Tenant>();
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();

        when(spy.hasAccess(scopeAccess)).thenReturn(true);

        List<OpenstackEndpoint> openstackEndpoints = spy.getAccessibleDomainEndpoints(endpoints, tenants, scopeAccess);
        assertThat("Endpoints", openstackEndpoints.size(), equalTo(0));
    }

    @Test
    public void convertPopulateEndpointList_withNull_returnsEmptyEndpointList() throws Exception {
        when(objFactory.createEndpointList()).thenReturn(new EndpointList());
        EndpointList endpointList = cloudUserAccessibility.convertPopulateEndpointList(null);
        assertThat("endpoing list", endpointList.getAny().size(), equalTo(0));
    }

    @Test
    public void convertPopulateEndpointList_withEmptyList_returnsEmptyEndpointList() throws Exception {
        when(objFactory.createEndpointList()).thenReturn(new EndpointList());
        EndpointList endpointList = cloudUserAccessibility.convertPopulateEndpointList(new ArrayList<OpenstackEndpoint>());
        assertThat("endpoing list", endpointList.getAny().size(), equalTo(0));
    }

    @Test
    public void convertPopulateEndpointList_withList_returnsEndpointList_withCorrectSize() throws Exception {
        ArrayList<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(123456);
        baseUrls.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(baseUrls);
        endpoints.add(openstackEndpoint);
        endpoints.add(openstackEndpoint);

        when(objFactory.createEndpointList()).thenReturn(new EndpointList());
        when(objFactory.createEndpoint()).thenReturn(new Endpoint());

        EndpointList endpointList = cloudUserAccessibility.convertPopulateEndpointList(endpoints);
        assertThat("endpoint list", endpointList.getEndpoint().size(), equalTo(2));
    }

    @Test
    public void convertPopulateEndpointList_withListOfEndpoints_withNoBaseUrls_returnsEmptyList() throws Exception {
        ArrayList<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        openstackEndpoint.setBaseUrls(new ArrayList<CloudBaseUrl>());
        endpoints.add(openstackEndpoint);
        endpoints.add(openstackEndpoint);

        when(objFactory.createEndpointList()).thenReturn(new EndpointList());
        when(objFactory.createEndpoint()).thenReturn(new Endpoint());

        EndpointList endpointList = cloudUserAccessibility.convertPopulateEndpointList(endpoints);
        assertThat("endpoint list", endpointList.getEndpoint().size(), equalTo(0));
    }

    @Test
    public void convertPopulateEndpointList_withList_setsEndpointFields() throws Exception {
        ArrayList<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setVersionId("versionId");
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123456);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setRegion("region");
        cloudBaseUrl.setOpenstackType("openStackType");
        baseUrls.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(baseUrls);
        openstackEndpoint.setTenantId("tenantId");
        endpoints.add(openstackEndpoint);

        when(objFactory.createEndpointList()).thenReturn(new EndpointList());
        when(objFactory.createEndpoint()).thenReturn(new Endpoint());

        EndpointList endpointList = cloudUserAccessibility.convertPopulateEndpointList(endpoints);

        Endpoint endpoint = endpointList.getEndpoint().get(0);
        assertThat("endpoint version id", endpoint.getVersion().getId(), equalTo("versionId"));
        assertThat("endpoint version info", endpoint.getVersion().getInfo(), equalTo("versionInfo"));
        assertThat("endpoint version list", endpoint.getVersion().getList(), equalTo("versionList"));
        assertThat("endpoint id", endpoint.getId(), equalTo(123456));
        assertThat("url", endpoint.getInternalURL(), equalTo("internalUrl"));
        assertThat("url", endpoint.getName(), equalTo("serviceName"));
        assertThat("url", endpoint.getPublicURL(), equalTo("publicUrl"));
        assertThat("region", endpoint.getRegion(), equalTo("region"));
        assertThat("type", endpoint.getType(), equalTo("openStackType"));
        assertThat("tenant Id", endpoint.getTenantId(), equalTo("tenantId"));
    }

    @Test
    public void convertPopulateEndpointList_withList_withNoVersionId_doesNotSetVersionFields() throws Exception {
        ArrayList<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setBaseUrlId(123456);
        baseUrls.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(baseUrls);
        endpoints.add(openstackEndpoint);

        when(objFactory.createEndpointList()).thenReturn(new EndpointList());
        when(objFactory.createEndpoint()).thenReturn(new Endpoint());

        EndpointList endpointList = cloudUserAccessibility.convertPopulateEndpointList(endpoints);

        Endpoint endpoint = endpointList.getEndpoint().get(0);
        assertThat("endpoint version", endpoint.getVersion(), nullValue());
    }

    @Test
    public void convertPopulateEndpointList_identityAdmin_setsEndpointFields() throws Exception {
        ArrayList<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        ArrayList<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setVersionId("versionId");
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123456);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setRegion("region");
        cloudBaseUrl.setOpenstackType("openStackType");
        baseUrls.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(baseUrls);
        openstackEndpoint.setTenantId("tenantId");
        endpoints.add(openstackEndpoint);
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        TenantRole role = new TenantRole();
        role.setName("identity:default");
        tenantRoleList.add(role);

        when(objFactory.createEndpointList()).thenReturn(new EndpointList());
        when(objFactory.createEndpoint()).thenReturn(new Endpoint());
        when(tenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(tenantRoleList);
        when(config.getString(anyString())).thenReturn("identity:default");

        EndpointList endpointList = cloudUserAccessibility.convertPopulateEndpointList(endpoints);

        Endpoint endpoint = endpointList.getEndpoint().get(0);
        assertThat("endpoint version id", endpoint.getVersion().getId(), equalTo("versionId"));
        assertThat("endpoint version info", endpoint.getVersion().getInfo(), equalTo("versionInfo"));
        assertThat("endpoint version list", endpoint.getVersion().getList(), equalTo("versionList"));
        assertThat("endpoint id", endpoint.getId(), equalTo(123456));
        assertThat("url", endpoint.getInternalURL(), equalTo("internalUrl"));
        assertThat("url", endpoint.getAdminURL(), equalTo("adminUrl"));
        assertThat("url", endpoint.getName(), equalTo("serviceName"));
        assertThat("url", endpoint.getPublicURL(), equalTo("publicUrl"));
        assertThat("region", endpoint.getRegion(), equalTo("region"));
        assertThat("type", endpoint.getType(), equalTo("openStackType"));
        assertThat("tenant Id", endpoint.getTenantId(), equalTo("tenantId"));
    }

    @Test
    public void userContainsRole_returnTrue(){
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("identity:user-admin");

        tenantRoles.add(tenantRole);

        when(tenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(tenantRoles);
        Boolean result = cloudUserAccessibility.userContainsRole(scopeAccess,"identity:user-admin");
        assertThat("roles",result,equalTo(true));
    }

}