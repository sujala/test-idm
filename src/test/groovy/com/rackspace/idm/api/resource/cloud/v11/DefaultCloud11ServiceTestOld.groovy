package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.converter.cloudv11.AuthConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import com.rackspace.idm.api.resource.cloud.CloudExceptionResponse;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.v20.AuthResponseTuple;
import com.rackspace.idm.api.resource.cloud.v20.AuthWithApiKeyCredentials;
import com.rackspace.idm.api.resource.cloud.v20.AuthWithPasswordCredentials
import com.rackspace.idm.api.security.AuthenticationContext
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.CloudAdminAuthorizationException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.Validator;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import com.rackspacecloud.docs.auth.api.v1.User;
import com.sun.jersey.api.uri.UriBuilderImpl;
import com.sun.jersey.core.util.Base64;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.mortbay.jetty.HttpHeaders;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest
import spock.lang.Specification;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;


public class DefaultCloud11ServiceTestOld extends Specification {

    AuthorizationService authorizationService;
    DefaultCloud11Service defaultCloud11Service;
    CredentialUnmarshaller credentialUnmarshaller;
    UserConverterCloudV11 userConverterCloudV11;
    UserValidator userValidator;
    UserService userService;
    EndpointService endpointService;
    EndpointConverterCloudV11 endpointConverterCloudV11;
    Configuration config;
    UriInfo uriInfo;
    TenantService tenantService;
    AuthHeaderHelper authHeaderHelper;
    User user = new User();
    com.rackspace.idm.domain.entity.User userDO = new com.rackspace.idm.domain.entity.User();
    HttpServletRequest request;
    private ScopeAccessService scopeAccessService;
    UserScopeAccess userScopeAccess;
    ImpersonatedScopeAccess impersonatedScopeAccess;
    javax.ws.rs.core.HttpHeaders httpHeaders;
    CloudExceptionResponse cloudExceptionResponse;
    Application application = new Application("id", "myApp");
    AtomHopperClient atomHopperClient;
    GroupService userGroupService, cloudGroupService;
    AuthConverterCloudV11 authConverterCloudv11;
    CredentialValidator credentialValidator;
    private CloudContractDescriptionBuilder cloudContratDescriptionBuilder;
    Validator validator;
    Tenant tenant;
    TokenRevocationService tokenRevocationService;
    AuthWithApiKeyCredentials authWithApiKeyCredentials;
    AuthWithPasswordCredentials authWithPasswordCredentials;
    private IdentityConfig identityConfig;

