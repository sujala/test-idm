package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.test.Cloud20TestHelper;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.User;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 4:21 PM
 */
public class Cloud20VersionResourceIntegrationTest extends AbstractAroundClassJerseyTest {

    private UserService userService;
    Cloud20TestHelper cloud20TestHelper = new Cloud20TestHelper();

    static String X_AUTH_TOKEN = "X-Auth-Token";
    static String userName = "testServiceAdmin_doNotDelete";
    static String invalidTenant = "999999";
    static String testDomain = "135792468";
    static String invalidDomain = "999999";
    static String email = "testEmail@rackspace.com";
    static String password = "Password1";
    User testServiceAdminUser;


    @Before
    public void setUp() throws Exception {
        ensureGrizzlyStarted("classpath:app-config.xml");
        String token = authenticate("auth", "Auth1234", MediaType.APPLICATION_XML);
        testServiceAdminUser = getUserByName(token, userName);
        if(testServiceAdminUser == null){
            testServiceAdminUser = createServiceAdminUser(token,userName,email,password);
        }
    }

    @Test
    public void getVersion_withValidPath_returns200() throws Exception {
        WebResource resource = resource().path("cloud/v2.0");
        ClientResponse clientResponse = resource.get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void fetDefaultRegionServices_returns200() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/default-region/services");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getVersion_acceptsXml_returnsVersion() throws Exception {
        WebResource resource = resource().path("cloud/v2.0");
        ClientResponse clientResponse = resource.accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        String entity = clientResponse.getEntity(String.class);
        assertThat("version", entity, Matchers.containsString("id=\"v2.0\""));
    }

    @Test
    public void getVersion_acceptsJson_returnsVersion() throws Exception {
        WebResource resource = resource().path("cloud/v2.0");
        ClientResponse clientResponse = resource.get(ClientResponse.class);
        String entity = clientResponse.getEntity(String.class);
        assertThat("version", entity, Matchers.containsString("\"id\":\"v2.0\""));
    }

