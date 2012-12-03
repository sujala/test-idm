package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/26/12
 * Time: 12:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class Cloud11VersionResourceTest {

    Configuration config;
    CloudClient cloudClient;
    Cloud11VersionResource cloudV11VersionResource;
    Cloud11VersionResource spy;
    DefaultCloud11Service defaultCloud11Service;
    DelegateCloud11Service delegateCloud11Service;

    @Before
    public void setUp() throws Exception {
        // setup
        config = mock(Configuration.class);
        cloudClient = mock(CloudClient.class);
        cloudV11VersionResource = new Cloud11VersionResource(config, cloudClient);

        // mocks
        defaultCloud11Service = mock(DefaultCloud11Service.class);
        delegateCloud11Service = mock(DelegateCloud11Service.class);

        // fields
        cloudV11VersionResource.setDefaultCloud11Service(defaultCloud11Service);
        cloudV11VersionResource.setDelegateCloud11Service(delegateCloud11Service);

        spy = spy(cloudV11VersionResource);
        doReturn(delegateCloud11Service).when(spy).getCloud11Service();
    }

    @Test
    public void getPublicCloud11VersionInfo_okResponse_returns200() throws Exception {
        when(config.getString("cloudAuth11url")).thenReturn("https://auth.staging.us.ccp.rackspace.net/v1.1/");
        when(cloudClient.get("https://auth.staging.us.ccp.rackspace.net/v1.1/", (HttpHeaders)null)).thenReturn(Response.ok());
        Response result = spy.getPublicCloud11VersionInfo(null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getCloud11VersionInfo_okResponse_returns200() throws Exception {
        when(defaultCloud11Service.getVersion(null)).thenReturn(Response.ok());
        Response result = spy.getCloud11VersionInfo();
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void authenticate_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.authenticate(null, null, null, null)).thenReturn(Response.ok());
        spy.authenticate(null, null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void authenticate_callsGetCloud11Service_callsAuthenticate() throws Exception {
        when(delegateCloud11Service.authenticate(null, null, null, null)).thenReturn(Response.ok());
        spy.authenticate(null,null, null, null);
        verify(delegateCloud11Service).authenticate(null, null, null, null);
    }

    @Test
    public void authenticate_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.authenticate(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.authenticate(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void hack_badRequest_returns400() throws Exception {
        Response result = spy.hack();
        assertThat("response code", result.getStatus(), equalTo(400));
    }

    @Test
    public void adminAuthenticate_callsGetCloud11Service_callsAdminAuthenticate() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(delegateCloud11Service.adminAuthenticate(null, null, null, null)).thenReturn(Response.ok());
        spy.adminAuthenticate(null, null, null, null);
        verify(delegateCloud11Service).adminAuthenticate(null, null, null, null);
    }

    @Test
    public void adminAuthenticate_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.authenticate(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.authenticate(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void validateToken_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.validateToken(null, null, null, null, null)).thenReturn(Response.ok());
        spy.validateToken(null, null, null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void validateToken_callsGetCloud11Service_callsValidateToken() throws Exception {
        when(delegateCloud11Service.validateToken(null, null, null, null, null)).thenReturn(Response.ok());
        spy.validateToken(null, null, null, null, null);
        verify(delegateCloud11Service).validateToken(null, null, null, null, null);
    }

    @Test
    public void validateToken_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.validateToken(null, null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.validateToken(null, null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void revokeToken_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.revokeToken(null, null, null)).thenReturn(Response.ok());
        spy.revokeToken(null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void revokeToken_callsGetCloud11Service_callsRevokeToken() throws Exception {
        when(delegateCloud11Service.revokeToken(null, null, null)).thenReturn(Response.ok());
        spy.revokeToken(null, null, null);
        verify(delegateCloud11Service).revokeToken(null, null, null);
    }

    @Test
    public void revokeToken_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.revokeToken(null, null, null)).thenReturn(Response.ok());
        Response result = spy.revokeToken(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void extensions_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.extensions(null)).thenReturn(Response.ok());
        spy.extensions(null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void extensions_callsGetCloud11Service_callsExtensions() throws Exception {
        when(delegateCloud11Service.extensions(null)).thenReturn(Response.ok());
        spy.extensions(null);
        verify(delegateCloud11Service).extensions(null);
    }

    @Test
    public void extensions_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.extensions(null)).thenReturn(Response.ok());
        Response result = spy.extensions(null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void extensionsWithAlias_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.getExtension(null, null)).thenReturn(Response.ok());
        spy.extensions(null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void extensionsWithAlias_callsGetCloud11Service_callsExtensions() throws Exception {
        when(delegateCloud11Service.getExtension(null, null)).thenReturn(Response.ok());
        spy.extensions(null, null);
        verify(delegateCloud11Service).getExtension(null, null);
    }

    @Test
    public void extensionsWithAlias_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.getExtension(null, null)).thenReturn(Response.ok());
        Response result = spy.extensions(null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserFromNastId_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.getUserFromNastId(null, null, null)).thenReturn(Response.ok());
        spy.getUserFromNastId(null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void getUserFromNastId_callsGetCloud11Service_callsGetUserFromNastId() throws Exception {
        when(delegateCloud11Service.getUserFromNastId(null, null, null)).thenReturn(Response.ok());
        spy.getUserFromNastId(null, null, null);
        verify(delegateCloud11Service).getUserFromNastId(null, null, null);
    }

    @Test
    public void getUserFromNastId_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.getUserFromNastId(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getUserFromNastId(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserFromMossoId_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.getUserFromMossoId(null, 0, null)).thenReturn(Response.ok());
        spy.getUserFromMossoId(null, 0, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void getUserFromMossoId_callsGetCloud11Service_callsGetUserFromMossoId() throws Exception {
        when(delegateCloud11Service.getUserFromMossoId(null, 0, null)).thenReturn(Response.ok());
        spy.getUserFromMossoId(null, 0, null);
        verify(delegateCloud11Service).getUserFromMossoId(null, 0, null);
    }

    @Test
    public void getUserFromMossoId_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.getUserFromMossoId(null, 0, null)).thenReturn(Response.ok());
        Response result = spy.getUserFromMossoId(null, 0, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getBaseURLs_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.getBaseURLs(null, null, null)).thenReturn(Response.ok());
        spy.getBaseURLs(null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void getBaseURLs_callsGetCloud11Service_callsGetBaseURLs() throws Exception {
        when(delegateCloud11Service.getBaseURLs(null, null, null)).thenReturn(Response.ok());
        spy.getBaseURLs(null, null, null);
        verify(delegateCloud11Service).getBaseURLs(null, null, null);
    }

    @Test
    public void getBaseURLs_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.getBaseURLs(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getBaseURLs(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void addBaseURL_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.addBaseURL(null, null, null)).thenReturn(Response.ok());
        spy.addBaseURL(null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void addBaseURL_callsGetCloud11Service_callsAddBaseURL() throws Exception {
        when(delegateCloud11Service.addBaseURL(null, null, null)).thenReturn(Response.ok());
        spy.addBaseURL(null, null, null);
        verify(delegateCloud11Service).addBaseURL(null, null, null);
    }

    @Test
    public void addBaseURL_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.addBaseURL(null, null, null)).thenReturn(Response.ok());
        Response result = spy.addBaseURL(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getBaseURLId_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.getBaseURLById(null, 0, null, null)).thenReturn(Response.ok());
        spy.getBaseURLById(null, 0, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void getBaseURLId_callsGetCloud11Service_callsGetBaseURLId() throws Exception {
        when(delegateCloud11Service.getBaseURLById(null, 0, null, null)).thenReturn(Response.ok());
        spy.getBaseURLById(null, 0, null, null);
        verify(delegateCloud11Service).getBaseURLById(null, 0, null, null);
    }

    @Test
    public void getBaseURLId_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.getBaseURLById(null, 0, null, null)).thenReturn(Response.ok());
        Response result = spy.getBaseURLById(null, 0, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getEnabledBaseURLs_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.getEnabledBaseURL(null, null, null)).thenReturn(Response.ok());
        spy.getEnabledBaseURLs(null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void getEnabledBaseURLs_callsGetCloud11Service_callsGetEnabledBaseURLs() throws Exception {
        when(delegateCloud11Service.getEnabledBaseURL(null, null, null)).thenReturn(Response.ok());
        spy.getEnabledBaseURLs(null, null, null);
        verify(delegateCloud11Service).getEnabledBaseURL(null, null, null);
    }

    @Test
    public void getEnabledBaseURLs_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.getEnabledBaseURL(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getEnabledBaseURLs(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void createUser_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.createUser(null, null, null, null)).thenReturn(Response.ok());
        spy.createUser(null, null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void createUser_callsGetCloud11Service_callsCreateUser() throws Exception {
        when(delegateCloud11Service.createUser(null, null, null, null)).thenReturn(Response.ok());
        spy.createUser(null, null, null, null);
        verify(delegateCloud11Service).createUser(null, null, null, null);
    }

    @Test
    public void createUser_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.createUser(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.createUser(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUser_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.getUser(null, null, null)).thenReturn(Response.ok());
        spy.getUser(null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void getUser_callsGetCloud11Service_callsGetUser() throws Exception {
        when(delegateCloud11Service.getUser(null, null, null)).thenReturn(Response.ok());
        spy.getUser(null, null, null);
        verify(delegateCloud11Service).getUser(null, null, null);
    }

    @Test
    public void getUser_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.getUser(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getUser(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteUser_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.deleteUser(null, null, null)).thenReturn(Response.ok());
        spy.deleteUser(null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void deleteUser_callsGetCloud11Service_callsDeleteUser() throws Exception {
        when(delegateCloud11Service.deleteUser(null, null, null)).thenReturn(Response.ok());
        spy.deleteUser(null, null, null);
        verify(delegateCloud11Service).deleteUser(null, null, null);
    }

    @Test
    public void deleteUser_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.deleteUser(null, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteUser(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void updateUser_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.updateUser(null, null, null, null)).thenReturn(Response.ok());
        spy.updateUser(null, null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void updateUser_callsGetCloud11Service_callsUpdateUser() throws Exception {
        when(delegateCloud11Service.updateUser(null, null, null, null)).thenReturn(Response.ok());
        spy.updateUser(null, null, null, null);
        verify(delegateCloud11Service).updateUser(null, null, null, null);
    }

    @Test
    public void updateUser_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.updateUser(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.updateUser(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserEnabled_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.getUserEnabled(null, null, null)).thenReturn(Response.ok());
        spy.getUserEnabled(null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void getUserEnabled_callsGetCloud11Service_callsGetUserEnabled() throws Exception {
        when(delegateCloud11Service.getUserEnabled(null, null, null)).thenReturn(Response.ok());
        spy.getUserEnabled(null, null, null);
        verify(delegateCloud11Service).getUserEnabled(null, null, null);
    }

    @Test
    public void getUserEnabled_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.getUserEnabled(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getUserEnabled(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void setUserEnabled_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.setUserEnabled(null, null, null, null)).thenReturn(Response.ok());
        spy.setUserEnabled(null, null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void setUserEnabled_callsGetCloud11Service_callsSetUserEnabled() throws Exception {
        when(delegateCloud11Service.setUserEnabled(null, null, null, null)).thenReturn(Response.ok());
        spy.setUserEnabled(null, null, null, null);
        verify(delegateCloud11Service).setUserEnabled(null, null, null, null);
    }

    @Test
    public void setUserEnabled_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.setUserEnabled(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.setUserEnabled(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserKey_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.getUserKey(null, null, null)).thenReturn(Response.ok());
        spy.getUserKey(null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void getUserKey_callsGetCloud11Service_callsGetUserKey() throws Exception {
        when(delegateCloud11Service.getUserKey(null, null, null)).thenReturn(Response.ok());
        spy.getUserKey(null, null, null);
        verify(delegateCloud11Service).getUserKey(null, null, null);
    }

    @Test
    public void getUserKey_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.getUserKey(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getUserKey(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void setUserKey_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.setUserKey(null, null, null, null)).thenReturn(Response.ok());
        spy.setUserKey(null, null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void setUserKey_callsGetCloud11Service_callsSetUserKey() throws Exception {
        when(delegateCloud11Service.setUserKey(null, null, null, null)).thenReturn(Response.ok());
        spy.setUserKey(null, null, null, null);
        verify(delegateCloud11Service).setUserKey(null, null, null, null);
    }

    @Test
    public void setUserKey_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.setUserKey(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.setUserKey(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getServiceCatalog_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.getServiceCatalog(null, null, null)).thenReturn(Response.ok());
        spy.getServiceCatalog(null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void getServiceCatalog_callsGetCloud11Service_callsGetServiceCatalog() throws Exception {
        when(delegateCloud11Service.getServiceCatalog(null, null, null)).thenReturn(Response.ok());
        spy.getServiceCatalog(null, null, null);
        verify(delegateCloud11Service).getServiceCatalog(null, null, null);
    }

    @Test
    public void getServiceCatalog_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.getServiceCatalog(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getServiceCatalog(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getBaseURLRefs_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.getBaseURLRefs(null, null, null)).thenReturn(Response.ok());
        spy.getBaseURLRefs(null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void getBaseURLRefs_callsGetCloud11Service_callsGetBaseURLRefs() throws Exception {
        when(delegateCloud11Service.getBaseURLRefs(null, null, null)).thenReturn(Response.ok());
        spy.getBaseURLRefs(null, null, null);
        verify(delegateCloud11Service).getBaseURLRefs(null, null, null);
    }

    @Test
    public void getBaseURLRefs_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.getBaseURLRefs(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getBaseURLRefs(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void addBaseURLRef_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.addBaseURLRef(null, null, null, null, null)).thenReturn(Response.ok());
        spy.addBaseURLRef(null, null, null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void addBaseURLRef_callsGetCloud11Service_callsAddBaseURLRef() throws Exception {
        when(delegateCloud11Service.addBaseURLRef(null, null, null, null, null)).thenReturn(Response.ok());
        spy.addBaseURLRef(null, null, null, null, null);
        verify(delegateCloud11Service).addBaseURLRef(null, null, null, null, null);
    }

    @Test
    public void addBaseURLRef_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.addBaseURLRef(null, null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.addBaseURLRef(null, null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getBaseURLRef_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.getBaseURLRef(null, null, null, null)).thenReturn(Response.ok());
        spy.getBaseURLRef(null, null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void getBaseURLRef_callsGetCloud11Service_callsGetBaseURLRef() throws Exception {
        when(delegateCloud11Service.getBaseURLRef(null, null, null, null)).thenReturn(Response.ok());
        spy.getBaseURLRef(null, null, null, null);
        verify(delegateCloud11Service).getBaseURLRef(null, null, null, null);
    }

    @Test
    public void getBaseURLRef_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.getBaseURLRef(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.getBaseURLRef(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteBaseURLRef_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.deleteBaseURLRef(null, null, null, null)).thenReturn(Response.ok());
        spy.deleteBaseURLRef(null, null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void deleteBaseURLRef_callsGetCloud11Service_callsDeleteBaseURLRef() throws Exception {
        when(delegateCloud11Service.deleteBaseURLRef(null, null, null, null)).thenReturn(Response.ok());
        spy.deleteBaseURLRef(null, null, null, null);
        verify(delegateCloud11Service).deleteBaseURLRef(null, null, null, null);
    }

    @Test
    public void deleteBaseURLRef_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.deleteBaseURLRef(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteBaseURLRef(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserGroups_callsGetCloud11Service() throws Exception {
        when(delegateCloud11Service.getUserGroups(null, null, null)).thenReturn(Response.ok());
        spy.getUserGroups(null, null, null);
        verify(spy).getCloud11Service();
    }

    @Test
    public void getUserGroups_callsGetCloud11Service_callsGetUserGroups() throws Exception {
        when(delegateCloud11Service.getUserGroups(null, null, null)).thenReturn(Response.ok());
        spy.getUserGroups(null, null, null);
        verify(delegateCloud11Service).getUserGroups(null, null, null);
    }

    @Test
    public void getUserGroups_responseOk_returns200() throws Exception {
        when(delegateCloud11Service.getUserGroups(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getUserGroups(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getCloud11Service_useCloudAuth_returnsDelegateCloud11Service() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        Cloud11Service result = cloudV11VersionResource.getCloud11Service();
        assertThat("service", result instanceof DelegateCloud11Service, equalTo(true));
    }

    @Test
    public void getCloud11Service_useCloudAuth_returnsDefaultCloud11Service() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        Cloud11Service result = cloudV11VersionResource.getCloud11Service();
        assertThat("service", result instanceof DefaultCloud11Service, equalTo(true));
    }
}
