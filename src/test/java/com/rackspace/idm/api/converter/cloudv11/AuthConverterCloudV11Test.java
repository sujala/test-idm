package com.rackspace.idm.api.converter.cloudv11;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspacecloud.docs.auth.api.v1.AuthData;
import com.rackspacecloud.docs.auth.api.v1.FullToken;
import com.rackspacecloud.docs.auth.api.v1.Service;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/12/12
 * Time: 1:25 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:app-config.xml")
public class AuthConverterCloudV11Test {
    @Autowired
    private AuthConverterCloudV11 authConverterCloudV11;
    @Autowired
    private Configuration config;
    @Autowired
    private TokenConverterCloudV11 tokenConverterCloudV11;
    @Autowired
    private EndpointConverterCloudV11 endpointConverterCloudV11;
    private CloudBaseUrl cloudBaseUrl;

    @Before
    public void setUp() throws Exception {
        cloudBaseUrl = new CloudBaseUrl();
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
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
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
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint cloudEndpoint = new OpenstackEndpoint();
        cloudEndpoint.setTenantName("tenant");
        List<CloudBaseUrl> baseUrls = new ArrayList<CloudBaseUrl>();
        baseUrls.add(cloudBaseUrl);
        cloudEndpoint.setBaseUrls(baseUrls);
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
