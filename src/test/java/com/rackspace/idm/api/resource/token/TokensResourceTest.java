package com.rackspace.idm.api.resource.token;

import com.rackspace.idm.api.converter.AuthConverter;
import com.rackspace.idm.api.converter.CredentialsConverter;
import com.rackspace.idm.domain.entity.AuthData;
import com.rackspace.idm.domain.entity.AuthorizationContext;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthenticationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TokenService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/19/12
 * Time: 9:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class TokensResourceTest {
    private TokensResource tokensResource;
    private InputValidator inputValidator;
    private AuthHeaderHelper authHeaderHelper;
    private AuthConverter authConverter;
    private TokenService tokenService;
    private AuthorizationService authorizationService;
    private CredentialsConverter credentialsConverter;
    private ScopeAccessService scopeAccessService;
    private AuthenticationService authenticationService;
    private HttpHeaders httpHeaders;

    @Before
    public void setUp() throws Exception {
        inputValidator = mock(InputValidator.class);
        authHeaderHelper = mock(AuthHeaderHelper.class);
        authConverter = mock(AuthConverter.class);
        tokenService = mock(TokenService.class);
        authorizationService = mock(AuthorizationService.class);
        credentialsConverter = mock(CredentialsConverter.class);
        scopeAccessService = mock(ScopeAccessService.class);
        authenticationService = mock(AuthenticationService.class);
        httpHeaders = mock(HttpHeaders.class);

        tokensResource = new TokensResource(authConverter, authorizationService, scopeAccessService, credentialsConverter,
                authenticationService, inputValidator);
    }

    @Test
    public void authenticate_mediaTypeJSONWithAuthCredentialsResponseOk_returns200() throws Throwable {
        String jsonString = "{\"AuthCredentials\": {}}";
        EntityHolder<String> holder = new EntityHolder<String>(jsonString);
        when(httpHeaders.getMediaType()).thenReturn(new MediaType("application","json"));
        Response response = tokensResource.authenticate(httpHeaders, holder);
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void authenticate_rackerCredentialsResponseOk_returns200() throws Throwable {
        String jsonString = "{\"rackerCredentials\": {}}";
        EntityHolder<String> holder = new EntityHolder<String>(jsonString);
        when(httpHeaders.getMediaType()).thenReturn(new MediaType("application","json"));
        Response response = tokensResource.authenticate(httpHeaders, holder);
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void authenticate_rsaCredentialsResponseOk_returns200() throws Throwable {
        String jsonString = "{\"rsaCredentials\": {}}";
        EntityHolder<String> holder = new EntityHolder<String>(jsonString);
        when(httpHeaders.getMediaType()).thenReturn(new MediaType("application","json"));
        Response response = tokensResource.authenticate(httpHeaders, holder);
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test (expected = BadRequestException.class)
    public void authenticate_mediaTypeNotJSON_throwsException() throws Throwable {
        String jsonString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <passwordCredentials username=\"jsmith\" password=\"theUsersPassword\"/>";
        EntityHolder<String> holder = new EntityHolder<String>(jsonString);
        when(httpHeaders.getMediaType()).thenReturn(new MediaType("application","xml"));
        Response response = tokensResource.authenticate(httpHeaders, holder);
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void validateAccessToken_callsScopeAccessService_getAccessTokenByAuthHeader() throws Exception {
        tokensResource.validateAccessToken("authHeader", "tokenString");
        verify(scopeAccessService).getAccessTokenByAuthHeader("authHeader");
    }

    @Test
    public void validateAccessToken_callsAuthorizationService_authorizeIdmSuperAdminOrRackspaceClient() throws Exception {
        tokensResource.validateAccessToken("authHeader", "tokenString");
        verify(authorizationService).authorizeIdmSuperAdminOrRackspaceClient(any(AuthorizationContext.class));
    }

    @Test
    public void validateAccessToken_callsAuthenticationService_getAuthDateFromToken() throws Exception {
        tokensResource.validateAccessToken("authHeader", "tokenString");
        verify(authenticationService).getAuthDataFromToken("tokenString");
    }

    @Test
    public void validateAccessToken_callsAuthConverter_toAuthDataJaxb() throws Exception {
        tokensResource.validateAccessToken("authHeader", "tokenString");
        verify(authConverter).toAuthDataJaxb(any(AuthData.class));
    }

    @Test
    public void validateAccessToken_responseOk_returns200() throws Exception {
        Response response = tokensResource.validateAccessToken("authHeader", "tokenString");
        assertThat("response code", response.getStatus(), equalTo(200));
    }
}
