package com.rackspace.idm.api.resource.cloud;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Ignore;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.CredentialListType;
import org.openstack.docs.identity.api.v2.EndpointList;
import org.openstack.docs.identity.api.v2.RoleList;
import org.openstack.docs.identity.api.v2.Tenants;
import org.openstack.docs.identity.api.v2.User;
import org.openstack.docs.identity.api.v2.UserList;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.exception.IdmException;
import com.rackspacecloud.docs.auth.api.v1.BaseURLList;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 8/6/12
 * Time: 12:08 PM
 * To change this template use File | Settings | File Templates.
 */

@Ignore
@RunWith(PowerMockRunner.class)
@PrepareForTest(WebResource.Builder.class)
public class MigrationClientTest {
    WebResource webResource;
    WebResource.Builder builder;
    MigrationClient migrationClient;
    Client client;
    String cloud20Host;
    String cloud11Host;

    @Before
    public void setUp() throws Exception {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        migrationClient = new MigrationClient();

        client = new Client();
        client = spy(client);

        webResource = client.resource("http://localhost");
        webResource = spy(webResource);

        builder = webResource.type(MediaType.APPLICATION_XML);
        builder = PowerMockito.spy(builder);

        //return webResource
        doReturn(webResource).when(client).resource(anyString());
        doReturn(builder).when(webResource).accept(anyString());
        doReturn(builder).when(webResource).type(anyString());
        doReturn(builder).when(webResource).header(anyString(), anyString());
        
        doReturn(builder).when(builder).accept(anyString());
        doReturn(builder).when(builder).type(anyString());
        doReturn(builder).when(builder).header(anyString(), anyString());

        // setting up fields
        cloud11Host = "cloud11host/";
        cloud20Host = "cloud20host/";
        migrationClient.setClient(client);
        migrationClient.setCloud11Host(cloud11Host);
        migrationClient.setCloud20Host(cloud20Host);
    }

    @Test
    public void authenticateWithPassword_returnsAuthenticateResponse() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<access xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "    <token id=\"ab48a9efdfedb23ty3494\" expires=\"2010-11-01T03:32:15-05:00\">\n" +
                "        <tenant id=\"345\" name=\"My Project\" />\n" +
                "    </token>\n" +
                "    <user id=\"123\" username=\"jqsmith\">\n" +
                "        <roles xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "            <role id=\"123\" name=\"compute:admin\" />\n" +
                "            <role id=\"234\" name=\"object-store:admin\" />\n" +
                "        </roles>\n" +
                "    </user>\n" +
                "</access>";