    def setup() {
        authWithApiKeyCredentials = mock(AuthWithApiKeyCredentials.class);
        authWithPasswordCredentials = mock(AuthWithPasswordCredentials.class);
        tokenRevocationService = mock(TokenRevocationService.class);
        userConverterCloudV11 = mock(UserConverterCloudV11.class);
        authConverterCloudv11 = mock(AuthConverterCloudV11.class);
        authHeaderHelper = mock(AuthHeaderHelper.class);
        credentialUnmarshaller = mock(CredentialUnmarshaller.class);
        cloudExceptionResponse = new CloudExceptionResponse();
        userService = mock(UserService.class);
        httpHeaders = mock(javax.ws.rs.core.HttpHeaders.class);
        scopeAccessService = mock(ScopeAccessService.class);
        userScopeAccess = mock(UserScopeAccess.class);
        impersonatedScopeAccess = mock(ImpersonatedScopeAccess.class);
        endpointService = mock(EndpointService.class);
        endpointConverterCloudV11 = mock(EndpointConverterCloudV11.class);
        uriInfo = mock(UriInfo.class);
        tenantService = mock(TenantService.class);
        config = mock(Configuration.class);
        request = mock(HttpServletRequest.class);
        userValidator = mock(UserValidator.class);
        authorizationService = mock(AuthorizationService.class);
        atomHopperClient = mock(AtomHopperClient.class);
        userGroupService = mock(GroupService.class);
        cloudGroupService = mock(GroupService.class);
        credentialValidator = mock(CredentialValidator.class);
        cloudContratDescriptionBuilder = mock(CloudContractDescriptionBuilder.class);
        validator = mock(Validator.class);
        tenant = new Tenant();
        tenant.setName("tenant");
        tenant.setEnabled(true);
        tenant.setTenantId("1");
        tenant.getBaseUrlIds().clear();

        userDO.setId("1");
        userDO.setMossoId(1);
        userDO.setNastId("nastId");

        identityConfig = mock(IdentityConfig.class);
        IdentityConfig.ReloadableConfig reloadableConfig = mock(IdentityConfig.ReloadableConfig.class);
        when(identityConfig.getReloadableConfig()).thenReturn(reloadableConfig);

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic YXV0aDphdXRoMTIz");
        UriBuilderImpl uriBuilder = mock(UriBuilderImpl.class);
        when(uriBuilder.build()).thenReturn(new URI(""));
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        com.rackspace.idm.domain.entity.User user1 = new com.rackspace.idm.domain.entity.User();
        user1.setId("userId");
        when(userConverterCloudV11.fromUser(user)).thenReturn(user1);
        when(config.getString("serviceName.cloudServers")).thenReturn("cloudServers");
        when(config.getString("serviceName.cloudFiles")).thenReturn("cloudFiles");
        application.setOpenStackType("foo");
        Application testService = new Application(null, "testService");
        testService.setOpenStackType("foo");
        defaultCloud11Service = new DefaultCloud11Service();
        defaultCloud11Service.setValidator(validator);
        defaultCloud11Service.setAuthorizationService(authorizationService);
        defaultCloud11Service.setAtomHopperClient(atomHopperClient);
        defaultCloud11Service.setCloudGroupService(cloudGroupService);
        defaultCloud11Service.setCredentialUnmarshaller(credentialUnmarshaller);
        defaultCloud11Service.setCredentialValidator(credentialValidator);
        defaultCloud11Service.setCloudContractDescriptionBuilder(cloudContratDescriptionBuilder);
        defaultCloud11Service.setAuthHeaderHelper(authHeaderHelper);
        defaultCloud11Service.setConfig(config);
        defaultCloud11Service.setScopeAccessService(scopeAccessService);
        defaultCloud11Service.setEndpointService(endpointService);
        defaultCloud11Service.setUserService(userService);
        defaultCloud11Service.setAuthConverterCloudv11(authConverterCloudv11);
        defaultCloud11Service.setUserConverterCloudV11(userConverterCloudV11);
        defaultCloud11Service.setEndpointConverterCloudV11(endpointConverterCloudV11);
        defaultCloud11Service.setCloudExceptionResponse(cloudExceptionResponse);
        defaultCloud11Service.setTenantService(tenantService);
        defaultCloud11Service.setTokenRevocationService(tokenRevocationService);
        defaultCloud11Service.setAuthWithApiKeyCredentials(authWithApiKeyCredentials);
        defaultCloud11Service.setAuthWithPasswordCredentials(authWithPasswordCredentials);
        defaultCloud11Service.setIdentityConfig(identityConfig);
        defaultCloud11Service.requestContextHolder = Mock(RequestContextHolder)
        defaultCloud11Service.requestContextHolder.getAuthenticationContext() >> Mock(AuthenticationContext)
    }

    def authenticateResponse_withNastCredential_redirects() {
        given:
        NastCredentials nastCredentials = new NastCredentials();

        when:
        Response response = defaultCloud11Service.authenticateResponse(uriInfo ,new JAXBElement<Credentials>(new QName(""), Credentials.class, nastCredentials)).build();

        then:
        assertThat("response status", response.getStatus(), equalTo(302));
    }

    def adminAuthenticateResponse_callsCredentialValidator_validateCredential() {
        given:
        NastCredentials nastCredentials = new NastCredentials();
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User();
        user.setUsername("name");
        when(userService.getUserByTenantId(anyString())).thenReturn(user);

        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(user, true));
        when(scopeAccessService.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(new AuthResponseTuple(user, new UserScopeAccess()));
        when(scopeAccessService.getServiceCatalogInfo(any(BaseUser.class))).thenReturn(new ServiceCatalogInfo());

        when:
        defaultCloud11Service.adminAuthenticateResponse(null, new JAXBElement<Credentials>(new QName(""), Credentials.class, nastCredentials));

        then:
        verify(credentialValidator).validateCredential(nastCredentials, userService);
    }

    def authenticateResponse_withMossoCredentials_redirects() {
        given:
        JAXBElement<MossoCredentials> credentials = new JAXBElement<MossoCredentials>(QName.valueOf("foo"), MossoCredentials.class, new MossoCredentials());

        when:
        Response response = defaultCloud11Service.authenticateResponse(uriInfo ,credentials).build();

        then:
        assertThat("response status", response.getStatus(), equalTo(302));
    }

