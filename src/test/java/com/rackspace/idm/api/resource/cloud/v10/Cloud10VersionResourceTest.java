package com.rackspace.idm.api.resource.cloud.v10;

import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.api.resource.cloud.v20.AuthResponseTuple;
import com.rackspace.idm.api.resource.cloud.v20.AuthWithApiKeyCredentials;
import com.rackspace.idm.api.resource.cloud.v20.MultiFactorCloud20Service;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
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
import static org.junit.Assert.assertEquals;
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
    MultiFactorCloud20Service multiFactorCloud20Service;
    AuthWithApiKeyCredentials authWithApiKeyCredentials;
    AuthorizationService authorizationService;
    IdentityConfig identityConfig;
    IdentityConfig.StaticConfig staticConfig;
    IdentityConfig.ReloadableConfig reloadableConfig;

    @Before
    public void setUp() throws Exception {
        tenantService = mock(TenantService.class);
        config = mock(Configuration.class);
        cloudClient = mock(CloudClient.class);
        scopeAccessService = mock(ScopeAccessService.class);
        endpointConverterCloudV11 = mock(EndpointConverterCloudV11.class);
        endpointService = mock(EndpointService.class);
        userService = mock(UserService.class);
        multiFactorCloud20Service = mock(MultiFactorCloud20Service.class);
        authWithApiKeyCredentials = mock(AuthWithApiKeyCredentials.class);
        authorizationService = mock(AuthorizationService.class);
        identityConfig = mock(IdentityConfig.class);
        staticConfig = mock(IdentityConfig.StaticConfig.class);
        reloadableConfig = mock(IdentityConfig.ReloadableConfig.class);
        when(identityConfig.getStaticConfig()).thenReturn(staticConfig);
        when(identityConfig.getReloadableConfig()).thenReturn(reloadableConfig);
        cloud10VersionResource = new Cloud10VersionResource(config, scopeAccessService, endpointConverterCloudV11, userService, multiFactorCloud20Service, authWithApiKeyCredentials, tenantService, authorizationService, identityConfig);
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
    public void getCloud10VersionInfo_notRouting_withNullUser_returnsUnauthorizedResponse() throws Exception {
        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenThrow(new NotAuthenticatedException());
        when(userService.getUser("username")).thenReturn(null);
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertEquals("response status", response.getStatus(), 401);
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_setsXAuthTokenHeader() throws Exception {
        User user = new User();
        when(userService.getUser("username")).thenReturn(user);
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(new ServiceCatalog());

        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(user, true));
        AuthResponseTuple authResponseTuple = new AuthResponseTuple();
        authResponseTuple.setUser(user);
        authResponseTuple.setUserScopeAccess(userScopeAccess);
        when(authWithApiKeyCredentials.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
        when(scopeAccessService.getServiceCatalogInfo(user)).thenReturn(new ServiceCatalogInfo());

        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response token", response.getMetadata().getFirst("X-Auth-Token").toString(), equalTo("token"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withDisabledUser_returns403Status() throws Exception {
        User user = new User();
        when(userService.getUser("username")).thenReturn(user);
        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenThrow(new UserDisabledException());
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response token", response.getStatus(), equalTo(403));
    }

    @Ignore
    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesService_withEndpoints_setsProperHeaders() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
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
        User user = new User();
        when(userService.getUser("username")).thenReturn(user);
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

        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(user, true));
        AuthResponseTuple authResponseTuple = new AuthResponseTuple();
        authResponseTuple.setUser(user);
        authResponseTuple.setUserScopeAccess(userScopeAccess);
        when(authWithApiKeyCredentials.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
        when(scopeAccessService.getServiceCatalogInfo(user)).thenReturn(new ServiceCatalogInfo());

        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-CDN-Management-Url").toString(), equalTo("publicUrl"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesCdnService_withEndpoints_withEmptyPublicUrl_doesNotAddHeader() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
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
        User user = new User();

        when(userService.getUser("username")).thenReturn(user);
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

        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(user, true));
        AuthResponseTuple authResponseTuple = new AuthResponseTuple();
        authResponseTuple.setUser(user);
        authResponseTuple.setUserScopeAccess(userScopeAccess);
        when(authWithApiKeyCredentials.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
        when(scopeAccessService.getServiceCatalogInfo(user)).thenReturn(new ServiceCatalogInfo());

        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-Server-Management-Url").toString(), equalTo("publicUrl"));
    }

    @Ignore
    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesService_withMultipleEndpoints_setsV1DefaultProperHeaders() throws Exception {
        when(userService.getUser("username")).thenReturn(new User());
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
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesCdnService_withMultipleEndpoints_setsV1DefaultProperHeaders() throws Exception {
        User user = new User();
        when(userService.getUser("username")).thenReturn(user);
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


        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(user, true));
        AuthResponseTuple authResponseTuple = new AuthResponseTuple();
        authResponseTuple.setUser(user);
        authResponseTuple.setUserScopeAccess(userScopeAccess);
        when(authWithApiKeyCredentials.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
        when(scopeAccessService.getServiceCatalogInfo(user)).thenReturn(new ServiceCatalogInfo());

        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-CDN-Management-Url").toString(), equalTo("publicUrl2"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudServersService_withMultipleEndpoints_setsV1DefaultProperHeaders() throws Exception {
        final User user = new User();
        when(userService.getUser("username")).thenReturn(user);
        final UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
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

        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(user, true));
        AuthResponseTuple authResponseTuple = new AuthResponseTuple();
        authResponseTuple.setUser(user);
        authResponseTuple.setUserScopeAccess(userScopeAccess);
        when(authWithApiKeyCredentials.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
        when(scopeAccessService.getServiceCatalogInfo(user)).thenReturn(new ServiceCatalogInfo());

        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-Server-Management-Url").toString(), equalTo("publicUrl2"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withNoServices_setsNoHeaders() throws Exception {
        User user = new User();
        when(userService.getUser("username")).thenReturn(new User());
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(serviceCatalog);

        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(user, true));
        AuthResponseTuple authResponseTuple = new AuthResponseTuple();
        authResponseTuple.setUser(user);
        authResponseTuple.setUserScopeAccess(userScopeAccess);
        when(authWithApiKeyCredentials.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
        when(scopeAccessService.getServiceCatalogInfo(user)).thenReturn(new ServiceCatalogInfo());

        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-Server-Management-Url"), nullValue());
        assertThat("response header", response.getMetadata().getFirst("X-CDN-Management-Url"), nullValue());
        assertThat("response header", response.getMetadata().getFirst("X-Storage-Url"), nullValue());
        assertThat("response header", response.getMetadata().getFirst("X-Storage-Internal-Url"), nullValue());
        assertThat("response header", response.getMetadata().getFirst("X-Storage-token"), nullValue());
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_setsCacheControlHeader() throws Exception {
        User user = new User();

        when(userService.getUser("username")).thenReturn(new User());
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(serviceCatalog);
        when(userScopeAccess.getAccessTokenExp()).thenReturn(new DateTime(1).toDate());

        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(user, true));
        AuthResponseTuple authResponseTuple = new AuthResponseTuple();
        authResponseTuple.setUser(user);
        authResponseTuple.setUserScopeAccess(userScopeAccess);
        when(authWithApiKeyCredentials.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
        when(scopeAccessService.getServiceCatalogInfo(user)).thenReturn(new ServiceCatalogInfo());

        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("Cache-Control"), notNullValue());
    }
}