        doReturn(body).when(builder).post(eq(String.class), anyString());
        AuthenticateResponse result = migrationClient.authenticateWithPassword("username", "password");
        assertThat("user id", result.getUser().getId(), equalTo("123"));
        assertThat("user id", result.getToken().getId(), equalTo("ab48a9efdfedb23ty3494"));
    }

    @Test
    public void authenticateWithApiKey_returnsAuthenticateResponse() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<access xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "    <token id=\"ab48a9efdfedb23ty3494\" expires=\"2010-11-01T03:32:15-05:00\">\n" +
                "        <tenant id=\"345\" name=\"My Project\" />\n" +
                "    </token>\n" +
                "    <user id=\"123\" username=\"jqsmith\">\n" +
                "        <roles xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "            <role id=\"123\" name=\"compute:admin\" />\n" +
                "            <role id=\"234\" name=\"object-store:admin\" />\n" +
                "        </roles>\n" +
                "    </user>\n" +
                "</access>";
        doReturn(body).when(builder).post(eq(String.class), anyString());
        AuthenticateResponse result = migrationClient.authenticateWithApiKey("username", "apikey");
        assertThat("user id", result.getUser().getId(), equalTo("123"));
        assertThat("user id", result.getToken().getId(), equalTo("ab48a9efdfedb23ty3494"));
    }

    @Test
    public void getTenants_returnsTenants() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<tenants xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "    <tenant enabled=\"true\" id=\"1234\" name=\"ACME Corp\">\n" +
                "        <description>A description...</description>\n" +
                "    </tenant>\n" +
                "    <tenant enabled=\"true\" id=\"3645\" name=\"Iron Works\">\n" +
                "        <description>A description...</description>\n" +
                "    </tenant>\n" +
                "</tenants>";
        doReturn(body).when(builder).get(eq(String.class));
        Tenants result = migrationClient.getTenants("token");
        assertThat("tenant id", result.getTenant().get(0).getId(), equalTo("1234"));
    }

    @Test
    public void getEndpointsByToken_returnsEndpointList() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                " \n" +
                "<endpoints\n" +
                "    xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "  <endpoint\n" +
                "      id=\"1\"\n" +
                "      tenantId=\"1\"\n" +
                "      type=\"compute\"\n" +
                "      name=\"Compute\"\n" +
                "      region=\"North\"\n" +
                "      publicURL=\"https://compute.north.public.com/v1\"\n" +
                "      internalURL=\"https://compute.north.internal.com/v1\"\n" +
                "      adminURL=\"https://compute.north.internal.com/v1\">\n" +
                "      <version\n" +
                "          id=\"1\"\n" +
                "          info=\"https://compute.north.public.com/v1/\"\n" +
                "          list=\"https://compute.north.public.com/\"\n" +
                "      />\n" +
                "  </endpoint>\n" +
                "</endpoints>";
        doReturn(body).when(builder).get(eq(String.class));
        EndpointList result = migrationClient.getEndpointsByToken("adminToken", "token");
        assertThat("public url", result.getEndpoint().get(0).getPublicURL(), equalTo("https://compute.north.public.com/v1"));
    }

    @Test
    public void getUser_returnsUser() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<user xmlns=\"http://docs.openstack.org/identity/api/v2.0\"\n" +
                "      xmlns:ns2=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\"\n" +
                "      xmlns:rax-auth=\"http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0\"\n" +
                "      id=\"123456\" username=\"jqsmith\"\n" +
                "      enabled=\"true\"\n" +
                "      email=\"john.smith@example.org\"\n" +
                "      rax-auth:defaultRegion=\"DFW\">\n" +
                "</user>";
        doReturn(body).when(builder).get(eq(String.class));
        User result = migrationClient.getUser("token", "username");
        assertThat("user id", result.getId(), equalTo("123456"));
    }

    @Test
    public void getRolesForUser_returnsRoleList() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                " \n" +
                "<roles xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "  <role id=\"123\" name=\"Admin\" description=\"All Access\" />\n" +
                "  <role id=\"234\" name=\"Guest\" description=\"Guest Access\" />\n" +
                "</roles>";
        doReturn(body).when(builder).get(eq(String.class));
        RoleList result = migrationClient.getRolesForUser("token", "userId");
        assertThat("role id", result.getRole().get(0).getId(), equalTo("123"));
        assertThat("role id", result.getRole().get(1).getId(), equalTo("234"));
    }

    @Test (expected = IdmException.class)
    public void getRolesForUser_badData_throwsIdmExpcetion() throws Exception {
        String body = "bad data";
        doReturn(body).when(builder).get(eq(String.class));
        migrationClient.getRolesForUser("token", "userId");
    }

    @Test
    public void getUsers_withUsers_returnsUserList() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<users xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "    <user xmlns=\"http://docs.openstack.org/identity/api/v2.0\"\n" +
                "          enabled=\"true\" email=\"john.smith@example.org\"\n" +
                "          username=\"jqsmith\" id=\"123456\"/>\n" +
                "</users>";
        doReturn(body).when(builder).get(eq(String.class));
        UserList result = migrationClient.getUsers("token");
        assertThat("", result.getUser().get(0).getId(), equalTo("123456"));
    }

    @Test
    public void getUsers_withUser_returnsUserList() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<user xmlns=\"http://docs.openstack.org/identity/api/v2.0\"\n" +
                "      xmlns:ns2=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\"\n" +
                "      xmlns:rax-auth=\"http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0\"\n" +
                "      id=\"123456\" username=\"jqsmith\"\n" +
                "      enabled=\"true\"\n" +
                "      email=\"john.smith@example.org\"\n" +
                "      rax-auth:defaultRegion=\"DFW\">\n" +
                "</user>";
        doReturn(body).when(builder).get(eq(String.class));
        UserList result = migrationClient.getUsers("token");
        assertThat("", result.getUser().get(0).getId(), equalTo("123456"));
    }

    @Test (expected = IdmException.class)
    public void getUsers_badData_throwsIdmException() throws Exception {
        String body = "bad data";
        doReturn(body).when(builder).get(eq(String.class));
        migrationClient.getUsers("token");
    }

    @Test
    public void getSecretQA_returnsSecretQA() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<secretQA xmlns=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\"\n" +
                "          question=\"What is the color of my eyes?\"\n" +
                "          answer=\"Leonardo Da Vinci\" />";
        doReturn(body).when(builder).get(eq(String.class));
        SecretQA result = migrationClient.getSecretQA("token", "userId");
        assertThat("question", result.getQuestion(), equalTo("What is the color of my eyes?"));
        assertThat("answer", result.getAnswer(), equalTo("Leonardo Da Vinci"));
    }

    @Test
    public void getGroups_returnsGroups() throws Exception {
        String body = "<groups xmlns=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\"\n" +
                "        xmlns:atom=\"http://www.w3.org/2005/Atom\">\n" +
                "    <group id=\"1234\" name=\"group1\">\n" +
                "        <description>A Description of the group</description>\n" +
                "    </group>\n" +
                "</groups>";
        doReturn(body).when(builder).get(eq(String.class));
        Groups result = migrationClient.getGroups("token");
        assertThat("group id", result.getGroup().get(0).getId(), equalTo("1234"));
    }

    @Test (expected = IdmException.class)
    public void getGroups_badData_throwsIdmException() throws Exception {
        String body = "bad data";
        doReturn(body).when(builder).get(eq(String.class));
        migrationClient.getGroups("token");
    }

    @Test
    public void getGroupsForUser_returnsGroups() throws Exception {
        String body = "<groups xmlns=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\">\n" +
                "    <group xmlns=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" id=\"test_global_group_add\">\n" +
                "        <description>A Description of the group</description>\n" +
                "    </group>\n" +
                "</groups>";
        doReturn(body).when(builder).get(eq(String.class));
        Groups result = migrationClient.getGroupsForUser("token", "userId");
        assertThat("group id", result.getGroup().get(0).getId(), equalTo("test_global_group_add"));
    }

    @Test (expected = IdmException.class)
    public void getGroupsForUser_badData_throwsIdmException() throws Exception {
        String body = "bad data";
        doReturn(body).when(builder).get(eq(String.class));
        migrationClient.getGroupsForUser("token", "userId");
    }

    @Test
    public void getUserCredentials_returnsCredentialListType() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<ns2:credentials\n" +
                "    xmlns:ns1=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\"\n" +
                "    xmlns:ns2=\"http://docs.openstack.org/identity/api/v2.0\"\n" +
                "    xmlns:ns3=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\">\n" +
                "    <ns3:apiKeyCredentials\n" +
                "        username=\"bobbuilder\"\n" +
                "        apiKey=\"0f97f489c848438090250d50c7e1eaXZ\"/>\n" +
                "</ns2:credentials>";
        doReturn(body).when(builder).get(eq(String.class));
        CredentialListType result = migrationClient.getUserCredentials("token", "userId");
        assertThat("", result.getCredential().get(0).getName().toString().contains("apiKeyCredentials"), equalTo(true));
    }

    @Test
    public void getUserTenantsBaseUrls_returnsUser() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                " \n" +
                "<user xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\"\n" +
                "      id=\"hub_cap\"\n" +
                "      mossoId=\"323676\"\n" +
                "      nastId=\"RackCloudFS_bf571533-d113-4249-962d-178648338863\"\n" +
                "      key=\"asdasdasd-adsasdads-asdasdasd-adsadsasd\"\n" +
                "      enabled=\"true\"\n" +
                "      created=\"2009-11-01T03:32:15-05:00\"\n" +
                "      updated=\"2010-10-02T02:22:22-05:00\">\n" +
                "    <baseURLRefs>\n" +
                "        <baseURLRef\n" +
                "                href=\"https://auth.api.rackspacecloud.com/v1.1/baseURLs/1\"\n" +
                "                id=\"1\" v1Default=\"true\"/>\n" +
                "    </baseURLRefs>\n" +
                "</user>";
        doReturn(body).when(builder).get(eq(String.class));
        com.rackspacecloud.docs.auth.api.v1.User result = migrationClient.getUserTenantsBaseUrls("username", "password", "userId");
        assertThat("base url ref href", result.getBaseURLRefs().getBaseURLRef().get(0).getHref(), equalTo("https://auth.api.rackspacecloud.com/v1.1/baseURLs/1"));
    }

    @Test
    public void getBaseUrls_returnsBaseUrlList() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                " \n" +
                "<baseURLs xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\">\n" +
                "  <baseURL\n" +
                "   id=\"1\"\n" +
                "   userType=\"NAST\"\n" +
                "   region=\"DFW\"\n" +
                "   default=\"true\"\n" +
                "   serviceName=\"cloudFiles\"\n" +
                "   publicURL=\"https://storage.clouddrive.com/v1\"\n" +
                "   internalURL=\"https://storage-snet.clouddrive.com/v1\"\n" +
                "   enabled=\"true\"\n/>\n" +
                "</baseURLs>";
        doReturn(body).when(builder).get(eq(String.class));
        BaseURLList result = migrationClient.getBaseUrls("username", "password");
        assertThat("public url", result.getBaseURL().get(0).getPublicURL(), equalTo("https://storage.clouddrive.com/v1"));
        assertThat("userType", result.getBaseURL().get(0).getUserType().value(), equalTo("NAST"));
    }

    @Test
    public void getRoles_returnsRoleList() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<roles xmlns=\"http://docs.openstack.org/identity/api/v2.0\">\n" +
                "  <role id=\"123\" name=\"Admin\" description=\"All Access\" />\n" +
                "  <role id=\"234\" name=\"Guest\" description=\"Guest Access\" />\n" +
                "</roles>";
        doReturn(body).when(builder).get(eq(String.class));
        RoleList result = migrationClient.getRoles("token");
        assertThat("role id 1", result.getRole().get(0).getId(), equalTo("123"));
        assertThat("role id 2", result.getRole().get(1).getId(), equalTo("234"));
    }

    @Test
    public void getEndpointTemplates_returnsEndpointTemplateList() throws Exception {
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                " \n" +
                "<endpointTemplates xmlns=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\">\n" +
                "  <endpointTemplate\n" +
                "      id=\"1\"\n" +
                "      tenantId=\"1\"\n" +
                "      type=\"compute\"\n" +
                "      name=\"Compute\"\n" +
                "      region=\"North\"\n" +
                "      publicURL=\"https://compute.north.public.com/v1\"\n" +
                "      internalURL=\"https://compute.north.internal.com/v1\"\n" +
                "      adminURL=\"https://compute.north.internal.com/v1\">\n" +
                "      <version\n" +
                "          id=\"1\"\n" +
                "          info=\"https://compute.north.public.com/v1/\"\n" +
                "          list=\"https://compute.north.public.com/\"\n" +
                "      />\n" +
                "  </endpointTemplate>\n" +
                "</endpointTemplates>";
        doReturn(body).when(builder).get(eq(String.class));
        EndpointTemplateList result = migrationClient.getEndpointTemplates("token");
        assertThat("endpoint id", result.getEndpointTemplate().get(0).getId(), equalTo(1));
        assertThat("public url", result.getEndpointTemplate().get(0).getPublicURL(), equalTo("https://compute.north.public.com/v1"));
    }

    @Test (expected = IdmException.class)
    public void getEndpointTemplates_badData_throwsIdmException() throws Exception {
        String body = "bad data";
        doReturn(body).when(builder).get(eq(String.class));
        migrationClient.getEndpointTemplates("token");
    }
}
