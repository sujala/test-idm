package testHelpers

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest
import com.rackspace.idm.JSONConstants
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.core.util.MultivaluedMapImpl
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import org.springframework.stereotype.Component
import spock.lang.Shared

import static com.rackspace.idm.JSONConstants.*
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static javax.ws.rs.core.MediaType.APPLICATION_XML

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

    static def RAX_GRPADM= "RAX-GRPADM"
    static def RAX_AUTH = "RAX-AUTH"
    static def RAX_KSQA = "RAX-KSQA"
    static def OS_KSCATALOG = "OS-KSCATALOG"
    static def X_AUTH_TOKEN = "X-Auth-Token"

    def init(){
        this.resource = ensureGrizzlyStarted("classpath:app-config.xml")
    }

    def authenticate(username, password) {
        def credentials = v2Factory.createPasswordAuthenticationRequest(username, password)
        resource.path(path20).path("tokens").accept(APPLICATION_XML).type(APPLICATION_XML).entity(credentials).post(ClientResponse)
    }

    def authenticateRacker(username, password){
        def credentials = v2Factory.createPasswordAuthenticationRequest(username, password)
        credentials.domain =  new Domain().with {
            it.name = "Rackspace"
            it
        }
        resource.path(path20).path("tokens").accept(APPLICATION_XML).type(APPLICATION_XML).entity(credentials).post(ClientResponse)
    }

    def validateToken(authToken, token) {
        resource.path(path20).path("tokens").path(token).header(X_AUTH_TOKEN, authToken).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getUserByName(String token, String name) {
        resource.path(path20).path("users").queryParam("name", name).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def createUser(String token, user) {
        resource.path(path20).path("users").accept(APPLICATION_XML).type(APPLICATION_XML).header(X_AUTH_TOKEN, token).entity(user).post(ClientResponse)
    }

    def addApiKeyToUser(String token, String userId, credential) {
        resource.path(path20).path("users").path(userId).path("OS-KSADM/credentials/RAX-KSKEY:apiKeyCredentials").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(credential).post(ClientResponse)
    }

    def hardDeleteUser(String token, String userId) {
        resource.path(path20).path("softDeleted").path("users").path(userId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def deleteTenant(String token, String tenantId) {
        resource.path(path20).path("tenants").path(tenantId).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def getUserApiKey(String token, String userId) {
        resource.path(path20).path("users").path(userId).path("OS-KSADM/credentials/RAX-KSKEY:apiKeyCredentials").accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def deleteUserApiKey(String token, String userId) {
        resource.path(path20).path("users").path(userId).path("OS-KSADM").path("credentials").path(RAX_KSKEY_API_KEY_CREDENTIALS).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def getUser(String token, URI location) {
        resource.uri(location).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def listUsers(String token) {
        resource.path(path20).path("users").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def listUsers(String token, offset, limit) {
        resource.path(path20).path("users").queryParams(pageParams(offset, limit)).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getUserById(String token, String userId) {
        resource.path(path20).path('users').path(userId).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUsersByEmail(String token, String email) {
        resource.path(path20).path('users').queryParam("email", email).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUsersByDomainId(String token, String domainId) {
        resource.path(path20).path('RAX-AUTH').path('domains').path(domainId).path('users').accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def updateUser(String token, String userId, user) {
        resource.path(path20).path('users').path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).post(ClientResponse)
    }

    def addCredential(String token, String userId, credential) {
        resource.path(path20).path('users').path(userId).path('OS-KSADM').path('credentials').entity(credential).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).post(ClientResponse)
    }

    def deleteUser(String token, String userId) {
        resource.path(path20).path('users').path(userId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def createGroup(String token, group) {
        resource.path(path20).path(RAX_GRPADM).path('groups').header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(group).post(ClientResponse)
    }

    def getGroup(String token, URI uri) {
        resource.uri(uri).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getGroupById(String token, String groupId) {
        resource.path(path20).path(RAX_GRPADM).path('groups').path(groupId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getGroupByName(String token, String name) {
        resource.path(path20).path(RAX_GRPADM).path('groups').queryParam("name", name).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getGroups(String token) {
        resource.path(path20).path(RAX_GRPADM).path('groups').accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def updateGroup(String token, String groupId, group) {
        resource.path(path20).path(RAX_GRPADM).path('groups').path(groupId).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(group).put(ClientResponse)
    }

    def deleteGroup(String  token, String groupId) {
        resource.path(path20).path(RAX_GRPADM).path('groups').path(groupId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addUserToGroup(String token, String groupId, String userId) {
        resource.path(path20).path(RAX_GRPADM).path('groups').path(groupId).path("users").path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }
    def removeUserFromGroup(String token, String groupId, String userId) {
        resource.path(path20).path(RAX_GRPADM).path('groups').path(groupId).path("users").path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def listGroupsForUser(String token, String userId) {
        resource.path(path20).path('users').path(userId).path("RAX-KSGRP").accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUsersFromGroup(String token, String groupId) {
        resource.path(path20).path(RAX_GRPADM).path('groups').path(groupId).path("users").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def authenticatePassword(String username, String password) {
        authenticate(v2Factory.createPasswordAuthenticationRequest(username, password))
    }

    def authenticateApiKey(String username, String apiKey) {
        authenticate(v2Factory.createApiKeyAuthenticationRequest(username, apiKey))
    }

    def authenticate(request) {
        resource.path(path20).path('tokens').accept(APPLICATION_XML).type(APPLICATION_XML).entity(request).post(ClientResponse)
    }

    def createRegion(String token, region) {
        resource.path(path20).path(RAX_AUTH).path("regions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(region).post(ClientResponse)
    }

    def getRegion(String token, String regionId) {
        resource.path(path20).path(RAX_AUTH).path("regions").path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getRegions(String token) {
        resource.path(path20).path(RAX_AUTH).path("regions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def updateRegion(String token, String regionId, region) {
        resource.path(path20).path(RAX_AUTH).path("regions").path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(region).put(ClientResponse)
    }

    def deleteRegion(String token, String regionId) {
        resource.path(path20).path(RAX_AUTH).path("regions").path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def listUsersWithRole(String token, String roleId) {
        resource.path(path20).path("OS-KSADM/roles").path(roleId).path("RAX-AUTH/users").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def listUsersWithRole(String token, String roleId, String offset, String limit) {
        resource.path(path20).path("OS-KSADM/roles").path(roleId).path("RAX-AUTH/users").queryParams(pageParams(offset, limit)).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def listUsersWithTenantId(String token, tenantId) {
        resource.path(path20).path("tenants").path(tenantId).path("users").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createRole(String token, Role role) {
        resource.path(path20).path("OS-KSADM/roles").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(role).post(ClientResponse)
    }

    def deleteRole(String token, String roleId) {
        resource.path(path20).path("OS-KSADM/roles").path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addApplicationRoleToUser(String token, String roleId, String userId) {
        resource.path(path20).path("users").path(userId).path("roles/OS-KSADM").path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }

    def listUserGlobalRoles(String token, String userId) {
        resource.path(path20).path("users").path(userId).path("roles").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def getUserApplicationRole(String token, String roleId, String userId) {
        resource.path(path20).path("users").path(userId).path("roles/OS-KSADM").path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def deleteApplicationRoleFromUser(String token, String roleId, String userId) {
        resource.path(path20).path("users").path(userId).path("roles/OS-KSADM").path(roleId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def addRoleToUserOnTenant(String token, String tenantId, String userId, String roleId) {
        resource.path(path20).path("tenants").path(tenantId).path("users").path(userId)
                .path("roles").path("OS-KSADM").path(roleId)
                .header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }

    def deleteRoleFromUserOnTenant(String token, String tenantId, String userId, String roleId) {
        resource.path(path20).path("tenants").path(tenantId).path("users").path(userId)
                .path("roles").path("OS-KSADM").path(roleId)
                .header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def listRolesForUserOnTenant(String token, String tenantId, String userId) {
        resource.path(path20).path("tenants").path(tenantId).path("users").path(userId).path("roles")
                .header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def addEndpoint(String token, String tenantId, endpointTemplate) {
        resource.path(path20).path("tenants").path(tenantId).path(OS_KSCATALOG).path("endpoints").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(endpointTemplate).post(ClientResponse)
    }

    def removeRoleFromUser(String token, String roleId, String userId) {
        resource.path(path20).path("users").path(userId).path("roles/OS-KSADM").path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete()
    }

    def createQuestion(String token, question) {
        resource.path(path20).path(RAX_AUTH).path("secretqa/questions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(question).post(ClientResponse)
    }

    def getQuestion(String token, questionId) {
        resource.path(path20).path(RAX_AUTH).path("secretqa/questions").path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def getQuestionFromLocation(String token, location) {
        resource.uri(location).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def getQuestions(String token) {
        resource.path(path20).path(RAX_AUTH).path("secretqa/questions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def updateQuestion(String token, String questionId, question) {
        resource.path(path20).path(RAX_AUTH).path("secretqa/questions").path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(question).put(ClientResponse)
    }

    def deleteQuestion(String token, String questionId) {
        resource.path(path20).path(RAX_AUTH).path("secretqa/questions").path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).delete(ClientResponse)
    }

    def addEndpointTemplate(String token, endpointTemplate) {
        resource.path(path20).path(OS_KSCATALOG).path("endpointTemplates").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(endpointTemplate).post(ClientResponse)
    }

    def deleteEndpointTemplate(String token, String endpointTemplateId) {
        resource.path(path20).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addPolicy(String token, policy) {
        resource.path(path20).path(RAX_AUTH).path("policies").header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(policy).post(ClientResponse)
    }

    def updatePolicy(String token, String policyId, policy) {
        resource.path(path20).path(RAX_AUTH).path("policies").path(policyId).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(policy).put(ClientResponse)
    }

    def getPolicy(String token, location) {
        resource.uri(location).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def deletePolicy(String token, policyId) {
        resource.path(path20).path(RAX_AUTH).path("policies").path(policyId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addPolicyToEndpointTemplate(String token, endpointTemplateId, policyId) {
        resource.path(path20).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).path(RAX_AUTH).path("policies").path(policyId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }

    def deletePolicyToEndpointTemplate(String token, endpointTemplateId, policyId) {
        resource.path(path20).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).path(RAX_AUTH).path("policies").path(policyId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def getPoliciesFromEndpointTemplate(String token, endpointTemplateId) {
        resource.path(path20).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).path(RAX_AUTH).path("policies").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def updatePoliciesForEndpointTemplate(String token, endpointTemplateId, policies) {
        resource.path(path20).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).path(RAX_AUTH).path("policies").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(policies).put(ClientResponse)
    }

    def addTenant(String token, Tenant tenant) {
        resource.path(path20).path("tenants").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(tenant).post(ClientResponse)
    }

    def getSecretQAs(String token, String userId){
        resource.path(path20).path('users').path(userId).path(RAX_AUTH).path('secretqas').header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createSecretQA(String token, String userId, secretqa){
        resource.path(path20).path('users').path(userId).path(RAX_AUTH).path('secretqas').header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(secretqa).post(ClientResponse)
    }

    def getSecretQA(String token, String userId){
        resource.path(path20).path('users').path(userId).path(RAX_KSQA).path('secretqa').header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def updateSecretQA(String token, String userId, secretqa){
        resource.path(path20).path('users').path(userId).path(RAX_KSQA).path('secretqa').header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(secretqa).put(ClientResponse)
    }

    def getRole(String token, String roleId) {
        resource.path(path20).path('OS-KSADM/roles').path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def validateToken(String token, String validateToken){
        resource.path(path20).path("tokens").path(validateToken).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def deleteDomain(String token, String domainId) {
        resource.path(path20).path("RAX-AUTH").path("domains").path(domainId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def listCredentials(String token, String userId){
        resource.path(path20).path("users").path(userId).path("OS-KSADM").path("credentials").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
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
        resource.path(path20).path("RAX-AUTH/impersonation-tokens").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(request).post(ClientResponse)
    }

    def impersonate(String token, User user) {
        def request = new ImpersonationRequest().with {
            it.user = user
            it.expireInSeconds = 10800
            it
        }
        resource.path(path20).path("RAX-AUTH/impersonation-tokens").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(request).post(ClientResponse)
    }

    def revokeUserToken(String token, String tokenToRevoke) {
        resource.path(path20).path("tokens").path(tokenToRevoke).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def listRoles(String token, String serviceId, String offset, String limit) {
        def queryParams = new MultivaluedMapImpl()
        if (serviceId != null) {
            queryParams.add("serviceId", serviceId)
        }
        if (offset != null) {
            queryParams.add("marker", offset)
        }
        if (limit != null) {
            queryParams.add("limit", limit)
        }
        resource.path(path20).path("OS-KSADM/roles").queryParams(queryParams).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def updateCredentials(String token, String userId, creds) {
        resource.path(path20).path("users").path(userId).path("OS-KSADM").path("credentials").path(PASSWORD_CREDENTIALS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(creds).post(ClientResponse)
    }

    def getAdminsForUser(String token, String userId) {
        resource.path(path20).path("users").path(userId).path("RAX-AUTH").path("admins").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createService(String token, service) {
        resource.path(path20).path("OS-KSADM").path("services").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(service).post(ClientResponse)
    }

    def deleteService(String token, String serviceId) {
        resource.path(path20).path("OS-KSADM").path("services").path(serviceId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
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
}
