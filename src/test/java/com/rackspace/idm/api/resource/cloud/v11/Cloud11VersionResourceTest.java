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
    DefaultCloud11Service cloud11Service;

    @Before
    public void setUp() throws Exception {
        // setup
        config = mock(Configuration.class);
        cloudClient = mock(CloudClient.class);
        cloudV11VersionResource = new Cloud11VersionResource(config, cloudClient);

        // mocks
        cloud11Service = mock(DefaultCloud11Service.class);

        // fields
        cloudV11VersionResource.setCloud11Service(cloud11Service);

        spy = spy(cloudV11VersionResource);
    }

    @Test
    public void getCloud11VersionInfo_okResponse_returns200() throws Exception {
        when(cloud11Service.getVersion(null)).thenReturn(Response.ok());
        Response result = spy.getCloud11VersionInfo();
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void authenticate_callsGetCloud11Service_callsAuthenticate() throws Exception {
        when(cloud11Service.authenticate(null, null, null, null)).thenReturn(Response.ok());
        spy.authenticate(null, null, null, null);
        verify(cloud11Service).authenticate(null, null, null, null);
    }

    @Test
    public void authenticate_responseOk_returns200() throws Exception {
        when(cloud11Service.authenticate(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.authenticate(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void hack_badRequest_returns400() throws Exception {
        Response result = spy.hack();
        assertThat("response code", result.getStatus(), equalTo(400));
    }

    @Test
    public void adminAuthenticate_responseOk_returns200() throws Exception {
        when(cloud11Service.authenticate(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.authenticate(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void validateToken_callsGetCloud11Service_callsValidateToken() throws Exception {
        when(cloud11Service.validateToken(null, null, null, null, null)).thenReturn(Response.ok());
        spy.validateToken(null, null, null, null, null);
        verify(cloud11Service).validateToken(null, null, null, null, null);
    }

    @Test
    public void validateToken_responseOk_returns200() throws Exception {
        when(cloud11Service.validateToken(null, null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.validateToken(null, null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void revokeToken_callsGetCloud11Service_callsRevokeToken() throws Exception {
        when(cloud11Service.revokeToken(null, null, null)).thenReturn(Response.ok());
        spy.revokeToken(null, null, null);
        verify(cloud11Service).revokeToken(null, null, null);
    }

    @Test
    public void revokeToken_responseOk_returns200() throws Exception {
        when(cloud11Service.revokeToken(null, null, null)).thenReturn(Response.ok());
        Response result = spy.revokeToken(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void extensions_callsGetCloud11Service_callsExtensions() throws Exception {
        when(cloud11Service.extensions(null)).thenReturn(Response.ok());
        spy.extensions(null);
        verify(cloud11Service).extensions(null);
    }

    @Test
    public void extensions_responseOk_returns200() throws Exception {
        when(cloud11Service.extensions(null)).thenReturn(Response.ok());
        Response result = spy.extensions(null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void extensionsWithAlias_callsGetCloud11Service_callsExtensions() throws Exception {
        when(cloud11Service.getExtension(null, null)).thenReturn(Response.ok());
        spy.extensions(null, null);
        verify(cloud11Service).getExtension(null, null);
    }

    @Test
    public void extensionsWithAlias_responseOk_returns200() throws Exception {
        when(cloud11Service.getExtension(null, null)).thenReturn(Response.ok());
        Response result = spy.extensions(null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserFromNastId_callsGetCloud11Service_callsGetUserFromNastId() throws Exception {
        when(cloud11Service.getUserFromNastId(null, null, null)).thenReturn(Response.ok());
        spy.getUserFromNastId(null, null, null);
        verify(cloud11Service).getUserFromNastId(null, null, null);
    }

    @Test
    public void getUserFromNastId_responseOk_returns200() throws Exception {
        when(cloud11Service.getUserFromNastId(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getUserFromNastId(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserFromMossoId_callsGetCloud11Service_callsGetUserFromMossoId() throws Exception {
        when(cloud11Service.getUserFromMossoId(null, 0, null)).thenReturn(Response.ok());
        spy.getUserFromMossoId(null, 0, null);
        verify(cloud11Service).getUserFromMossoId(null, 0, null);
    }

    @Test
    public void getUserFromMossoId_responseOk_returns200() throws Exception {
        when(cloud11Service.getUserFromMossoId(null, 0, null)).thenReturn(Response.ok());
        Response result = spy.getUserFromMossoId(null, 0, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getBaseURLs_callsGetCloud11Service_callsGetBaseURLs() throws Exception {
        when(cloud11Service.getBaseURLs(null, null, null)).thenReturn(Response.ok());
        spy.getBaseURLs(null, null, null);
        verify(cloud11Service).getBaseURLs(null, null, null);
    }

    @Test
    public void getBaseURLs_responseOk_returns200() throws Exception {
        when(cloud11Service.getBaseURLs(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getBaseURLs(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void addBaseURL_callsGetCloud11Service_callsAddBaseURL() throws Exception {
        when(cloud11Service.addBaseURL(null, null, null)).thenReturn(Response.ok());
        spy.addBaseURL(null, null, null);
        verify(cloud11Service).addBaseURL(null, null, null);
    }

    @Test
    public void addBaseURL_responseOk_returns200() throws Exception {
        when(cloud11Service.addBaseURL(null, null, null)).thenReturn(Response.ok());
        Response result = spy.addBaseURL(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getEnabledBaseURLs_callsGetCloud11Service_callsGetEnabledBaseURLs() throws Exception {
        when(cloud11Service.getEnabledBaseURL(null, null, null)).thenReturn(Response.ok());
        spy.getEnabledBaseURLs(null, null, null);
        verify(cloud11Service).getEnabledBaseURL(null, null, null);
    }

    @Test
    public void getEnabledBaseURLs_responseOk_returns200() throws Exception {
        when(cloud11Service.getEnabledBaseURL(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getEnabledBaseURLs(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void createUser_callsGetCloud11Service_callsCreateUser() throws Exception {
        when(cloud11Service.createUser(null, null, null, null)).thenReturn(Response.ok());
        spy.createUser(null, null, null, null);
        verify(cloud11Service).createUser(null, null, null, null);
    }

    @Test
    public void createUser_responseOk_returns200() throws Exception {
        when(cloud11Service.createUser(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.createUser(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUser_callsGetCloud11Service_callsGetUser() throws Exception {
        when(cloud11Service.getUser(null, null, null)).thenReturn(Response.ok());
        spy.getUser(null, null, null);
        verify(cloud11Service).getUser(null, null, null);
    }

    @Test
    public void getUser_responseOk_returns200() throws Exception {
        when(cloud11Service.getUser(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getUser(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteUser_callsGetCloud11Service_callsDeleteUser() throws Exception {
        when(cloud11Service.deleteUser(null, null, null)).thenReturn(Response.ok());
        spy.deleteUser(null, null, null);
        verify(cloud11Service).deleteUser(null, null, null);
    }

    @Test
    public void deleteUser_responseOk_returns200() throws Exception {
        when(cloud11Service.deleteUser(null, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteUser(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void updateUser_callsGetCloud11Service_callsUpdateUser() throws Exception {
        when(cloud11Service.updateUser(null, null, null, null)).thenReturn(Response.ok());
        spy.updateUser(null, null, null, null);
        verify(cloud11Service).updateUser(null, null, null, null);
    }

    @Test
    public void updateUser_responseOk_returns200() throws Exception {
        when(cloud11Service.updateUser(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.updateUser(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserEnabled_callsGetCloud11Service_callsGetUserEnabled() throws Exception {
        when(cloud11Service.getUserEnabled(null, null, null)).thenReturn(Response.ok());
        spy.getUserEnabled(null, null, null);
        verify(cloud11Service).getUserEnabled(null, null, null);
    }

    @Test
    public void getUserEnabled_responseOk_returns200() throws Exception {
        when(cloud11Service.getUserEnabled(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getUserEnabled(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void setUserEnabled_callsGetCloud11Service_callsSetUserEnabled() throws Exception {
        when(cloud11Service.setUserEnabled(null, null, null, null)).thenReturn(Response.ok());
        spy.setUserEnabled(null, null, null, null);
        verify(cloud11Service).setUserEnabled(null, null, null, null);
    }

    @Test
    public void setUserEnabled_responseOk_returns200() throws Exception {
        when(cloud11Service.setUserEnabled(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.setUserEnabled(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserKey_callsGetCloud11Service_callsGetUserKey() throws Exception {
        when(cloud11Service.getUserKey(null, null, null)).thenReturn(Response.ok());
        spy.getUserKey(null, null, null);
        verify(cloud11Service).getUserKey(null, null, null);
    }

    @Test
    public void getUserKey_responseOk_returns200() throws Exception {
        when(cloud11Service.getUserKey(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getUserKey(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void setUserKey_callsGetCloud11Service_callsSetUserKey() throws Exception {
        when(cloud11Service.setUserKey(null, null, null, null)).thenReturn(Response.ok());
        spy.setUserKey(null, null, null, null);
        verify(cloud11Service).setUserKey(null, null, null, null);
    }

    @Test
    public void setUserKey_responseOk_returns200() throws Exception {
        when(cloud11Service.setUserKey(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.setUserKey(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getServiceCatalog_callsGetCloud11Service_callsGetServiceCatalog() throws Exception {
        when(cloud11Service.getServiceCatalog(null, null, null)).thenReturn(Response.ok());
        spy.getServiceCatalog(null, null, null);
        verify(cloud11Service).getServiceCatalog(null, null, null);
    }

    @Test
    public void getServiceCatalog_responseOk_returns200() throws Exception {
        when(cloud11Service.getServiceCatalog(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getServiceCatalog(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getBaseURLRefs_callsGetCloud11Service_callsGetBaseURLRefs() throws Exception {
        when(cloud11Service.getBaseURLRefs(null, null, null)).thenReturn(Response.ok());
        spy.getBaseURLRefs(null, null, null);
        verify(cloud11Service).getBaseURLRefs(null, null, null);
    }

    @Test
    public void getBaseURLRefs_responseOk_returns200() throws Exception {
        when(cloud11Service.getBaseURLRefs(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getBaseURLRefs(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void addBaseURLRef_callsGetCloud11Service_callsAddBaseURLRef() throws Exception {
        when(cloud11Service.addBaseURLRef(null, null, null, null, null)).thenReturn(Response.ok());
        spy.addBaseURLRef(null, null, null, null, null);
        verify(cloud11Service).addBaseURLRef(null, null, null, null, null);
    }

    @Test
    public void addBaseURLRef_responseOk_returns200() throws Exception {
        when(cloud11Service.addBaseURLRef(null, null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.addBaseURLRef(null, null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getBaseURLRef_callsGetCloud11Service_callsGetBaseURLRef() throws Exception {
        when(cloud11Service.getBaseURLRef(null, null, null, null)).thenReturn(Response.ok());
        spy.getBaseURLRef(null, null, null, null);
        verify(cloud11Service).getBaseURLRef(null, null, null, null);
    }

    @Test
    public void getBaseURLRef_responseOk_returns200() throws Exception {
        when(cloud11Service.getBaseURLRef(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.getBaseURLRef(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void deleteBaseURLRef_callsGetCloud11Service_callsDeleteBaseURLRef() throws Exception {
        when(cloud11Service.deleteBaseURLRef(null, null, null, null)).thenReturn(Response.ok());
        spy.deleteBaseURLRef(null, null, null, null);
        verify(cloud11Service).deleteBaseURLRef(null, null, null, null);
    }

    @Test
    public void deleteBaseURLRef_responseOk_returns200() throws Exception {
        when(cloud11Service.deleteBaseURLRef(null, null, null, null)).thenReturn(Response.ok());
        Response result = spy.deleteBaseURLRef(null, null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }

    @Test
    public void getUserGroups_callsGetCloud11Service_callsGetUserGroups() throws Exception {
        when(cloud11Service.getUserGroups(null, null, null)).thenReturn(Response.ok());
        spy.getUserGroups(null, null, null);
        verify(cloud11Service).getUserGroups(null, null, null);
    }

    @Test
    public void getUserGroups_responseOk_returns200() throws Exception {
        when(cloud11Service.getUserGroups(null, null, null)).thenReturn(Response.ok());
        Response result = spy.getUserGroups(null, null, null);
        assertThat("response code", result.getStatus(), equalTo(200));
    }
}
