package com.rackspace.idm.util;

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/22/12
 * Time: 10:26 AM
 * To change this template use File | Settings | File Templates.
 */
public class RSAClientTest {
    private RSAClient rsaClient;
    private Configuration config;

    @Before
    public void setUp() throws Exception {
        rsaClient = new RSAClient();

        //mocks
        config = mock(Configuration.class);

        rsaClient.setConfig(config);
    }

    @Test
    public void authenticate_callsClient_authenticate() throws Exception {
        when(config.getString("rsa.host")).thenReturn("Rsa.ord1.rackspace.com");
        when(config.getString("rsa.sharedSecret")).thenReturn("zrwgR1Y4");
        boolean response = rsaClient.authenticate("userId", "passCode");
        assertThat("boolean", response, equalTo(false));
    }
}
