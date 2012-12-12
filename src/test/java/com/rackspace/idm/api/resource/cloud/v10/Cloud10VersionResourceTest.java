package com.rackspace.idm.api.resource.cloud.v10;

import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspacecloud.docs.auth.api.v1.Endpoint;
import com.rackspacecloud.docs.auth.api.v1.Service;
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/26/12
 * Time: 10:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class Cloud10VersionResourceTest {
    Configuration config;
    CloudClient cloudClient;
    ScopeAccessService scopeAccessService;
    EndpointService endpointService;
    EndpointConverterCloudV11 endpointConverterCloudV11;
    UserService userService;
    Cloud10VersionResource cloud10VersionResource;
    TenantService tenantService;
    
    @Before
    public void setUp() throws Exception {
        tenantService = mock(TenantService.class);
        config = mock(Configuration.class);
        cloudClient = mock(CloudClient.class);
        scopeAccessService = mock(ScopeAccessService.class);
        endpointConverterCloudV11 = mock(EndpointConverterCloudV11.class);
        endpointService = mock(EndpointService.class);
        userService = mock(UserService.class);
        cloud10VersionResource = new Cloud10VersionResource(config, cloudClient, scopeAccessService, endpointConverterCloudV11, userService);

        cloud10VersionResource.setTenantService(tenantService);
        cloud10VersionResource.setEndpointService(endpointService);
    }

    @Test
    public void getCloud10VersionInfo_withBlankUsername_returns401Status() throws Exception {
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "   ", null, null, null);
        assertThat("response status", response.getStatus(), equalTo(401));
    }

    @Test
    public void getCloud10VersionInfo_withNullUsername_returns401Status() throws Exception {
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, null, null, null, null);
        assertThat("response status", response.getStatus(), equalTo(401));
    }

    @Test
    public void getCloud10VersionInfo_usingCloudAuthAndNotMigratedUser_with204ResponseAndNotNullStorageUser_callsScopeAccessService_updateUserScopeAccessTokenForClientIdByUser() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(userService.isMigratedUser(any(User.class))).thenReturn(false);
        when(config.getBoolean("useCloudAuth", false)).thenReturn(true);
        when(cloudClient.get(anyString(), any(HttpHeaders.class))).thenReturn(Response.status(204).header("x-auth-token", "token"));
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, null, null, "username", "password");
        verify(scopeAccessService).updateUserScopeAccessTokenForClientIdByUser(any(User.class), anyString(), anyString(), any(Date.class));
        assertThat("response status", response.getStatus(), equalTo(204));
    }

    @Test
    public void getCloud10VersionInfo_usingCloudAuthAndNotMigratedUser_with204ResponseAndNotNullUser_callsScopeAccessService_updateUserScopeAccessTokenForClientIdByUser() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(userService.isMigratedUser(any(User.class))).thenReturn(false);
        when(config.getBoolean("useCloudAuth", false)).thenReturn(true);
        when(cloudClient.get(anyString(), any(HttpHeaders.class))).thenReturn(Response.status(204).header("x-auth-token", "token"));
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        verify(scopeAccessService).updateUserScopeAccessTokenForClientIdByUser(any(User.class), anyString(), anyString(), any(Date.class));
        assertThat("response status", response.getStatus(), equalTo(204));
    }

    @Test
    public void getCloud10VersionInfo_usingCloudAuthAndNotMigratedUser_withAnyResponseAndNullUser_DoesNotCallScopeAccessService_updateUserScopeAccessTokenForClientIdByUser() throws Exception {
        when(userService.getUser("username")).thenReturn(null);
        when(userService.isMigratedUser(any(User.class))).thenReturn(false);
        when(config.getBoolean("useCloudAuth", false)).thenReturn(true);
        when(cloudClient.get(anyString(), any(HttpHeaders.class))).thenReturn(Response.status(204));
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        verify(scopeAccessService, never()).updateUserScopeAccessTokenForClientIdByUser(any(User.class), anyString(), anyString(), any(Date.class));
        assertThat("response status", response.getStatus(), equalTo(204));
    }

    @Test
    public void getCloud10VersionInfo_usingCloudAuthAndNotMigratedUser_withNon204ResponseAndUser_DoesNotCallScopeAccessService_updateUserScopeAccessTokenForClientIdByUser() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(userService.isMigratedUser(any(User.class))).thenReturn(false);
        when(config.getBoolean("useCloudAuth", false)).thenReturn(true);
        when(cloudClient.get(anyString(), any(HttpHeaders.class))).thenReturn(Response.status(404));
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(new ServiceCatalog());
        when(userScopeAccess.getAccessTokenExp()).thenReturn(new DateTime(1).toDate());
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        verify(scopeAccessService, never()).updateUserScopeAccessTokenForClientIdByUser(any(User.class), anyString(), anyString(), any(Date.class));
        assertThat("response status", response.getStatus(), equalTo(204));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNullUser_returnsUnauthorizedResponse() throws Exception {
        when(userService.getUser("username")).thenReturn(null);
        when(config.getBoolean("useCloudAuth", false)).thenReturn(false);
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response status", response.getStatus(), equalTo(401));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_setsXAuthTokenHeader() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(config.getBoolean("useCloudAuth", false)).thenReturn(false);
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(new ServiceCatalog());
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response token", response.getMetadata().getFirst("X-Auth-Token").toString(), equalTo("token"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNotAuthenticatedUser_returns401Status() throws Exception {
        User user = new User();
        when(userService.getUser("username")).thenReturn(user);
        when(userService.isMigratedUser(user)).thenReturn(true);
        when(config.getBoolean("useCloudAuth", false)).thenReturn(false);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenThrow(new NotAuthenticatedException());
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response token", response.getStatus(), equalTo(401));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withDisabledUser_returns403Status() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(config.getBoolean("useCloudAuth", false)).thenReturn(false);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenThrow(new UserDisabledException());
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response token", response.getStatus(), equalTo(403));
    }

    @Ignore
    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesService_withEndpoints_setsProperHeaders() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(config.getBoolean("useCloudAuth", false)).thenReturn(false);
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        Service service = new Service();
        Endpoint endpoint = new Endpoint();
        endpoint.setPublicURL("publicUrl");
        endpoint.setInternalURL("internalUrl");
        service.getEndpoint().add(endpoint);
        service.setName("cloudFiles");
        serviceCatalog.getService().add(service);
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(serviceCatalog);


        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-Storage-Url").toString(), equalTo("publicUrl"));
        assertThat("response header", response.getMetadata().getFirst("X-Storage-Internal-Url").toString(), equalTo("internalUrl"));
        assertThat("response header", response.getMetadata().getFirst("X-Storage-token").toString(), equalTo("token"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesCdnService_withEndpoints_setsProperHeaders() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(config.getBoolean("useCloudAuth", false)).thenReturn(false);
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        Service service = new Service();
        Endpoint endpoint = new Endpoint();
        endpoint.setPublicURL("publicUrl");
        service.getEndpoint().add(endpoint);
        service.setName("cloudFilesCDN");
        serviceCatalog.getService().add(service);
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(serviceCatalog);


        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-CDN-Management-Url").toString(), equalTo("publicUrl"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesCdnService_withEndpoints_withEmptyPublicUrl_doesNotAddHeader() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(config.getBoolean("useCloudAuth", false)).thenReturn(false);
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        Service service = new Service();
        Endpoint endpoint = new Endpoint();
        endpoint.setPublicURL("");
        service.getEndpoint().add(endpoint);
        service.setName("cloudFilesCDN");
        serviceCatalog.getService().add(service);
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(serviceCatalog);


        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().toString().contains("X-CDN-Management-Url"), equalTo(false));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudServersService_withEndpoints_setsProperHeaders() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(config.getBoolean("useCloudAuth", false)).thenReturn(false);
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        Service service = new Service();
        Endpoint endpoint = new Endpoint();
        endpoint.setPublicURL("publicUrl");
        service.getEndpoint().add(endpoint);
        service.setName("cloudServers");
        serviceCatalog.getService().add(service);
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(serviceCatalog);


        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-Server-Management-Url").toString(), equalTo("publicUrl"));
    }

    @Ignore
    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesService_withMultipleEndpoints_setsV1DefaultProperHeaders() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(config.getBoolean("useCloudAuth", false)).thenReturn(false);
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        Service service = new Service();
        Endpoint endpoint = new Endpoint();
        endpoint.setPublicURL("publicUrl");
        endpoint.setInternalURL("internalUrl");
        Endpoint endpoint2 = new Endpoint();
        endpoint2.setPublicURL("publicUrl2");
        endpoint2.setInternalURL("internalUrl2");
        endpoint2.setV1Default(true);
        service.getEndpoint().add(endpoint2);
        service.getEndpoint().add(endpoint);
        service.setName("cloudFiles");
        serviceCatalog.getService().add(service);
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(serviceCatalog);


        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-Storage-Url").toString(), equalTo("publicUrl2"));
        assertThat("response header", response.getMetadata().getFirst("X-Storage-Internal-Url").toString(), equalTo("internalUrl2"));
        assertThat("response header", response.getMetadata().getFirst("X-Storage-token").toString(), equalTo("token"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesService_withMultipleEndpoints_v1DefaultProperHeaders() throws Exception {
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        Tenant tenant = new Tenant();
        tenant.setTenantId("1");
        CloudBaseUrl baseURL = new CloudBaseUrl();
        baseURL.setServiceName("cloudFiles");
        baseURL.setUniqueId("someId");
        baseURL.setPublicUrl("http://someEndpoint");
        tenant.setV1Defaults((new String[]{"123"}));
        openstackEndpoint.setBaseUrls(Arrays.asList(baseURL));
        openstackEndpoint.setTenantId(tenant.getTenantId());
        List<OpenstackEndpoint> cloudEndpoint = Arrays.asList(openstackEndpoint);
        when(userService.getUser("username")).thenReturn(new User());
        when(config.getBoolean("useCloudAuth", false)).thenReturn(false);
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(tenantService.getTenant(anyString())).thenReturn(tenant);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(userScopeAccess)).thenReturn(cloudEndpoint);
        when(endpointService.getBaseUrlById(anyInt())).thenReturn(baseURL);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        Service service = new Service();
        Endpoint endpoint = new Endpoint();
        endpoint.setPublicURL("publicUrl");
        endpoint.setInternalURL("internalUrl");
        Endpoint endpoint2 = new Endpoint();
        endpoint2.setPublicURL("publicUrl2");
        endpoint2.setInternalURL("internalUrl2");
        endpoint2.setV1Default(true);
        Endpoint endpoint3 = new Endpoint();
        endpoint3.setPublicURL(baseURL.getPublicUrl()+ "/1");
        endpoint3.setV1Default(true);
        service.getEndpoint().add(endpoint3);
        service.getEndpoint().add(endpoint2);
        service.getEndpoint().add(endpoint);
        service.setName("cloudFiles");
        serviceCatalog.getService().add(service);
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(serviceCatalog);


        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-Storage-Url").toString(), equalTo("http://someEndpoint/1"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesCdnService_withMultipleEndpoints_setsV1DefaultProperHeaders() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(config.getBoolean("useCloudAuth", false)).thenReturn(false);
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        Service service = new Service();
        Endpoint endpoint = new Endpoint();
        endpoint.setPublicURL("publicUrl");
        Endpoint endpoint2 = new Endpoint();
        endpoint2.setPublicURL("publicUrl2");
        endpoint2.setV1Default(true);
        service.getEndpoint().add(endpoint2);
        service.getEndpoint().add(endpoint);
        service.setName("cloudFilesCDN");
        serviceCatalog.getService().add(service);
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(serviceCatalog);


        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-CDN-Management-Url").toString(), equalTo("publicUrl2"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudServersService_withMultipleEndpoints_setsV1DefaultProperHeaders() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(config.getBoolean("useCloudAuth", false)).thenReturn(false);
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        Service service = new Service();
        Endpoint endpoint = new Endpoint();
        endpoint.setPublicURL("publicUrl");
        Endpoint endpoint2 = new Endpoint();
        endpoint2.setPublicURL("publicUrl2");
        endpoint2.setV1Default(true);
        service.getEndpoint().add(endpoint2);
        service.getEndpoint().add(endpoint);
        service.setName("cloudServers");
        serviceCatalog.getService().add(service);
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(serviceCatalog);


        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-Server-Management-Url").toString(), equalTo("publicUrl2"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withNoServices_setsNoHeaders() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(config.getBoolean("useCloudAuth", false)).thenReturn(false);
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(serviceCatalog);


        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-Server-Management-Url"), nullValue());
        assertThat("response header", response.getMetadata().getFirst("X-CDN-Management-Url"), nullValue());
        assertThat("response header", response.getMetadata().getFirst("X-Storage-Url"), nullValue());
        assertThat("response header", response.getMetadata().getFirst("X-Storage-Internal-Url"), nullValue());
        assertThat("response header", response.getMetadata().getFirst("X-Storage-token"), nullValue());
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_setsCacheControlHeader() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
        when(config.getBoolean("useCloudAuth", false)).thenReturn(false);
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(serviceCatalog);
        when(userScopeAccess.getAccessTokenExp()).thenReturn(new DateTime(1).toDate());

        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("Cache-Control"), notNullValue());
    }
}
