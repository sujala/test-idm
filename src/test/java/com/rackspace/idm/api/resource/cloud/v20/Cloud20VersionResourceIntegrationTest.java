package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain;
import com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.test.Cloud20TestHelper;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
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

    static boolean setupComplete = false;
    static String X_AUTH_TOKEN = "X-Auth-Token";
    static String identityUserName = "testIdentityAdmin_doNotDelete";
    static String userAdminName = "testUserAdmin_doNotDelete";
    static String noRolesName = "testUserNoRoles_doNotDelete";
    static String tenantId="kurtTestTenant";
    static String roleId="10010967";
    static String invalidTenant = "999999";
    static String testDomainId = "135792468";
    static String invalidDomainId = "999999";
    static String disabledDomainId = "888888";
    static String email = "testEmail@rackspace.com";
    static String password = "Password1";
    static String endpointTemplateId = "105009002";

    static User testIdentityAdminUser;
    static User testUserAdmin;
    static User testUserNoRoles;
    static Domain disabledDomain;
    static Domain testDomain;
    static String identityToken;
    static String uberToken;

    @Before
    public void setUp() throws Exception {
        ensureGrizzlyStarted("classpath:app-config.xml");
        if (!setupComplete) {
            setupComplete = true;
            uberToken = authenticate("authQE", "Auth1234", MediaType.APPLICATION_XML);

            disabledDomain = getDomainById(uberToken, disabledDomainId);
            if (disabledDomain == null) {
                disabledDomain = createDomain(uberToken, "DEV-999-999-998", disabledDomainId, false, MediaType.APPLICATION_XML);
            }

            testDomain = getDomainById(uberToken, testDomainId);
            if (testDomain == null) {
                testDomain = createDomain(uberToken, "DEV-999-999-999", testDomainId, true, MediaType.APPLICATION_XML);
            }

            //Create Users if they do not exist.
            testIdentityAdminUser = getUserByName(uberToken, identityUserName);
            if(testIdentityAdminUser == null){
                testIdentityAdminUser = createIdentityAdminUser(uberToken, identityUserName, email, password, MediaType.APPLICATION_XML);
            }

            identityToken = authenticate(identityUserName, "Password1", MediaType.APPLICATION_XML);
            testUserAdmin = getUserByName(identityToken, userAdminName);
            if(testUserAdmin == null){
                testUserAdmin = createUserAdminUser(identityToken, userAdminName, email, password, testDomainId, MediaType.APPLICATION_XML);
                addRolesToUserOnTenant(uberToken, tenantId, testUserAdmin.getId(), roleId);
            }

            testUserNoRoles = getUserByName(uberToken, noRolesName);
            if (testUserNoRoles == null) {
                testUserNoRoles = createUserAdminUser(identityToken, noRolesName, email, password, testDomainId, MediaType.APPLICATION_XML);
            }
        }
    }

    @Test
    public void getVersion_withValidPath_returns200() throws Exception {
        WebResource resource = resource().path("cloud/v2.0");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getDefaultRegionServices_returns200() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/default-region/services");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).get(ClientResponse.class);
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
                        "<auth xmlns=\"http://docs.openstack.org/identity/api/v2.0\"><passwordCredentials username=\"" + identityUserName + "\" password=\"Password1\"/></auth>");
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
                                "            \"username\":\"" + identityUserName + "\",\n" +
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
                        "{" + "\"auth\":{" + "\"passwordCredentials\":{" + "\"username\":\"" + identityUserName + "\",\"password\":\"Password1\"},\"tenantId\":\"1234\" }");
        assertThat("response code", clientResponse.getStatus(), equalTo(400));
    }


    @Test
    public void authenticate_badJsonNoPw_returns400() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens");

        ClientResponse clientResponse = resource
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(ClientResponse.class,
                        "{" + "\"auth\":{" + "\"passwordCredentials\":{" + "\"username\":\"" + identityUserName + "\"},\"tenantId\":\"1234\" }}");
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
        WebResource resource = resource().path("cloud/v2.0/tokens/" + identityToken);
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void validateToken_withoutAuthToken_returns401() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens/" + identityToken);
        ClientResponse clientResponse = resource.get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Test
    public void validateToken_asDefaultUser_returns403() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/"+token);
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(403));
    }

    @Test
    public void validateToken_againstNonExistentToken_returns404() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens/badTokenId");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void validateToken_belongsToIsFalse_returns404() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens/" + identityToken).queryParam("belongsTo", "DoesntBelong");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void validateToken_belongsToIsTrue_returns200() throws Exception {
        String token = authenticate(userAdminName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token).queryParam("belongsTo", "kurtTestTenant");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void checkToken_returns200() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens/"+ identityToken);
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void checkToken_withoutAuthToken_returns401() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens/"+ identityToken);
        ClientResponse clientResponse = resource.get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Test
    public void checkToken_asDefaultUser_returns403() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token);
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, token).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(403));
    }

    @Test
    public void checkToken_againstNonExistentToken_returns404() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens/badTokenId");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void checkToken_belongsToIsFalse_returns404() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens/" + identityToken).queryParam("belongsTo", "DoesntBelong");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void checkToken_belongsToIsTrue_returns200() throws Exception {
        String token = authenticate(userAdminName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tokens/" + token).queryParam("belongsTo", "kurtTestTenant");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointsForToken_returns200() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens/" + identityToken + "/endpoints");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointsForToken_withInvalidToken_returns404() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tokens/" + "someBadToken" + "/endpoints");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).get(ClientResponse.class);

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
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/impersonation-tokens");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).type(MediaType.APPLICATION_XML_TYPE).post(ClientResponse.class , "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/impersonation-tokens");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).type(MediaType.APPLICATION_XML_TYPE).post(ClientResponse.class , "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<impersonation\n" +
                "</impersonation>");

        assertThat("response code", clientResponse.getStatus(), equalTo(400));
    }

    @Test
    public void impersonate_withDefaultUserAuth_returns403() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/impersonation-tokens");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, token).type(MediaType.APPLICATION_XML_TYPE).post(ClientResponse.class , "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
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
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void addUser_identityAdminWithDomain_returns400() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/users");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, uberToken).type(MediaType.APPLICATION_XML_TYPE).post(ClientResponse.class,
                "<user username=\"serviceAdminD1\"\n" +
                        "    email=\"domainxml@example.com\"\n" +
                        "    enabled=\"true\"\n" +
                        "    ns1:password=\"Password1\"\n" +
                        "    rax-auth:domainId=\"135792468\"\n" +
                        "    xmlns:rax-auth=\"http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0\"\n" +
                        "    xmlns:ns1=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\"\n" +
                        "    xmlns:ns2=\"http://docs.openstack.org/identity/api/v2.0\" />");

        assertThat("response code", clientResponse.getStatus(), equalTo(400));
    }

    @Test
    public void addUser_userAdminWithoutDomain_returns400() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/users");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).type(MediaType.APPLICATION_XML_TYPE).post(ClientResponse.class,
                "<user username=\"userAdminNoDomain\"\n" +
                        "    email=\"domainxml@example.com\"\n" +
                        "    enabled=\"true\"\n" +
                        "    ns1:password=\"Password1\"\n" +
                        "    xmlns:rax-auth=\"http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0\"\n" +
                        "    xmlns:ns1=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\"\n" +
                        "    xmlns:ns2=\"http://docs.openstack.org/identity/api/v2.0\" />");

        assertThat("response code", clientResponse.getStatus(), equalTo(400));
    }

    @Test
    public void getUser_withDefaultUser_byName_returns200() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/users").queryParam("name", "kurtDefaultUser");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getUser_withDefaultUser_searchingUserAdmin_returns403() throws Exception {
        String token = authenticate("kurtDefaultUser", "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/users").queryParam("name", userAdminName);
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(403));
    }

    @Test
    public void getUser_withUserAdmin_searchingUserAdmin_returns200() throws Exception {
        String token = getAuthToken(userAdminName, "Password1");
        WebResource resource = resource().path("cloud/v2.0/users").queryParam("name", userAdminName);
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getUser_withUserAdmin_searchingServiceAdmin_returns403() throws Exception {
        String token = getAuthToken(userAdminName, "Password1");
        WebResource resource = resource().path("cloud/v2.0/users").queryParam("name", identityUserName);
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(403));
    }

    @Test
    public void getUser_withServiceAdmin_searchingServiceAdmin_returns200() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/users").queryParam("name", identityUserName);
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRoles() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/users/10043198/roles");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getTenants_withDefaultUser_returns200() throws Exception {
        String token = authenticate(userAdminName, "Password1", MediaType.APPLICATION_XML);
        WebResource resource = resource().path("cloud/v2.0/tenants");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }


    @Test
    public void getTenants_UserAdmin_returns200() throws Exception {
        String token = getAuthToken(userAdminName, "Password1");
        WebResource resource = resource().path("cloud/v2.0/tenants");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void getTenants_badToken_returns401() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/tenants");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, "bad").accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }

    @Test
    public void listUserGroups_returns200() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/users/" + testIdentityAdminUser.getId() + "/RAX-KSGRP");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void listUserGroups_invalidAuthToken_returns401() throws Exception {
        String token = "invalid";
        WebResource resource = resource().path("cloud/v2.0/users/104472/RAX-KSGRP");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, token).accept(MediaType.APPLICATION_XML_TYPE).get(ClientResponse.class);
        assertThat("response code", clientResponse.getStatus(), equalTo(401));
    }


    @Test
    public void listEndpointTemplates_returns200() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/OS-KSCATALOG/endpointTemplates");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).get(ClientResponse.class);
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
        WebResource resource = resource().path("cloud/v2.0/users/" + testUserAdmin.getId());
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, "{\n" +
                "  \"user\": {\n" +
                "    \"id\":\"" + testUserAdmin.getId() + "\",\n" +
                "    \"username\":\"" + userAdminName + "\",\n" +
                "    \"email\": \"testuser@example.org\"\n" +
                "  }\n" +
                "}");
        assertThat("response code", clientResponse.getStatus(), equalTo(200));
    }

    @Test
    public void updateUser_withNewUsername_withUsernameAlreadyInUse_returns409() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/users/" + testUserAdmin.getId());
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, "{\n" +
                "  \"user\": {\n" +
                "    \"id\":\"" + testUserAdmin.getId() + "\",\n" +
                "    \"username\": \"" + identityUserName + "\",\n" +
                "    \"email\": \"testuser@example.org\"\n" +
                "  }\n" +
                "}");
        assertThat("response code", clientResponse.getStatus(), equalTo(409));
    }

    @Test
    public void getUsersByDomainId_invalidDomainId_returns404() throws Exception {
        WebResource resource = resource().path("RAX-AUTH/domains/" + testDomainId + "/users");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void addTenantToDomain_withInvalidTenantId_returns404() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/domains/" + testDomainId + "/tenants/" + invalidTenant);
        ClientResponse clientResponse = resource.header("X-Auth-Token", identityToken).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void addUserToDomain_disabledDomain_returns403() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/domains/" + disabledDomainId + "/users/123");
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(403));
    }

    @Ignore
    @Test
    public void addUserToDomain_noRole_returns403() throws Exception {
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/domains/" + testDomainId + "/users/" + testUserNoRoles.getId());
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, identityToken).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class);

        assertThat("response code", clientResponse.getStatus(), equalTo(403));
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
        Policy policy = createPolicy(identityToken, "name", "blob", "type");

        assertThat("create policy", policy, notNullValue());

        policy = getPolicy(identityToken, policy.getId());

        assertThat("get policy", policy, notNullValue());

        deletePolicy(identityToken, policy.getId());

        policy = null;

        try {
            policy = getPolicy(identityToken, policy.getId());
        } catch (Exception e) {
        }

        assertThat("delete policy", policy, equalTo(null));
    }

    @Test
    public void addPolicyToEndpoint_withoutEndpointWithoutPolicy_returns404() throws JAXBException {
        String policyId = "101010101011";
        String endpointTemplateId = "110101101101011";

        ClientResponse clientResponse = addPolicyToEndpointTemplate(identityToken, endpointTemplateId, policyId);

        assertThat("delete policy", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void addPolicyToEndpoint_withEndpointWithoutPolicy_returns404() throws JAXBException {
        String policyId = "101010101011";

        EndpointTemplate endpointTemplate = createEndpointTemplate(identityToken, endpointTemplateId);

        ClientResponse clientResponse = addPolicyToEndpointTemplate(identityToken, endpointTemplateId, policyId);

        deleteEndpointTemplate(identityToken, endpointTemplateId);

        assertThat("add policy", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void addPolicyToEndpoint_withEndpointWithPolicy_returns204() throws JAXBException {

        EndpointTemplate endpointTemplate = createEndpointTemplate(identityToken, endpointTemplateId);
        Policy policy = createPolicy(identityToken, "name", "blob", "type");
        String policyId = policy.getId();

        ClientResponse clientResponse = addPolicyToEndpointTemplate(identityToken, endpointTemplateId, policyId);
        assertThat("add policy to endpointTemplate", clientResponse.getStatus(), equalTo(204));

        Policies policies = getPoliciesFromEndpointTemplate(identityToken, endpointTemplateId);
        assertThat("get policies from endpointTemplate", policies.getPolicy().size(), equalTo(1));

        clientResponse = deletePolicyToEndpointTemplate(identityToken, endpointTemplateId, policyId);
        assertThat("remove policy to endpointTemplate", clientResponse.getStatus(), equalTo(204));

        deleteEndpointTemplate(identityToken, endpointTemplateId);
        deletePolicy(identityToken, policyId);
    }

    @Test
    public void updatePolicyToEndpoint_withoutEndpointWithoutPolicy_returns404() throws JAXBException {
        String policyId = "101010101011";
        String endpointTemplateId = "110101101101011";

        Policies policies = new Policies();
        Policy policy = new Policy();
        policy.setId(policyId);
        policies.getPolicy().add(policy);

        String request = cloud20TestHelper.getPolicies(policies);

        ClientResponse clientResponse = updatePolicyToEndpointTemplate(identityToken, endpointTemplateId, request);

        assertThat("update policy", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void updatePolicyToEndpoint_withEndpointWithoutPolicy_returns404() throws JAXBException {
        EndpointTemplate endpointTemplate = createEndpointTemplate(identityToken, endpointTemplateId);
        String policyId = "101010101011";

        Policies policies = new Policies();
        Policy policy = new Policy();
        policy.setId(policyId);
        policies.getPolicy().add(policy);

        String request = cloud20TestHelper.getPolicies(policies);

        ClientResponse clientResponse = updatePolicyToEndpointTemplate(identityToken, endpointTemplateId, request);

        deleteEndpointTemplate(identityToken, endpointTemplateId);

        assertThat("update policy", clientResponse.getStatus(), equalTo(404));
    }

    @Test
    public void updatePolicyToEndpoint_withEndpointWithPolicy_returns204() throws JAXBException {
        EndpointTemplate endpointTemplate = createEndpointTemplate(identityToken, endpointTemplateId);
        Policy policy = createPolicy(identityToken, "name", "blob", "type");
        String policyId = policy.getId();
        Policies policies = new Policies();
        policies.getPolicy().add(policy);

        String request = cloud20TestHelper.getPolicies(policies);

        ClientResponse clientResponse = updatePolicyToEndpointTemplate(identityToken, endpointTemplateId, request);
        assertThat("update policy for endpointTemplate", clientResponse.getStatus(), equalTo(204));

        clientResponse = deletePolicyToEndpointTemplate(identityToken, endpointTemplateId, policyId);
        assertThat("remove policy to endpointTemplate", clientResponse.getStatus(), equalTo(204));

        deleteEndpointTemplate(identityToken, endpointTemplateId);
        deletePolicy(identityToken, policyId);
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
            createPolicy(identityToken, "name", null, "type");
        } catch (Exception ex) {
            assertThat("Status Code", ((UniformInterfaceException) ex).getResponse().getStatus(), equalTo(400));
        }
    }

    @Test
    public void getPolicy_validValue_returns400() throws Exception {
        Policy policy = createPolicy(identityToken, "someName", "someBlob", "someType");
        Policy policy2 = getPolicy(identityToken, policy.getId());
        assertThat("Policy Value", policy2.getBlob(),equalTo("someBlob"));
        assertThat("Policy Value", policy2.getName(),equalTo("someName"));
        assertThat("Policy Value", policy2.getType(),equalTo("someType"));
    }

    @Test
    public void getPolicy_invalidValue_returns404() throws Exception {
        try {
            getPolicy(identityToken, "00000");
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
            addPolicyToEndpointTemplate(token, endpointTemplateId, policyId);
            deletePolicy(token,policyId);

        } catch (Exception ex) {
            assertThat("Status Code", ((UniformInterfaceException) ex).getResponse().getStatus(), equalTo(400));
            deleteEndpointTemplate(token, endpointTemplateId);
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
                .header(X_AUTH_TOKEN, token)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(ClientResponse.class);
        if (clientResponse.getStatus() == 200) {
            Object response = clientResponse.getEntity(User.class);
            return (User)response;
        }
        return null;
    }

    private Domain getDomainById(String token, String disabledDomainId) {
        WebResource resource = resource().path("cloud/v2.0/RAX-AUTH/domains/" + disabledDomainId);
        ClientResponse clientResponse = resource.header(X_AUTH_TOKEN, token).accept(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        if (clientResponse.getStatus() == 200) {
            Object response = clientResponse.getEntity(Domain.class);
            return (Domain)response;
        }

        return null;
    }

    private Domain createDomain(String token, String name, String disabledDomainId, boolean enabled, String mediaType) throws JAXBException {
        String request = cloud20TestHelper.createDomain(disabledDomainId, name, enabled);
        String response = getWebResourceBuilder("cloud/v2.0/RAX-AUTH/domains", mediaType)
                .header(X_AUTH_TOKEN, token)
                .post(String.class, request);

        return cloud20TestHelper.getDomain(response);
    }
}