    def authenticateResponse_withPasswordCredentials_redirects() {
        given:
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("username");
        passwordCredentials.setPassword("pass");
        JAXBElement<PasswordCredentials> credentials =
                new JAXBElement<PasswordCredentials>(QName.valueOf("foo"), PasswordCredentials.class, passwordCredentials);

        when:
        Response response = defaultCloud11Service.authenticateResponse(uriInfo ,credentials).build();

        then:
        assertThat("response status", response.getStatus(), equalTo(302));
    }

    def authenticateResponse_withUnknownCredentials_returns404Status() {
        given:
        Credentials passwordCredentials = Mock(Credentials)
        JAXBElement<Credentials> credentials =
                new JAXBElement<Credentials>(QName.valueOf("foo"), Credentials.class, passwordCredentials);

        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(uriInfo ,credentials);

        then:
        assertThat("Response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    def adminAuthenticateResponse_withPasswordCredentials_callsScopeAccessService_getUserScopeAccessForClientIdByUsernameAndPassword() {
        given:
        JAXBElement credentials = mock(JAXBElement.class);
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("username");
        passwordCredentials.setPassword("password");
        when(credentials.getValue()).thenReturn(passwordCredentials);
        userDO.setEnabled(true);

        when(userService.getUser("username")).thenReturn(userDO);
        when(authWithPasswordCredentials.authenticate(any(AuthenticationRequest.class))).thenReturn(new UserAuthenticationResult(userDO, true));
        when(scopeAccessService.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(new AuthResponseTuple(userDO, new UserScopeAccess()));
        when(scopeAccessService.getServiceCatalogInfo(any(BaseUser.class))).thenReturn(new ServiceCatalogInfo());

        when:
        Response.ResponseBuilder response = defaultCloud11Service.adminAuthenticateResponse(null, credentials);

        then:
        assertEquals(response.build().getStatus(), 200);
    }

    def adminAuthenticateResponse_withNastCredentials_callsUserService_getUserByNastId() {
        given:
        JAXBElement credentials = mock(JAXBElement.class);
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User();
        user.setUsername("name");
        when(credentials.getValue()).thenReturn(new NastCredentials());
        when(userService.getUserByTenantId(anyString())).thenReturn(user);

        when(userService.getUser("username")).thenReturn(userDO);
        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(userDO, true));
        when(scopeAccessService.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(new AuthResponseTuple(userDO, new UserScopeAccess()));
        when(scopeAccessService.getServiceCatalogInfo(any(BaseUser.class))).thenReturn(new ServiceCatalogInfo());

        when:
        defaultCloud11Service.adminAuthenticateResponse(null, credentials);

        then:
        verify(userService).getUserByTenantId(null) == null
    }

    def adminAuthenticateResponse_withMossoCredentials_callsUserService_getUserByMossoId() {
        given:
        JAXBElement credentials = mock(JAXBElement.class);
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User();
        user.setUsername("name");
        when(userService.getUserByTenantId(anyString())).thenReturn(user);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(mossoCredentials.getMossoId()).thenReturn(12345);

        when(userService.getUser("username")).thenReturn(userDO);
        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(userDO, true));
        when(scopeAccessService.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(new AuthResponseTuple(userDO, new UserScopeAccess()));
        when(scopeAccessService.getServiceCatalogInfo(any(BaseUser.class))).thenReturn(new ServiceCatalogInfo());

        when:
        defaultCloud11Service.adminAuthenticateResponse(null, credentials);

        then:
        verify(userService).getUserByTenantId("12345") == null
    }

    def adminAuthenticateResponse_withMossoCredentials_callsAuthConverterCloudV11_toCloudv11AuthDataJaxb() {
        given:
        JAXBElement credentials = mock(JAXBElement.class);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(mossoCredentials.getMossoId()).thenReturn(12345);
        when(userService.getUserByTenantId(anyString())).thenReturn(userDO);
        userDO.setEnabled(true);

        when(userService.getUser("username")).thenReturn(userDO);
        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(userDO, true));
        when(scopeAccessService.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(new AuthResponseTuple(userDO, new UserScopeAccess()));
        when(scopeAccessService.getServiceCatalogInfo(any(BaseUser.class))).thenReturn(new ServiceCatalogInfo());

        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(userDO.getUsername(), "apiKey", null)).thenReturn(new UserScopeAccess());

        when:
        defaultCloud11Service.adminAuthenticateResponse(null, credentials);

        then:
        verify(authConverterCloudv11).toCloudv11AuthDataJaxb(any(UserScopeAccess.class), anyList()) == null
    }

    def adminAuthenticateResponse_withMossoCredentials_returns200Status() {
        given:
        JAXBElement credentials = mock(JAXBElement.class);
        MossoCredentials mossoCredentials = mock(MossoCredentials.class);
        when(credentials.getValue()).thenReturn(mossoCredentials);
        when(mossoCredentials.getKey()).thenReturn("apiKey");
        when(mossoCredentials.getMossoId()).thenReturn(12345);
        when(userService.getUserByTenantId(anyString())).thenReturn(userDO);
        userDO.setEnabled(true);

        when(userService.getUser("username")).thenReturn(userDO);
        when(authWithApiKeyCredentials.authenticate(anyString(), anyString())).thenReturn(new UserAuthenticationResult(userDO, true));
        when(scopeAccessService.createScopeAccessForUserAuthenticationResult(any(UserAuthenticationResult.class))).thenReturn(new AuthResponseTuple(userDO, new UserScopeAccess()));
        when(scopeAccessService.getServiceCatalogInfo(any(BaseUser.class))).thenReturn(new ServiceCatalogInfo());

        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(userDO.getUsername(), "apiKey", null)).thenReturn(new UserScopeAccess());

        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.adminAuthenticateResponse(null, credentials);

        then:
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    def revokeToken_scopeAccessServiceReturnsNull_returnsNotFoundResponse() {
        given:
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(null);
        DefaultCloud11Service spy1 = PowerMockito.spy(defaultCloud11Service);
        PowerMockito.doReturn(new UserScopeAccess()).when(spy1, "authenticateAndAuthorizeCloudAdminUser", request);

        when:
        Response.ResponseBuilder returnedResponse = spy1.revokeToken(request, "test", null);

        then:
        assertThat("notFoundResponseReturned", returnedResponse.build().getStatus(), equalTo(404));
    }

    def revokeToken_scopeAccessServiceReturnsNonUserAccess_returnsNotFoundResponse() {
        given:
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(new RackerScopeAccess());
        DefaultCloud11Service tempSpy = PowerMockito.spy(defaultCloud11Service);
        PowerMockito.doReturn(new UserScopeAccess()).when(tempSpy, "authenticateAndAuthorizeCloudAdminUser", request);

        when:
        Response.ResponseBuilder returnedResponse = tempSpy.revokeToken(request, "test", null);

        then:
        assertThat("notFoundResponseReturned", returnedResponse.build().getStatus(), equalTo(404));
    }

    def revokeToken_scopeAccessServiceReturnsExpiredToken_returnsNotFoundResponse() {
        given:
        DefaultCloud11Service tempSpy = PowerMockito.spy(defaultCloud11Service);
        PowerMockito.doReturn(new UserScopeAccess()).when(tempSpy, "authenticateAndAuthorizeCloudAdminUser", request);
        UserScopeAccess userScopeAccessMock = mock(UserScopeAccess.class);
        when(userScopeAccessMock.isAccessTokenExpired(Matchers.<DateTime>anyObject())).thenReturn(true);
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(userScopeAccessMock);

        when:
        Response.ResponseBuilder returnedResponse = tempSpy.revokeToken(request, "test", null);

        then:
        assertThat("notFoundResponseReturned", returnedResponse.build().getStatus(), equalTo(404));
    }

    def revokeToken_userScopeAccess_revokeToken() {
        given:
        DefaultCloud11Service tempSpy = PowerMockito.spy(defaultCloud11Service);
        PowerMockito.doReturn(new UserScopeAccess()).when(tempSpy, "authenticateAndAuthorizeCloudAdminUser", request);
        UserScopeAccess userScopeAccessMock = mock(UserScopeAccess.class);
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(userScopeAccessMock);

        when:
        tempSpy.revokeToken(request, "test", null);

        then:
        verify(tokenRevocationService).revokeToken(anyString());
    }

    def authenticateCloudAdminUserForGetRequests_callsAuthHeaderHelper_parseBasicParams_throwsCloudAdminAuthorizationException() {
        when:
        doThrow(new CloudAdminAuthorizationException()).when(authHeaderHelper).parseBasicParams(anyString())
        defaultCloud11Service.authenticateCloudAdminUserForGetRequests(request)

        then:
        thrown NotAuthorizedException
    }

    def authenticateCloudAdminUserForGetRequests_withNoBasicParams_throwsNotAuthorized() {
        given:
        request = mock(HttpServletRequest.class);

        when:
        when(authHeaderHelper.parseBasicParams(any(String.class))).thenReturn(null)
        defaultCloud11Service.authenticateCloudAdminUserForGetRequests(request)

        then:
        thrown NotAuthorizedException
    }

    def authenticateCloudAdminUserForGetRequests_withInvalidAuthHeaders() {
        when:
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + Base64.encode("auth"))
        defaultCloud11Service.authenticateCloudAdminUserForGetRequests(request)

        then:
        thrown NotAuthorizedException
    }