    @Test
    public void authenticate_withXml_returns200() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens");
        ClientResponse clientResponse = resource
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class,
                        "<auth xmlns=\"http://docs.openstack.org/identity/api/v2.0\"><passwordCredentials username=\"hectorServiceAdmin\" password=\"Password1\"/></auth>");
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void authenticate_withJson_returns200() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens");

        ClientResponse clientResponse = resource
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class,
                        "{\n" +
                                "    \"auth\":{\n" +
                                "        \"passwordCredentials\":{\n" +
                                "            \"username\":\"hectorServiceAdmin\",\n" +
                                "            \"password\":\"Password1\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}");
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }


    @Test
    public void authenticate_badJson_returns400() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens");

        ClientResponse clientResponse = resource
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class,
                        "{" + "\"auth\":{" + "\"passwordCredentials\":{" + "\"username\":\"hectorServiceAdmin\",\"password\":\"Password1\"},\"tenantId\":\"1234\" }");
        assertThat("response code", clientResponse.getStatus(), equalTo(400));
    }


    @Test
    public void authenticate_badJsonNoPw_returns400() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens");

        ClientResponse clientResponse = resource
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class,
                        "{" + "\"auth\":{" + "\"passwordCredentials\":{" + "\"username\":\"hectorServiceAdmin\"},\"tenantId\":\"1234\" }}");
        assertThat("response code", clientResponse.getStatus(), equalTo(400));
    }

    @Test
    public void authenticate_MissingCredentials_returns400() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens");
        ClientResponse clientResponse = resource
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(400));
    }

    @Test
    public void authenticate_invalidUsername_returns401(){
         WebResource resource = resource().path("cloud/v2.0/tokens");

        ClientResponse clientResponse = resource
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class,
                        "{\n" +
                                "    \"auth\":{\n" +
                                "        \"passwordCredentials\":{\n" +
                                "            \"username\":\"bad-user\",\n" +
                                "            \"password\":\"Password1\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}");
        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Test
    public void validateToken_returns200() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/"+token);
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void validateToken_withoutAuthToken_returns401() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/"+token);
        ClientResponse clientResponse = resource.get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Test
    public void validateToken_asDefaultUser_returns403() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token);
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(403));
    }

    @Test
    public void validateToken_againstNonExistentToken_returns404() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/badTokenId");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void validateToken_belongsToIsFalse_returns404() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token).queryParam("belongsTo", "DoesntBelong");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void validateToken_belongsToIsTrue_returns200() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token).queryParam("belongsTo", "kurtTestTenant");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void checkToken_returns200() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/"+token);
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void checkToken_withoutAuthToken_returns401() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/"+token);
        ClientResponse clientResponse = resource.get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Test
    public void checkToken_asDefaultUser_returns403() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token);
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(403));
    }

    @Test
    public void checkToken_againstNonExistentToken_returns404() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/badTokenId");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void checkToken_belongsToIsFalse_returns404() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token).queryParam("belongsTo", "DoesntBelong");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void checkToken_belongsToIsTrue_returns200() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token).queryParam("belongsTo", "kurtTestTenant");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointsForToken_returns200() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token + "/endpoints");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointsForToken_withInvalidToken_returns404() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + "someBadToken" + "/endpoints");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void listEndpointsForToken_withNoAuth_returns401() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens/" + "someBadToken" + "/endpoints");
        ClientResponse clientResponse = resource.get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Test
    public void impersonate_returns200() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/impersonation-tokens");
        ClientResponse clientResponse = resource.header("x-auth-token", token).type(MediaType.APPLICATION_XML_TYPE).post(ClientResponse.class , "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<impersonation\n" +
                "    xmlns=\"http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0\"\n" +
                "    xmlns:ns2=\"http://docs.openstack.org/identity/api/v2.0\"\n" +
                "    xmlns:ns3=\"http://www.w3.org/2005/Atom\">\n" +
                "    <user username=\"kurtDefaultUser\"/>\n" +
                "</impersonation>");

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void impersonate_withNoAuth_returns403() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/impersonation-tokens");
        ClientResponse clientResponse = resource.type(MediaType.APPLICATION_XML_TYPE).post(ClientResponse.class , "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<impersonation\n" +
                "    xmlns=\"http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0\"\n" +
                "    xmlns:ns2=\"http://docs.openstack.org/identity/api/v2.0\"\n" +
                "    xmlns:ns3=\"http://www.w3.org/2005/Atom\">\n" +
                "    <user username=\"kurtDefaultUser\"/>\n" +
                "</impersonation>");

        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Test
    public void impersonate_withInvalidBody_returns400() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/impersonation-tokens");
        ClientResponse clientResponse = resource.header("x-auth-token", token).type(MediaType.APPLICATION_XML_TYPE).post(ClientResponse.class, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<impersonation\n" +
                "</impersonation>");

        assertThat("response code", clientResponse.getStatus(), equalTo(400));
    }

    @Test
    public void impersonate_withDefaultUserAuth_returns403() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/impersonation-tokens");
        ClientResponse clientResponse = resource.header("x-auth-token", token).type(MediaType.APPLICATION_XML_TYPE).post(ClientResponse.class, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<impersonation\n" +
                "    xmlns=\"http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0\"\n" +
                "    xmlns:ns2=\"http://docs.openstack.org/identity/api/v2.0\"\n" +
                "    xmlns:ns3=\"http://www.w3.org/2005/Atom\">\n" +
                "    <user username=\"kurtDefaultUser\"/>\n" +
                "</impersonation>");

        assertThat("response code", clientResponse.getStatus(), equalTo(403));
    }

    @Test
    public void extensions_withDefaultUserAuth() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/extensions");
        ClientResponse clientResponse = resource.header("x-auth-token", token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getUser_withDefaultUser_byName_returns200() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/users").queryParam("name", "kurtDefaultUser");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getUser_withDefaultUser_searchingUserAdmin_returns403() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        
        WebResource resource = resource().path("cloud/v2.0/users").queryParam("name", "kurtUserAdmin");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(403));
    }

    @Test
    public void getUser_withUserAdmin_searchingUserAdmin_returns200() throws Exception {
        String token = authenticate("kurtUserAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/users").queryParam("name", "kurtUserAdmin");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getUser_withUserAdmin_searchingServiceAdmin_returns403() throws Exception {
        String token = authenticate("kurtUserAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/users").queryParam("name", "hectorServiceAdmin");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(403));
    }

    @Test
    public void getUser_withServiceAdmin_searchingServiceAdmin_returns200() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/users").queryParam("name", "hectorServiceAdmin");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRoles() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/users/" + testServiceAdminUser.getId() + "/roles");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getTenants_withDefaultUser_returns200() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tenants");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }


    @Test
    public void getTenants_UserAdmin_returns200() throws Exception {
        String token = authenticate("kurtUserAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tenants");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getTenants_badToken_returns401() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tenants");
        ClientResponse clientResponse = resource.header("X-Auth-Token", "bad").accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Test
    public void listUserGroups_returns200() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/users/10013387/RAX-KSGRP");
        ClientResponse clientResponse = resource.header("X-Auth-Token",token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void listUserGroups_invalidAuthToken_returns401() throws Exception {
        String token = "invalid";
        WebResource resource = resource().path("cloud/v2.0/users/104472/RAX-KSGRP");
        ClientResponse clientResponse = resource.header("X-Auth-Token",token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }


    @Test
    public void listEndpointTemplates_returns200() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/OS-KSCATALOG/endpointTemplates");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointTemplates_withMissingCredentials_returns401() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/OS-KSCATALOG/endpointTemplates");
        ClientResponse clientResponse = resource.get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Test
    public void updateUser_withNewUsernameEqualToOldUsername_returns200() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        User user = getUserByName(token, "kurUserAdmin");
        WebResource resource = resource().path("cloud/v2.0/users/" + user.getId());
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, "{\n" +
                "  \"user\": {\n" +
                "    \"id\":\"" + user.getId() + "\",\n" +
                "    \"username\":\"kurUserAdmin\",\n" +
                "    \"email\": \"testuser@example.org\"\n" +
                "  }\n" +
                "}");
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void updateUser_withNewUsername_withUsernameAlreadyInUse_returns409() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        User user = getUserByName(token,"kurUserAdmin");
        WebResource resource = resource().path("cloud/v2.0/users/" + user.getId());
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, "{\n" +
                "  \"user\": {\n" +
                "    \"id\":\"" + user.getId() + "\",\n" +
                "    \"username\": \"hectorServiceAdmin\",\n" +
                "    \"email\": \"testuser@example.org\"\n" +
                "  }\n" +
                "}");
        assertThat("response code", clientResponse.getStatus(), equalTo(409));
    }

    @Test
    public void getUsersByDomainId_invalidDomainId_returns404() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("RAX-AUTH/domains/" + testDomain + "/users");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void addTenantToDomain_withInvalidTenantId_returns404() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/domains/" + testDomain + "/tenants/" + invalidTenant);
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void getTenantDomain_withInvalidDomain_returns404() throws Exception {
        String token = authenticate("hectorServiceAdmin", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/domains/" + invalidDomain + "/tenants");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void createEndpoint_validEndpoint_returnsEndpoint() throws JAXBException {
        String token = authenticate("testServiceAdmin_doNotDelete", "Password1", MediaType.APPLICATION_XML);

        String endpointTemplateId = "105009002";

        try {
            EndpointTemplate endpointTemplate = getEndpointTemplate(token, endpointTemplateId);
            deleteEndpointTemplate(token, endpointTemplateId);
        } catch (Exception e) {
        }

        EndpointTemplate endpointTemplate = createEndpointTemplate(token, endpointTemplateId);

        assertThat("endpointTemplate", endpointTemplate, notNullValue());
    }

    private EndpointTemplate createEndpointTemplate(String token, String endpointTemplateId) throws JAXBException {
        String request = cloud20TestHelper.getEndpointTemplateString(endpointTemplateId);
        String response = getWebResourceBuilder("cloud/v2.0/OS-KSCATALOG/endpointTemplates", MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, token).post(String.class, request);

        return cloud20TestHelper.getEndpointTemplateObject(response);
    }

    private EndpointTemplate getEndpointTemplate(String token, String endpointTemplateId) throws JAXBException {
        String response = getWebResourceBuilder("cloud/v2.0/OS-KSCATALOG/endpointTemplates/" + endpointTemplateId, MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, token).get(String.class);
        return cloud20TestHelper.getEndpointTemplateObject(response);
    }

    private void deleteEndpointTemplate(String token, String endpointTemplateId) throws JAXBException {
        getWebResourceBuilder("cloud/v2.0/OS-KSCATALOG/endpointTemplates/" + endpointTemplateId, MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, token).delete();
    }

    private String authenticate(String user, String pwd, String mediaType) throws JAXBException {
        String request = cloud20TestHelper.getAuthenticationRequest(user, pwd);

        String response = getWebResourceBuilder("cloud/v2.0/tokens", mediaType).post(String.class, request);

        AuthenticateResponse authenticateResponse = cloud20TestHelper.getAuthenticateResponse(response);
        return authenticateResponse.getToken().getId();
    }



    private WebResource.Builder getWebResourceBuilder(String path, String mediaType) {
        WebResource.Builder builder = null;

        if (mediaType == MediaType.APPLICATION_XML) {
            builder = resource().path(path).type(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);
        } else {
            builder = resource().path(path).type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
        }

        return builder;
    }

    @Test
    public void getTenantDomain_validDomain_returns200() throws Exception {
        String token = authenticate(testServiceAdminUser.getUsername(), "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/domains/" + testDomain + "/tenants");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    private User createServiceAdminUser(String token, String name, String email, String password){
        WebResource resource = resource().path("cloud/v2.0/users");
        ClientResponse clientResponse = resource
                .header("X-Auth-Token",token)
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class,
                        "<user username=\"" + name + "\" email=\"" + email + "\" enabled=\"true\" ns1:password=\"" + password + "\" xmlns:ns1=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:RAX-AUTH=\"http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0\"/>");
        if (clientResponse.getStatus() == 201) {
            Object response = clientResponse.getEntity(User.class);
            return (User)response;
        }
        return null;
    }

    private User getUserByName(String token, String name){
        WebResource resource = resource().path("cloud/v2.0/users").queryParam("name", name);
        ClientResponse clientResponse = resource
                .header("X-Auth-Token", token)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(ClientResponse.class);
        if (clientResponse.getStatus() == 200) {
            Object response = clientResponse.getEntity(User.class);
            return (User)response;
        }
        return null;
    }
}
