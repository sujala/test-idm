package com.rackspace.idm.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.junit.Before;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/22/12
 * Time: 9:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class NastXmlRpcClientWrapperTest {
    private NastXmlRpcClientWrapper nastXmlRpcClientWrapper;
    private NastConfiguration nastConfiguration;
    private URL url;

    @Before
    public void setUp() throws Exception {
        nastXmlRpcClientWrapper = new NastXmlRpcClientWrapper();

        //mocks
        nastConfiguration = mock(NastConfiguration.class);
        nastXmlRpcClientWrapper.setAuthConfiguration(nastConfiguration);
        url = new URL("http://localhost");
    }

    @Test
    public void getClient_returnsNewClient() throws Exception {
        XmlRpcClient rpcClient = nastXmlRpcClientWrapper.getClient(url);
        assertThat("rpc client", ((XmlRpcClientConfigImpl)rpcClient.getClientConfig()).getServerURL(), equalTo(url));
    }

    @Test
    public void removeNastPrefix_accountIdIsBlank_returnBlank() throws Exception {
        String response = nastXmlRpcClientWrapper.removeNastPrefix("");
        assertThat("string", response, equalTo(""));
    }

    @Test
    public void removeNastPrefix_accountIdStartWithNastResellerName_replaceFirst() throws Exception {
        when(nastConfiguration.getNastResellerName()).thenReturn("nast.xmlrpc.reseller");
        String response = nastXmlRpcClientWrapper.removeNastPrefix("nast.xmlrpc.reseller_Test");
        assertThat("string", response, equalTo("Test"));
    }

    @Test
    public void removeNastPrefix_accountIdNotStartWithNastResellerName_returnsAccountId() throws Exception {
        when(nastConfiguration.getNastResellerName()).thenReturn("nast.xmlrpc.reseller");
        String response = nastXmlRpcClientWrapper.removeNastPrefix("test");
        assertThat("string", response, equalTo("test"));
    }
}
