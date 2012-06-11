package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.AuthCredentials;
import com.rackspace.api.idm.v1.AuthGrantType;
import com.rackspace.idm.domain.entity.Credentials;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/11/12
 * Time: 11:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class CredentialsConverterTest {
    CredentialsConverter credentialsConverter;
    AuthCredentials authCredentials;

    @Before
    public void setUp() throws Exception {
        credentialsConverter = new CredentialsConverter();
        authCredentials = new AuthCredentials();
    }

    @Test
    public void toCredentialsDO_withApiAuthCredentials_returnsDomainAuthCredentials() throws Exception {
        Credentials credentials = credentialsConverter.toCredentialsDO(authCredentials);
        assertThat("credentials type", credentials, is(com.rackspace.idm.domain.entity.AuthCredentials.class));
    }

    @Test
    public void toCredentialsDO_withApiAuthCredentials_setsClientId() throws Exception {
        authCredentials.setClientId("clientId");
        Credentials credentials = credentialsConverter.toCredentialsDO(authCredentials);
        assertThat("client id", credentials.getClientId(), equalTo("clientId"));
    }

    @Test
    public void toCredentialsDO_withApiAuthCredentials_setsClientSecret() throws Exception {
        authCredentials.setClientSecret("clientSecret");
        Credentials credentials = credentialsConverter.toCredentialsDO(authCredentials);
        assertThat("client secret", credentials.getClientSecret(), equalTo("clientSecret"));
    }

    @Test
    public void toCredentialsDO_withApiAuthCredentials_setsPassword() throws Exception {
        authCredentials.setPassword("password");
        Credentials credentials = credentialsConverter.toCredentialsDO(authCredentials);
        assertThat("password", credentials.getPassword(), equalTo("password"));
    }

    @Test
    public void toCredentialsDO_withApiAuthCredentials_setsUsername() throws Exception {
        authCredentials.setUsername("username");
        Credentials credentials = credentialsConverter.toCredentialsDO(authCredentials);
        assertThat("username", credentials.getUsername(), equalTo("username"));
    }

    @Test
    public void toCredentialsDO_withApiAuthCredentials_setsRefreshToken() throws Exception {
        authCredentials.setRefreshToken("refreshToken");
        Credentials credentials = credentialsConverter.toCredentialsDO(authCredentials);
        assertThat("refresh token", credentials.getRefreshToken(), equalTo("refreshToken"));
    }

    @Test
    public void toCredentialsDO_withApiAuthCredentials_setsAuthorizationCode() throws Exception {
        authCredentials.setAuthorizationCode("AuthorizationCode");
        Credentials credentials = credentialsConverter.toCredentialsDO(authCredentials);
        assertThat("Authorization code", credentials.getAuthorizationCode(), equalTo("authorizationCode"));
    }

    @Test
    public void toCredentialsDO_withApiAuthCredentials_setsGrantType() throws Exception {
        authCredentials.setGrantType(AuthGrantType.RACKER);
        Credentials credentials = credentialsConverter.toCredentialsDO(authCredentials);
        assertThat("grant Type", credentials.getGrantType(), equalTo(AuthGrantType.RACKER.toString()));
    }

    @Test
    public void toCredentialsDO_withApiAuthCredentialsAndNoGrantType_hasNullGrantType() throws Exception {
        Credentials credentials = credentialsConverter.toCredentialsDO(authCredentials);
        assertThat("grant type", credentials.getGrantType(), nullValue());
    }
}
