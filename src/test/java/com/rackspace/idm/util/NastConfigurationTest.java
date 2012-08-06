package com.rackspace.idm.util;

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/20/12
 * Time: 5:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class NastConfigurationTest {
    private NastConfiguration nastConfiguration;
    private Configuration config;

    @Before
    public void setUp() throws Exception {
        config = mock(Configuration.class);
        nastConfiguration = new NastConfiguration();
        nastConfiguration.setConfiguration(config);
    }

    @Test
    public void isNastXmlRpcEnabled_callsConfig_getBoolean() throws Exception {
        nastConfiguration.isNastXmlRpcEnabled();
        verify(config).getBoolean("nast.xmlrpc.enabled");
    }

    @Test
    public void getNastResellerName_callsConfig_getString() throws Exception {
        nastConfiguration.getNastResellerName();
        verify(config).getString("nast.xmlrpc.reseller");
    }

    @Test
    public void getNastXmlRpcUrl_callsConfig_getString() throws Exception {
        when(config.getString("nast.xmlrpc.url")).thenReturn("http://services.stg.swift.racklabs.com:10000/RPC2");
        nastConfiguration.getNastXmlRpcUrl();
        verify(config).getString("nast.xmlrpc.url");
    }

    @Test
    public void getNastXmlRpcUrl_returnsNewUrl() throws Exception {
        when(config.getString("nast.xmlrpc.url")).thenReturn("http://services.stg.swift.racklabs.com:10000/RPC2");
        URL url = nastConfiguration.getNastXmlRpcUrl();
        assertThat("url", url.toString(), equalTo("http://services.stg.swift.racklabs.com:10000/RPC2"));
    }
}
