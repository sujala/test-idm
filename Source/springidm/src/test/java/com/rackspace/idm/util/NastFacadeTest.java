package com.rackspace.idm.util;

import com.rackspace.idm.exception.ApiException;
import com.rackspacecloud.docs.auth.api.v1.User;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/20/12
 * Time: 5:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class NastFacadeTest {
    private NastFacade nastFacade;
    private NastConfiguration nastConfiguration;
    private NastXmlRpcClientWrapper nastXmlRpcClientWrapper;

    @Before
    public void setUp() throws Exception {
        nastConfiguration = mock(NastConfiguration.class);
        nastXmlRpcClientWrapper = mock(NastXmlRpcClientWrapper.class);

        nastFacade = new NastFacade();

        nastFacade.setAuthConfiguration(nastConfiguration);
        nastFacade.setNastXMLRpcClientWrapper(nastXmlRpcClientWrapper);
    }

    @Test
    public void removeNastUser_nastXmlRpcEnabledAndSuccessfullyRemoveAccount_returnsTrue() throws Exception {
        when(nastConfiguration.isNastXmlRpcEnabled()).thenReturn(true);
        when(nastXmlRpcClientWrapper.removeResellerStorageAccount("")).thenReturn(true);
        Boolean removed = nastFacade.removeNastUser("");
        assertThat("boolean", removed, equalTo(true));
    }

    @Test (expected = ApiException.class)
    public void removeNastUser_removeResellerStorageAccount_throwsApiException() throws Exception {
        when(nastConfiguration.isNastXmlRpcEnabled()).thenReturn(true);
        doThrow(new ApiException(1, "message", "details")).when(nastXmlRpcClientWrapper).removeResellerStorageAccount("");
        nastFacade.removeNastUser("");
    }

    @Test
    public void removeNastUser_nastXmlRpcNotEnabled_returnsFalse() throws Exception {
        Boolean removed = nastFacade.removeNastUser("");
        assertThat("boolean", removed, equalTo(false));
    }

    @Test
    public void addNastUser_nastXmlRpcNotEnabled_returnsNull() throws Exception {
        String response = nastFacade.addNastUser(new User());
        assertThat("string", response, equalTo(null));
    }

    @Test
    public void addNastUser_addsUserSuccess_returnsAccountId() throws Exception {
        when(nastXmlRpcClientWrapper.addResellerStorageAccount(any(String[].class))).thenReturn("test");
        when(nastConfiguration.isNastXmlRpcEnabled()).thenReturn(true);
        String response = nastFacade.addNastUser(new User());
        assertThat("string", response, equalTo("test"));
    }

    @Test (expected = ApiException.class)
    public void addNastUser_throwsApiException() throws Exception {
        doThrow(new ApiException(1, "message", "details")).when(nastXmlRpcClientWrapper).addResellerStorageAccount(any(String[].class));
        when(nastConfiguration.isNastXmlRpcEnabled()).thenReturn(true);
        nastFacade.addNastUser(new User());
    }

    @Test
    public void addNastUser_succeeds_returnsString() throws Exception {
        when(nastXmlRpcClientWrapper.addResellerStorageAccount(any(String[].class))).thenReturn("test");
        String response = nastFacade.addNastUser("name", "id");
        assertThat("string", response, equalTo("test"));
    }

    @Test
    public void hasResellerInfo_nameIsBlankAndIdIsBlank_returnsFalse() throws Exception {
        boolean response = nastFacade.hasResellerInfo("", "");
        assertThat("boolean", response, equalTo(false));
    }

    @Test
    public void hasResellerInfo_nameIsBlankAndIdNotBlank_returnsFalse() throws Exception {
        boolean response = nastFacade.hasResellerInfo("", "id");
        assertThat("boolean", response, equalTo(false));
    }

    @Test
    public void hasResellerInfo_nameNotBlankAndIdIsBlank_returnsFalse() throws Exception {
        boolean response = nastFacade.hasResellerInfo("name", "");
        assertThat("boolean", response, equalTo(false));
    }

    @Test
    public void hasResellerInfo_nameNotBlankAndIdNotBlank_returnsTrue() throws Exception {
        boolean response = nastFacade.hasResellerInfo("name", "id");
        assertThat("boolean", response, equalTo(true));
    }

    @Test
    public void addNastUser_withNastXmlRpcNotEnabled_returnsNull() throws Exception {
        when(nastConfiguration.isNastXmlRpcEnabled()).thenReturn(false);
        String response = nastFacade.addNastUser();
        assertThat("string", response, equalTo(null));
    }

    @Test
    public void addNastUser_emptyResponse_returnsNull() throws Exception {
        when(nastXmlRpcClientWrapper.addResellerStorageAccount(null)).thenReturn(null);
        String response = nastFacade.addNastUser((String[]) null);
        assertThat("string", response, equalTo(null));
    }
}
