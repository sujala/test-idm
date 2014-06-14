package testHelpers

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.idm.api.resource.cloud.v20.MultiFactorCloud20Service
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.core.util.MultivaluedMapImpl
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import spock.lang.Shared

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.JSONConstants.*
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static javax.ws.rs.core.MediaType.APPLICATION_JSON
import static javax.ws.rs.core.MediaType.APPLICATION_XML
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 6/27/13
 * Time: 1:00 PM
 * To change this template use File | Settings | File Templates.
 */


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

    //path constants
    static def SERVICE_PATH_MOBILE_PHONES = "mobile-phones"
    static def SERVICE_PATH_MULTI_FACTOR = "multi-factor"
    static def SERVICE_PATH_VERIFY = "verify"
    static def SERVICE_PATH_VERIFICATION_CODE = "verificationcode"

    static def ENDPOINTS = "endpoints"

    def init(){
        this.resource = ensureGrizzlyStarted("classpath:app-config.xml")
        mediaType = new MediaTypeContext()
    }

    def authenticate(username, password, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        def credentials = v2Factory.createPasswordAuthenticationRequest(username, password)
        resource.path(path20).path(TOKENS).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(credentials).post(ClientResponse)
    }

    def authenticateMFAWithSessionIdAndPasscode(sessionId, passcode, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        def credentials = v2Factory.createPasscodeAuthenticationRequest(passcode)
        resource.path(path20).path(TOKENS).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).header(X_SESSION_ID, sessionId).entity(credentials).post(ClientResponse)
    }

    //TODO: remove once auth plugin is fixed
    def invalidAuthenticatePassword(username,password) {
        def body = String.format('{"auth":{"passwordCredentials":{"username":"%s","password":"%s", "tenantId":"blah", "tenantName": "blah"}}}', username, password)
        resource.path(path20).path(TOKENS).accept(APPLICATION_XML).type(APPLICATION_JSON).entity(body).post(ClientResponse)
    }

    //TODO: remove once auth plugin is fixed
    def invalidAuthenticateApiKey(username, key) {
        def body = String.format('{"auth":{"RAX-KSKEY:apiKeyCredentials":{"username":"%s","apiKey":"%s", "tenantId":"blah", "tenantName": "blah"}}}', username, key)
        resource.path(path20).path(TOKENS).accept(APPLICATION_XML).type(APPLICATION_JSON).entity(body).post(ClientResponse)
    }

    def authenticateRacker(username, password){
        def credentials = v2Factory.createPasswordAuthenticationRequest(username, password)
        credentials.domain =  new Domain().with {
            it.name = "Rackspace"
            it
        }
        resource.path(path20).path(TOKENS).accept(APPLICATION_XML).type(APPLICATION_XML).entity(credentials).post(ClientResponse)
    }

    def validateToken(authToken, token) {
        resource.path(path20).path(TOKENS).path(token).header(X_AUTH_TOKEN, authToken).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getUserByName(String token, String name, MediaType mediaType = APPLICATION_XML_TYPE) {
        resource.path(path20).path(USERS).queryParam("name", name).accept(mediaType).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def createUser(String token, user) {
        resource.path(path20).path(USERS).accept(APPLICATION_XML).type(APPLICATION_XML).header(X_AUTH_TOKEN, token).entity(user).post(ClientResponse)
    }

    def addApiKeyToUser(String token, String userId, credential) {
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).path(RAX_KSKEY_API_KEY_CREDENTIALS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(credential).post(ClientResponse)
    }

    def resetUserApiKey(String token, String userId) {
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).path(RAX_KSKEY_API_KEY_CREDENTIALS).path(RAX_AUTH).path("reset").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).post(ClientResponse)
    }

    def getPasswordCredentials(String token, String userId) {
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).path(PASSWORD_CREDENTIALS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def hardDeleteUser(String token, String userId) {
        resource.path(path20).path("softDeleted").path(USERS).path(userId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def deleteTenant(String token, String tenantId) {
        resource.path(path20).path(TENANTS).path(tenantId).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def getUserApiKey(String token, String userId) {
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).path(RAX_KSKEY_API_KEY_CREDENTIALS).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def deleteUserApiKey(String token, String userId) {
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).path(RAX_KSKEY_API_KEY_CREDENTIALS).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def getUser(String token, URI location) {
        resource.uri(location).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def listUsers(String token, MediaType mediaType = APPLICATION_XML_TYPE) {
        resource.path(path20).path(USERS).header(X_AUTH_TOKEN, token).accept(mediaType).get(ClientResponse)
    }

    def listUsers(String token, offset, limit, MediaType mediaType = APPLICATION_XML_TYPE) {
        resource.path(path20).path(USERS).queryParams(pageParams(offset, limit)).header(X_AUTH_TOKEN, token).accept(mediaType).get(ClientResponse)
    }

    def getUserById(String token, String userId, MediaType mediaType = APPLICATION_XML_TYPE) {
        resource.path(path20).path(USERS).path(userId).accept(mediaType).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUsersByEmail(String token, String email, MediaType mediaType = APPLICATION_XML_TYPE) {
        resource.path(path20).path(USERS).queryParam("email", email).accept(mediaType).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUsersByDomainId(String token, String domainId, MediaType mediaType = APPLICATION_XML_TYPE) {
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(USERS).accept(mediaType).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def updateUser(String token, String userId, user, MediaType acceptMediaType = APPLICATION_XML_TYPE, MediaType requestMediaType = APPLICATION_XML_TYPE) {
        resource.path(path20).path(USERS).path(userId).header(X_AUTH_TOKEN, token).accept(acceptMediaType).type(requestMediaType).entity(user).post(ClientResponse)
    }

    def addCredential(String token, String userId, credential) {
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).entity(credential).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).post(ClientResponse)
    }

    def deleteUser(String token, String userId) {
        resource.path(path20).path(USERS).path(userId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def createGroup(String token, group) {
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(group).post(ClientResponse)
    }

    def getGroup(String token, URI uri) {
        resource.uri(uri).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getGroupById(String token, String groupId) {
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).path(groupId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getGroupByName(String token, String name) {
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).queryParam("name", name).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getGroups(String token) {
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def updateGroup(String token, String groupId, group) {
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).path(groupId).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(group).put(ClientResponse)
    }

    def deleteGroup(String  token, String groupId) {
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).path(groupId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addUserToGroup(String token, String groupId, String userId) {
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).path(groupId).path(USERS).path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }
    def removeUserFromGroup(String token, String groupId, String userId) {
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).path(groupId).path(USERS).path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def listGroupsForUser(String token, String userId) {
        resource.path(path20).path(USERS).path(userId).path(RAX_KSGRP).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUsersFromGroup(String token, String groupId) {
        resource.path(path20).path(RAX_GRPADM).path(GROUPS).path(groupId).path(USERS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def authenticatePassword(String username, String password) {
        authenticate(v2Factory.createPasswordAuthenticationRequest(username, password))
    }

    def authenticateApiKey(String username, String apiKey) {
        authenticate(v2Factory.createApiKeyAuthenticationRequest(username, apiKey))
    }

    def authenticate(request) {
        resource.path(path20).path(TOKENS).accept(APPLICATION_XML).type(APPLICATION_XML).entity(request).post(ClientResponse)
    }

    def samlAuthenticate(request) {
        resource.path(path20).path(RAX_AUTH).path(SAML_TOKENS).accept(APPLICATION_XML).type(APPLICATION_XML).entity(request).post(ClientResponse)
    }

    def createRegion(String token, region) {
        resource.path(path20).path(RAX_AUTH).path(REGIONS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(region).post(ClientResponse)
    }

    def getRegion(String token, String regionId) {
        resource.path(path20).path(RAX_AUTH).path(REGIONS).path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getRegions(String token) {
        resource.path(path20).path(RAX_AUTH).path(REGIONS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def updateRegion(String token, String regionId, region) {
        resource.path(path20).path(RAX_AUTH).path(REGIONS).path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(region).put(ClientResponse)
    }

    def deleteRegion(String token, String regionId) {
        resource.path(path20).path(RAX_AUTH).path(REGIONS).path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def listUsersWithRole(String token, String roleId) {
        resource.path(path20).path(OS_KSADM).path(ROLES).path(roleId).path(RAX_AUTH).path(USERS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def listUsersWithRole(String token, String roleId, String offset, String limit) {
        resource.path(path20).path(OS_KSADM).path(ROLES).path(roleId).path(RAX_AUTH).path(USERS).queryParams(pageParams(offset, limit)).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def listUsersWithTenantId(String token, tenantId) {
        resource.path(path20).path(TENANTS).path(tenantId).path(USERS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def listEndpointsForTenant(String token, tenantId, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        resource.path(path20).path(TENANTS).path(tenantId).path(OS_KSCATALOG).path(ENDPOINTS).header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).get(ClientResponse)
    }

    def createRole(String token, Role role) {
        resource.path(path20).path(OS_KSADM).path(ROLES).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(role).post(ClientResponse)
    }

    def deleteRole(String token, String roleId) {
        resource.path(path20).path(OS_KSADM).path(ROLES).path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addApplicationRoleToUser(String token, String roleId, String userId) {
        resource.path(path20).path(USERS).path(userId).path(ROLES).path(OS_KSADM).path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }

    def listUserGlobalRoles(String token, String userId) {
        resource.path(path20).path(USERS).path(userId).path(ROLES).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def getUserApplicationRole(String token, String roleId, String userId) {
        resource.path(path20).path(USERS).path(userId).path(ROLES).path(OS_KSADM).path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def deleteUserProductRoles(String token, String userId, String roleType) {
        resource.path(path20).path("users").path(userId).path("roles").queryParam("type", roleType).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).delete(ClientResponse)
    }

    def deleteApplicationRoleFromUser(String token, String roleId, String userId) {
        resource.path(path20).path(USERS).path(userId).path(ROLES).path(OS_KSADM).path(roleId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def addRoleToUserOnTenant(String token, String tenantId, String userId, String roleId) {
        resource.path(path20).path(TENANTS).path(tenantId).path(USERS).path(userId)
                .path(ROLES).path(OS_KSADM).path(roleId)
                .header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }

    def addPhoneToUser(String token, String userId, MobilePhone requestMobilePhone, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_MOBILE_PHONES)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(requestMobilePhone).post(ClientResponse)
    }

    def listDevices(String token, String userId, MediaType accept = mediaType.accept, MediaType contentType = mediaType.contentType) {
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_MOBILE_PHONES)
                .header(X_AUTH_TOKEN, token).accept(accept).type(contentType).get(ClientResponse)
    }

    def sendVerificationCode(String token, String userId, String mobilePhoneId, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_MOBILE_PHONES).path(mobilePhoneId).path(SERVICE_PATH_VERIFICATION_CODE)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).post(ClientResponse)
    }

    def verifyVerificationCode(String token, String userId, String mobilePhoneId, VerificationCode verificationCode, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR).path(SERVICE_PATH_MOBILE_PHONES).path(mobilePhoneId).path(SERVICE_PATH_VERIFY)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(verificationCode).post(ClientResponse)
    }

    def updateMultiFactorSettings(String token, String userId, MultiFactor multiFactorSettings, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).entity(multiFactorSettings).put(ClientResponse)
    }

    def deleteMultiFactor(String token, String userId, MediaType requestContentMediaType = MediaType.APPLICATION_XML_TYPE, MediaType acceptMediaType = MediaType.APPLICATION_XML_TYPE) {
        resource.path(path20).path(USERS).path(userId)
                .path(RAX_AUTH).path(SERVICE_PATH_MULTI_FACTOR)
                .header(X_AUTH_TOKEN, token).accept(acceptMediaType.toString()).type(requestContentMediaType.toString()).delete(ClientResponse)
    }

    def addUserRole(String token, String userId, String roleId) {
        resource.path(path20).path(USERS).path(userId)
                .path(ROLES).path(OS_KSADM).path(roleId)
                .header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }

    def deleteRoleFromUserOnTenant(String token, String tenantId, String userId, String roleId) {
        resource.path(path20).path(TENANTS).path(tenantId).path(USERS).path(userId)
                .path(ROLES).path(OS_KSADM).path(roleId)
                .header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def listRolesForUserOnTenant(String token, String tenantId, String userId) {
        resource.path(path20).path(TENANTS).path(tenantId).path(USERS).path(userId).path(ROLES)
                .header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def addEndpoint(String token, String tenantId, endpointTemplate) {
        resource.path(path20).path(TENANTS).path(tenantId).path(OS_KSCATALOG).path(ENDPOINTS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(endpointTemplate).post(ClientResponse)
    }

    def removeRoleFromUser(String token, String roleId, String userId) {
        resource.path(path20).path(USERS).path(userId).path(ROLES).path(OS_KSADM).path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete()
    }

    def createQuestion(String token, question) {
        resource.path(path20).path(RAX_AUTH).path(SECRETQA).path(QUESTIONS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(question).post(ClientResponse)
    }

    def getQuestion(String token, questionId) {
        resource.path(path20).path(RAX_AUTH).path(SECRETQA).path(QUESTIONS).path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def getQuestionFromLocation(String token, location) {
        resource.uri(location).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def getQuestions(String token) {
        resource.path(path20).path(RAX_AUTH).path(SECRETQA).path(QUESTIONS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def updateQuestion(String token, String questionId, question) {
        resource.path(path20).path(RAX_AUTH).path(SECRETQA).path(QUESTIONS).path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(question).put(ClientResponse)
    }

    def deleteQuestion(String token, String questionId) {
        resource.path(path20).path(RAX_AUTH).path(SECRETQA).path(QUESTIONS).path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).delete(ClientResponse)
    }

    def addEndpointTemplate(String token, endpointTemplate) {
        resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(endpointTemplate).post(ClientResponse)
    }

    def deleteEndpointTemplate(String token, String endpointTemplateId) {
        resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).path(endpointTemplateId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addPolicy(String token, policy) {
        resource.path(path20).path(RAX_AUTH).path(POLICIES).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(policy).post(ClientResponse)
    }

    def updatePolicy(String token, String policyId, policy) {
        resource.path(path20).path(RAX_AUTH).path(POLICIES).path(policyId).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(policy).put(ClientResponse)
    }

    def getPolicy(String token, location) {
        resource.uri(location).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def deletePolicy(String token, policyId) {
        resource.path(path20).path(RAX_AUTH).path(POLICIES).path(policyId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addPolicyToEndpointTemplate(String token, endpointTemplateId, policyId) {
        resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).path(endpointTemplateId).path(RAX_AUTH).path(POLICIES).path(policyId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }

    def deletePolicyToEndpointTemplate(String token, endpointTemplateId, policyId) {
        resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).path(endpointTemplateId).path(RAX_AUTH).path(POLICIES).path(policyId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def getPoliciesFromEndpointTemplate(String token, endpointTemplateId) {
        resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).path(endpointTemplateId).path(RAX_AUTH).path(POLICIES).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def updatePoliciesForEndpointTemplate(String token, endpointTemplateId, policies) {
        resource.path(path20).path(OS_KSCATALOG).path(ENDPOINT_TEMPLATES).path(endpointTemplateId).path(RAX_AUTH).path(POLICIES).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(policies).put(ClientResponse)
    }

    def addTenant(String token, Tenant tenant) {
        resource.path(path20).path(TENANTS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(tenant).post(ClientResponse)
    }

    def getSecretQAs(String token, String userId){
        resource.path(path20).path(USERS).path(userId).path(RAX_AUTH).path(SECRETQAS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createSecretQA(String token, String userId, secretqa){
        resource.path(path20).path(USERS).path(userId).path(RAX_AUTH).path(SECRETQAS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(secretqa).post(ClientResponse)
    }

    def getSecretQA(String token, String userId){
        resource.path(path20).path(USERS).path(userId).path(RAX_KSQA).path(SECRETQA).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def updateSecretQA(String token, String userId, secretqa){
        resource.path(path20).path(USERS).path(userId).path(RAX_KSQA).path(SECRETQA).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(secretqa).put(ClientResponse)
    }

    def getRole(String token, String roleId) {
        resource.path(path20).path(OS_KSADM).path(ROLES).path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def validateToken(String token, String validateToken){
        resource.path(path20).path(TOKENS).path(validateToken).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def addDomain(String token, Domain domain) {
        resource.path(path20).path(RAX_AUTH).path("domains").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(domain).post(ClientResponse)
    }

    def updateDomain(String token, String domainId, domain) {
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(domain).put(ClientResponse)
    }

    def deleteDomain(String token, String domainId) {
        resource.path(path20).path(RAX_AUTH).path("domains").path(domainId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addTenantToDomain(String token, String domainId, String tenantId) {
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(TENANTS).path(tenantId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).put(ClientResponse)
    }

    def getDomainTenants(String token, String domainId, boolean enabled = true) {
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(TENANTS).queryParam(ENABLED, enabled.toString()).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getEndpointsByDomain(String token, String domainId) {
        resource.path(path20).path(RAX_AUTH).path(DOMAINS).path(domainId).path(ENDPOINTS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def listCredentials(String token, String userId){
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getGetPasswordCredentials(token, userId) {
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).path(RAX_KSKEY_API_KEY_CREDENTIALS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(credential).post ClientResponse
    }

    def impersonate(String token, User user, Integer expireTime) {
        def request = new ImpersonationRequest().with {
            it.user = new User().with {
                it.username = user.username
                it
            }
            it.expireInSeconds = expireTime
            it
        }
        resource.path(path20).path(RAX_AUTH).path("impersonation-tokens").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(request).post(ClientResponse)
    }

    def impersonate(String token, User user) {
        def request = new ImpersonationRequest().with {
            it.user = user
            it.expireInSeconds = 10800
            it
        }
        resource.path(path20).path(RAX_AUTH).path("impersonation-tokens").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(request).post(ClientResponse)
    }

    def revokeUserToken(String token, String tokenToRevoke) {
        resource.path(path20).path(TOKENS).path(tokenToRevoke).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def revokeToken(String token) {
        resource.path(path20).path(TOKENS).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def listRoles(String token, String serviceId, String masker, String limit) {
        def queryParams = new MultivaluedMapImpl()
        if (serviceId != null) {
            queryParams.add("serviceId", serviceId)
        }
        if (masker != null) {
            queryParams.add("marker", masker)
        }
        if (limit != null) {
            queryParams.add("limit", limit)
        }
        resource.path(path20).path(OS_KSADM).path(ROLES).queryParams(queryParams).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def updateCredentials(String token, String userId, creds) {
        resource.path(path20).path(USERS).path(userId).path(OS_KSADM).path(CREDENTIALS).path(PASSWORD_CREDENTIALS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(creds).post(ClientResponse)
    }

    def getAdminsForUser(String token, String userId) {
        resource.path(path20).path(USERS).path(userId).path(RAX_AUTH).path("admins").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createService(String token, service) {
        resource.path(path20).path(OS_KSADM).path("services").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(service).post(ClientResponse)
    }

    def deleteService(String token, String serviceId) {
        resource.path(path20).path(OS_KSADM).path("services").path(serviceId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def destroyUser(String token, String userId) {
        deleteUser(token, userId)
        hardDeleteUser(token, userId)
    }

    def pageParams(String offset, String limit) {
        new MultivaluedMapImpl().with {
            it.add("marker", offset)
            it.add("limit", limit)
            return it
        }
    }

    def getEndpointsForToken(String authToken, String token) {
        resource.path(path20).path(TOKENS).path(token).path(ENDPOINTS).header(X_AUTH_TOKEN, authToken).accept(APPLICATION_XML).get(ClientResponse)
    }
}
