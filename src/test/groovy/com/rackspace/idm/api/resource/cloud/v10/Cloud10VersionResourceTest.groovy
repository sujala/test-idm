package com.rackspace.idm.api.resource.cloud.v10;

import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.api.resource.cloud.v20.AuthResponseTuple;
import com.rackspace.idm.api.resource.cloud.v20.AuthWithApiKeyCredentials
import com.rackspace.idm.api.security.AuthenticationContext
import com.rackspace.idm.api.security.RequestContextHolder;
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
import org.junit.Test
import spock.lang.Shared
import spock.lang.Specification;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class Cloud10VersionResourceTest extends Specification {
    @Shared CloudClient cloudClient;
    @Shared ScopeAccessService scopeAccessService;
    @Shared EndpointService endpointService;
    @Shared EndpointConverterCloudV11 endpointConverterCloudV11;
    @Shared Cloud10VersionResource cloud10VersionResource;
    @Shared AuthWithApiKeyCredentials authWithApiKeyCredentials;
    @Shared AuthorizationService authorizationService;

    public void setup() {
        cloudClient = mock(CloudClient.class);
        scopeAccessService = mock(ScopeAccessService.class);
        endpointConverterCloudV11 = mock(EndpointConverterCloudV11.class);
        endpointService = mock(EndpointService.class);
        authWithApiKeyCredentials = mock(AuthWithApiKeyCredentials.class);
        authorizationService = mock(AuthorizationService.class);
        cloud10VersionResource = new Cloud10VersionResource(scopeAccessService, endpointConverterCloudV11, authWithApiKeyCredentials, authorizationService);
        cloud10VersionResource.requestContextHolder = Mock(RequestContextHolder)
        cloud10VersionResource.requestContextHolder.getAuthenticationContext() >> Mock(AuthenticationContext)
    }

    def getCloud10VersionInfo_withBlankUsername_returns401Status() {
        when:
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "   ", null, null, null);

        then:
        assertThat("response status", response.getStatus(), equalTo(401))
    }

    def getCloud10VersionInfo_withNullUsername_returns401Status() {
        when:
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, null, null, null, null);

        then:
        assertThat("response status", response.getStatus(), equalTo(401));
    }

    def getCloud10VersionInfo_notRouting_withNullUser_returnsUnauthorizedResponse() {
        when:
        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenThrow(new NotAuthenticatedException());
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);

        then:
        assertEquals("response status", response.getStatus(), 401);
    }

    def getCloud10VersionInfo_notRouting_withNonNullUser_setsXAuthTokenHeader() {
        given:
        User user = new User();
        UserScopeAccess userScopeAccess = mock(UserScopeAccess.class);
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(anyString(), anyString(), anyString())).thenReturn(userScopeAccess);
        when(userScopeAccess.getAccessTokenString()).thenReturn("token");
        when(endpointConverterCloudV11.toServiceCatalog(anyList())).thenReturn(new ServiceCatalog());

        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(user, true));
        AuthResponseTuple authResponseTuple = new AuthResponseTuple(user, userScopeAccess);
        when(scopeAccessService.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(authResponseTuple);
        when(scopeAccessService.getServiceCatalogInfo(user)).thenReturn(new ServiceCatalogInfo());

        when:
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);

        then:
        assertThat("response token", response.getMetadata().getFirst("X-Auth-Token").toString(), equalTo("token"));
    }

    def getCloud10VersionInfo_notRouting_withDisabledUser_returns403Status() {
        given:
        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenThrow(new UserDisabledException());

        when:
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);

        then:
        assertThat("response token", response.getStatus(), equalTo(403));
    }

    def getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesCdnService_withEndpoints_setsProperHeaders() throws Exception {
        given:
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

        when:
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);

        then:
        assertThat("response header", response.getMetadata().getFirst("X-CDN-Management-Url").toString(), equalTo("publicUrl"));
    }

    def getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesCdnService_withEndpoints_withEmptyPublicUrl_doesNotAddHeader() throws Exception {
        given:
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

        when:
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);

        then:
        assertThat("response header", response.getMetadata().toString().contains("X-CDN-Management-Url"), equalTo(false));
    }

    def getCloud10VersionInfo_notRouting_withNonNullUser_withCloudServersService_withEndpoints_setsProperHeaders() throws Exception {
        given:
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

        when:
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);

        then:
        assertThat("response header", response.getMetadata().getFirst("X-Server-Management-Url").toString(), equalTo("publicUrl"));
    }

    def getCloud10VersionInfo_notRouting_withNonNullUser_withCloudFilesCdnService_withMultipleEndpoints_setsV1DefaultProperHeaders() throws Exception {
        given:
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

        when:
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);

        then:
        assertThat("response header", response.getMetadata().getFirst("X-CDN-Management-Url").toString(), equalTo("publicUrl2"));
    }

    def getCloud10VersionInfo_notRouting_withNonNullUser_withCloudServersService_withMultipleEndpoints_setsV1DefaultProperHeaders() throws Exception {
        given:
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

        when:
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);

        then:
        assertThat("response header", response.getMetadata().getFirst("X-Server-Management-Url").toString(), equalTo("publicUrl2"));
    }

    def getCloud10VersionInfo_notRouting_withNonNullUser_withNoServices_setsNoHeaders() throws Exception {
        given:
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

        when:
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);

        then:
        assertThat("response header", response.getMetadata().getFirst("X-Server-Management-Url"), nullValue());
        assertThat("response header", response.getMetadata().getFirst("X-CDN-Management-Url"), nullValue());
        assertThat("response header", response.getMetadata().getFirst("X-Storage-Url"), nullValue());
        assertThat("response header", response.getMetadata().getFirst("X-Storage-Internal-Url"), nullValue());
        assertThat("response header", response.getMetadata().getFirst("X-Storage-token"), nullValue());
    }

    def getCloud10VersionInfo_notRouting_withNonNullUser_setsCacheControlHeader() throws Exception {
        given:
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

        when:
        Response response = cloud10VersionResource.getCloud10VersionInfo(null, "username", "password", null, null);

        then:
        assertThat("response header", response.getMetadata().getFirst("Cache-Control"), notNullValue());
    }

}
