package testHelpers

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.v20.DelegationAgreementRoleSearchParams
import com.rackspace.idm.api.resource.cloud.v20.IdentityProviderSearchParams
import com.rackspace.idm.api.resource.cloud.v20.ListEffectiveRolesForUserParams
import com.rackspace.idm.api.resource.cloud.v20.ListUsersForTenantParams
import com.rackspace.idm.api.resource.cloud.v20.MultiFactorCloud20Service
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.domain.entity.PasswordPolicy
import com.rackspace.idm.modules.usergroups.api.resource.UserGroupRoleSearchParams
import com.rackspace.idm.modules.usergroups.api.resource.UserGroupSearchParams
import com.rackspace.idm.modules.usergroups.api.resource.UserSearchCriteria
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.core.util.MultivaluedMapImpl
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang.StringUtils
import org.opensaml.security.credential.Credential
import org.openstack.docs.identity.api.v2.*
import org.springframework.stereotype.Component
import spock.lang.Shared
import testHelpers.saml.SamlCredentialUtils

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap

import static com.rackspace.idm.Constants.DEFAULT_PASSWORD
import static com.rackspace.idm.JSONConstants.*
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static javax.ws.rs.core.MediaType.*
import static org.apache.http.HttpStatus.SC_CREATED

@Component
class Cloud20Methods {

    @Shared WebResource resource
    @Shared def v2Factory = new V2Factory()
    @Shared def v1Factory = new V1Factory()
    @Shared String path20 = "cloud/v2.0/"

    MediaTypeContext mediaType

    //Extensions
    static def RAX_GRPADM= "RAX-GRPADM"
    static def OS_KSADM = "OS-KSADM"
    static def RAX_AUTH = "RAX-AUTH"
    static def RAX_KSQA = "RAX-KSQA"
    static def OS_KSCATALOG = "OS-KSCATALOG"
    static def RAX_KSGRP = "RAX-KSGRP"

    //Constants
    static def X_AUTH_TOKEN = "X-Auth-Token"
    static def X_SESSION_ID = MultiFactorCloud20Service.X_SESSION_ID_HEADER_NAME
    static def GET_RULE_DETAIL_PARAM = "responseDetail"

    //path constants
    static def SERVICE_PATH_MOBILE_PHONES = "mobile-phones"
    static def SERVICE_PATH_MULTI_FACTOR = "multi-factor"
    static def SERVICE_PATH_VERIFY = "verify"
    static def SERVICE_PATH_RESET = "reset"
    static def SERVICE_PATH_VERIFICATION_CODE = "verificationcode"
    static def SERVICE_PATH_BYPASS_CODES = "bypass-codes"
    static def SERVICE_PATH_OTP_DEVICES = "otp-devices"
    static def SERVICE_PATH_MULTI_FACTOR_DEVICES = "devices"
    static def SERVICE_PATH_IDENTITY_PROVIDERS = "identity-providers"
    static def SERVICE_PATH_FORGOT_PASSWORD = "forgot-pwd"
    static def SERVICE_PATH_PASSWORD_RESET = "pwd-reset"
    static def SERVICE_PATH_RULES = "rules"
    static def SERVICE_PATH_MAPPING = "mapping"
    static def SERVICE_PATH_METADATA = "metadata"
    static def SERVICE_PATH_DOMAINS = "domains"
    static def SERVICE_PATH_PASSWORD_POLICY = "password-policy"
    static def SERVICE_PATH_CHANGE_PASSWORD = "change-pwd"
    static def SERVICE_PATH_USER_GROUPS ="groups"
    static def SERVICE_PATH_ROLES ="roles"
    static def SERVICE_PATH_USERS ="users"
    static def SERVICE_PATH_DA ="delegation-agreements"
    static def SERVICE_PATH_DELEGATES ="delegates"

    static def ENDPOINTS = "endpoints"
    static def ENDPOINT_TEMPLATES = "endpointTemplates"
    static def RULES = "rules"
    static def DOMAIN_ADMINISTRATOR_CHANGE_PATH = "domainAdministratorChange"
    static def RCN_PATH = "rcn"

    def init(){
        mediaType = new MediaTypeContext()
    }

    def initOnUse(){
        resource = ensureGrizzlyStarted("classpath:app-config.xml");
    }

    def authenticate(String username, String password, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        def credentials = v2Factory.createPasswordAuthenticationRequest(username, password)
        resource.path(path20).path(TOKENS).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(credentials).post(ClientResponse)
    }

