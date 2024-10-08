package com.rackspace.idm.domain.entity;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/3/12
 * Time: 5:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClientAuthenticationResultTest {
    private ClientAuthenticationResult clientAuthenticationResult;

    @Before
    public void setUp() throws Exception {
        clientAuthenticationResult = new ClientAuthenticationResult(null, false);
    }

    @Test
    public void setClient_setsClient() throws Exception {
        Application client = new Application();
        clientAuthenticationResult.setClient(client);
        assertThat("client", clientAuthenticationResult.getClient(), equalTo(client));
    }

    @Test
    public void equals_objIsSame_returnsTrue() throws Exception {
        boolean result = clientAuthenticationResult.equals(clientAuthenticationResult);
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void equals_superNotSame_returnsFalse() throws Exception {
        boolean result = clientAuthenticationResult.equals("");
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_clientNullOtherClientNull_returnsTrue() throws Exception {
        AuthenticationResult test = new ClientAuthenticationResult(null, false);
        boolean result = clientAuthenticationResult.equals(test);
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void equals_clientNullOtherClientNotNull_returnsFalse() throws Exception {
        AuthenticationResult test = new ClientAuthenticationResult(new Application(), false);
        boolean result = clientAuthenticationResult.equals(test);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_clientNotNullAndNotEqual_returnsFalse() throws Exception {
        clientAuthenticationResult.setClient(new Application());
        AuthenticationResult test = new ClientAuthenticationResult(null, false);
        boolean result = clientAuthenticationResult.equals(test);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_authenticatedDifferent_returnsFalse() throws Exception {
        clientAuthenticationResult.setClient(null);
        AuthenticationResult test = new ClientAuthenticationResult(null, true);
        boolean result = clientAuthenticationResult.equals(test);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_clientNotNullAndEqualClient_returnsTrue() throws Exception {
        Application client = new Application();
        clientAuthenticationResult.setClient(client);
        AuthenticationResult test = new ClientAuthenticationResult(client, false);
        boolean result = clientAuthenticationResult.equals(test);
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void toString_returnsString() throws Exception {
        String result = clientAuthenticationResult.toString();
        assertThat("string", result, equalTo("ClientAuthenticationResult [client=null, authenticated=false]"));
    }
}
