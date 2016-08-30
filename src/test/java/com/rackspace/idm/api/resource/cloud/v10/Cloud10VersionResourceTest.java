package com.rackspace.idm.api.resource.cloud.v10;

import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.api.resource.cloud.v20.AuthResponseTuple;
import com.rackspace.idm.api.resource.cloud.v20.AuthWithApiKeyCredentials;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.ServiceCatalogInfo;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.UserDisabledException;
import com.rackspacecloud.docs.auth.api.v1.Endpoint;
import com.rackspacecloud.docs.auth.api.v1.Service;
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class Cloud10VersionResourceTest {
    CloudClient cloudClient;
    ScopeAccessService scopeAccessService;
    EndpointService endpointService;
    EndpointConverterCloudV11 endpointConverterCloudV11;
    Cloud10VersionResource cloud10VersionResource;
    AuthWithApiKeyCredentials authWithApiKeyCredentials;
    AuthorizationService authorizationService;

    @Before
    public void setUp() throws Exception {
        cloudClient = mock(CloudClient.class);
        scopeAccessService = mock(ScopeAccessService.class);
        endpointConverterCloudV11 = mock(EndpointConverterCloudV11.class);
        endpointService = mock(EndpointService.class);
        authWithApiKeyCredentials = mock(AuthWithApiKeyCredentials.class);
        authorizationService = mock(AuthorizationService.class);
        cloud10VersionResource = new Cloud10VersionResource(scopeAccessService, endpointConverterCloudV11, authWithApiKeyCredentials, authorizationService);
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
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertEquals("response status", response.getStatus(), 401);
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_setsXAuthTokenHeader() throws Exception {
        User user = new User();
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(new ServiceCatalog());

        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(user, true));
        AuthResponseTuple authResponseTuple = new AuthResponseTuple(user, userScopeAccess);
        when(scopeAccessService.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
        when(scopeAccessService.getServiceCatalogInfo(user)).thenReturn(new ServiceCatalogInfo());

        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response token", response.getMetadata().getFirst("X-Auth-Token").toString(), equalTo("token"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withDisabledUser_returns403Status() throws Exception {
        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenThrow(new UserDisabledException());
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response token", response.getStatus(), equalTo(403));
    }

    @Ignore
    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesService_withEndpoints_setsProperHeaders() throws Exception {
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
        AuthResponseTuple authResponseTuple = new AuthResponseTuple(user, userScopeAccess);
        when(scopeAccessService.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
        when(scopeAccessService.getServiceCatalogInfo(user)).thenReturn(new ServiceCatalogInfo());

        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-CDN-Management-Url").toString(), equalTo("publicUrl"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesCdnService_withEndpoints_withEmptyPublicUrl_doesNotAddHeader() throws Exception {
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
        AuthResponseTuple authResponseTuple = new AuthResponseTuple(user, userScopeAccess);
        when(scopeAccessService.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
        when(scopeAccessService.getServiceCatalogInfo(user)).thenReturn(new ServiceCatalogInfo());

        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-Server-Management-Url").toString(), equalTo("publicUrl"));
    }

    @Ignore
    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesService_withMultipleEndpoints_setsV1DefaultProperHeaders() throws Exception {
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
        AuthResponseTuple authResponseTuple = new AuthResponseTuple(user, userScopeAccess);
        when(scopeAccessService.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
        when(scopeAccessService.getServiceCatalogInfo(user)).thenReturn(new ServiceCatalogInfo());

        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-CDN-Management-Url").toString(), equalTo("publicUrl2"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withCloudServersService_withMultipleEndpoints_setsV1DefaultProperHeaders() throws Exception {
        final User user = new User();
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
        AuthResponseTuple authResponseTuple = new AuthResponseTuple(user, userScopeAccess);
        when(scopeAccessService.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
        when(scopeAccessService.getServiceCatalogInfo(user)).thenReturn(new ServiceCatalogInfo());

        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("X-Server-Management-Url").toString(), equalTo("publicUrl2"));
    }

    @Test
    public void getCloud10VersionInfo_notRouting_withNonNullUser_withNoServices_setsNoHeaders() throws Exception {
        User user = new User();
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(serviceCatalog);

        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(user, true));
        AuthResponseTuple authResponseTuple = new AuthResponseTuple(user, userScopeAccess);
        when(scopeAccessService.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
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
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        ServiceCatalog serviceCatalog = new ServiceCatalog();
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(serviceCatalog);
        when(userScopeAccess.getAccessTokenExp()).thenReturn(new DateTime(1).toDate());

        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(user, true));
        AuthResponseTuple authResponseTuple = new AuthResponseTuple(user, userScopeAccess);
        when(scopeAccessService.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
        when(scopeAccessService.getServiceCatalogInfo(user)).thenReturn(new ServiceCatalogInfo());

        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);
        assertThat("response header", response.getMetadata().getFirst("Cache-Control"), notNullValue());
    }
}
