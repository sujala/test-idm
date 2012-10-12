package com.rackspace.idm.api.converter.cloudv11;

import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspace.idm.domain.entity.User;
import com.rackspacecloud.docs.auth.api.v1.UserWithId;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyEnabled;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyKey;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.xml.datatype.DatatypeFactory;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/13/12
 * Time: 1:35 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:app-config.xml")
public class UserConverterCloudV11Test {
    @Autowired
    private UserConverterCloudV11 userConverterCloudV11;
    @Autowired
    private EndpointConverterCloudV11 endpointConverterCloudV11;
    @Autowired
    private Configuration config;
    private CloudBaseUrl cloudBaseUrl;

    @Before
    public void setUp() throws Exception {
        cloudBaseUrl = new CloudBaseUrl();
        // Fields
        cloudBaseUrl.setBaseUrlId(1);
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlType("nast");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(true);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setOpenstackType("openStackType");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setRegion("region");
        cloudBaseUrl.setServiceName("serviceName");
        cloudBaseUrl.setVersionId("versionId");
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setUniqueId("uniqueId");
    }

    @Test
    public void toUserDO_emptyUser_createsUserDomainWithEmptyValues() throws Exception {
        com.rackspacecloud.docs.auth.api.v1.User user = new com.rackspacecloud.docs.auth.api.v1.User();
        User entityUser = userConverterCloudV11.toUserDO(user);
        assertThat("user", entityUser.toString(), equalTo("username=null, customer=null"));
    }

    @Test
    public void toUserDO_nonEmptyUser_createsUserDomain() throws Exception {
        com.rackspacecloud.docs.auth.api.v1.User user = new com.rackspacecloud.docs.auth.api.v1.User();
        user.setId("id");
        user.setMossoId(1);
        user.setNastId("nastId");
        user.setKey("key");
        user.setEnabled(true);
        User entityUser = userConverterCloudV11.toUserDO(user);
        assertThat("id", entityUser.getUsername(), equalTo("id"));
        assertThat("mosso id", entityUser.getMossoId(), equalTo(1));
        assertThat("nast id", entityUser.getNastId(), equalTo("nastId"));
        assertThat("api key", entityUser.getApiKey(), equalTo("key"));
        assertThat("enabled", entityUser.isEnabled(), equalTo(true));
    }

    @Test
    public void toCloudV11User_cloudEndpointIsNull_createsUser() throws Exception {
        User user = new User();
        user.setUsername("username");
        com.rackspacecloud.docs.auth.api.v1.User jaxbUser = userConverterCloudV11.toCloudV11User(user, null);
        assertThat("username", jaxbUser.getId(), equalTo("username"));
    }

    @Test
    public void toCloudV11User_cloudEndpointSizeZero_createsUser() throws Exception {
        User user = new User();
        user.setUsername("username");
        List<OpenstackEndpoint> endpointList = new ArrayList<OpenstackEndpoint>();
        com.rackspacecloud.docs.auth.api.v1.User jaxbUser = userConverterCloudV11.toCloudV11User(user, endpointList);
        assertThat("username", jaxbUser.getId(), equalTo("username"));
    }

    @Test
    public void toCloudV11User_cloudEndpointSizeMoreThanZero_createsUserWIthAddedBaseUrlReference() throws Exception {
        User user = new User();
        user.setUsername("username");
        List<OpenstackEndpoint> endpointList = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(1);
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setV1Default(true);
        cloudBaseUrlList.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(cloudBaseUrlList);
        endpointList.add(openstackEndpoint);
        com.rackspacecloud.docs.auth.api.v1.User jaxbUser = userConverterCloudV11.toCloudV11User(user, endpointList);
        assertThat("username", jaxbUser.getId(), equalTo("username"));
        assertThat("id", jaxbUser.getBaseURLRefs().getBaseURLRef().get(0).getId(), equalTo(1));
        assertThat("v1default", jaxbUser.getBaseURLRefs().getBaseURLRef().get(0).isV1Default(), equalTo(true));
        assertThat("reference string", jaxbUser.getBaseURLRefs().getBaseURLRef().get(0).getHref(), equalTo("https://dev.identity.api.rackspacecloud.com/v1.1/baseURLs/1"));
    }

    @Test
    public void openstackToCloudV11User_openStackEndpointIsNull_createsUser() throws Exception {
        User user = new User();
        user.setUsername("username");
        com.rackspacecloud.docs.auth.api.v1.User jaxbUser = userConverterCloudV11.openstackToCloudV11User(user, null);
        assertThat("username", jaxbUser.getId(), equalTo("username"));
    }

    @Test
    public void openstackToCloudV11User_openStackSizeIsZero_createsUser() throws Exception {
        List<OpenstackEndpoint> endpointList = new ArrayList<OpenstackEndpoint>();
        User user = new User();
        user.setUsername("username");
        com.rackspacecloud.docs.auth.api.v1.User jaxbUser = userConverterCloudV11.openstackToCloudV11User(user, endpointList);
        assertThat("username", jaxbUser.getId(), equalTo("username"));
    }