    def authenticateCloudAdminUserForGetRequests_withServiceAndServiceAdmin_withoutExceptions() {
        when:
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);

        then:
        defaultCloud11Service.authenticateCloudAdminUserForGetRequests(request);
    }

    def authenticateCloudAdminUserForGetRequests_withService() {
        given:
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);

        when:
        defaultCloud11Service.authenticateCloudAdminUserForGetRequests(request);

        then:
        assertTrue("no Exception thrown", true);
    }

    def NauthenticateCloudAdminUserForGetRequests_withServiceAdmin() {
        given:
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);

        when:
        defaultCloud11Service.authenticateCloudAdminUserForGetRequests(request);

        then:
        assertTrue("no Exception thrown", true);
    }

    def extensions_returns200() {
        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.extensions(httpHeaders);

        then:
        assertThat("response code", responseBuilder.build().getStatus(), org.hamcrest.Matchers.equalTo(200));
    }

    def extensions_withNonNullExtension_returns200() {
        given:
        defaultCloud11Service.extensions(httpHeaders);

        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.extensions(httpHeaders);

        then:
        assertThat("response code", responseBuilder.build().getStatus(), org.hamcrest.Matchers.equalTo(200));
    }

    def getExtension_blankExtensionAlias_throwsBadRequestException() {
        given:
        when(validator.isBlank(anyString())).thenReturn(true);

        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.getExtension(httpHeaders, "");

        then:
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(400));
    }

    def getExtension_withCurrentExtensions_throwsNotFoundException() {  //There are no extensions at this time.
        given:
        defaultCloud11Service.extensions(httpHeaders);

        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.getExtension(httpHeaders, "123");

        then:
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));

    }

    def getExtension_withExtensionMap_throwsNotFoundException() {
        given:
        defaultCloud11Service.getExtension(httpHeaders, "123");

        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.getExtension(httpHeaders, "123");

        then:
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    def getExtension_invalidExtension_throwsNotFoundException() {
        when:
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.getExtension(httpHeaders, "INVALID");

        then:
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(404));
    }

    def authenticateCloudAdminUser_withInvalidAuthHeaders() {
        given:
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + Base64.encode("auth"));

        when:
        defaultCloud11Service.authenticateAndAuthorizeCloudAdminUser(request);

        then:
        thrown CloudAdminAuthorizationException
    }

    def authenticateCloudAdminUser_nullStringMap() {
        given:
        when(authHeaderHelper.parseBasicParams(any(String.class))).thenReturn(null);

        when:
        defaultCloud11Service.authenticateAndAuthorizeCloudAdminUser(request);

        then:
        thrown CloudAdminAuthorizationException
    }

    def authenticateCloudAdminUser_withServiceAndServiceAdmin_withoutExceptions() {
        when:
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);

        then:
        defaultCloud11Service.authenticateAndAuthorizeCloudAdminUser(request) == null
    }

    def authenticateCloudAdminUser_withServiceAndServiceAdminFalse() {
        given:
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);

        when:
        defaultCloud11Service.authenticateAndAuthorizeCloudAdminUser(request);

        then:
        thrown CloudAdminAuthorizationException
    }

    def authenticateCloudAdminUser_withService() {
        given:
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);

        when:
        defaultCloud11Service.authenticateAndAuthorizeCloudAdminUser(request);

        then:
        assertTrue("no Exception thrown", true);
    }

    def authenticateCloudAdminUser_withServiceAdmin() {
        given:
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);

        when:
        defaultCloud11Service.authenticateAndAuthorizeCloudAdminUser(request);

        then:
        assertTrue("no Exception thrown", true);
    }

}
