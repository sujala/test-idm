package com.rackspace.idm.util;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

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
    private NastXmlRpcClientWrapper spy;

    @Before
    public void setUp() throws Exception {
        nastXmlRpcClientWrapper = new NastXmlRpcClientWrapper();

        //mocks
        nastConfiguration = mock(NastConfiguration.class);

        nastXmlRpcClientWrapper.setAuthConfiguration(nastConfiguration);

        spy = spy(nastXmlRpcClientWrapper);
    }

    @Test
    public void getClient_callsNastConfig_getNastXmlRpcUrl() throws Exception {
        nastXmlRpcClientWrapper.getClient();
        verify(nastConfiguration).getNastXmlRpcUrl();
    }

    @Test
    public void getClient_returnsNewClient() throws Exception {
        XmlRpcClient rpcClient = nastXmlRpcClientWrapper.getClient();
        assertThat("rpc client", ((XmlRpcClientConfigImpl)rpcClient.getClientConfig()).getServerURL(), equalTo(null));
    }

    @Test
    public void addResellerStorageAccount_returnsResponse() throws Exception {
        XmlRpcClient xmlRpcClient = mock(XmlRpcClient.class);
        doReturn(xmlRpcClient).when(spy).getClient();
        when(xmlRpcClient.execute("reseller.add_storage_account", new String[]{"1", "2", "3"})).thenReturn("response");
        String response = spy.addResellerStorageAccount(new String[]{"1", "2", "3"});
        assertThat("response string", response, equalTo("response"));
    }

    @Test
    public void removeResellerStorageAccount_returnsFalse() throws Exception {
        XmlRpcClient xmlRpcClient = mock(XmlRpcClient.class);
        doReturn(xmlRpcClient).when(spy).getClient();
        when(xmlRpcClient.execute(anyString(), any(Object[].class))).thenReturn(false);
        Boolean response = spy.removeResellerStorageAccount("");
        assertThat("boolean", response, equalTo(false));
    }

    @Test
    public void removeResellerStorageAccount_returnsTrue() throws Exception {
        XmlRpcClient xmlRpcClient = mock(XmlRpcClient.class);
        doReturn(xmlRpcClient).when(spy).getClient();
        when(xmlRpcClient.execute(anyString(), any(String[].class))).thenReturn(true);
        Boolean response = spy.removeResellerStorageAccount("");
        assertThat("boolean", response, equalTo(true));
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