    def authenticateMFAWithSessionIdAndPasscode(sessionId, passcode, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        def credentials = v2Factory.createPasscodeAuthenticationRequest(passcode)
        resource.path(path20).path(TOKENS).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).header(X_SESSION_ID, sessionId).entity(credentials).post(ClientResponse)
    }

    def authenticateMFAWithSessionIdAndPasscodeAndTenant(sessionId, passcode, tenantId, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        def credentials = v2Factory.createPasscodeAuthenticationRequest(passcode)
        credentials.tenantId = tenantId
        resource.path(path20).path(TOKENS).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).header(X_SESSION_ID, sessionId).entity(credentials).post(ClientResponse)
    }

    //TODO: remove once auth plugin is fixed
    def invalidAuthenticatePassword(username,password) {
        initOnUse()
        def body = String.format('{"auth":{"passwordCredentials":{"username":"%s","password":"%s", "tenantId":"blah", "tenantName": "blah"}}}', username, password)
        resource.path(path20).path(TOKENS).accept(APPLICATION_XML).type(APPLICATION_JSON).entity(body).post(ClientResponse)
    }

    //TODO: remove once auth plugin is fixed
    def invalidAuthenticateApiKey(username, key) {
        initOnUse()
        def body = String.format('{"auth":{"RAX-KSKEY:apiKeyCredentials":{"username":"%s","apiKey":"%s", "tenantId":"blah", "tenantName": "blah"}}}', username, key)
        resource.path(path20).path(TOKENS).accept(APPLICATION_XML).type(APPLICATION_JSON).entity(body).post(ClientResponse)
    }

    def authenticateRacker(username, password){
        initOnUse()
        def credentials = v2Factory.createPasswordAuthenticationRequest(username, password)
        credentials.domain =  new Domain().with {
            it.name = "Rackspace"
            it
        }
        resource.path(path20).path(TOKENS).accept(APPLICATION_XML).type(APPLICATION_XML).entity(credentials).post(ClientResponse)
    }

    def validateToken(authToken, token, MediaType accept = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(TOKENS).path(token).header(X_AUTH_TOKEN, authToken).accept(accept).get(ClientResponse)
    }

    def validateTokenApplyRcnRoles(authToken, token, String applyRcnRoles, MediaType accept = APPLICATION_XML_TYPE) {
        initOnUse()
        def queryParams = new MultivaluedMapImpl()
        if (applyRcnRoles != null) {
            queryParams.add("apply_rcn_roles", applyRcnRoles)
        }
        resource.path(path20).path(TOKENS).queryParams(queryParams).path(token).header(X_AUTH_TOKEN, authToken).accept(accept).get(ClientResponse)
    }

    def getUserByName(String token, String name, MediaType mediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).queryParam("name", name).accept(mediaType).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def createUser(String token, user, MediaType request = APPLICATION_XML_TYPE, MediaType accept = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).accept(accept).type(request).header(X_AUTH_TOKEN, token).entity(user).post(ClientResponse)
    }

    /**
     * Takes in an int domainId because cloud accounts MUST have an int domain on user creation to create the nast/mosso
     * tenants
     *
     * @param token
     * @param username
     * @param domainId
     * @return user admin created for new cloud account
     */
    def createCloudAccount(String token, String username=RandomStringUtils.randomAlphabetic(30), int domainId = Integer.parseInt(RandomStringUtils.randomNumeric(9))) {
        def user = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, String.valueOf(domainId), DEFAULT_PASSWORD)
        user.secretQA = v1Factory.createRaxKsQaSecretQA()
        def response = createUser(token, user)

        assert (response.status == SC_CREATED)
        def entity = response.getEntity(User).value
        assert (entity != null)
        return entity
    }

    /**
     * Creates a generic user admin account without any tenants. Uses standard identity admin account.
     * @param domainId
     * @return
     */
    def createGenericAccount(String token, String username=RandomStringUtils.randomAlphabetic(30), String domainId = RandomStringUtils.randomAlphanumeric(9)) {
        def user = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, String.valueOf(domainId), DEFAULT_PASSWORD)
        def response = createUser(token, user)

        assert (response.status == SC_CREATED)
        def entity = response.getEntity(User).value
        assert (entity != null)
        return entity
    }

    def createSubUser(String token, String username=RandomStringUtils.randomAlphabetic(30)) {
        def user = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, null, DEFAULT_PASSWORD)
        def response = createUser(token, user)

        assert (response.status == SC_CREATED)
        def entity = response.getEntity(User).value
        assert (entity != null)
        return entity
    }

    def updateDomainPasswordPolicy(String token, String domainId, PasswordPolicy policy) {
        updateDomainPasswordPolicy(token, domainId, policy.toJson())
    }

    def updateDomainPasswordPolicy(String token, String domainId, String jsonPolicy) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DOMAINS).path(domainId).path(SERVICE_PATH_PASSWORD_POLICY).type(APPLICATION_JSON_TYPE).header(X_AUTH_TOKEN, token).entity(jsonPolicy).put(ClientResponse)
    }

    def getDomainPasswordPolicy(String token, String domainId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DOMAINS).path(domainId).path(SERVICE_PATH_PASSWORD_POLICY).accept(APPLICATION_JSON_TYPE).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def deleteDomainPasswordPolicy(String token, String domainId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DOMAINS).path(domainId).path(SERVICE_PATH_PASSWORD_POLICY).accept(APPLICATION_JSON_TYPE).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def changeUserPassword(String username, String currentPassword, String newPassword, MediaType request = APPLICATION_XML_TYPE, MediaType accept = APPLICATION_XML_TYPE) {
        initOnUse()
        def cp = v2Factory.createChangePasswordCredential(username, currentPassword, newPassword)
        resource.path(path20).path(USERS).path(RAX_AUTH).path(SERVICE_PATH_CHANGE_PASSWORD).accept(accept).type(request).entity(cp).post(ClientResponse)
    }

    def listUserEffectiveRolesWithSources(String token, String userId, ListEffectiveRolesForUserParams params = new ListEffectiveRolesForUserParams(null), MediaType media = APPLICATION_XML_TYPE) {
        initOnUse()
        def queryParams = new MultivaluedMapImpl()
        if (params.onTenantId != null) {
            queryParams.add("onTenantId", params.onTenantId)
        }
        resource.path(path20).path(USERS).path(userId).path(RAX_AUTH).path(SERVICE_PATH_ROLES).queryParams(queryParams).accept(media).type(media).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def addApiKeyToUser(String token, String userId, credential) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).path(RAX_KSKEY_API_KEY_CREDENTIALS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(credential).post(ClientResponse)
    }

    def resetUserApiKey(String token, String userId) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).path(RAX_KSKEY_API_KEY_CREDENTIALS).path(RAX_AUTH).path("reset").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).post(ClientResponse)
    }

    def getPasswordCredentials(String token, String userId) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).path(PASSWORD_CREDENTIALS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def deleteTenant(String token, String tenantId) {
        initOnUse()
        resource.path(path20).path(TENANTS).path(tenantId).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def getTenant(String token, String tenantId, accept = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(TENANTS).path(tenantId).accept(accept).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getTenantByName(String token, String tenantName, accept = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(TENANTS).queryParam(NAME, tenantName).accept(accept).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def listTenants(String token, applyRcnRoles = false, accept = APPLICATION_XML_TYPE) {
        initOnUse()
        def queryParams = new MultivaluedMapImpl()
        if (applyRcnRoles != null) {
            queryParams.add("apply_rcn_roles", applyRcnRoles)
        }
        resource.path(path20).path(TENANTS).queryParams(queryParams).accept(accept).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUserApiKey(String token, String userId) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).path(RAX_KSKEY_API_KEY_CREDENTIALS).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def deleteUserApiKey(String token, String userId) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).path(RAX_KSKEY_API_KEY_CREDENTIALS).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def getUser(String token, URI location) {
        initOnUse()
        resource.uri(location).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def listUsers(String token, MediaType mediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).header(X_AUTH_TOKEN, token).accept(mediaType).get(ClientResponse)
    }

    def listUsers(String token, offset, limit, MediaType mediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).queryParams(pageParams(offset, limit)).header(X_AUTH_TOKEN, token).accept(mediaType).get(ClientResponse)
    }

    def listUsersInDomain(String token, String domainId, enabled = null, MediaType mediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        def request = resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(USERS)
        if(enabled != null) {
            request = request.queryParam("enabled", "" + enabled)
        }
        request.header(X_AUTH_TOKEN, token).accept(mediaType).get(ClientResponse)
    }

    def changeDomainAdministrator(String token, String domainId, DomainAdministratorChange domainAdministratorChange, MediaType requestMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(DOMAIN_ADMINISTRATOR_CHANGE_PATH).type(requestMediaType).entity(domainAdministratorChange).header(X_AUTH_TOKEN, token).accept(acceptMediaType).put(ClientResponse)
    }

    def domainRcnSwitch(String token, String domainId, String destinationRcn) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(RCN_PATH).path(destinationRcn).header(X_AUTH_TOKEN, token).put(ClientResponse)
    }

    def getUserById(String token, String userId, MediaType mediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).accept(mediaType).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUsersByEmail(String token, String email, MediaType mediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).queryParam("email", email).accept(mediaType).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUsersByDomainId(String token, String domainId, MediaType mediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(USERS).accept(mediaType).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUsersByDomainIdAndEnabledFlag(String token, String domainId, boolean enabled, MediaType mediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(USERS).queryParam("enabled", "" + enabled).accept(mediaType).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def updateUser(String token, String userId, user, MediaType acceptMediaType = APPLICATION_XML_TYPE, MediaType requestMediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).header(X_AUTH_TOKEN, token).accept(acceptMediaType).type(requestMediaType).entity(user).post(ClientResponse)
    }

    def addCredential(String token, String userId, credential) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).entity(credential).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).post(ClientResponse)
    }

    def deleteUser(String token, String userId) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def createGroup(String token, group) {
        initOnUse()
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(group).post(ClientResponse)
    }

    def getGroup(String token, URI uri) {
        initOnUse()
        resource.uri(uri).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getGroupById(String token, String groupId) {
        initOnUse()
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).path(groupId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getGroupByName(String token, String name) {
        initOnUse()
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).queryParam("name", name).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getGroups(String token) {
        initOnUse()
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def updateGroup(String token, String groupId, group) {
        initOnUse()
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).path(groupId).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(group).put(ClientResponse)
    }

    def deleteGroup(String  token, String groupId) {
        initOnUse()
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).path(groupId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addUserToGroup(String token, String groupId, String userId) {
        initOnUse()
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).path(groupId).path(USERS).path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }

    def removeUserFromGroup(String token, String groupId, String userId) {
        initOnUse()
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).path(groupId).path(USERS).path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def listGroupsForUser(String token, String userId) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(RAX_KSGRP).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUsersFromGroup(String token, String groupId, String limit = null, String offset = null) {
        initOnUse()
        def request = resource.path(path20).path(RAX_GRPADM).path(GROUPS).path(groupId).path(USERS)
        if(limit != null || offset != null) {
            request = request.queryParams(pageParams(offset, limit))
        }
        request.header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def authenticateTokenAndTenant(token, tenantId, applyRcnRoles = false, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        def request = v2Factory.createTokenAuthenticationRequest(token, tenantId.toString(), null)
        def queryParams = new MultivaluedMapImpl()
        queryParams.add("apply_rcn_roles", applyRcnRoles)
        resource.path(path20).path(TOKENS).queryParams(queryParams).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(request).post(ClientResponse)
    }

    def authenticateTokenAndDelegationAgreement(token, delegationAgreementId, MediaType mediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        def request = v2Factory.createTokenDelegationAuthenticationRequest(token, delegationAgreementId)
        resource.path(path20).path(TOKENS).accept(mediaType.toString()).type(mediaType.toString()).entity(request).post(ClientResponse)
    }

    def authenticateTokenAndTenantApplyRcn(token, tenantId, String applyRcnRoles = null) {
        initOnUse()
        authenticate(v2Factory.createTokenAuthenticationRequest(token, tenantId.toString(), null), applyRcnRoles)
    }

    def authenticatePassword(String username, String password=DEFAULT_PASSWORD, String applyRcnRoles = null) {
        initOnUse()
        authenticate(v2Factory.createPasswordAuthenticationRequest(username, password), applyRcnRoles)
    }

    String authenticateForToken(username, String password=DEFAULT_PASSWORD) {
        def authResponse = authenticatePassword(username, password).getEntity(AuthenticateResponse)
        assert authResponse.value instanceof AuthenticateResponse
        return authResponse.value.token.id
    }

    def authenticatePasswordWithScope(String username, String password, String scope) {
        initOnUse()
        authenticate(v2Factory.createPasswordAuthenticationRequestWithScope(username, password, scope))
    }

    def authenticateApiKey(username, apiKey, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        def credentials = v2Factory.createApiKeyAuthenticationRequest(username, apiKey)
        resource.path(path20).path(TOKENS).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(credentials).post(ClientResponse)
    }

    def authenticateApiKeyWithScope(String username, String apiKey, String scope) {
        initOnUse()
        authenticate(v2Factory.createApiKeyAuthenticationRequestWithScope(username, apiKey, scope))
    }

    def authenticate(AuthenticationRequest request, String applyRcnRoles = null) {
        initOnUse()
        def queryParams = new MultivaluedMapImpl()
        if (applyRcnRoles != null) {
            queryParams.add("apply_rcn_roles", applyRcnRoles)
        }
        resource.path(path20).path(TOKENS).queryParams(queryParams).accept(APPLICATION_XML).type(APPLICATION_XML).entity(request).post(ClientResponse)
    }

    def samlAuthenticate(request, accept = APPLICATION_XML, contentType = APPLICATION_XML) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SAML_TOKENS).accept(accept).type(contentType).entity(request).post(ClientResponse)
    }

    def federatedAuthenticate(request, applyRcnRoles = false, String version = null, accept = APPLICATION_XML) {
        initOnUse()
        def queryParams = new MultivaluedMapImpl()
        queryParams.add("apply_rcn_roles", applyRcnRoles)
        WebResource.Builder builder = resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SAML).path(AUTH).queryParams(queryParams).accept(accept).type(APPLICATION_XML).entity(request)
        if (StringUtils.isNotEmpty(version)) {
            builder = builder.header(GlobalConstants.HEADER_IDENTITY_API_VERSION, version)
        }
        builder.post(ClientResponse)
    }

    def federatedAuthenticateV2(request, accept = APPLICATION_XML) {
        federatedAuthenticate(request, false, GlobalConstants.FEDERATION_API_V2_0, accept)
    }

    def federatedLogout(request, accept = APPLICATION_XML) {
        initOnUse()
        MultivaluedMap<String, String> map = new MultivaluedMapImpl();
        map.put("SAMLRequest", [request]);
        resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SAML).path(LOGOUT).accept(accept).type(MediaType.APPLICATION_FORM_URLENCODED).entity(map).post(ClientResponse)
    }

    def createIdentityProvider(token, identityProvider, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SERVICE_PATH_IDENTITY_PROVIDERS).header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(identityProvider).post(ClientResponse)
    }

    def createIdentityProviderWithMetadata(token, metadata, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SERVICE_PATH_IDENTITY_PROVIDERS).header(X_AUTH_TOKEN, token).accept(acceptMediaType).type(requestContentMediaType).entity(metadata).post(ClientResponse)
    }

    def updateIdentityProvider(token, identityProviderId, identityProvider, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SERVICE_PATH_IDENTITY_PROVIDERS).path(identityProviderId).header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(identityProvider).put(ClientResponse)
    }

    def updateIdentityProviderUsingMetadata(token, String identityProviderId, metadata, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SERVICE_PATH_IDENTITY_PROVIDERS).path(identityProviderId).path(SERVICE_PATH_METADATA).header(X_AUTH_TOKEN, token).accept(acceptMediaType).type(MediaType.APPLICATION_XML_TYPE).entity(metadata).put(ClientResponse)
    }

    def getIdentityProvider(token, identityProviderId, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SERVICE_PATH_IDENTITY_PROVIDERS).path(identityProviderId).header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).get(ClientResponse)
    }

    def updateIdentityProviderPolicy(token, identityProviderId, policy, MediaType requestContentMediaType = MediaType.APPLICATION_JSON_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SERVICE_PATH_IDENTITY_PROVIDERS).path(identityProviderId).path(SERVICE_PATH_MAPPING).header(X_AUTH_TOKEN, token).type(requestContentMediaType.toString()).entity(policy).put(ClientResponse)
    }

    def getIdentityProviderPolicy(token, identityProviderId, MediaType acceptMediaType = MediaType.APPLICATION_JSON_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SERVICE_PATH_IDENTITY_PROVIDERS).path(identityProviderId).path(SERVICE_PATH_MAPPING).header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).get(ClientResponse)
    }

    def getIdentityProviderPolicyMultipleAcceptTypes(token, identityProviderId, MediaType... mediaTypes) {
        initOnUse()
        WebResource webResource = resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SERVICE_PATH_IDENTITY_PROVIDERS).path(identityProviderId).path(SERVICE_PATH_MAPPING)
        return webResource.header(X_AUTH_TOKEN, token).accept(mediaTypes).get(ClientResponse)    }

    def getIdentityProviderMetadata(token, identityProviderId, MediaType acceptMediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SERVICE_PATH_IDENTITY_PROVIDERS).path(identityProviderId).path(SERVICE_PATH_METADATA).header(X_AUTH_TOKEN, token).accept(acceptMediaType).get(ClientResponse)
    }

    def listIdentityProviders(token, IdentityProviderSearchParams identityProviderSearchParams = new IdentityProviderSearchParams(), MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        WebResource webResource = resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SERVICE_PATH_IDENTITY_PROVIDERS)
        if (StringUtils.isNotBlank(identityProviderSearchParams.name)) {
            webResource = webResource.queryParam("name", identityProviderSearchParams.name)
        }
        if (StringUtils.isNotBlank(identityProviderSearchParams.issuer)) {
            webResource = webResource.queryParam("issuer", identityProviderSearchParams.issuer)
        }
        if (StringUtils.isNotBlank(identityProviderSearchParams.approvedDomainId)) {
            webResource = webResource.queryParam("approvedDomainId", identityProviderSearchParams.approvedDomainId)
        }
        if (StringUtils.isNotBlank(identityProviderSearchParams.idpType)) {
            webResource = webResource.queryParam("idpType", identityProviderSearchParams.idpType)
        }
        if (StringUtils.isNotBlank(identityProviderSearchParams.approvedTenantId)) {
            webResource = webResource.queryParam("approvedTenantId", identityProviderSearchParams.approvedTenantId)
        }
        if (StringUtils.isNotBlank(identityProviderSearchParams.emailDomain)) {
            webResource = webResource.queryParam("emailDomain", identityProviderSearchParams.emailDomain)
        }
        return webResource.header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).get(ClientResponse)
    }

    def deleteIdentityProvider(token, identityProviderId, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SERVICE_PATH_IDENTITY_PROVIDERS).path(identityProviderId).header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).delete(ClientResponse)
    }

    def createIdentityProviderCertificates(token, identityProviderId, publicCertificate, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptContentMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SERVICE_PATH_IDENTITY_PROVIDERS).path(identityProviderId).path(CERTIFICATES).header(X_AUTH_TOKEN, token).type(requestContentMediaType.toString()).accept(acceptContentMediaType).entity(publicCertificate).put(ClientResponse)
    }

    def deleteIdentityProviderCertificates(token, identityProviderId, certificateId, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(FEDERATION).path(SERVICE_PATH_IDENTITY_PROVIDERS).path(identityProviderId).path(CERTIFICATES).path(certificateId).header(X_AUTH_TOKEN, token).type(requestContentMediaType.toString()).delete(ClientResponse)
    }

    def createRegion(String token, region) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(REGIONS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(region).post(ClientResponse)
    }

    def getRegion(String token, String regionId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(REGIONS).path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getRegions(String token) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(REGIONS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def updateRegion(String token, String regionId, region) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(REGIONS).path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(region).put(ClientResponse)
    }

    def deleteRegion(String token, String regionName) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(REGIONS).path(regionName).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def listUsersWithRole(String token, String roleId, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(OS_KSADM).path(ROLES).path(roleId).path(RAX_AUTH).path(USERS).header(X_AUTH_TOKEN, token).accept(acceptMediaType).get(ClientResponse)
    }

    def listUsersWithRole(String token, String roleId, String offset, String limit) {
        initOnUse()
        resource.path(path20).path(OS_KSADM).path(ROLES).path(roleId).path(RAX_AUTH).path(USERS).queryParams(pageParams(offset, limit)).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def listUsersWithTenantId(String token, tenantId, ListUsersForTenantParams listUsersForTenantParams = new ListUsersForTenantParams(null, null, new PaginationParams()), MediaType acceptMediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        MultivaluedMap<String, String> params = new MultivaluedMapImpl()
        if (listUsersForTenantParams.contactId != null) {
            params.add("contactId", listUsersForTenantParams.contactId)
        }
        if (listUsersForTenantParams.roleId != null) {
            params.add("roleId", listUsersForTenantParams.roleId)
        }

        def pageParams = listUsersForTenantParams.paginationRequest
        if (pageParams != null && pageParams.limit != null) {
            params.add("limit", pageParams.limit)
        }
        if (pageParams != null && pageParams.marker != null) {
            params.add("marker", pageParams.marker)
        }

        resource.path(path20).path(TENANTS).path(tenantId).path(USERS).queryParams(params).header(X_AUTH_TOKEN, token).accept(acceptMediaType).get(ClientResponse)
    }

    def listUserAdminsOnTenant(String token, String tenantId, MediaType mediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(TENANTS).path(tenantId).path(RAX_AUTH).path(ADMINS).header(X_AUTH_TOKEN, token).accept(mediaType).get(ClientResponse)
    }

    def listUsersWithTenantIdAndRole(String token, tenantId, roleId, offset = "0", limit = "1000") {
        initOnUse()
        MultivaluedMap<String, String> pageParams = pageParams(offset, limit)
        pageParams.add("roleId", roleId)
        resource.path(path20).path(TENANTS).path(tenantId).path(USERS).queryParams(pageParams).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def listEndpointsForTenant(String token, tenantId, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(TENANTS).path(tenantId).path(OS_KSCATALOG).path(ENDPOINTS).header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).get(ClientResponse)
    }

    def listEndpointsForToken(String authToken, token, boolean applyRcnRoles = false, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        def queryParams = new MultivaluedMapImpl()
        queryParams.add("apply_rcn_roles", applyRcnRoles)
        resource.path(path20).path(TOKENS).path(token).path(ENDPOINTS).queryParams(queryParams).header(X_AUTH_TOKEN, authToken).accept(acceptMediaType.toString()).get(ClientResponse)
    }

    def createRole(String token, Role role, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(OS_KSADM).path(ROLES).header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(role).post(ClientResponse)
    }

    def createRole(String token, String role, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(OS_KSADM).path(ROLES).header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(role).post(ClientResponse)
    }

    def deleteRole(String token, String roleId) {
        initOnUse()
        resource.path(path20).path(OS_KSADM).path(ROLES).path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addApplicationRoleToUser(String token, String roleId, String userId) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(ROLES).path(OS_KSADM).path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }

    def listUserGlobalRoles(String token, String userId, serviceId = null, applyRcnRoles = true, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        def queryParams = new MultivaluedMapImpl()
        if (serviceId != null) {
            queryParams.add("serviceId", serviceId)
        }
        if (applyRcnRoles != null) {
            queryParams.add("apply_rcn_roles", applyRcnRoles)
        }
        resource.path(path20).path(USERS).path(userId).path(ROLES).queryParams(queryParams).header(X_AUTH_TOKEN, token).accept(acceptMediaType).type(requestContentMediaType).get(ClientResponse)
    }

    def getUserApplicationRole(String token, String roleId, String userId) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(ROLES).path(OS_KSADM).path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def deleteUserProductRoles(String token, String userId, String roleType) {
        initOnUse()
        resource.path(path20).path("users").path(userId).path("roles").queryParam("type", roleType).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).delete(ClientResponse)
    }

    def deleteApplicationRoleFromUser(String token, String roleId, String userId) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(ROLES).path(OS_KSADM).path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addRoleToUserOnTenant(String token, String tenantId, String userId, String roleId, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE, requestContentMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(TENANTS).path(tenantId).path(USERS).path(userId) .path(ROLES).path(OS_KSADM).path(roleId) .header(X_AUTH_TOKEN, token).accept(acceptMediaType).type(requestContentMediaType).put(ClientResponse)
    }

    def addPhoneToUser(String token, String userId, MobilePhone requestMobilePhone, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_MOBILE_PHONES)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(requestMobilePhone).post(ClientResponse)
    }

    def deletePhoneFromUser(String token, String userId, String mobilePhoneId, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_MOBILE_PHONES).path(mobilePhoneId)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).delete(ClientResponse)
    }

    def getPhoneFromUser(String token, String userId, String mobilePhoneId, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_MOBILE_PHONES).path(mobilePhoneId)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).get(ClientResponse)
    }

    def addOTPDeviceToUser(String token, String userId, OTPDevice requestOTPDevice, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_OTP_DEVICES)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString())
                .entity(requestOTPDevice).post(ClientResponse)
    }

    def getOTPDeviceFromUser(String token, String userId, String deviceId, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_OTP_DEVICES).path(deviceId)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString())
                .get(ClientResponse)
    }

    def getOTPDevicesFromUser(String token, String userId, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_OTP_DEVICES)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString())
                .get(ClientResponse)
    }

    def getMultiFactorDevicesFromUser(String token, String userId, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_MULTI_FACTOR_DEVICES)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString())
                .get(ClientResponse)
    }

    def deleteOTPDeviceFromUser(String token, String userId, String deviceId, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_OTP_DEVICES).path(deviceId)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString())
                .delete(ClientResponse)
    }

    def verifyOTPDevice(String token, String userId, String deviceId, VerificationCode verificationCode, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_OTP_DEVICES).path(deviceId).path(SERVICE_PATH_VERIFY)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString())
                .entity(verificationCode).post(ClientResponse)
    }

    def listDevices(String token, String userId, MediaType accept = MediaType.APPLICATION_XML_TYPE, MediaType contentType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_MOBILE_PHONES)
                .header(X_AUTH_TOKEN, token).accept(accept).type(contentType).get(ClientResponse)
    }

    def sendVerificationCode(String token, String userId, String mobilePhoneId, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_MOBILE_PHONES).path(mobilePhoneId).path(SERVICE_PATH_VERIFICATION_CODE)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).post(ClientResponse)
    }

    def verifyVerificationCode(String token, String userId, String mobilePhoneId, VerificationCode verificationCode, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_MOBILE_PHONES).path(mobilePhoneId).path(SERVICE_PATH_VERIFY)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(verificationCode).post(ClientResponse)
    }

    def updateMultiFactorSettings(String token, String userId, multiFactorSettings, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(multiFactorSettings).put(ClientResponse)
    }

    def updateMultiFactorDomainSettings(String token, String domainId, multiFactorDomainSettings, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId)
                .path(SERVICE_PATH_MULTI_FACTOR)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(multiFactorDomainSettings).put(ClientResponse)
    }

    def getBypassCodes(String token, String userId, bypassCodes, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_BYPASS_CODES)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(bypassCodes).post(ClientResponse)
    }

    def deleteMultiFactor(String token, String userId, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).delete(ClientResponse)
    }

    def addUserRole(String token, String userId, String roleId, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId)
                .path(ROLES).path(OS_KSADM).path(roleId)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).put(ClientResponse)
    }

    def deleteRoleFromUserOnTenant(String token, String tenantId, String userId, String roleId) {
        initOnUse()
        resource.path(path20).path(TENANTS).path(tenantId).path(USERS).path(userId)
                .path(ROLES).path(OS_KSADM).path(roleId)
                .header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def listRolesForUserOnTenant(String token, String tenantId, String userId, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        listRolesForUserOnTenant(token, tenantId, userId, false, acceptMediaType)
    }

    def listRolesForUserOnTenant(String token, String tenantId, String userId, boolean applyRcnRoles, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(TENANTS).path(tenantId).path(USERS).path(userId).path(ROLES)
                .queryParam(APPLY_RCN_ROLES, applyRcnRoles.toString())
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType).get(ClientResponse)
    }

    def addEndpoint(String token, String tenantId, endpointTemplate, accept = APPLICATION_XML_TYPE, request = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(TENANTS).path(tenantId).path(OS_KSCATALOG).path(ENDPOINTS).header(X_AUTH_TOKEN, token).accept(accept).type(request).entity(endpointTemplate).post(ClientResponse)
    }

    def deleteApplicationRoleOnUser(String token, String roleId, String userId) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(ROLES).path(OS_KSADM).path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).delete(ClientResponse)
    }

    def createQuestion(String token, question) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SECRETQA).path(QUESTIONS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(question).post(ClientResponse)
    }

    def getQuestion(String token, questionId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SECRETQA).path(QUESTIONS).path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def getQuestionFromLocation(String token, location) {
        initOnUse()
        resource.uri(location).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def getQuestions(String token) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SECRETQA).path(QUESTIONS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def updateQuestion(String token, String questionId, question) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SECRETQA).path(QUESTIONS).path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(question).put(ClientResponse)
    }

    def deleteQuestion(String token, String questionId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SECRETQA).path(QUESTIONS).path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).delete(ClientResponse)
    }

    def addEndpointTemplate(String token, endpointTemplate, MediaType acceptMediaType = APPLICATION_XML_TYPE, MediaType requestMediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).header(X_AUTH_TOKEN, token).accept(acceptMediaType).type(requestMediaType).entity(endpointTemplate).post(ClientResponse)
    }

    def getEndpointTemplate(String token, String endpointTemplateId, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).path(endpointTemplateId).header(X_AUTH_TOKEN, token).accept(acceptMediaType).type(APPLICATION_XML).get(ClientResponse)
    }

    def listEndpointTemplates(String token, accept = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).header(X_AUTH_TOKEN, token).accept(accept).get(ClientResponse)
    }

    def updateEndpointTemplate(String token, endpointId, endpointTemplate, accept = APPLICATION_XML, request = APPLICATION_XML) {
        initOnUse()
        resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).path(endpointId).header(X_AUTH_TOKEN, token).accept(accept).type(request).entity(endpointTemplate).put(ClientResponse)
    }

    def deleteEndpointTemplate(String token, String endpointTemplateId) {
        initOnUse()
        resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).path(endpointTemplateId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addTenant(String token, Tenant tenant, accept = APPLICATION_XML_TYPE, request = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(TENANTS).header(X_AUTH_TOKEN, token).accept(accept).type(request).entity(tenant).post(ClientResponse)
    }

    def addTenantType(String token, TenantType tenantType, accept = APPLICATION_XML_TYPE, request = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(TENANT_TYPES_URL).header(X_AUTH_TOKEN, token).accept(accept).type(request).entity(tenantType).post(ClientResponse)
    }

    def getTenantType(String token, String tenantTypeName, accept = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(TENANT_TYPES_URL).path(tenantTypeName).accept(accept).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def listTenantTypes(String token, accept = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(TENANT_TYPES_URL).accept(accept).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def deleteTenantType(String token, String name) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(TENANT_TYPES_URL).path(name).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def updateTenant(String token, String tenantId, Tenant tenant, accept = APPLICATION_XML_TYPE, request = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(TENANTS).path(tenantId).header(X_AUTH_TOKEN, token).accept(accept).type(request).entity(tenant).post(ClientResponse)
    }

    def getSecretQAs(String token, String userId){
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(RAX_AUTH).path(SECRETQAS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createSecretQA(String token, String userId, secretqa){
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(RAX_AUTH).path(SECRETQAS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(secretqa).post(ClientResponse)
    }

    def getSecretQA(String token, String userId){
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(RAX_KSQA).path(SECRETQA).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def updateSecretQA(String token, String userId, secretqa){
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(RAX_KSQA).path(SECRETQA).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(secretqa).put(ClientResponse)
    }

    def getRole(String token, String roleId) {
        initOnUse()
        resource.path(path20).path(OS_KSADM).path(ROLES).path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getPhonePin(String token, String userId, MediaType accept = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(RAX_AUTH).path(PHONE_PIN_URL).header(X_AUTH_TOKEN, token).accept(accept).get(ClientResponse)
    }

    def verifyPhonePin(String token, String userId, phonePin, MediaType requestType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(RAX_AUTH).path(PHONE_PIN_URL).path(SERVICE_PATH_VERIFY).header(X_AUTH_TOKEN, token).type(requestType).entity(phonePin).post(ClientResponse)
    }

    def resetPhonePin(String token, String userId, MediaType accept = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(RAX_AUTH).path(PHONE_PIN_URL).path(SERVICE_PATH_RESET).header(X_AUTH_TOKEN, token).accept(accept).post(ClientResponse)
    }

    def validateToken(String token, String validateToken, accept = APPLICATION_XML_TYPE){
        initOnUse()
        resource.path(path20).path(TOKENS).path(validateToken).header(X_AUTH_TOKEN, token).accept(accept).get(ClientResponse)
    }

    def addDomain(String token, domain, MediaType acceptMediaType = APPLICATION_XML_TYPE, MediaType requestMediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).header(X_AUTH_TOKEN, token).accept(acceptMediaType).type(requestMediaType).entity(domain).post(ClientResponse)
    }

    def updateDomain(String token, String domainId, domain, MediaType acceptMediaType = APPLICATION_XML_TYPE, MediaType requestMediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).header(X_AUTH_TOKEN, token).accept(acceptMediaType).type(requestMediaType).entity(domain).put(ClientResponse)
    }

    def deleteDomain(String token, String domainId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def getDomain(String token, String domainId, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).accept(acceptMediaType.toString()).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getAccessibleDomains(String token, String marker = null, String limit = null, String rcn = null, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        def queryParams = new MultivaluedMapImpl()
        if (marker != null) {
            queryParams.add("marker", marker)
        }
        if (limit != null) {
            queryParams.add("limit", limit)
        }
        if (rcn != null) {
            queryParams.add("rcn", rcn)
        }
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).queryParams(queryParams).accept(acceptMediaType.toString()).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getAccessibleDomainsForUser(String token, String userId, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(RAX_AUTH).path(DOMAINS).accept(acceptMediaType.toString()).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def addUserToDomain(String token, String userId, String domainId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(USERS).path(userId).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).put(ClientResponse)
    }

    def addTenantToDomain(String token, String domainId, String tenantId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(TENANTS).path(tenantId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).put(ClientResponse)
    }

    def deleteTenantFromDomain(String token, String domainId, String tenantId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(TENANTS).path(tenantId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def getDomainTenants(String token, String domainId, boolean enabled = true, accept = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(TENANTS).queryParam(ENABLED, enabled.toString()).header(X_AUTH_TOKEN, token).accept(accept).get(ClientResponse)
    }

    def getEndpointsByDomain(String token, String domainId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(ENDPOINTS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def listCredentials(String token, String userId){
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getGetPasswordCredentials(token, userId) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).path(RAX_KSKEY_API_KEY_CREDENTIALS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(credential).post ClientResponse
    }

    def impersonate(String token, User user, Integer expireTime = 10800, MediaType requestType=MediaType.APPLICATION_XML_TYPE, MediaType accept=MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        def request = new ImpersonationRequest().with {
            it.user = new User().with {
                it.username = user.username
                if(user.federatedIdp != null) {
                    it.federatedIdp = user.federatedIdp
                }
                it
            }
            it.expireInSeconds = expireTime
            it
        }
        resource.path(path20).path(RAX_AUTH).path("impersonation-tokens").header(X_AUTH_TOKEN, token).accept(accept).type(requestType).entity(request).post(ClientResponse)
    }

    def revokeUserToken(String token, String tokenToRevoke) {
        initOnUse()
        resource.path(path20).path(TOKENS).path(tokenToRevoke).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def revokeToken(String token) {
        initOnUse()
        resource.path(path20).path(TOKENS).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def listRoles(String token, String serviceId = null, String marker = null, String limit = null, String roleName = null, accept = APPLICATION_XML_TYPE) {
        initOnUse()
        def queryParams = new MultivaluedMapImpl()
        if (serviceId != null) {
            queryParams.add("serviceId", serviceId)
        }
        if (roleName != null) {
            queryParams.add("roleName", roleName)
        }
        if (marker != null) {
            queryParams.add("marker", marker)
        }
        if (limit != null) {
            queryParams.add("limit", limit)
        }
        resource.path(path20).path(OS_KSADM).path(ROLES).queryParams(queryParams).header(X_AUTH_TOKEN, token).accept(accept).get(ClientResponse)
    }

    def updateCredentials(String token, String userId, creds) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).path(PASSWORD_CREDENTIALS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(creds).post(ClientResponse)
    }

    def getAdminsForUser(String token, String userId) {
        initOnUse()
        resource.path(path20).path(USERS).path(userId).path(RAX_AUTH).path(ADMINS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createService(String token, service) {
        initOnUse()
        resource.path(path20).path(OS_KSADM).path("services").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(service).post(ClientResponse)
    }

    def listServices(String token, MediaType accept=APPLICATION_XML_TYPE, String name = null, String marker = "0", String limit = "1000") {
        initOnUse()
        WebResource webResource = resource.path(path20).path(OS_KSADM).path("services")
        def queryParams = new MultivaluedMapImpl()
        if (marker != null) {
            queryParams.add("marker", marker)
        }
        if (limit != null) {
            queryParams.add("limit", limit)
        }
        if (name != null){
            queryParams.add("name", name)
        }
        webResource.queryParams(queryParams).header(X_AUTH_TOKEN, token).accept(accept).get(ClientResponse)
    }

    def deleteService(String token, String serviceId) {
        initOnUse()
        resource.path(path20).path(OS_KSADM).path("services").path(serviceId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def destroyUser(String token, String userId) {
        initOnUse()
        deleteUser(token, userId)
    }

    def updateDefaultRegionServices(String token, defaultRegionServices) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path("default-region").path("services").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(defaultRegionServices).put(ClientResponse)
    }

    def pageParams(String offset, String limit) {
        new MultivaluedMapImpl().with {
            if (offset != null) {
                it.add("marker", offset)
            }
            if (limit != null) {
                it.add("limit", limit)
            }
            return it
        }
    }

    def getEndpointsForToken(String authToken, String token) {
        initOnUse()
        resource.path(path20).path(TOKENS).path(token).path(ENDPOINTS).header(X_AUTH_TOKEN, authToken).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getUserByTenantId(String token, String tenantId, MediaType mediaType = APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(TENANTS).path(tenantId).path(RAX_AUTH).path(ADMINS).accept(mediaType).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def forgotPassword(ForgotPasswordCredentials forgotPasswordCredentials, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(RAX_AUTH).path(SERVICE_PATH_FORGOT_PASSWORD).type(requestContentMediaType).entity(forgotPasswordCredentials).post(ClientResponse)
    }

    def resetPassword(def token, def passwordReset, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(RAX_AUTH).path(SERVICE_PATH_PASSWORD_RESET).type(requestContentMediaType).entity(passwordReset).header(X_AUTH_TOKEN, token).post(ClientResponse)
    }

    def getVersion(MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).accept(requestContentMediaType).get(ClientResponse)
    }

    def addEndpointAssignmentRule(String token, endpointAssignmentRule, MediaType requestType=MediaType.APPLICATION_XML_TYPE, MediaType accept=MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).path(RAX_AUTH).path(SERVICE_PATH_RULES).header(X_AUTH_TOKEN, token).type(requestType).accept(accept).entity(endpointAssignmentRule).post(ClientResponse)
    }

    def deleteEndpointAssignmentRule(String token, ruleId) {
        initOnUse()
        resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).path(RAX_AUTH).path(SERVICE_PATH_RULES).path(ruleId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def getEndpointAssignmentRule(String token, ruleId, detailLevel = null, MediaType requestType=MediaType.APPLICATION_XML_TYPE, MediaType accept=MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        WebResource webResource = resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).path(RAX_AUTH).path(SERVICE_PATH_RULES).path(ruleId)
        if (detailLevel != null) {
            webResource = webResource.queryParam(GET_RULE_DETAIL_PARAM, detailLevel)
        }
        webResource.type(requestType).accept(accept).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def listEndpointAssignmentRules(String token, MediaType requestType=MediaType.APPLICATION_XML_TYPE, MediaType accept=MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).path(RAX_AUTH).path(SERVICE_PATH_RULES).type(requestType).accept(accept).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def createUserGroup(String token, UserGroup userGroup, MediaType media=MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DOMAINS).path(userGroup.domainId).path(SERVICE_PATH_USER_GROUPS).type(media).accept(media).header(X_AUTH_TOKEN, token).entity(userGroup).post(ClientResponse)
    }

    def updateUserGroup(String token, String groupId, String domainId, UserGroup userGroup, MediaType request=MediaType.APPLICATION_XML_TYPE, MediaType accept=MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DOMAINS).path(domainId).path(SERVICE_PATH_USER_GROUPS).path(groupId).type(request).accept(accept).header(X_AUTH_TOKEN, token).entity(userGroup).put(ClientResponse)
    }

    def getUserGroup(String token, UserGroup userGroup, MediaType media=MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DOMAINS).path(userGroup.domainId).path(SERVICE_PATH_USER_GROUPS).path(userGroup.getId()).accept(media).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def listRoleAssignmentsOnUserGroup(String token, UserGroup userGroup, UserGroupRoleSearchParams searchParams = null, MediaType media=MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        WebResource resource = resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DOMAINS).path(userGroup.domainId).path(SERVICE_PATH_USER_GROUPS)
                .path(userGroup.id).path(SERVICE_PATH_ROLES)

        if (searchParams != null && searchParams.paginationRequest != null) {
            resource = resource.queryParams(pageParams(String.valueOf(searchParams.getPaginationRequest().marker), String.valueOf(searchParams.getPaginationRequest().limit)))
        }
        resource.accept(media).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getRoleAssignmentOnUserGroup(String token, UserGroup userGroup, String roleId, MediaType media=MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DOMAINS).path(userGroup.domainId).path(SERVICE_PATH_USER_GROUPS)
                .path(userGroup.id).path(SERVICE_PATH_ROLES).path(roleId).accept(media).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def revokeRoleAssignmentFromUserGroup(String token, UserGroup userGroup, String roleId, MediaType media=MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DOMAINS).path(userGroup.domainId).path(SERVICE_PATH_USER_GROUPS).path(userGroup.id).path(SERVICE_PATH_ROLES).path(roleId).type(media).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def grantRoleOnTenantToGroup(String token, UserGroup userGroup, String roleId, String tenantId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DOMAINS).path(userGroup.domainId).path(SERVICE_PATH_USER_GROUPS).path(userGroup.id).path(SERVICE_PATH_ROLES).path(roleId).path(TENANTS).path(tenantId).header(X_AUTH_TOKEN, token).put(ClientResponse)
    }

    def revokeRoleOnTenantToGroup(String token, UserGroup userGroup, String roleId, String tenantId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DOMAINS).path(userGroup.domainId).path(SERVICE_PATH_USER_GROUPS).path(userGroup.id).path(SERVICE_PATH_ROLES).path(roleId).path(TENANTS).path(tenantId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def grantRoleAssignmentsOnUserGroup(String token, UserGroup userGroup, RoleAssignments roleAssignments, MediaType media=MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DOMAINS).path(userGroup.domainId).path(SERVICE_PATH_USER_GROUPS).path(userGroup.id).path(SERVICE_PATH_ROLES).type(media).accept(media).header(X_AUTH_TOKEN, token).entity(roleAssignments).put(ClientResponse)
    }

    def deleteUserGroup(String token, UserGroup userGroup) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DOMAINS).path(userGroup.domainId).path(SERVICE_PATH_USER_GROUPS).path(userGroup.id).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def listUserGroupsForDomain(String token, String domainId, UserGroupSearchParams userGroupSearchParams = null, MediaType media=APPLICATION_XML_TYPE) {
        initOnUse()
        WebResource webResource = resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DOMAINS).path(domainId).path(SERVICE_PATH_USER_GROUPS)
        def queryParams = new MultivaluedMapImpl()
        if (userGroupSearchParams != null ) {
            if (userGroupSearchParams.name != null) {
                queryParams.add("name", userGroupSearchParams.name)
            }
            if (userGroupSearchParams.userId != null) {
                queryParams.add("userId", userGroupSearchParams.userId)
            }
        }
        webResource.queryParams(queryParams).accept(media).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def addUserToUserGroup(String token, String domainId, String groupId, String userId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(GROUPS).path(groupId).path(USERS).path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }

    def removeUserFromUserGroup(String token, String domainId, String groupId, String userId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(GROUPS).path(groupId).path(USERS).path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).delete(ClientResponse)
    }

    def getUsersInUserGroup(String token, String domainId, String groupId, UserSearchCriteria userSearchCriteria = null, MediaType accept = APPLICATION_XML_TYPE) {
        initOnUse()
        WebResource resource = resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(GROUPS).path(groupId).path(USERS)
        if (userSearchCriteria != null && userSearchCriteria.paginationRequest != null) {
            resource = resource.queryParams(pageParams(String.valueOf(userSearchCriteria.getPaginationRequest().marker), String.valueOf(userSearchCriteria.getPaginationRequest().limit)))
        }
        resource.header(X_AUTH_TOKEN, token).accept(accept).get(ClientResponse)
    }

    def grantRoleAssignmentsOnUser(String token, User user, RoleAssignments roleAssignments, MediaType media=MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(USERS).path(user.id).path(RAX_AUTH).path(SERVICE_PATH_ROLES).type(media).accept(media).header(X_AUTH_TOKEN, token).entity(roleAssignments).put(ClientResponse)
    }

    def createIdentityProviderWithCred(String token, IdentityProviderFederationTypeEnum type, Credential cred) {
        def pubCertPemString1 = SamlCredentialUtils.getCertificateAsPEMString(cred.entityCertificate)
        def pubCerts1 = v2Factory.createPublicCertificate(pubCertPemString1)
        def publicCertificates = v2Factory.createPublicCertificates(pubCerts1)

        def approvedDomainGroup = ApprovedDomainGroupEnum.GLOBAL
        if (type == IdentityProviderFederationTypeEnum.RACKER) {
            approvedDomainGroup = null
        }

        def idp = v2Factory.createIdentityProvider(UUID.randomUUID().toString(), "blah", UUID.randomUUID().toString(), type, approvedDomainGroup, null).with {
            it.publicCertificates = publicCertificates
            it
        }
        createIdentityProvider(token, idp)
    }

    def createDomainIdIdentityProviderWithCred(String token, List<String> domainIds, Credential cred) {
        def pubCertPemString1 = SamlCredentialUtils.getCertificateAsPEMString(cred.entityCertificate)
        def pubCerts1 = v2Factory.createPublicCertificate(pubCertPemString1)
        def publicCertificates = v2Factory.createPublicCertificates(pubCerts1)

        def idp = v2Factory.createIdentityProvider(UUID.randomUUID().toString(), "blah", UUID.randomUUID().toString(), IdentityProviderFederationTypeEnum.DOMAIN, null, domainIds).with {
            it.publicCertificates = publicCertificates
            it
        }
        createIdentityProvider(token, idp)
    }

    def createDelegationAgreement(token, delegationAgreement, MediaType mediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DA).header(X_AUTH_TOKEN, token).accept(mediaType.toString()).type(mediaType.toString()).entity(delegationAgreement).post(ClientResponse)
    }

    def getDelegationAgreement(token, delegationAgreementId, MediaType mediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DA).path(delegationAgreementId).header(X_AUTH_TOKEN, token).accept(mediaType.toString()).type(mediaType.toString()).get(ClientResponse)
    }

    def listDelegationAgreements(token, relationship = null, MediaType mediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()

        WebResource resource = resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DA)
        if (StringUtils.isNotBlank(relationship)) {
            MultivaluedMapImpl map = new MultivaluedMapImpl().with {
                it.add("relationship", relationship)
                return it
            }
            resource = resource.queryParams(map)
        }
        resource.header(X_AUTH_TOKEN, token).accept(mediaType).get(ClientResponse)
    }

    def deleteDelegationAgreement(token, delegationAgreementId, MediaType mediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DA).path(delegationAgreementId).header(X_AUTH_TOKEN, token).accept(mediaType.toString()).type(mediaType.toString()).delete(ClientResponse)
    }

    def addUserDelegate(token, delegationAgreementId, String userId, MediaType mediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DA).path(delegationAgreementId).path(SERVICE_PATH_DELEGATES).path(SERVICE_PATH_USERS).path(userId).header(X_AUTH_TOKEN, token).accept(mediaType.toString()).put(ClientResponse)
    }

    def deleteUserDelegate(token, delegationAgreementId, String userId, MediaType mediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DA).path(delegationAgreementId).path(SERVICE_PATH_DELEGATES).path(SERVICE_PATH_USERS).path(userId).header(X_AUTH_TOKEN, token).accept(mediaType.toString()).delete(ClientResponse)
    }

    def addUserGroupDelegate(token, delegationAgreementId, String userGroupId, MediaType mediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DA).path(delegationAgreementId).path(SERVICE_PATH_DELEGATES).path(SERVICE_PATH_USER_GROUPS).path(userGroupId).header(X_AUTH_TOKEN, token).accept(mediaType.toString()).put(ClientResponse)
    }

    def deleteUserGroupDelegate(token, delegationAgreementId, String userGroupId, MediaType mediaType = MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DA).path(delegationAgreementId).path(SERVICE_PATH_DELEGATES).path(SERVICE_PATH_USER_GROUPS).path(userGroupId).header(X_AUTH_TOKEN, token).accept(mediaType.toString()).delete(ClientResponse)
    }

    def grantRoleAssignmentsOnDelegationAgreement(String token, DelegationAgreement delegationAgreement, RoleAssignments roleAssignments, MediaType media=MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DA).path(delegationAgreement.id).path(SERVICE_PATH_ROLES).type(media).accept(media).header(X_AUTH_TOKEN, token).entity(roleAssignments).put(ClientResponse)
    }

    def listRolesOnDelegationAgreement(String token, DelegationAgreement delegationAgreement, DelegationAgreementRoleSearchParams searchParams = null, MediaType media=MediaType.APPLICATION_XML_TYPE) {
        initOnUse()
        WebResource resource = resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DA).path(delegationAgreement.id).path(SERVICE_PATH_ROLES)

        if (searchParams != null && searchParams.paginationRequest != null) {
            resource = resource.queryParams(pageParams(String.valueOf(searchParams.getPaginationRequest().marker), String.valueOf(searchParams.getPaginationRequest().limit)))
        }
        resource.accept(media).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def revokeRoleAssignmentFromDelegationAgreement(String token, DelegationAgreement delegationAgreement, String roleId) {
        initOnUse()
        resource.path(path20).path(RAX_AUTH).path(SERVICE_PATH_DA).path(delegationAgreement.id).path(SERVICE_PATH_ROLES).path(roleId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }
    /**
     * Creates a new IDP, verifying the IDP was created successfully, and return the IDP rather than the raw response
     * @param type
     * @param cred
     * @return
     */
    def generateIdentityProviderWithCred(String token, IdentityProviderFederationTypeEnum type, Credential cred) {
        def response = createIdentityProviderWithCred(token, type, cred)
        assert (response.status == SC_CREATED)
        return response.getEntity(IdentityProvider)
    }

    /**
     * Creates a new IDP, verifying the IDP was created successfully, and return the IDP rather than the raw response
     * @param type
     * @param cred
     * @return
     */
    def generateDomainIdIdentityProviderWithCred(String token, List<String> domainIds, Credential cred) {
        def response = createDomainIdIdentityProviderWithCred(token, domainIds, cred)
        assert (response.status == SC_CREATED)
        return response.getEntity(IdentityProvider)
    }
}
