package com.rackspace.idm.api.converter.cloudv11;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.CloudEndpoint;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspacecloud.docs.auth.api.v1.AuthData;
import com.rackspacecloud.docs.auth.api.v1.FullToken;
import com.rackspacecloud.docs.auth.api.v1.Service;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/12/12
 * Time: 1:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class AuthConverterCloudV11Test {
    private AuthConverterCloudV11 authConverterCloudV11;
    private Configuration config;
    private TokenConverterCloudV11 tokenConverterCloudV11;
    private EndpointConverterCloudV11 endpointConverterCloudV11;
    private CloudBaseUrl cloudBaseUrl;

    @Before
    public void setUp() throws Exception {

        // Setting up mock
        config = mock(Configuration.class);

        // Initiations
        endpointConverterCloudV11 = new EndpointConverterCloudV11(config);
        tokenConverterCloudV11 = new TokenConverterCloudV11();
        authConverterCloudV11 = new AuthConverterCloudV11(config, tokenConverterCloudV11, endpointConverterCloudV11);
        cloudBaseUrl = new CloudBaseUrl();

        // Setting fields
        cloudBaseUrl.setBaseUrlId(1);
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlType("cloud");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(true);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setOpenstackType("openStackType");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setRegion("region");
        cloudBaseUrl.setServiceName("foo");
        cloudBaseUrl.setVersionId("versionId");
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setUniqueId("uniqueId");
    }

    @Test
    public void toCloudV11AuthDataJaxb_callsSetToken() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        List<CloudEndpoint> endpoints = new ArrayList<CloudEndpoint>();
        userScopeAccess.setAccessTokenString("token");
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        AuthData authData = authConverterCloudV11.toCloudv11AuthDataJaxb(userScopeAccess, endpoints);
        assertThat("token id", authData.getToken().getId(), equalTo("token"));
    }

    @Test
    public void toCloudV11AuthDataJaxb_endpointsNull_returnsToken() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("token");
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        AuthData authData = authConverterCloudV11.toCloudv11AuthDataJaxb(userScopeAccess, null);
        assertThat("token id", authData.getToken().getId(), equalTo("token"));
    }

    @Test
    public void toCloudV11AuthDataJaxb_callsSetServiceCatalog() throws Exception {
        CloudEndpoint cloudEndpoint = new CloudEndpoint();
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        List<CloudEndpoint> endpoints = new ArrayList<CloudEndpoint>();
        cloudEndpoint.setMossoId(1);
        cloudEndpoint.setNastId("nastId");
        cloudEndpoint.setUsername("username");
        cloudEndpoint.setV1preferred(true);
        cloudEndpoint.setBaseUrl(cloudBaseUrl);
        endpoints.add(cloudEndpoint);
        userScopeAccess.setAccessTokenString("token");
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        AuthData authData = authConverterCloudV11.toCloudv11AuthDataJaxb(userScopeAccess, endpoints);
        List<Service> services = authData.getServiceCatalog().getService();
        assertThat("service name", services.get(0).getName(), equalTo("foo"));
    }

    @Test
    public void toCloudV11TokenJaxb_createsToken_succeeds() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setUsername("username");
        userScopeAccess.setAccessTokenString("token");
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        when(config.getString("cloud.user.ref.string")).thenReturn("https://identity.api.rackspacecloud.com/");
        when(config.getInt("token.cloudAuthExpirationSeconds")).thenReturn(86400);
        FullToken token = authConverterCloudV11.toCloudV11TokenJaxb(userScopeAccess, "requestUrl");
        assertThat("token id", token.getId(), equalTo("token"));
        assertThat("token userId", token.getUserId(), equalTo("username"));
        assertThat("token url", token.getUserURL(), equalTo("requestUrlusers/username"));
    }

    @Test
    public void toCloudV11TokenJaxb_tokenExpIsNull_returnsToken() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setUsername("username");
        userScopeAccess.setAccessTokenString("token");
        FullToken token = authConverterCloudV11.toCloudV11TokenJaxb(userScopeAccess, "requestUrl");
        assertThat("token id", token.getId(), equalTo("token"));
        assertThat("token userId", token.getUserId(), equalTo("username"));
        assertThat("token url", token.getUserURL(), equalTo("requestUrlusers/username"));
    }
}
