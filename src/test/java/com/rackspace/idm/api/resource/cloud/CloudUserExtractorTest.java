package com.rackspace.idm.api.resource.cloud;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentialsWithOnlyApiKey;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspacecloud.docs.auth.api.v1.*;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.openstack.docs.identity.api.v2.TokenForAuthenticationRequest;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/20/12
 * Time: 4:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class CloudUserExtractorTest {

    CloudExceptionResponse cloudExceptionResponse;
    UserService userService;
    ScopeAccessService scopeAccessService;
    CloudUserExtractor cloudUserExtractor;

    @Before
    public void setUp() throws Exception {
        userService = mock(UserService.class);
        cloudExceptionResponse = new CloudExceptionResponse();
        scopeAccessService = mock(ScopeAccessService.class);

        cloudUserExtractor = new CloudUserExtractor(cloudExceptionResponse, userService, scopeAccessService);
    }

    @Test
    public void getUserByV20CredentialType_withTokenCredentials_callsScopeAccessService_getScopeAccessByAccessToken() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest token = new TokenForAuthenticationRequest();
        token.setId("tokenId");
        authenticationRequest.setToken(token);
        cloudUserExtractor.getUserByV20CredentialType(authenticationRequest);
        verify(scopeAccessService).getScopeAccessByAccessToken("tokenId");
    }

    @Test
    public void getUserByV20CredentialType_withTokenCredentials_callsUserService_getUser() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest token = new TokenForAuthenticationRequest();
        token.setId("tokenId");
        authenticationRequest.setToken(token);
        when(scopeAccessService.getScopeAccessByAccessToken("tokenId")).thenReturn(new UserScopeAccess());
        cloudUserExtractor.getUserByV20CredentialType(authenticationRequest);
        verify(userService).getUser(anyString());
    }

    @Test
    public void getUserByV20CredentialType_withPasswordCredentialsRequiredUsername_callsUserService_getUser() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        PasswordCredentialsRequiredUsername passwordCredentials = new PasswordCredentialsRequiredUsername();
        passwordCredentials.setUsername("username");
        passwordCredentials.setPassword("password");
        authenticationRequest.setCredential(new JAXBObjectFactories().getOpenStackIdentityV2Factory().createPasswordCredentials(passwordCredentials));
        cloudUserExtractor.getUserByV20CredentialType(authenticationRequest);
        verify(userService).getUser("username");
    }

    @Test
    public void getUserByV20CredentialType_withApiKeyCredentials_callsUserService_getUser() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
        apiKeyCredentials.setUsername("username");
        apiKeyCredentials.setApiKey("apiKey");
        authenticationRequest.setCredential(new JAXBObjectFactories().getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(apiKeyCredentials));
        cloudUserExtractor.getUserByV20CredentialType(authenticationRequest);
        verify(userService).getUser("username");
    }

    @Test
    public void getUserByV20CredentialType_withExceptionThrown_returnsNull() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
        apiKeyCredentials.setUsername("username");
        apiKeyCredentials.setApiKey("apiKey");
        authenticationRequest.setCredential(new JAXBObjectFactories().getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(apiKeyCredentials));
        when(userService.getUser(anyString())).thenThrow(new NullPointerException());
        User user = cloudUserExtractor.getUserByV20CredentialType(authenticationRequest);
        assertThat("user", user, nullValue());
    }

    @Test
    public void getUserByCredentialType_withNullCredentialValue_returnsNull() throws Exception {
        JAXBElement<Credentials> jaxbElement = new JAXBElement<Credentials>(new QName(""), Credentials.class, null);
        User userByCredentialType = cloudUserExtractor.getUserByCredentialType(jaxbElement);
        assertThat("user", userByCredentialType, nullValue());
    }

    @Test
    public void getUserByCredentialType_withUserCredentials_callsUserService_getUser() throws Exception {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUsername("username");
        userCredentials.setKey("apiKey");
        JAXBElement<Credentials> jaxbElement = new JAXBElement<Credentials>(new QName(""), Credentials.class, UserCredentials.class, userCredentials);
        cloudUserExtractor.getUserByCredentialType(jaxbElement);
        verify(userService).getUser("username");
    }

    @Test
    public void getUserByCredentialType_withUserCredentialsWithNoUsername_ThrowsExceptionWith400Status() throws Exception {
        try {
            UserCredentials userCredentials = new UserCredentials();
            userCredentials.setKey("apiKey");
            JAXBElement<Credentials> jaxbElement = new JAXBElement<Credentials>(new QName(""), Credentials.class, UserCredentials.class, userCredentials);
            cloudUserExtractor.getUserByCredentialType(jaxbElement);
            assertTrue("expecting exception", false);
        } catch (CloudExceptionResponse cloudExceptionResponse) {
            assertThat("exception response status", cloudExceptionResponse.getResponse().getStatus(), equalTo(400));
        }
    }

    @Test
    public void getUserByCredentialType_withUserCredentialsWithNoApiKey_ThrowsExceptionWith400Status() throws Exception {
        try {
            UserCredentials userCredentials = new UserCredentials();
            userCredentials.setUsername("username");
            JAXBElement<Credentials> jaxbElement = new JAXBElement<Credentials>(new QName(""), Credentials.class, UserCredentials.class, userCredentials);
            cloudUserExtractor.getUserByCredentialType(jaxbElement);
            assertTrue("expecting exception", false);
        } catch (CloudExceptionResponse cloudExceptionResponse) {
            assertThat("exception response status", cloudExceptionResponse.getResponse().getStatus(), equalTo(400));
        }
    }

    @Test
    public void getUserByCredentialType_withPasswordCredentials_callsUserService_getUser() throws Exception {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("username");
        passwordCredentials.setPassword("password");
        JAXBElement<Credentials> jaxbElement = new JAXBElement<Credentials>(new QName(""), Credentials.class, PasswordCredentials.class, passwordCredentials);
        cloudUserExtractor.getUserByCredentialType(jaxbElement);
        verify(userService).getUser("username");
    }

    @Test
    public void getUserByCredentialType_withPasswordCredentialsWithNoPassword_ThrowsExceptionWith400Status() throws Exception {
        try {
        PasswordCredentials passwordCredentials = new PasswordCredentials();
        passwordCredentials.setUsername("username");
        JAXBElement<Credentials> jaxbElement = new JAXBElement<Credentials>(new QName(""), Credentials.class, PasswordCredentials.class, passwordCredentials);
        cloudUserExtractor.getUserByCredentialType(jaxbElement);
        assertTrue("expecting exception", false);
        } catch (CloudExceptionResponse cloudExceptionResponse) {
            assertThat("exception response status", cloudExceptionResponse.getResponse().getStatus(), equalTo(400));
        }
    }

    @Test
    public void getUserByCredentialType_withPasswordCredentialsWithNoUsername_ThrowsExceptionWith400Status() throws Exception {
        try {
            PasswordCredentials passwordCredentials = new PasswordCredentials();
            passwordCredentials.setPassword("password");
            JAXBElement<Credentials> jaxbElement = new JAXBElement<Credentials>(new QName(""), Credentials.class, PasswordCredentials.class, passwordCredentials);
            cloudUserExtractor.getUserByCredentialType(jaxbElement);
            assertTrue("expecting exception", false);
        } catch (CloudExceptionResponse cloudExceptionResponse) {
            assertThat("exception response status", cloudExceptionResponse.getResponse().getStatus(), equalTo(400));
        }
    }

    @Test
    public void getUserByCredentialType_withMossoCredentials_callsUserService_getUserByMossoId() throws Exception {
        MossoCredentials mossoCredentials = new MossoCredentials();
        mossoCredentials.setMossoId(123456);
        mossoCredentials.setKey("mossoKey");
        JAXBElement<Credentials> jaxbElement = new JAXBElement<Credentials>(new QName(""), Credentials.class, MossoCredentials.class, mossoCredentials);
        cloudUserExtractor.getUserByCredentialType(jaxbElement);
        verify(userService).getUserByMossoId(123456);
    }

    @Test
    public void getUserByCredentialType_withMossoCredentialsWithNoMossoKey_ThrowsExceptionWith400Status() throws Exception {
        try {
            MossoCredentials mossoCredentials = new MossoCredentials();
            JAXBElement<Credentials> jaxbElement = new JAXBElement<Credentials>(new QName(""), Credentials.class, MossoCredentials.class, mossoCredentials);
            cloudUserExtractor.getUserByCredentialType(jaxbElement);
            assertTrue("expecting exception", false);
        } catch (CloudExceptionResponse cloudExceptionResponse) {
            assertThat("exception response status", cloudExceptionResponse.getResponse().getStatus(), equalTo(400));
        }
    }

    @Test
    public void getUserByCredentialType_withNastCredentials_callsUserService_getUserByNastId() throws Exception {
        NastCredentials nastCredentials = new NastCredentials();
        nastCredentials.setNastId("nastId");
        nastCredentials.setKey("nastKey");
        JAXBElement<Credentials> jaxbElement = new JAXBElement<Credentials>(new QName(""), Credentials.class, NastCredentials.class, nastCredentials);
        cloudUserExtractor.getUserByCredentialType(jaxbElement);
        verify(userService).getUserByNastId("nastId");
    }

    @Test
    public void getUserByCredentialType_withNastCredentialsWithNoNastId_ThrowsExceptionWith400Status() throws Exception {
        try {
            NastCredentials nastCredentials = new NastCredentials();
            nastCredentials.setKey("nastKey");
            JAXBElement<Credentials> jaxbElement = new JAXBElement<Credentials>(new QName(""), Credentials.class, NastCredentials.class, nastCredentials);
            cloudUserExtractor.getUserByCredentialType(jaxbElement);
            assertTrue("expecting exception", false);
        } catch (CloudExceptionResponse cloudExceptionResponse) {
            assertThat("exception response status", cloudExceptionResponse.getResponse().getStatus(), equalTo(400));
        }
    }


}
