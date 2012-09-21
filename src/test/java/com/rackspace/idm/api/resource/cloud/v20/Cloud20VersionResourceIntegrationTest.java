package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy;
import com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.test.Cloud20TestHelper;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.v2.AuthenticateResponse;
import org.openstack.docs.identity.api.v2.User;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
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

    boolean setupComplete = false;
    static String X_AUTH_TOKEN = "X-Auth-Token";
    static String identityUserName = "testIdentityAdmin_doNotDelete";
    static String userAdminName = "testUserAdmin_doNotDelete";
    static String tenantId="kurtTestTenant";
    static String roleId="10010967";
    static String invalidTenant = "999999";
    static String testDomain = "135792468";
    static String invalidDomain = "999999";
    static String email = "testEmail@rackspace.com";
    static String password = "Password1";
    static String endpointTemplateId = "105009002";
    User testIdentityAdminUser;
    User testUserAdmin;

    @Before
    public void setUp() throws Exception {
        ensureGrizzlyStarted("classpath:app-config.xml");
        String token = authenticate("authQE", "Auth1234", MediaType.APPLICATION_XML);
        if (!setupComplete) {
            setupComplete = true;
            //Create Users if they do not exist.
            testIdentityAdminUser = getUserByName(token, identityUserName);
            if(testIdentityAdminUser == null){
                testIdentityAdminUser = createIdentityAdminUser(token, identityUserName, email, password, MediaType.APPLICATION_XML);
            }
            String identityToken = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
            testUserAdmin = getUserByName(identityToken, userAdminName);
            if(testUserAdmin == null){
                testUserAdmin = createUserAdminUser(identityToken, userAdminName, email, password, testDomain, MediaType.APPLICATION_XML);
                addRolesToUserOnTenant(token, tenantId, testUserAdmin.getId(), roleId);
            }

        }
    }

    @Test
    public void getVersion_withValidPath_returns200() throws Exception {
        String token = getAuthToken(identityUserName, "Password1");
        WebResource resource = resource().path("cloud/v2.0");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getDefaultRegionServices_returns200() throws Exception {
        String token = getAuthToken(identityUserName, "Password1");
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
                        "<auth xmlns=\"http://docs.openstack.org/identity/api/v2.0\"><passwordCredentials username=\""+identityUserName+"\" password=\"Password1\"/></auth>");
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
                                "            \"username\":\""+identityUserName+"\",\n" +
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
                        "{" + "\"auth\":{" + "\"passwordCredentials\":{" + "\"username\":\""+identityUserName+"\",\"password\":\"Password1\"},\"tenantId\":\"1234\" }");
        assertThat("response code", clientResponse.getStatus(), equalTo(400));
    }


    @Test
    public void authenticate_badJsonNoPw_returns400() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens");

        ClientResponse clientResponse = resource
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class,
                        "{" + "\"auth\":{" + "\"passwordCredentials\":{" + "\"username\":\""+identityUserName+"\"},\"tenantId\":\"1234\" }}");
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
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/"+token);
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void validateToken_withoutAuthToken_returns401() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/"+token);
        ClientResponse clientResponse = resource.get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Test
    public void validateToken_asDefaultUser_returns403() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/"+token);
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(403));
    }

    @Test
    public void validateToken_againstNonExistentToken_returns404() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/badTokenId");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void validateToken_belongsToIsFalse_returns404() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token).queryParam("belongsTo", "DoesntBelong");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void validateToken_belongsToIsTrue_returns200() throws Exception {
        String adminToken = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        String token = authenticate(userAdminName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token).queryParam("belongsTo", tenantId);
        ClientResponse clientResponse = resource.header("x-auth-token", adminToken).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void checkToken_returns200() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/"+token);
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void checkToken_withoutAuthToken_returns401() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/"+token);
        ClientResponse clientResponse = resource.get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Test
    public void checkToken_asDefaultUser_returns403() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/"+token);
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(403));
    }

    @Test
    public void checkToken_againstNonExistentToken_returns404() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/badTokenId");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void checkToken_belongsToIsFalse_returns404() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token).queryParam("belongsTo", "DoesntBelong");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void checkToken_belongsToIsTrue_returns200() throws Exception {
        String adminToken = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        String token = authenticate(userAdminName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token).queryParam("belongsTo", "kurtTestTenant");
        ClientResponse clientResponse = resource.header("x-auth-token", adminToken).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointsForToken_returns200() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token + "/endpoints");
        ClientResponse clientResponse = resource.header("x-auth-token", token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointsForToken_withInvalidToken_returns404() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
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
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
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
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/impersonation-tokens");
        ClientResponse clientResponse = resource.header("x-auth-token", token).type(MediaType.APPLICATION_XML_TYPE).post(ClientResponse.class , "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<impersonation\n" +
                "</impersonation>");

        assertThat("response code", clientResponse.getStatus(), equalTo(400));
    }

    @Test
    public void impersonate_withDefaultUserAuth_returns403() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/impersonation-tokens");
        ClientResponse clientResponse = resource.header("x-auth-token", token).type(MediaType.APPLICATION_XML_TYPE).post(ClientResponse.class , "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
        String token = getAuthToken("kurtUserAdmin", "Password1");
        WebResource resource = resource().path("cloud/v2.0/users").queryParam("name", "kurtUserAdmin");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getUser_withUserAdmin_searchingServiceAdmin_returns403() throws Exception {
        String token = getAuthToken("kurtUserAdmin", "Password1");
        WebResource resource = resource().path("cloud/v2.0/users").queryParam("name", identityUserName);
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(403));
    }

    @Test
    public void getUser_withServiceAdmin_searchingServiceAdmin_returns200() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/users").queryParam("name", identityUserName);
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRoles() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/users/10043198/roles");
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
        String token = getAuthToken("kurtUserAdmin", "Password1");
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
        String adminToken = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/users/"+testUserAdmin.getId()+"/RAX-KSGRP");
        ClientResponse clientResponse = resource.header("X-Auth-Token",adminToken).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
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
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
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
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/users/10043198");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, "{\n" +
                "  \"user\": {\n" +
                "    \"id\":\"10043198\",\n" +
                "    \"username\":\"kurtUserAdmin\",\n" +
                "    \"email\": \"testuser@example.org\"\n" +
                "  }\n" +
                "}");
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void updateUser_withNewUsername_withUsernameAlreadyInUse_returns409() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/users/"+testUserAdmin.getId());
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, "{\n" +
                "  \"user\": {\n" +
                "    \"id\":\""+testUserAdmin.getId()+"\",\n" +
                "    \"username\": \""+identityUserName+"\",\n" +
                "    \"email\": \"testuser@example.org\"\n" +
                "  }\n" +
                "}");
        assertThat("response code", clientResponse.getStatus(), equalTo(409));
    }

    @Test
    public void getUsersByDomainId_invalidDomainId_returns404() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("RAX-AUTH/domains/" + testDomain + "/users");
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void addTenantToDomain_withInvalidTenantId_returns404() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/domains/" + testDomain + "/tenants/" + invalidTenant);
        ClientResponse clientResponse = resource.header("X-Auth-Token", token).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    private String getAuthToken(String username, String password) {
        WebResource resource = resource().path("cloud/v2.0/tokens");
        ClientResponse clientResponse = resource
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .post(ClientResponse.class,
                        "<auth xmlns=\"http://docs.openstack.org/identity/api/v2.0\"><passwordCredentials username=\""+username+"\" password=\""+password+"\"/></auth>");
        Object response =  clientResponse.getEntity(AuthenticateResponse.class);
        if(((JAXBElement) response).getDeclaredType() == AuthenticateResponse.class){
            JAXBElement<AuthenticateResponse> res = (JAXBElement<AuthenticateResponse>)response;
            return res.getValue().getToken().getId();
        }
        else{
            return "BadToken";
        }
    }

    @Test
    public void PROPER_delegateAuthenticate_returnGlobalResponseAfterSync() throws Exception {
        WebResource resource = resource().path("/cloud/v2.0/tokens");
        String authRequest = cloud20TestHelper.getAuthenticationRequest(identityUserName, password);

        ClientResponse clientResponse = resource.post(ClientResponse.class, authRequest);

        MultivaluedMap<String, String> headers = clientResponse.getHeaders();

        String source = headers.getFirst("response-source");

        assertThat(source, equalTo(null));
    }

    @Test
    public void policyCrud_validPolicy_returnsCorrectValues() throws JAXBException {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);

        Policy policy = createPolicy(token,"name","blob","type");

        assertThat("create policy", policy, notNullValue());

        policy = getPolicy(token, policy.getId());

        assertThat("get policy", policy, notNullValue());

        deletePolicy(token, policy.getId());

        policy = null;

        try {
            policy = getPolicy(token, policy.getId());
        } catch (Exception e) {
        }

        assertThat("delete policy", policy, equalTo(null));
    }

    @Test
    public void addPolicyToEndpoint_withoutEndpointWithoutPolicy_returns404() throws JAXBException {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);

        String policyId = "101010101011";
        String endpointTemplateId = "110101101101011";

        ClientResponse clientResponse = addPolicyToEndpointTemplate(token, endpointTemplateId, policyId);

        assertThat("delete policy", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void addPolicyToEndpoint_withEndpointWithoutPolicy_returns404() throws JAXBException {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);

        String policyId = "101010101011";

        EndpointTemplate endpointTemplate = createEndpointTemplate(token, endpointTemplateId);

        ClientResponse clientResponse = addPolicyToEndpointTemplate(token, endpointTemplateId, policyId);

        deleteEndpointTemplate(token, endpointTemplateId);

        assertThat("add policy", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void addPolicyToEndpoint_withEndpointWithPolicy_returns204() throws JAXBException {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);

        EndpointTemplate endpointTemplate = createEndpointTemplate(token, endpointTemplateId);
        Policy policy = createPolicy(token, "name", "blob", "type");
        String policyId = policy.getId();

        ClientResponse clientResponse = addPolicyToEndpointTemplate(token, endpointTemplateId, policyId);
        assertThat("add policy to endpointTemplate", clientResponse.getStatus(), equalTo(204));

        Policies policies = getPoliciesFromEndpointTemplate(token, endpointTemplateId);
        assertThat("get policies from endpointTemplate", policies.getPolicy().size(), equalTo(1));

        clientResponse = deletePolicyToEndpointTemplate(token, endpointTemplateId, policyId);
        assertThat("remove policy to endpointTemplate", clientResponse.getStatus(), equalTo(204));

        deleteEndpointTemplate(token, endpointTemplateId);
        deletePolicy(token, policyId);
    }

    @Test
    public void updatePolicyToEndpoint_withoutEndpointWithoutPolicy_returns404() throws JAXBException {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);

        String policyId = "101010101011";
        String endpointTemplateId = "110101101101011";

        Policies policies = new Policies();
        Policy policy = new Policy();
        policy.setId(policyId);
        policies.getPolicy().add(policy);

        String request = cloud20TestHelper.getPolicies(policies);

        ClientResponse clientResponse = updatePolicyToEndpointTemplate(token, endpointTemplateId, request);

        assertThat("update policy", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void updatePolicyToEndpoint_withEndpointWithoutPolicy_returns404() throws JAXBException {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);

        EndpointTemplate endpointTemplate = createEndpointTemplate(token, endpointTemplateId);
        String policyId = "101010101011";

        Policies policies = new Policies();
        Policy policy = new Policy();
        policy.setId(policyId);
        policies.getPolicy().add(policy);

        String request = cloud20TestHelper.getPolicies(policies);

        ClientResponse clientResponse = updatePolicyToEndpointTemplate(token, endpointTemplateId, request);

        deleteEndpointTemplate(token, endpointTemplateId);

        assertThat("update policy", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void updatePolicyToEndpoint_withEndpointWithPolicy_returns204() throws JAXBException {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);

        EndpointTemplate endpointTemplate = createEndpointTemplate(token, endpointTemplateId);
        Policy policy = createPolicy(token, "name", "blob", "type");
        String policyId = policy.getId();
        Policies policies = new Policies();
        policies.getPolicy().add(policy);

        String request = cloud20TestHelper.getPolicies(policies);

        ClientResponse clientResponse = updatePolicyToEndpointTemplate(token, endpointTemplateId, request);
        assertThat("update policy for endpointTemplate", clientResponse.getStatus(), equalTo(204));

        clientResponse = deletePolicyToEndpointTemplate(token, endpointTemplateId, policyId);
        assertThat("remove policy to endpointTemplate", clientResponse.getStatus(), equalTo(204));

        deleteEndpointTemplate(token, endpointTemplateId);
        deletePolicy(token, policyId);
    }

    private ClientResponse updatePolicyToEndpointTemplate(String token, String endpointTemplateId, String policies) {
        return getWebResourceBuilder(
                "cloud/v2.0/OS-KSCATALOG/endpointTemplates/" + endpointTemplateId + "/RAX-AUTH/policies",
                MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, token).put(ClientResponse.class, policies);

    }

    private ClientResponse addPolicyToEndpointTemplate(String token, String endpointTemplateId, String policyId) {
        return getWebResourceBuilder(
                "cloud/v2.0/OS-KSCATALOG/endpointTemplates/" + endpointTemplateId + "/RAX-AUTH/policies/" + policyId,
                MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, token).put(ClientResponse.class);
    }

    private Policies getPoliciesFromEndpointTemplate(String token, String endpointTemplateId) throws JAXBException {
        String response = getWebResourceBuilder(
                "cloud/v2.0/OS-KSCATALOG/endpointTemplates/" + endpointTemplateId + "/RAX-AUTH/policies",
                MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, token).get(String.class);

        return cloud20TestHelper.getPolicies(response);
    }

    private ClientResponse deletePolicyToEndpointTemplate(String token, String endpointTemplateId, String policyId) {
        return getWebResourceBuilder(
                "cloud/v2.0/OS-KSCATALOG/endpointTemplates/" + endpointTemplateId + "/RAX-AUTH/policies/" + policyId,
                MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, token).delete(ClientResponse.class);
    }

    private void deleteEndpointTemplate(String token, String endpointTemplateId) throws JAXBException {
        getWebResourceBuilder("cloud/v2.0/OS-KSCATALOG/endpointTemplates/" + endpointTemplateId, MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, token).delete();
    }

    @Test
    public void createPolicy_validPolicy_invalidToken_returns401() throws JAXBException {
        try {
            createPolicy("badToken", "name", "blob", "type");
        } catch (Exception ex) {
            assertThat("Status Code", ((UniformInterfaceException) ex).getResponse().getStatus(),equalTo(401));
        }
    }

    @Test
    public void createPolicy_invalidPolicy_returns400() throws JAXBException {
        try {
            String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
            createPolicy(token, "name", null, "type");
        } catch (Exception ex) {
            assertThat("Status Code", ((UniformInterfaceException) ex).getResponse().getStatus(), equalTo(400));
        }
    }

    @Test
    public void getPolicy_validValue_returns400() throws Exception {
        String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
        Policy policy = createPolicy(token, "someName", "someBlob", "someType");
        Policy policy2 = getPolicy(token, policy.getId());
        assertThat("Policy Value", policy2.getBlob(),equalTo("someBlob"));
        assertThat("Policy Value", policy2.getName(),equalTo("someName"));
        assertThat("Policy Value", policy2.getType(),equalTo("someType"));
    }

    @Test
    public void getPolicy_invalidValue_returns404() throws Exception {
        try {
            String token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
            getPolicy(token, "00000");
        } catch (Exception ex) {
            assertThat("Status Code", ((UniformInterfaceException) ex).getResponse().getStatus(), equalTo(404));
        }
    }

    @Test
    public void deletePolicy_policyBelongsToEndpoint_return400() throws Exception {
        String token = null;
        EndpointTemplate endpointTemplate = null;
        String policyId = null;
        try {
            token = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
            endpointTemplate = createEndpointTemplate(token, endpointTemplateId);
            Policy policy = createPolicy(token, "name", "blob", "type");
            policyId = policy.getId();
            addPolicyToEndpointTemplate(token, String.valueOf(endpointTemplate.getId()), policyId);
            deletePolicy(token,policyId);

        } catch (Exception ex) {
            assertThat("Status Code", ((UniformInterfaceException) ex).getResponse().getStatus(), equalTo(400));
            deleteEndpointTemplate(token, String.valueOf(endpointTemplate.getId()));
            deletePolicy(token,policyId);
        }
    }

    private Policy createPolicy(String token, String name, String blob, String type) throws JAXBException {
        String request = cloud20TestHelper.getPolicyString(name, blob, type);
        String response =  getWebResourceBuilder("cloud/v2.0/RAX-AUTH/policies", MediaType.APPLICATION_XML)
                    .header(X_AUTH_TOKEN, token).post(String.class, request);
        return cloud20TestHelper.getPolicyObject(response);
    }

    private Policy getPolicy(String token, String policyId) throws JAXBException {
        String response = getWebResourceBuilder("cloud/v2.0/RAX-AUTH/policies/" + policyId, MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, token).get(String.class);

        return cloud20TestHelper.getPolicyObject(response);
    }

    private void deletePolicy(String token, String policyId) {
        getWebResourceBuilder("cloud/v2.0/RAX-AUTH/policies/" + policyId, MediaType.APPLICATION_XML)
                .header(X_AUTH_TOKEN, token).delete();
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

    private User createIdentityAdminUser(String token, String name, String email, String password, String mediaType) throws JAXBException {
        String request = cloud20TestHelper.createIdentityAdmin(name, password, email);
        String response = getWebResourceBuilder("cloud/v2.0/users", mediaType)
                .header(X_AUTH_TOKEN, token)
                .post(String.class, request);
        return cloud20TestHelper.getUser(response);
    }

    private User createUserAdminUser(String token, String name, String email, String password, String domainId, String mediaType) throws JAXBException {
        String request = cloud20TestHelper.createUserAdmin(name, password, email, domainId);
        String response = getWebResourceBuilder("cloud/v2.0/users", mediaType)
                .header(X_AUTH_TOKEN, token)
                .post(String.class, request);
        return cloud20TestHelper.getUser(response);
    }

    private void addRolesToUserOnTenant(String token, String tenantId, String id, String roleId) {
        getWebResourceBuilder("cloud/v2.0/tenants/"+tenantId+"/users/"+id+"/roles/OS-KSADM/"+roleId, null)
                .header(X_AUTH_TOKEN, token)
                .put(String.class);
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
