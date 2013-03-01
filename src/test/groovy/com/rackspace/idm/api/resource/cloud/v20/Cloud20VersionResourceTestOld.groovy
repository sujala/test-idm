package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DefaultRegionServices;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import com.rackspace.idm.api.serviceprofile.ServiceDescriptionTemplateUtil;
import com.rackspace.idm.domain.dao.impl.FileSystemApiDocRepository;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.common.api.v1.VersionChoice;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/6/12
 * Time: 9:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class Cloud20VersionResourceTestOld {
    def cloud20VersionResource;
    Configuration config;
    CloudContractDescriptionBuilder cloudContractDescriptionBuilder;
    FileSystemApiDocRepository fileSystemApiDocRepository;
    ServiceDescriptionTemplateUtil serviceDescriptionTemplateUtil;
    HttpHeaders httpHeaders;
    AuthenticationRequest authenticationRequest;
    DelegateCloud20Service delegateCloud20Service;
    Cloud20VersionResource spy;
    DefaultCloud20Service defaultCloud20Service;
    UriInfo uriInfo;

    @Before
    public void setUp() throws Exception {
        // setting up for version resource
        fileSystemApiDocRepository = new FileSystemApiDocRepository();
        serviceDescriptionTemplateUtil = new ServiceDescriptionTemplateUtil();
        config = mock(Configuration.class);
        cloudContractDescriptionBuilder = new CloudContractDescriptionBuilder(fileSystemApiDocRepository, serviceDescriptionTemplateUtil);

        cloud20VersionResource = new Cloud20VersionResource();
        cloud20VersionResource.config = config
        cloud20VersionResource.cloudContractDescriptionBuilder = cloudContractDescriptionBuilder

        // mock
        httpHeaders = mock(HttpHeaders.class);
        delegateCloud20Service = mock(DelegateCloud20Service.class);
        authenticationRequest = mock(AuthenticationRequest.class);
        defaultCloud20Service = mock(DefaultCloud20Service.class);
        uriInfo = mock(UriInfo.class);

        // setter
        cloud20VersionResource.setDefaultCloud20Service(defaultCloud20Service);
        cloud20VersionResource.setDelegateCloud20Service(delegateCloud20Service);

        spy = spy(cloud20VersionResource);
        doReturn(delegateCloud20Service).when(spy).getCloud20Service();
    }

    @Test
    public void setDefaultRegionServices_callsDefaultService_listDefaultRegionServices() throws Exception {
        DefaultRegionServices defaultRegionServices = new DefaultRegionServices();
        when(defaultCloud20Service.setDefaultRegionServices("token",defaultRegionServices)).thenReturn(Response.noContent());
        cloud20VersionResource.setDefaultRegionServices("token", defaultRegionServices);
        verify(defaultCloud20Service).setDefaultRegionServices("token", defaultRegionServices);
    }

    @Test
    public void getDefaultRegionServices_callsDefaultService_listDefaultRegionServices() throws Exception {
        when(defaultCloud20Service.listDefaultRegionServices("token")).thenReturn(Response.ok());
        cloud20VersionResource.listDefaultRegionServices("token");
        verify(defaultCloud20Service).listDefaultRegionServices("token");
    }

    @Test
    public void getCloud20VersionInfo_returnsVersionInfo() throws Exception {
        Response response = cloud20VersionResource.getCloud20VersionInfo();
        VersionChoice object =  (VersionChoice)response.getEntity();
        assertThat("version", object.getId(), equalTo("v2.0"));
    }

    @Test
    public void authenticate_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.authenticate(httpHeaders, authenticationRequest)).thenReturn(Response.ok());
        spy.authenticate(httpHeaders, authenticationRequest);
        verify(spy).getCloud20Service();
    }

    @Test
    public void authenticate_callsCloud20Service_callsAuthenticate() throws Exception {
        when(delegateCloud20Service.authenticate(httpHeaders, authenticationRequest)).thenReturn(Response.ok());
        spy.authenticate(httpHeaders, authenticationRequest);
        verify(delegateCloud20Service).authenticate(httpHeaders, authenticationRequest);
    }

    @Test
    public void authenticate_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.authenticate(httpHeaders, authenticationRequest)).thenReturn(Response.ok());
        Response result = spy.authenticate(httpHeaders, authenticationRequest);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void validateToken_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.validateToken(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.validateToken(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void validateToken_callsCloud20Service_callsValidateToken() throws Exception {
        when(delegateCloud20Service.validateToken(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.validateToken(httpHeaders, null, null, null);
        verify(delegateCloud20Service).validateToken(httpHeaders, null, null, null);
    }

    @Test
    public void validateToken_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.validateToken(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.validateToken(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void checkToken_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.validateToken(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.checkToken(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void checkToken_callsCloud20Service_callsValidateToken() throws Exception {
        when(delegateCloud20Service.validateToken(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.checkToken(httpHeaders, null, null, null);
        verify(delegateCloud20Service).validateToken(httpHeaders, null, null, null);
    }

    @Test
    public void checkToken_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.validateToken(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.checkToken(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointsForToken_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listEndpointsForToken(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.listEndpointsForToken(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listEndpointsForToken_callsCloud20Service_callsListEndpointsForToken() throws Exception {
        when(delegateCloud20Service.listEndpointsForToken(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.listEndpointsForToken(httpHeaders, null, null);
        verify(delegateCloud20Service).listEndpointsForToken(httpHeaders, null, null);
    }

    @Test
    public void listEndpointsForToken_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.listEndpointsForToken(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.listEndpointsForToken(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void impersonate_callsCloud20Service_callsImpersonate() throws Exception {
        when(defaultCloud20Service.impersonate(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.impersonate(httpHeaders, null, null);
        verify(defaultCloud20Service).impersonate(httpHeaders, null, null);
    }

    @Test
    public void impersonate_responseOk_returns200() throws Exception {
        when(defaultCloud20Service.impersonate(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.impersonate(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listExtensions_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listExtensions(httpHeaders)).thenReturn(Response.ok());
        spy.listExtensions(httpHeaders);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listExtensions_callsCloud20Service_callsListExtensions() throws Exception {
        when(delegateCloud20Service.listExtensions(httpHeaders)).thenReturn(Response.ok());
        spy.listExtensions(httpHeaders);
        verify(delegateCloud20Service).listExtensions(httpHeaders);
    }

    @Test
    public void listExtensions_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.listExtensions(httpHeaders)).thenReturn(Response.ok());
        Response result = spy.listExtensions(httpHeaders);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getExtension_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getExtension(httpHeaders, null)).thenReturn(Response.ok());
        spy.getExtension(httpHeaders, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getExtension_callsCloud20Service_callsGetExtension() throws Exception {
        when(delegateCloud20Service.getExtension(httpHeaders, null)).thenReturn(Response.ok());
        spy.getExtension(httpHeaders, null);
        verify(delegateCloud20Service).getExtension(httpHeaders, null);
    }

    @Test
    public void getExtension_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.getExtension(httpHeaders, null)).thenReturn(Response.ok());
        Response result = spy.getExtension(httpHeaders, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserByName_nameIsBlank_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listUsers(httpHeaders, uriInfo, null, null, null)).thenReturn(Response.ok());
        spy.getUserByName(httpHeaders, uriInfo, null, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getUserByName_callsGetCloud20Service_callsListUsers() throws Exception {
        when(delegateCloud20Service.listUsers(httpHeaders, uriInfo, null, null, null)).thenReturn(Response.ok());
        spy.getUserByName(httpHeaders, uriInfo, null, null, null, null);
        verify(delegateCloud20Service).listUsers(httpHeaders, uriInfo, null, null, null);
    }

    @Test
    public void getUserByName_nameIsBlank_responseOkReturns200() throws Exception {
        when(delegateCloud20Service.listUsers(httpHeaders, uriInfo, null, null, null)).thenReturn(Response.ok());
        Response result = spy.getUserByName(httpHeaders, uriInfo, null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserByName_nameNotBlank_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getUserByName(httpHeaders, null, "name")).thenReturn(Response.ok());
        spy.getUserByName(httpHeaders, uriInfo, null, "name", null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getUserByName_callsGetCloud20Service_callsGetUserByName() throws Exception {
        when(delegateCloud20Service.getUserByName(httpHeaders, null, "name")).thenReturn(Response.ok());
        spy.getUserByName(httpHeaders, uriInfo, null, "name", null, null);
        verify(delegateCloud20Service).getUserByName(httpHeaders, null, "name");
    }

    @Test
    public void getUserByName_nameNotBlank_responseOkReturns200() throws Exception {
        when(delegateCloud20Service.getUserByName(httpHeaders, null, "name")).thenReturn(Response.ok());
        Response result = spy.getUserByName(httpHeaders, uriInfo, null, "name", null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRoles_serviceIdIsBlank_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listUserGlobalRoles(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.listUserGlobalRoles(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listUserGlobalRoles_callsGetCloud20Service_callsListUserGlobalRoles() throws Exception {
        when(delegateCloud20Service.listUserGlobalRoles(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.listUserGlobalRoles(httpHeaders, null, null, null);
        verify(delegateCloud20Service).listUserGlobalRoles(httpHeaders, null, null);
    }

    @Test
    public void listUserGlobalRoles_serviceIdIsBlank_responseOkReturns200() throws Exception {
        when(delegateCloud20Service.listUserGlobalRoles(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.listUserGlobalRoles(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRoles_serviceIdNotBlank_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listUserGlobalRolesByServiceId(httpHeaders, null, null, "serviceId")).thenReturn(Response.ok());
        spy.listUserGlobalRoles(httpHeaders, null, null, "serviceId");
        verify(spy).getCloud20Service();
    }

    @Test
    public void listUserGlobalRoles_callsGetCloud20Service_callsListUserGlobalRolesByServiceId() throws Exception {
        when(delegateCloud20Service.listUserGlobalRolesByServiceId(httpHeaders, null, null, "serviceId")).thenReturn(Response.ok());
        spy.listUserGlobalRoles(httpHeaders, null, null, "serviceId");
        verify(delegateCloud20Service).listUserGlobalRolesByServiceId(httpHeaders, null, null, "serviceId");
    }

    @Test
    public void listUserGlobalRoles_serviceIdNotBlank_responseOkReturns200() throws Exception {
        when(delegateCloud20Service.listUserGlobalRolesByServiceId(httpHeaders, null, null, "serviceId")).thenReturn(Response.ok());
        Response result = spy.listUserGlobalRoles(httpHeaders, null, null, "serviceId");
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listTenants_getTenantByName_nameIsBlank_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listTenants(httpHeaders, null, null, 1)).thenReturn(Response.ok());
        spy.listTenantsAndGetTenantByName(httpHeaders, null, null, null, 1);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listTenants_getTenantByName_callsGetCloud20Service_callsListTenants() throws Exception {
        when(delegateCloud20Service.listTenants(httpHeaders, null, null, 1)).thenReturn(Response.ok());
        spy.listTenantsAndGetTenantByName(httpHeaders, null, null, null, 1);
        verify(delegateCloud20Service).listTenants(httpHeaders, null, null, 1);
    }

    @Test
    public void listTenants_getTenantByName_nameIsBlank_responseOkReturns200() throws Exception {
        when(delegateCloud20Service.listTenants(httpHeaders, null, null, 1)).thenReturn(Response.ok());
        Response result = spy.listTenantsAndGetTenantByName(httpHeaders, null, null, null, 1);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listTenants_getTenantByName_nameNotBlank_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getTenantByName(httpHeaders, null, "name")).thenReturn(Response.ok());
        spy.listTenantsAndGetTenantByName(httpHeaders, null, "name", null, 1);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listTenants_getTenantByName_callsGetCloud20Service_callsGetTenantByName() throws Exception {
        when(delegateCloud20Service.getTenantByName(httpHeaders, null, "name")).thenReturn(Response.ok());
        spy.listTenantsAndGetTenantByName(httpHeaders, null, "name", null, 1);
        verify(delegateCloud20Service).getTenantByName(httpHeaders, null, "name");
    }

    @Test
    public void listTenants_getTenantByName_nameNotBlank_responseOkReturns200() throws Exception {
        when(delegateCloud20Service.getTenantByName(httpHeaders, null, "name")).thenReturn(Response.ok());
        Response result = spy.listTenantsAndGetTenantByName(httpHeaders, null, "name", null, 1);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getTenantById_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getTenantById(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getTenantById(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getTenantById_callsCloud20Service_callsGetTenantById() throws Exception {
        when(delegateCloud20Service.getTenantById(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getTenantById(httpHeaders, null, null);
        verify(delegateCloud20Service).getTenantById(httpHeaders, null, null);
    }

    @Test
    public void getTenantById_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.getTenantById(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.getTenantById(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listRolesForUserOnTenant_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listRolesForUserOnTenant(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.listRolesForUserOnTenant(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listRolesForUserOnTenant_callsCloud20Service_callsListRolesForUserOnTenant() throws Exception {
        when(delegateCloud20Service.listRolesForUserOnTenant(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.listRolesForUserOnTenant(httpHeaders, null, null, null);
        verify(delegateCloud20Service).listRolesForUserOnTenant(httpHeaders, null, null, null);
    }

    @Test
    public void listRolesForUserOnTenant_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.listRolesForUserOnTenant(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.listRolesForUserOnTenant(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void addUser_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.addUser(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addUser(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void addUser_callsCloud20Service_callsAddUser() throws Exception {
        when(delegateCloud20Service.addUser(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addUser(httpHeaders, null, null, null);
        verify(delegateCloud20Service).addUser(httpHeaders, null, null, null);
    }

    @Test
    public void addUser_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.addUser(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.addUser(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void updateUser_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.updateUser(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.updateUser(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void updateUser_callsCloud20Service_callsUpdateUser() throws Exception {
        when(delegateCloud20Service.updateUser(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.updateUser(httpHeaders, null, null, null);
        verify(delegateCloud20Service).updateUser(httpHeaders, null, null, null);
    }

    @Test
    public void updateUser_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.updateUser(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.updateUser(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteUser_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.deleteUser(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.deleteUser(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void deleteUser_callsCloud20Service_callsDeleteUser() throws Exception {
        when(delegateCloud20Service.deleteUser(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.deleteUser(httpHeaders, null, null);
        verify(delegateCloud20Service).deleteUser(httpHeaders, null, null);
    }

    @Test
    public void deleteUser_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.deleteUser(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteUser(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void setUserEnabled_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.setUserEnabled(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.setUserEnabled(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void setUserEnabled_callsCloud20Service_callsSetUserEnabled() throws Exception {
        when(delegateCloud20Service.setUserEnabled(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.setUserEnabled(httpHeaders, null, null, null);
        verify(delegateCloud20Service).setUserEnabled(httpHeaders, null, null, null);
    }

    @Test
    public void setUserEnabled_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.setUserEnabled(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.setUserEnabled(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listUserGroups_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listUserGroups(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.listUserGroups(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listUserGroups_callsCloud20Service_callsListUserGroups() throws Exception {
        when(delegateCloud20Service.listUserGroups(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.listUserGroups(httpHeaders, null, null);
        verify(delegateCloud20Service).listUserGroups(httpHeaders, null, null);
    }

    @Test
    public void listUserGroups_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.listUserGroups(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.listUserGroups(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void addUserRole_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.addUserRole(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addUserRole(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void addUserRole_callsCloud20Service_callsAddUserRole() throws Exception {
        when(delegateCloud20Service.addUserRole(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addUserRole(httpHeaders, null, null, null);
        verify(delegateCloud20Service).addUserRole(httpHeaders, null, null, null);
    }

    @Test
    public void addUserRole_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.addUserRole(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.addUserRole(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserRole_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getUserRole(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.getUserRole(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getUserRole_callsCloud20Service_callsGetUserRole() throws Exception {
        when(delegateCloud20Service.getUserRole(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.getUserRole(httpHeaders, null, null, null);
        verify(delegateCloud20Service).getUserRole(httpHeaders, null, null, null);
    }

    @Test
    public void getUserRole_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.getUserRole(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.getUserRole(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteUserRole_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.deleteUserRole(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.deleteUserRole(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void deleteUserRole_callsCloud20Service_callsDeleteUserRole() throws Exception {
        when(delegateCloud20Service.deleteUserRole(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.deleteUserRole(httpHeaders, null, null, null);
        verify(delegateCloud20Service).deleteUserRole(httpHeaders, null, null, null);
    }

    @Test
    public void deleteUserRole_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.deleteUserRole(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteUserRole(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void addUserCredential_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.addUserCredential(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        spy.addUserCredential(httpHeaders, null, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void addUserCredential_callsCloud20Service_callsAddUserCredential() throws Exception {
        when(delegateCloud20Service.addUserCredential(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        spy.addUserCredential(httpHeaders, null, null, null, null);
        verify(delegateCloud20Service).addUserCredential(httpHeaders, null, null, null, null);
    }

    @Test
    public void addUserCredential_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.addUserCredential(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.addUserCredential(httpHeaders, null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listCredentials_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listCredentials(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        spy.listCredentials(httpHeaders, null, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listCredentials_callsCloud20Service_callsListCredentials() throws Exception {
        when(delegateCloud20Service.listCredentials(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        spy.listCredentials(httpHeaders, null, null, null, null);
        verify(delegateCloud20Service).listCredentials(httpHeaders, null, null, null, null);
    }

    @Test
    public void listCredentials_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.listCredentials(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.listCredentials(httpHeaders, null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void updateUserPasswordCredentials_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.updateUserPasswordCredentials(httpHeaders, null, null, "passwordCredentials", null)).thenReturn(Response.ok());
        spy.updateUserPasswordCredentials(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void updateUserPasswordCredentials_callsCloud20Service_callsUpdateUserPasswordCredentials() throws Exception {
        when(delegateCloud20Service.updateUserPasswordCredentials(httpHeaders, null, null, "passwordCredentials", null)).thenReturn(Response.ok());
        spy.updateUserPasswordCredentials(httpHeaders, null, null, null);
        verify(delegateCloud20Service).updateUserPasswordCredentials(httpHeaders, null, null, "passwordCredentials", null);
    }

    @Test
    public void updateUserPasswordCredentials_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.updateUserPasswordCredentials(httpHeaders, null, null, "passwordCredentials", null)).thenReturn(Response.ok());
        Response result = spy.updateUserPasswordCredentials(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void updateUserApiKeyCredentials_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.updateUserApiKeyCredentials(httpHeaders, null, null, JSONConstants.APIKEY_CREDENTIALS, null)).thenReturn(Response.ok());
        spy.updateUserApiKeyCredentials(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void updateUserApiKeyCredentials_callsCloud20Service_callsUpdateUserApiKeyCredentials() throws Exception {
        when(delegateCloud20Service.updateUserApiKeyCredentials(httpHeaders, null, null, JSONConstants.APIKEY_CREDENTIALS, null)).thenReturn(Response.ok());
        spy.updateUserApiKeyCredentials(httpHeaders, null, null, null);
        verify(delegateCloud20Service).updateUserApiKeyCredentials(httpHeaders, null, null, JSONConstants.APIKEY_CREDENTIALS, null);
    }

    @Test
    public void updateUserApiKeyCredentials_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.updateUserApiKeyCredentials(httpHeaders, null, null, JSONConstants.APIKEY_CREDENTIALS, null)).thenReturn(Response.ok());
        Response result = spy.updateUserApiKeyCredentials(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserCredentialKey_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getUserApiKeyCredentials(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getUserCredentialKey(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getUserCredentialKey_callsCloud20Service_callsGetUserCredential() throws Exception {
        when(delegateCloud20Service.getUserApiKeyCredentials(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getUserCredentialKey(httpHeaders, null, null);
        verify(delegateCloud20Service).getUserApiKeyCredentials(httpHeaders, null, null);
    }

    @Test
    public void getUserCredentialKey_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.getUserApiKeyCredentials(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.getUserCredentialKey(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserCredential_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getUserPasswordCredentials(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getUserCredential(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getUserCredential_callsCloud20Service_callsGetUserCredential() throws Exception {
        when(delegateCloud20Service.getUserPasswordCredentials(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getUserCredential(httpHeaders, null, null);
        verify(delegateCloud20Service).getUserPasswordCredentials(httpHeaders, null, null);
    }

    @Test
    public void getUserCredential_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.getUserPasswordCredentials(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.getUserCredential(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteUserKeyCredential_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.deleteUserCredential(httpHeaders, null, null, JSONConstants.APIKEY_CREDENTIALS)).thenReturn(Response.ok());
        spy.deleteUserKeyCredential(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void deleteUserKeyCredential_callsCloud20Service_callsDeleteUserCredential() throws Exception {
        when(delegateCloud20Service.deleteUserCredential(httpHeaders, null, null, JSONConstants.APIKEY_CREDENTIALS)).thenReturn(Response.ok());
        spy.deleteUserKeyCredential(httpHeaders, null, null);
        verify(delegateCloud20Service).deleteUserCredential(httpHeaders, null, null, JSONConstants.APIKEY_CREDENTIALS);
    }

    @Test
    public void deleteUserKeyCredential_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.deleteUserCredential(httpHeaders, null, null, JSONConstants.APIKEY_CREDENTIALS)).thenReturn(Response.ok());
        Response result = spy.deleteUserKeyCredential(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteUserCredential_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.deleteUserCredential(httpHeaders, null, null, JSONConstants.PASSWORD_CREDENTIALS)).thenReturn(Response.ok());
        spy.deleteUserCredential(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void deleteUserCredential_callsCloud20Service_callsDeleteUserCredential() throws Exception {
        when(delegateCloud20Service.deleteUserCredential(httpHeaders, null, null, JSONConstants.PASSWORD_CREDENTIALS)).thenReturn(Response.ok());
        spy.deleteUserCredential(httpHeaders, null, null);
        verify(delegateCloud20Service).deleteUserCredential(httpHeaders, null, null, JSONConstants.PASSWORD_CREDENTIALS);
    }

    @Test
    public void deleteUserCredential_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.deleteUserCredential(httpHeaders, null, null, JSONConstants.PASSWORD_CREDENTIALS)).thenReturn(Response.ok());
        Response result = spy.deleteUserCredential(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void addTenant_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.addTenant(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addTenant(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void addTenant_callsCloud20Service_callsAddTenant() throws Exception {
        when(delegateCloud20Service.addTenant(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addTenant(httpHeaders, null, null, null);
        verify(delegateCloud20Service).addTenant(httpHeaders, null, null, null);
    }

    @Test
    public void addTenant_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.addTenant(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.addTenant(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void updateTenant_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.updateTenant(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.updateTenant(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void updateTenant_callsCloud20Service_callsUpdateTenant() throws Exception {
        when(delegateCloud20Service.updateTenant(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.updateTenant(httpHeaders, null, null, null);
        verify(delegateCloud20Service).updateTenant(httpHeaders, null, null, null);
    }

    @Test
    public void updateTenant_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.updateTenant(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.updateTenant(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteTenant_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.deleteTenant(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.deleteTenant(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void deleteTenant_callsCloud20Service_callsDeleteTenant() throws Exception {
        when(delegateCloud20Service.deleteTenant(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.deleteTenant(httpHeaders, null, null);
        verify(delegateCloud20Service).deleteTenant(httpHeaders, null, null);
    }

    @Test
    public void deleteTenant_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.deleteTenant(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteTenant(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listRolesForTenant_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listRolesForTenant(httpHeaders, null, null, null, 1)).thenReturn(Response.ok());
        spy.listRolesForTenant(httpHeaders, null, null, null, 1);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listRolesForTenant_callsCloud20Service_callsListRolesForTenant() throws Exception {
        when(delegateCloud20Service.listRolesForTenant(httpHeaders, null, null, null, 1)).thenReturn(Response.ok());
        spy.listRolesForTenant(httpHeaders, null, null, null, 1);
        verify(delegateCloud20Service).listRolesForTenant(httpHeaders, null, null, null, 1);
    }

    @Test
    public void listRolesForTenant_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.listRolesForTenant(httpHeaders, null, null, null, 1)).thenReturn(Response.ok());
        Response result = spy.listRolesForTenant(httpHeaders, null, null, null, 1);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listUsersForTenant_listUsersWithRoleForTenant_roleIsNotNull_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listUsersWithRoleForTenant(httpHeaders, null, null, "roleId", null, 1)).thenReturn(Response.ok());
        spy.listUsersForTenantAndListUsersWithRoleForTenant(httpHeaders, null, null, "roleId", null, 1);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listUsersForTenant_listUsersWithRoleForTenant_roleIsNotNull_callsGetCloud20Service_callsListUsersWithRoleForTenant() throws Exception {
        when(delegateCloud20Service.listUsersWithRoleForTenant(httpHeaders, null, null, "roleId", null, 1)).thenReturn(Response.ok());
        spy.listUsersForTenantAndListUsersWithRoleForTenant(httpHeaders, null, null, "roleId", null, 1);
        verify(delegateCloud20Service).listUsersWithRoleForTenant(httpHeaders, null, null, "roleId", null, 1);
    }

    @Test
    public void listUsersForTenant_listUsersWithRoleForTenant_roleIsNotNull_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.listUsersWithRoleForTenant(httpHeaders, null, null, "roleId", null, 1)).thenReturn(Response.ok());
        Response result = spy.listUsersForTenantAndListUsersWithRoleForTenant(httpHeaders, null, null, "roleId", null, 1);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listUsersForTenant_listUsersWithRoleForTenant_roleNull_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listUsersForTenant(httpHeaders, null, null, null, 1)).thenReturn(Response.ok());
        spy.listUsersForTenantAndListUsersWithRoleForTenant(httpHeaders, null, null, null, null, 1);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listUsersForTenant_listUsersWithRoleForTenant_roleNull_callsGetCloud20Service_callsListUsersForTenant() throws Exception {
        when(delegateCloud20Service.listUsersForTenant(httpHeaders, null, null, null, 1)).thenReturn(Response.ok());
        spy.listUsersForTenantAndListUsersWithRoleForTenant(httpHeaders, null, null, null, null, 1);
        verify(delegateCloud20Service).listUsersForTenant(httpHeaders, null, null, null, 1);
    }

    @Test
    public void listUsersForTenant_listUsersWithRoleForTenant_roleIsNull_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.listUsersForTenant(httpHeaders, null, null, null, 1)).thenReturn(Response.ok());
        Response result = spy.listUsersForTenantAndListUsersWithRoleForTenant(httpHeaders, null, null, null, null, 1);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void addRolesToUserOnTenant_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.addRolesToUserOnTenant(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        spy.addRolesToUserOnTenant(httpHeaders, null, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void addRolesToUserOnTenant_callsCloud20Service_callsAddRolesToUserOnTenant() throws Exception {
        when(delegateCloud20Service.addRolesToUserOnTenant(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        spy.addRolesToUserOnTenant(httpHeaders, null, null, null, null);
        verify(delegateCloud20Service).addRolesToUserOnTenant(httpHeaders, null, null, null, null);
    }

    @Test
    public void addRolesToUserOnTenant_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.addRolesToUserOnTenant(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.addRolesToUserOnTenant(httpHeaders, null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteRoleFromUserOnTenant_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.deleteRoleFromUserOnTenant(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        spy.deleteRoleFromUserOnTenant(httpHeaders, null, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void deleteRoleFromUserOnTenant_callsCloud20Service_callsDeleteRoleFromUserOnTenant() throws Exception {
        when(delegateCloud20Service.deleteRoleFromUserOnTenant(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        spy.deleteRoleFromUserOnTenant(httpHeaders, null, null, null, null);
        verify(delegateCloud20Service).deleteRoleFromUserOnTenant(httpHeaders, null, null, null, null);
    }

    @Test
    public void deleteRoleFromUserOnTenant_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.deleteRoleFromUserOnTenant(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteRoleFromUserOnTenant(httpHeaders, null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listRoles_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listRoles(httpHeaders, null, null, null, null, null)).thenReturn(Response.ok());
        spy.listRoles(httpHeaders, null, null, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listRoles_callsCloud20Service_callsListRoles() throws Exception {
        when(delegateCloud20Service.listRoles(httpHeaders, null, null, null, null, null)).thenReturn(Response.ok());
        spy.listRoles(httpHeaders, null, null, null, null, null);
        verify(delegateCloud20Service).listRoles(httpHeaders, null, null, null, null, null);
    }

    @Test
    public void listRoles_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.listRoles(httpHeaders, null, null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.listRoles(httpHeaders, null, null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void addRole_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.addRole(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addRole(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void addRole_callsCloud20Service_callsAddRole() throws Exception {
        when(delegateCloud20Service.addRole(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addRole(httpHeaders, null, null, null);
        verify(delegateCloud20Service).addRole(httpHeaders, null, null, null);
    }

    @Test
    public void addRole_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.addRole(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.addRole(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }
    
    @Test
    public void getRole_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getRole(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getRole(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getRole_callsCloud20Service_callsGetRole() throws Exception {
        when(delegateCloud20Service.getRole(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getRole(httpHeaders, null, null);
        verify(delegateCloud20Service).getRole(httpHeaders, null, null);
    }

    @Test
    public void getRole_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.getRole(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.getRole(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteRole_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.deleteRole(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.deleteRole(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void deleteRole_callsCloud20Service_callsDeleteRole() throws Exception {
        when(delegateCloud20Service.deleteRole(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.deleteRole(httpHeaders, null, null);
        verify(delegateCloud20Service).deleteRole(httpHeaders, null, null);
    }

    @Test
    public void deleteRole_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.deleteRole(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteRole(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listServices_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listServices(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.listServices(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listServices_callsCloud20Service_callsListServices() throws Exception {
        when(delegateCloud20Service.listServices(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.listServices(httpHeaders, null, null, null);
        verify(delegateCloud20Service).listServices(httpHeaders, null, null, null);
    }

    @Test
    public void listServices_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.listServices(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.listServices(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void addService_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.addService(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addService(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void addService_callsCloud20Service_callsAddService() throws Exception {
        when(delegateCloud20Service.addService(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addService(httpHeaders, null, null, null);
        verify(delegateCloud20Service).addService(httpHeaders, null, null, null);
    }

    @Test
    public void addService_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.addService(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.addService(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getService_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getService(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getService(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getService_callsCloud20Service_callsGetService() throws Exception {
        when(delegateCloud20Service.getService(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getService(httpHeaders, null, null);
        verify(delegateCloud20Service).getService(httpHeaders, null, null);
    }

    @Test
    public void getService_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.getService(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.getService(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteService_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.deleteService(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.deleteService(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void deleteService_callsCloud20Service_callsDeleteService() throws Exception {
        when(delegateCloud20Service.deleteService(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.deleteService(httpHeaders, null, null);
        verify(delegateCloud20Service).deleteService(httpHeaders, null, null);
    }

    @Test
    public void deleteService_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.deleteService(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteService(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointTemplates_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listEndpointTemplates(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.listEndpointTemplates(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listEndpointTemplates_callsCloud20Service_callsListEndpointTemplates() throws Exception {
        when(delegateCloud20Service.listEndpointTemplates(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.listEndpointTemplates(httpHeaders, null, null);
        verify(delegateCloud20Service).listEndpointTemplates(httpHeaders, null, null);
    }

    @Test
    public void listEndpointTemplates_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.listEndpointTemplates(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.listEndpointTemplates(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void addEndpointTemplate_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.addEndpointTemplate(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addEndpointTemplate(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void addEndpointTemplate_callsCloud20Service_callsAddEndpointTemplate() throws Exception {
        when(delegateCloud20Service.addEndpointTemplate(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addEndpointTemplate(httpHeaders, null, null, null);
        verify(delegateCloud20Service).addEndpointTemplate(httpHeaders, null, null, null);
    }

    @Test
    public void addEndpointTemplate_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.addEndpointTemplate(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.addEndpointTemplate(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getEndpointTemplate_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getEndpointTemplate(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getEndpointTemplate(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getEndpointTemplate_callsCloud20Service_callsGetEndpointTemplate() throws Exception {
        when(delegateCloud20Service.getEndpointTemplate(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getEndpointTemplate(httpHeaders, null, null);
        verify(delegateCloud20Service).getEndpointTemplate(httpHeaders, null, null);
    }

    @Test
    public void getEndpointTemplate_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.getEndpointTemplate(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.getEndpointTemplate(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteEndpointTemplate_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.deleteEndpointTemplate(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.deleteEndpointTemplate(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void deleteEndpointTemplate_callsCloud20Service_callsDeleteEndpointTemplate() throws Exception {
        when(delegateCloud20Service.deleteEndpointTemplate(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.deleteEndpointTemplate(httpHeaders, null, null);
        verify(delegateCloud20Service).deleteEndpointTemplate(httpHeaders, null, null);
    }

    @Test
    public void deleteEndpointTemplate_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.deleteEndpointTemplate(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteEndpointTemplate(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listEndpoints_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listEndpoints(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.listEndpoints(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void listEndpoints_callsCloud20Service_callsListEndpoints() throws Exception {
        when(delegateCloud20Service.listEndpoints(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.listEndpoints(httpHeaders, null, null);
        verify(delegateCloud20Service).listEndpoints(httpHeaders, null, null);
    }

    @Test
    public void listEndpoints_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.listEndpoints(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.listEndpoints(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void addEndpoint_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.addEndpoint(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addEndpoint(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void addEndpoint_callsCloud20Service_callsAddEndpoint() throws Exception {
        when(delegateCloud20Service.addEndpoint(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addEndpoint(httpHeaders, null, null, null);
        verify(delegateCloud20Service).addEndpoint(httpHeaders, null, null, null);
    }

    @Test
    public void addEndpoint_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.addEndpoint(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.addEndpoint(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getEndpoint_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getEndpoint(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.getEndpoint(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getEndpoint_callsCloud20Service_callsGetEndpoint() throws Exception {
        when(delegateCloud20Service.getEndpoint(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.getEndpoint(httpHeaders, null, null, null);
        verify(delegateCloud20Service).getEndpoint(httpHeaders, null, null, null);
    }

    @Test
    public void getEndpoint_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.getEndpoint(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.getEndpoint(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteEndpoint_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.deleteEndpoint(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.deleteEndpoint(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void deleteEndpoint_callsCloud20Service_callsDeleteEndpoint() throws Exception {
        when(delegateCloud20Service.deleteEndpoint(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.deleteEndpoint(httpHeaders, null, null, null);
        verify(delegateCloud20Service).deleteEndpoint(httpHeaders, null, null, null);
    }

    @Test
    public void deleteEndpoint_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.deleteEndpoint(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteEndpoint(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getSecretQA_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getSecretQA(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getSecretQA(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getSecretQA_callsCloud20Service_callsGetSecretQA() throws Exception {
        when(delegateCloud20Service.getSecretQA(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getSecretQA(httpHeaders, null, null);
        verify(delegateCloud20Service).getSecretQA(httpHeaders, null, null);
    }

    @Test
    public void getSecretQA_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.getSecretQA(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.getSecretQA(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void updateSecretQA_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.updateSecretQA(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.updateSecretQA(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void updateSecretQA_callsCloud20Service_callsUpdateSecretQA() throws Exception {
        when(delegateCloud20Service.updateSecretQA(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.updateSecretQA(httpHeaders, null, null, null);
        verify(delegateCloud20Service).updateSecretQA(httpHeaders, null, null, null);
    }

    @Test
    public void updateSecretQA_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.updateSecretQA(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.updateSecretQA(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void addGroup_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.addGroup(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addGroup(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void addGroup_callsCloud20Service_callsAddGroup() throws Exception {
        when(delegateCloud20Service.addGroup(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.addGroup(httpHeaders, null, null, null);
        verify(delegateCloud20Service).addGroup(httpHeaders, null, null, null);
    }

    @Test
    public void addGroup_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.addGroup(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.addGroup(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getGroups_groupNameNotNull_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getGroup(httpHeaders, null, "groupName")).thenReturn(Response.ok());
        spy.getGroups(httpHeaders, null, "groupName", null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getGroups_groupNameNotNull_callsGetCloud20Service_callsGetGroup() throws Exception {
        when(delegateCloud20Service.getGroup(httpHeaders, null, "groupName")).thenReturn(Response.ok());
        spy.getGroups(httpHeaders, null, "groupName", null, null);
        verify(delegateCloud20Service).getGroup(httpHeaders, null, "groupName");
    }

    @Test
    public void getGroups_groupNameNotNull_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.getGroup(httpHeaders, null, "groupName")).thenReturn(Response.ok());
        Response result = spy.getGroups(httpHeaders, null, "groupName", null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getGroups_groupNameIsNull_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.listGroups(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        spy.getGroups(httpHeaders, null, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getGroups_groupNameIsNull_callsGetCloud20Service_callsListGroups() throws Exception {
        when(delegateCloud20Service.listGroups(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        spy.getGroups(httpHeaders, null, null, null, null);
        verify(delegateCloud20Service).listGroups(httpHeaders, null, null, null, null);
    }

    @Test
    public void getGroups_groupNameIsNull_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.listGroups(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.getGroups(httpHeaders, null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getGroupById_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getGroupById(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getGroupById(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getGroupById_callsCloud20Service_callsGetGroupById() throws Exception {
        when(delegateCloud20Service.getGroupById(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getGroupById(httpHeaders, null, null);
        verify(delegateCloud20Service).getGroupById(httpHeaders, null, null);
    }

    @Test
    public void getGroupById_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.getGroupById(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.getGroupById(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void updateGroup_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.updateGroup(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.updateGroup(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void updateGroup_callsCloud20Service_callsUpdateGroup() throws Exception {
        when(delegateCloud20Service.updateGroup(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.updateGroup(httpHeaders, null, null, null);
        verify(delegateCloud20Service).updateGroup(httpHeaders, null, null, null);
    }

    @Test
    public void updateGroup_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.updateGroup(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.updateGroup(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteGroupById_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.deleteGroup(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.deleteGroupById(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void deleteGroupById_callsCloud20Service_callsDeleteGroup() throws Exception {
        when(delegateCloud20Service.deleteGroup(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.deleteGroupById(httpHeaders, null, null);
        verify(delegateCloud20Service).deleteGroup(httpHeaders, null, null);
    }

    @Test
    public void deleteGroupById_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.deleteGroup(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteGroupById(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUsersFromGroup_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getUsersForGroup(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        spy.getUsersFromGroup(httpHeaders, null, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getUsersFromGroup_callsCloud20Service_callsGetUsersForGroup() throws Exception {
        when(delegateCloud20Service.getUsersForGroup(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        spy.getUsersFromGroup(httpHeaders, null, null, null, null);
        verify(delegateCloud20Service).getUsersForGroup(httpHeaders, null, null, null, null);
    }

    @Test
    public void getUsersFromGroup_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.getUsersForGroup(httpHeaders, null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.getUsersFromGroup(httpHeaders, null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void putUserGroups_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.addUserToGroup(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.putUserGroups(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void putUserGroups_callsCloud20Service_callsAddUserToGroup() throws Exception {
        when(delegateCloud20Service.addUserToGroup(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.putUserGroups(httpHeaders, null, null, null);
        verify(delegateCloud20Service).addUserToGroup(httpHeaders, null, null, null);
    }

    @Test
    public void putUserGroups_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.addUserToGroup(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.putUserGroups(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteUserGroups_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.removeUserFromGroup(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.deleteUserGroups(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void deleteUserGroups_callsCloud20Service_callsRemoveUserFromGroup() throws Exception {
        when(delegateCloud20Service.removeUserFromGroup(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.deleteUserGroups(httpHeaders, null, null, null);
        verify(delegateCloud20Service).removeUserFromGroup(httpHeaders, null, null, null);
    }

    @Test
    public void deleteUserGroups_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.removeUserFromGroup(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteUserGroups(httpHeaders, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteSoftDeletedUser_allowSoftDelete_callsGetCloud20Service() throws Exception {
        when(config.getBoolean("allowSoftDeleteDeletion")).thenReturn(true);
        when(delegateCloud20Service.deleteUserFromSoftDeleted(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.deleteSoftDeletedUser(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void deleteSoftDeletedUser_allowSoftDelete_callsGetCloud20Service_callsDeleteUserFromSoftDeleted() throws Exception {
        when(config.getBoolean("allowSoftDeleteDeletion")).thenReturn(true);
        when(delegateCloud20Service.deleteUserFromSoftDeleted(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.deleteSoftDeletedUser(httpHeaders, null, null);
        verify(delegateCloud20Service).deleteUserFromSoftDeleted(httpHeaders, null, null);
    }

    @Test
    public void deleteSoftDeletedUser_allowSoftDelete_responseOk_returns200() throws Exception {
        when(config.getBoolean("allowSoftDeleteDeletion")).thenReturn(true);
        when(delegateCloud20Service.deleteUserFromSoftDeleted(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteSoftDeletedUser(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test (expected = NotFoundException.class)
    public void deleteSOftDeletedUser_softDeleteNotAllowed_thrwosNotFoundException() throws Exception {
        spy.deleteSoftDeletedUser(httpHeaders, null, null);
    }

    @Test
    public void getCloud20Service_useCloudAuth_returnsDelegateCloud20Service() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        Cloud20Service result = cloud20VersionResource.getCloud20Service();
        assertThat("service", result instanceof DelegateCloud20Service, equalTo(true));
    }

    @Test
    public void getCloud20Service_notUseCloudAuth_returnsDefaultCloud20Service() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        Cloud20Service result = cloud20VersionResource.getCloud20Service();
        assertThat("service", result instanceof DefaultCloud20Service, equalTo(true));
    }

    @Test
    public void getUserById_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.getUserById(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getUserById(httpHeaders, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void getUserById_callsCloud20Service_callsgetUserById() throws Exception {
        when(delegateCloud20Service.getUserById(httpHeaders, null, null)).thenReturn(Response.ok());
        spy.getUserById(httpHeaders, null, null);
        verify(delegateCloud20Service).getUserById(httpHeaders, null, null);
    }

    @Test
    public void getUserById_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.getUserById(httpHeaders, null, null)).thenReturn(Response.ok());
        Response result = spy.getUserById(httpHeaders, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void listUsersWithRole_responseOk_returns200() throws Exception {
        when(defaultCloud20Service.listUsersWithRole(httpHeaders, uriInfo, "token", "3", null, null)).thenReturn(Response.ok());
        Response response = spy.listUsersWithRole(httpHeaders, uriInfo, "token", "3", null, null);
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void listUsersWithRole_callsDefaultCloud20Service_listUsersWithRole() throws Exception {
        when(defaultCloud20Service.listUsersWithRole(httpHeaders, uriInfo, "token", "3", null, null)).thenReturn(Response.ok());
        spy.listUsersWithRole(httpHeaders, uriInfo, "token", "3", null, null);
        verify(defaultCloud20Service).listUsersWithRole(httpHeaders, uriInfo, "token", "3", null, null);
    }

    @Test
    public void revokeToken_callsDefaultCloud20Service() throws Exception {
        String token = "1234567890";
        when(defaultCloud20Service.revokeToken(httpHeaders, token)).thenReturn(Response.ok());
        spy.revokeToken(httpHeaders, token);
        verify(defaultCloud20Service).revokeToken(httpHeaders, token);
    }

    @Test
    public void revokeUserToken_callsDefaultCloud20Service() throws Exception {
        String token = "1234567890";
        when(defaultCloud20Service.revokeToken(httpHeaders, token, token)).thenReturn(Response.ok());
        spy.revokeUserToken(httpHeaders, token, token);
        verify(defaultCloud20Service).revokeToken(httpHeaders, token, token);
    }

}
