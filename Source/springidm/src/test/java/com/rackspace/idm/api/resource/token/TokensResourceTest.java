package com.rackspace.idm.api.resource.token;

import com.rackspace.idm.api.converter.AuthConverter;
import com.rackspace.idm.api.converter.CredentialsConverter;
import com.rackspace.idm.domain.service.AuthenticationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TokenService;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
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

        tokensResource = new TokensResource(tokenService, authHeaderHelper, authConverter, authorizationService, scopeAccessService, credentialsConverter,
                authenticationService, inputValidator, tokenService);
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

    @Ignore
    @Test
    public void authenticate_mediaTypeNotJSONResponseOk_returns200() throws Throwable {
        String jsonString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<mossoCredentials " +
                "xlmns=\"http://docs.openstack.org/identity/api/v2.0\"" +
                "mossoId=\"323676\"" +
                "key=\"a86850deb2742ec3cb41518e26aa2d89\"/>";
        EntityHolder<String> holder = new EntityHolder<String>(jsonString);
        when(httpHeaders.getMediaType()).thenReturn(new MediaType("application","xml"));
        Response response = tokensResource.authenticate(httpHeaders, holder);
        assertThat("response code", response.getStatus(), equalTo(200));
    }
}
