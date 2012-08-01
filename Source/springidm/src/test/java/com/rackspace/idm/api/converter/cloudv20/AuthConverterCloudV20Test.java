package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.*;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.ServiceCatalog;
import org.openstack.docs.identity.api.v2.Token;
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/13/12
 * Time: 12:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class AuthConverterCloudV20Test {

    AuthConverterCloudV20 authConverter;
    EndpointConverterCloudV20 endpointConverterCloudV20;
    TokenConverterCloudV20 tokenConverterCloudV20;
    UserConverterCloudV20 userConverterCloudV20;

    @Before
    public void setUp() throws Exception {
        authConverter = new AuthConverterCloudV20();
        authConverter.setObjFactories(new JAXBObjectFactories());
        endpointConverterCloudV20 = mock(EndpointConverterCloudV20.class);
        tokenConverterCloudV20 = mock(TokenConverterCloudV20.class);
        userConverterCloudV20 = mock(UserConverterCloudV20.class);

        authConverter.setEndpointConverterCloudV20(endpointConverterCloudV20);
        authConverter.setTokenConverterCloudV20(tokenConverterCloudV20);
        authConverter.setUserConverterCloudV20(userConverterCloudV20);
    }

    @Test
    public void toAuthenticationResponse_setsAuthFields() throws Exception {
        Token token = new Token();
        token.setId("tokenId");
        when(tokenConverterCloudV20.toToken(any(ScopeAccess.class), anyList())).thenReturn(token);
        UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
        userForAuthenticateResponse.setId("userForAuthenticateResponseId");
        when(userConverterCloudV20.toUserForAuthenticateResponse(any(User.class), anyList())).thenReturn(userForAuthenticateResponse);
        when(endpointConverterCloudV20.toServiceCatalog(anyList())).thenReturn(new ServiceCatalog());
        AuthenticateResponse authenticateResponse = authConverter.toAuthenticationResponse(new User("username"), new ScopeAccess(), new ArrayList<TenantRole>(), new ArrayList<OpenstackEndpoint>());
        assertThat("token id", authenticateResponse.getToken().getId(), equalTo("tokenId"));
        assertThat("authenticate response", authenticateResponse.getUser().getId(), equalTo("userForAuthenticateResponseId"));
        assertThat("services catalog", authenticateResponse.getServiceCatalog(), not(nullValue()));
    }

    @Test
    public void toImpersonationResponse_setsToken() throws Exception {
        Token token = new Token();
        token.setId("tokenId");
        when(tokenConverterCloudV20.toToken(any(ScopeAccess.class))).thenReturn(token);
        ImpersonationResponse impersonationResponse = authConverter.toImpersonationResponse(new ScopeAccess());
        assertThat("token id", impersonationResponse.getToken().getId(), equalTo("tokenId"));
    }
}