    @Test
    public void openstackToCloudV11User_openStackSizeMoreThanZero_createsUser() throws Exception {
        List<OpenstackEndpoint> endpointList = new ArrayList<OpenstackEndpoint>();
        List<CloudBaseUrl> urlList = new ArrayList<CloudBaseUrl>();
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        cloudBaseUrl.setV1Default(true);
        urlList.add(cloudBaseUrl);
        openstackEndpoint.setBaseUrls(urlList);
        endpointList.add(openstackEndpoint);
        User user = new User();
        user.setUsername("username");
        com.rackspacecloud.docs.auth.api.v1.User jaxbUser = userConverterCloudV11.openstackToCloudV11User(user, endpointList);
        assertThat("username", jaxbUser.getId(), equalTo("username"));
        assertThat("id", jaxbUser.getBaseURLRefs().getBaseURLRef().get(0).getId(), equalTo(1));
        assertThat("v1default", jaxbUser.getBaseURLRefs().getBaseURLRef().get(0).isV1Default(), equalTo(true));
        assertThat("reference string", jaxbUser.getBaseURLRefs().getBaseURLRef().get(0).getHref(), equalTo("https://dev.identity.api.rackspacecloud.com/v1.1/baseURLs/1"));
    }

    @Test
    public void toCloudV11User_userGetCreatedIsNull_returnsUser() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setApiKey("apiKey");
        user.setMossoId(1);
        user.setNastId("nastId");
        user.setEnabled(true);
        com.rackspacecloud.docs.auth.api.v1.User jaxbUser = userConverterCloudV11.toCloudV11User(user);
        assertThat("username", jaxbUser.getId(), equalTo("username"));
    }

    @Test
    public void toCloudV11User_userGetCreatedNotNull_returnsUserWithCreated() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setApiKey("apiKey");
        user.setMossoId(1);
        user.setNastId("nastId");
        user.setEnabled(true);
        user.setCreated(new DateTime(1));
        com.rackspacecloud.docs.auth.api.v1.User jaxbUser = userConverterCloudV11.toCloudV11User(user);
        assertThat("created date", jaxbUser.getCreated(), equalTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(new DateTime(1).toGregorianCalendar())));
    }

    @Test
    public void toCloudV11UserGetUpdatedNotNull_returnsUserWithUPdated() throws Exception {
        User user = new User();
        user.setUsername("username");
        user.setApiKey("apiKey");
        user.setMossoId(1);
        user.setNastId("nastId");
        user.setEnabled(true);
        user.setUpdated(new DateTime(1));
        com.rackspacecloud.docs.auth.api.v1.User jaxbUser = userConverterCloudV11.toCloudV11User(user);
        assertThat("updated date", jaxbUser.getUpdated(), equalTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(new DateTime(1).toGregorianCalendar())));
    }

    @Test
    public void toCloudV11UserWithOnlyEnabled_createsUser_returnsUser() throws Exception {
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        User user = new User();
        user.setUsername("username");
        user.setEnabled(true);
        UserWithOnlyEnabled jaxbUser = userConverterCloudV11.toCloudV11UserWithOnlyEnabled(user,endpoints);
        assertThat("id", jaxbUser.getId(), equalTo("username"));
        assertThat("enabled", jaxbUser.isEnabled(), equalTo(true));
    }

    @Test
    public void toCloudV11UserWithOnlyEnabled_emptyUser_returnsEmptyUser() throws Exception {
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        User user = new User();
        UserWithOnlyEnabled jaxbUser = userConverterCloudV11.toCloudV11UserWithOnlyEnabled(user,endpoints);
        assertThat("id", jaxbUser.getId(), equalTo(null));
        assertThat("enabled", jaxbUser.isEnabled(), equalTo(true));
    }

    @Test
    public void toCloudV11UserWithId_emptyUser_returnsEmptyUser() throws Exception {
        User user = new User();
        UserWithId jaxbUser = userConverterCloudV11.toCloudV11UserWithId(user);
        assertThat("id", jaxbUser.getId(), equalTo(null));
    }

    @Test
    public void toCloudV11UserWithId_createsUser_returnsUser() throws Exception {
        User user = new User();
        user.setUsername("username");
        UserWithId jaxbUser = userConverterCloudV11.toCloudV11UserWithId(user);
        assertThat("id", jaxbUser.getId(), equalTo("username"));
    }

    @Test
    public void toCloudV11UserWithOnlyKeys_emptyUser_returnsEmptyUser() throws Exception {
         List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        User user = new User();
        UserWithOnlyKey jaxbUser = userConverterCloudV11.toCloudV11UserWithOnlyKey(user,endpoints);
        assertThat("key", jaxbUser.getKey(), equalTo(null));
    }

    @Test
    public void toCloudV11UserWithOnlyKeys_createsUser_returnsUser() throws Exception {
         List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        User user = new User();
        user.setApiKey("apiKey");
        UserWithOnlyKey jaxbUser = userConverterCloudV11.toCloudV11UserWithOnlyKey(user,endpoints);
        assertThat("key", jaxbUser.getKey(), equalTo("apiKey"));
    }
}
