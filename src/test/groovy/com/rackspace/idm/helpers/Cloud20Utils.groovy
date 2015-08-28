package com.rackspace.idm.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhones
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDomain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspace.idm.api.resource.cloud.v20.DefaultMultiFactorCloud20Service
import com.rackspace.idm.util.JSONReaderForRoles
import com.rackspace.idm.util.OTPHelper
import com.sun.jersey.api.client.ClientResponse
import com.unboundid.util.Base32
import groovy.json.JsonSlurper
import org.apache.http.HttpStatus
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA
import org.apache.http.client.utils.URLEncodedUtils
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.*
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.EndpointList
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.RoleList
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import spock.lang.Shared
import testHelpers.Cloud20Methods
import testHelpers.V1Factory
import testHelpers.V2Factory

import javax.annotation.PostConstruct
import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.*
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE
import static org.apache.http.HttpStatus.*

@Component
class Cloud20Utils {

    @Autowired
    Cloud20Methods methods

    @Autowired
    V2Factory factory

    @Autowired
    V1Factory v1Factory

    @Autowired
    CloudTestUtils testUtils

    @Autowired
    OTPHelper otpHelper

    @Shared
    def serviceAdminToken

    @PostConstruct
    def init() {
        methods.init()
    }

    def getToken(username, password=DEFAULT_PASSWORD) {
        def response = methods.authenticatePassword(username, password)
        assert (response.status == SC_OK)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)
        entity.token.id
    }

    def getMFAToken(username, passcode, password=DEFAULT_PASSWORD) {
        def response = methods.authenticatePassword(username, password)
        assert (response.status == SC_UNAUTHORIZED)
        def sessionId = extractSessionIdFromWwwAuthenticateHeader(response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE))
        response = methods.authenticateMFAWithSessionIdAndPasscode(sessionId, passcode)
        assert (response.status == SC_OK)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)
        entity.token.id
    }

    def authenticate(User user, password=DEFAULT_PASSWORD) {
        def response = methods.authenticatePassword(user.username, password)
        assert (response.status == SC_OK)
        return response.getEntity(AuthenticateResponse).value
    }

    def authenticateApiKey(User user, String apikey) {
        def response = methods.authenticateApiKey(user.username, apikey)
        assert (response.status == SC_OK)
        return response.getEntity(AuthenticateResponse).value
    }

    def authenticateApiKey(String username, String apikey = DEFAULT_API_KEY) {
        def response = methods.authenticateApiKey(username, apikey)
        assert (response.status == SC_OK)
        return response.getEntity(AuthenticateResponse).value
    }

    def getTokenFromApiKeyAuth(String username, String apikey = DEFAULT_API_KEY) {
        def response = methods.authenticateApiKey(username, apikey)
        assert (response.status == SC_OK)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)
        return entity.token.id
    }

    def getTokenFromAuthWithToken(token, tenantId) {
        def response = methods.authenticateTokenAndTenant(token, tenantId)
        assert(response.status == 200)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)
        return entity.token.id
    }

    def createUser(token, username=testUtils.getRandomUUID(), domainId=null) {
        def response = methods.createUser(token, factory.createUserForCreate(username, "display", "${username}@rackspace.com", true, null, domainId, DEFAULT_PASSWORD))

        assert (response.status == SC_CREATED)

        def entity = response.getEntity(User).value
        assert (entity != null)
        return entity
    }

    def createUserWithUser(user, username=testUtils.getRandomUUID(), domainId=null) {
        def token = authenticate(user).token.id
        def response = methods.createUser(token, factory.createUserForCreate(username, "display", "${username}@email.com", true, null, domainId, DEFAULT_PASSWORD))

        assert (response.status == SC_CREATED)

        def entity = response.getEntity(User).value
        assert (entity != null)
        return entity
    }

    def createUserWithTenantsAndRole(token, username=testUtils.getRandomUUID(), domainId, rolename, tenantId) {

        def user = factory.createUserForCreate(username, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD)
        user.secretQA = v1Factory.createRaxKsQaSecretQA()
        user.groups = new Groups()
        user.groups.group.add(v1Factory.createGroup("Default", "0" , null))
        user.roles = new RoleList()
        user.roles.role.add(v1Factory.createRole(rolename, tenantId))
        def response = methods.createUser(token, user)

        assert (response.status == SC_CREATED)

        def entity = response.getEntity(User).value
        assert (entity != null)
        return entity
    }

    def createUserWithTenants(token, username=testUtils.getRandomUUID(), domainId=null) {
        def user = factory.createUserForCreate(username, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD)
        user.secretQA = v1Factory.createRaxKsQaSecretQA()
        user.groups = new Groups()
        user.groups.group.add(v1Factory.createGroup("Default", "0" , null))
        def response = methods.createUser(token, user)

        assert (response.status == SC_CREATED)

        def entity = response.getEntity(User).value
        assert (entity != null)
        return entity
    }

    def addRoleToUser(user, roleId, token=getServiceAdminToken()) {
        def response = methods.addApplicationRoleToUser(token, roleId, user.id)

        assert (response.status == SC_OK)
    }

    def deleteUser(user) {
        def response = methods.deleteUser(getServiceAdminToken(), user.id)
        assert (response.status == SC_NO_CONTENT)
    }

    def createDomain() {
        testUtils.getRandomIntegerString()
    }

    def updateDomain(domainId, domain, String token=getServiceAdminToken()) {
        def response = methods.updateDomain(token, domainId, domain)
        assert (response.status == SC_OK)
    }

    def disableDomain(domainId) {
        def domainToUpdate = v1Factory.createDomain().with {
            it.id = domainId
            it.enabled = false
            it
        }
        updateDomain(domainId, domainToUpdate)
    }

    def deleteDomain(domainId) {
        if (domainId == null) {
            return
        }
        def response = methods.deleteDomain(getServiceAdminToken(), domainId)
        assert (response.status == SC_NO_CONTENT)
    }

    def Domain getDomain(domainId, String token=getServiceAdminToken()) {
        def response = methods.getDomain(token, domainId)
        assert (response.status == SC_OK)

        def entity = response.getEntity(Domain)
        assert (entity != null)
        return entity
    }

    def getServiceAdminToken() {
        if (serviceAdminToken == null) {
            serviceAdminToken = getToken(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PASSWORD)
        }
        return serviceAdminToken
    }

    def resetServiceAdminToken() {
        serviceAdminToken = getToken(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PASSWORD)
    }

    def getIdentityAdminToken() {
        getToken(IDENTITY_ADMIN_USERNAME, IDENTITY_ADMIN_PASSWORD)
    }

    def createIdentityAdmin() {
        def serviceAdminToken = getServiceAdminToken()
        return createUser(serviceAdminToken, testUtils.getRandomUUID("identityAdmin"))
    }

    def createUserAdminWithTenantsAndRole(domainId, rolename, tenantId) {
        def identityAdmin = createIdentityAdmin()

        def identityAdminToken = getToken(identityAdmin.username)

        def userAdmin = createUserWithTenantsAndRole(identityAdminToken, testUtils.getRandomUUID("userAdmin"), domainId, rolename, tenantId)

        return [userAdmin, [identityAdmin, userAdmin].asList()]
    }

    def createUserAdminWithTenants(domainId) {
        def identityAdmin = createIdentityAdmin()

        def identityAdminToken = getToken(identityAdmin.username)

        def userAdmin = createUserWithTenants(identityAdminToken, testUtils.getRandomUUID("userAdmin"), domainId)

        return [userAdmin, [identityAdmin, userAdmin].asList()]
    }

    def createUserAdmin(domainId=testUtils.getRandomIntegerString()) {
        def identityAdmin = createIdentityAdmin()

        def identityAdminToken = getToken(identityAdmin.username, DEFAULT_PASSWORD)

        def userAdmin = createUser(identityAdminToken, testUtils.getRandomUUID("userAdmin"), domainId)

        return [userAdmin, [identityAdmin, userAdmin].asList()]
    }

    def createDefaultUser(domainId) {
        def identityAdmin = createIdentityAdmin()

        def identityAdminToken = getToken(identityAdmin.username)

        def userAdmin = createUser(identityAdminToken, testUtils.getRandomUUID("userAdmin"), domainId)

        def userAdminToken  = getToken(userAdmin.username)

        def defaultUser = createUser(userAdminToken, testUtils.getRandomUUID("defaultUser"), domainId)

        return [defaultUser, [defaultUser, userAdmin, identityAdmin].asList()]
    }

    def createFederatedUser(domainId) {
        def identityAdmin = createIdentityAdmin()

        def identityAdminToken = getToken(identityAdmin.username)

        def userAdmin = createUser(identityAdminToken, testUtils.getRandomUUID("userAdmin"), domainId)

        def userAdminToken  = getToken(userAdmin.username)

        def defaultUser = createUser(userAdminToken, testUtils.getRandomUUID("defaultUser"), domainId)

        return [defaultUser, [defaultUser, userAdmin, identityAdmin].asList()]
    }


    def createUsers(domainId) {
        def identityAdmin = createIdentityAdmin()

        def identityAdminToken = getToken(identityAdmin.username, DEFAULT_PASSWORD)

        def userAdmin = createUser(identityAdminToken, testUtils.getRandomUUID("userAdmin"), domainId)
        def userAdminToken = getToken(userAdmin.username, DEFAULT_PASSWORD)

        def userManage = createUser(userAdminToken, testUtils.getRandomUUID("userManage"), domainId)
        addRoleToUser(userManage, USER_MANAGE_ROLE_ID)

        def defaultUser = createUser(userAdminToken, testUtils.getRandomUUID("defaultUser"), domainId)

        return [identityAdmin, userAdmin, userManage, defaultUser]
    }

    def deleteUsers(... users) {
        deleteUsers(users as List)
    }

    //delete users order matters.  pass default users first followed by user-managed, etc...
    def deleteUsers(List users) {
        for (User user : users) {
            if (user == null) {
                continue
            }
            deleteUser(user)
        }
    }

    def impersonateWithToken(token, user) {
        def response = methods.impersonate(token, user)
        assert (response.status == SC_OK)
        response.getEntity(ImpersonationResponse)
    }

    def impersonate(impersonator, user) {
        methods.impersonate(getToken(impersonator.username, DEFAULT_PASSWORD), user)
    }

    def impersonate(String token, User user, Integer expireTime) {
        def response = methods.impersonate(token, user, expireTime)
        assert (response.status == SC_OK)
        response.getEntity(ImpersonationResponse)
    }

    def impersonateWithRacker(user, expireTime = 10800) {
        def auth = authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD)
        def response = methods.impersonate(auth.token.id, user, expireTime)
        assert (response.status == SC_OK)
        response.getEntity(ImpersonationResponse)
    }

    def getImpersonatedToken(impersonator, user) {
        def response = impersonate(impersonator, user)

        assert (response.status == SC_OK)

        def entity = response.getEntity(ImpersonationResponse)
        assert (entity != null)
        entity.token.id
    }

    def updateUser(user, userId = user.id, MediaType requestMediaType = APPLICATION_XML_TYPE) {
        def response = methods.updateUser(getServiceAdminToken(), userId, user, APPLICATION_XML_TYPE, requestMediaType)
        assert (response.status == SC_OK)
        response.getEntity(User).value
    }

    def disableUser(user) {
        user.enabled = false
        updateUser(user)
    }

    def validateToken(token) {
        def response = methods.validateToken(getServiceAdminToken(), token)
        assert (response.status == SC_OK)
        response.getEntity(AuthenticateResponse).value
    }

    def validateToken(token, tokenToValidate) {
        def response = methods.validateToken(token, tokenToValidate)
        assert (response.status == SC_OK)
        response.getEntity(AuthenticateResponse).value
    }

    def revokeToken(token) {
        def response = methods.revokeUserToken(getServiceAdminToken(), token)
        assert (response.status == SC_NO_CONTENT)
    }

    def listRoles(token, serviceId, marker, limit) {
        def response = methods.listRoles(token, serviceId, marker, limit)
        assert (response.status == SC_OK)
        response.getEntity(RoleList).value
    }

    def listUserGlobalRoles(token, userId) {
        def response = methods.listUserGlobalRoles(token, userId)
        assert (response.status == SC_OK)
        response.getEntity(RoleList).value
    }

    def listEndpointsForTenant(token, tenantId) {
        def response = methods.listEndpointsForTenant(token, tenantId)
        assert (response.status == SC_OK)
        response.getEntity(EndpointList).value
    }


    def createService() {
        def serviceName = testUtils.getRandomUUID("service")
        def service = v1Factory.createService(serviceName, serviceName)
        def response = methods.createService(getServiceAdminToken(), service)
        assert (response.status == SC_CREATED)
        response.getEntity(Service)
    }

    def deleteService(service) {
        def response = methods.deleteService(getServiceAdminToken(), service.id)
        assert (response.status == SC_NO_CONTENT)
    }

    def createPropagatingRole(service=null) {
        def role = factory.createRole().with {
            it.name = testUtils.getRandomUUID("role")
            it.propagate = true
            it.serviceId = service == null ? null : service.id
            it
        }
        createRole(role)
    }

    def createRole(service=null) {
        def roleName = testUtils.getRandomUUID("role")
        def role = factory.createRole(roleName)
        if(service != null){
            role.serviceId = service.id
        }
        createRole(role)
    }

    def createRole(Role role) {
        def response = methods.createRole(getServiceAdminToken(), role)
        assert (response.status == SC_CREATED)
        response.getEntity(Role).value
    }

    def deleteRole(role) {
        def response = methods.deleteRole(getServiceAdminToken(), role.id)
        assert (response.status == SC_NO_CONTENT)
    }

    def createTenant(name=testUtils.getRandomUUID("tenant"), enabled=true, displayName=testUtils.getRandomUUID("tenant")) {
        def tenant = factory.createTenant(name, displayName, enabled)
        def response = methods.addTenant(getServiceAdminToken(), tenant)
        assert (response.status == SC_CREATED)
        response.getEntity(Tenant).value
    }

    def deleteTenant(def tenant) {
        def response = methods.deleteTenant(getServiceAdminToken(), tenant.id)
        assert (response.status == SC_NO_CONTENT)
    }

    def deleteTenantById(String tenantId) {
        def response = methods.deleteTenant(getServiceAdminToken(), tenantId)
        assert (response.status == SC_NO_CONTENT)
    }

    def addRoleToUserOnTenant(user, tenant, roleId=MOSSO_ROLE_ID) {
        def response = methods.addRoleToUserOnTenant(getServiceAdminToken(), tenant.id, user.id, roleId)
        assert (response.status == SC_OK)
    }

    def addRoleToUserOnTenantId(user, tenantId, roleId=MOSSO_ROLE_ID) {
        def response = methods.addRoleToUserOnTenant(getServiceAdminToken(), tenantId, user.id, roleId)
        assert (response.status == SC_OK)
    }

    def createEndpointTemplate(EndpointTemplate endpointTemplate) {
        def response = methods.addEndpointTemplate(getServiceAdminToken(), endpointTemplate)
        assert (response.status == SC_CREATED)
        response.getEntity(EndpointTemplate).value
    }

    def updateEndpointTemplate(EndpointTemplate endpointTemplate, String endpointId) {
        def response = methods.updateEndpointTemplate(getServiceAdminToken(), endpointId, endpointTemplate)
        assert (response.status == SC_OK)
        response.getEntity(EndpointTemplate).value
    }

    def createAndUpdateEndpointTemplate(EndpointTemplate endpointTemplate, String endpointId) {
        createEndpointTemplate(endpointTemplate)
        updateEndpointTemplate(endpointTemplate, endpointId)
    }

    def createEndpointTemplate(global=false, tenantAlias=null, enabled=true, type="compute", region="ORD", id=testUtils.getRandomIntegerString(), publicUrl=testUtils.getRandomUUID("http://"), name="cloudServers") {
        def endpointTemplate =v1Factory.createEndpointTemplate(id, type, publicUrl, name).with {
            it.global = global
            it.region = region
            it.tenantAlias = tenantAlias
            it.enabled = enabled
            it
        }
        def endpointTemplateResp = createEndpointTemplate(endpointTemplate)

        if(global || enabled) {
            endpointTemplateResp = methods.updateEndpointTemplate(getServiceAdminToken(), id, endpointTemplate).getEntity(EndpointTemplate).value
        }

        return endpointTemplateResp
    }

    def deleteEndpointTemplate(endpointTemplate) {
        def response = methods.deleteEndpointTemplate(getServiceAdminToken(), endpointTemplate.id.toString())
        assert (response.status == SC_NO_CONTENT)
    }

    def authenticateUser(String username, String password = DEFAULT_PASSWORD) {
        def response = methods.authenticatePassword(username, password)
        assert (response.status == SC_OK)
        response.getEntity(AuthenticateResponse).value
    }

    def authenticateRacker(String racker, String password) {
        def response = methods.authenticateRacker(racker, password)
        assert (response.status == SC_OK)
        response.getEntity(AuthenticateResponse).value
    }

    def createGroup() {
        def group = v1Factory.createGroup(testUtils.getRandomUUID('group'), "description")
        def response = methods.createGroup(getServiceAdminToken(), group)
        assert (response.status == SC_CREATED)
        response.getEntity(Group).value
    }

    def deleteGroup(group) {
        def response = methods.deleteGroup(getServiceAdminToken(), group.id)
        assert (response.status == SC_NO_CONTENT)
    }

    def addCredentialToUser(userId, password=testUtils.getRandomUUID()) {
        def passwordCred = factory.createPasswordCredentialsBase(userId, password)
        methods.addCredential(getServiceAdminToken(), userId, passwordCred)
    }

    def addApiKeyToUser(User user, String apiKey = DEFAULT_API_KEY) {
        def credentials = v1Factory.createApiKeyCredentials(user.username, apiKey)
        def response = methods.addApiKeyToUser(getServiceAdminToken(), user.id, credentials)
        assert (response.status == SC_OK)
        response.getEntity(ApiKeyCredentials)
    }

    def addApiKeyToUser(com.rackspace.idm.domain.entity.User user, String apiKey = DEFAULT_API_KEY) {
        def credentials = v1Factory.createApiKeyCredentials(user.username, apiKey)
        def response = methods.addApiKeyToUser(getServiceAdminToken(), user.id, credentials)
        assert (response.status == SC_OK)
        response.getEntity(ApiKeyCredentials)
    }

    def getUserApiKey(User user, String token=getServiceAdminToken()){
        def response = methods.getUserApiKey(token, user.id)
        assert (response.status == SC_OK)
        response.getEntity(ApiKeyCredentials)
    }

    def listUserCredentials(User user, String token=getServiceAdminToken()){
        def response = methods.listCredentials(token, user.id)
        assert (response.status == SC_OK)
        response.getEntity(CredentialListType).value
    }

    def getUserById(String id, String token=getServiceAdminToken(), MediaType mediaType = APPLICATION_XML_TYPE){
        def response = methods.getUserById(token, id, mediaType)
        assert (response.status == SC_OK)
        if (mediaType == APPLICATION_XML_TYPE) {
            return response.getEntity(User).value
        } else {
            return new ObjectMapper().readValue(response.getEntity(String), Map)
        }
    }

    def User getUserByIdReturnUser(String id, String token=getServiceAdminToken(), MediaType mediaType = APPLICATION_XML_TYPE){
        def response = methods.getUserById(token, id, mediaType)
        assert (response.status == SC_OK)
        def entity = response.getEntity(User)
        if (mediaType == APPLICATION_XML_TYPE) {
            return entity.value
        } else {
            return entity
        }
    }

    def User getUserByName(String username, String token=getServiceAdminToken(), MediaType mediaType = APPLICATION_XML_TYPE){
        def response = methods.getUserByName(token, username, mediaType)
        assert (response.status == SC_OK)
        def entity = response.getEntity(User)
        if (mediaType == APPLICATION_XML_TYPE) {
            return entity.value
        } else {
            return entity
        }
    }

    def getUsersByDomainId(String domainId, String token=getServiceAdminToken(), MediaType mediaType = APPLICATION_XML_TYPE) {
        def response = methods.getUsersByDomainId(token, domainId, mediaType)
        assert(response.status == SC_OK)
        if (mediaType == APPLICATION_XML_TYPE) {
            List<User> users = response.getEntity(UserList).value.user
            return users
        } else {
            return new ObjectMapper().readValue(response.getEntity(String), Map)
        }
    }

    def getUsersByEmail(String email, String token=getServiceAdminToken(), MediaType mediaType = APPLICATION_XML_TYPE){
        def response = methods.getUsersByEmail(token, email, mediaType)
        assert (response.status == SC_OK)
        if (mediaType == APPLICATION_XML_TYPE) {
            List<User> users = response.getEntity(UserList).value.user
            return users
        } else {
            return new ObjectMapper().readValue(response.getEntity(String), Map)
        }
    }

    def getUserByEmail(String email, String token=getServiceAdminToken()){
        def users = getUsersByEmail(email, token)
        assert (users.size() == 1)
        users.get(0)
    }

    def listUsers(String token=getServiceAdminToken()){
        def response = methods.listUsers(token)
        assert (response.status == SC_OK)
        List<User> users = response.getEntity(UserList).value.user
        users
    }

    def addUserToGroup(Group group, User user, String token=getServiceAdminToken()) {
        def response = methods.addUserToGroup(token, group.id, user.id)
        assert (response.status == SC_NO_CONTENT)
    }

    def resetApiKey(User user, String token=getServiceAdminToken()) {
        def response = methods.resetUserApiKey(token, user.id)
        assert (response.status == SC_OK)
    }

    def createSecretQA(User user, String token=getServiceAdminToken()) {
        def secretqa = v1Factory.createSecretQA(DEFAULT_SECRET_QUESTION_ID, DEFAULT_SECRET_ANWSER)
        def response = methods.createSecretQA(token, user.id, secretqa)
        assert (response.status == SC_OK)
    }

    def getSecretQA(User user, String token=getServiceAdminToken()) {
        def response = methods.getSecretQA(token, user.id)
        assert (response.status == SC_OK)
        response.getEntity(SecretQA)
    }

    def updateSecretQA(User user, String token=getServiceAdminToken()) {
        def secretqa = v1Factory.createRaxKsQaSecretQA(user.username, DEFAULT_RAX_KSQA_SECRET_ANWSER, DEFAULT_RAX_KSQA_SECRET_QUESTION)
        def response = methods.updateSecretQA(token, user.id, secretqa)
        assert (response.status == SC_OK)
    }

    def addPhone(token, userId, com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone mobilePhone = factory.createMobilePhone()) {
        def response = methods.addPhoneToUser(token, userId, mobilePhone)
        assert(response.status == SC_CREATED)
        response.getEntity(com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone)
    }

    def getPhoneFromUser(token, userId, mobilePhoneId) {
        def response = methods.getPhoneFromUser(token, userId, mobilePhoneId)
        assert(response.status == SC_OK)
        response.getEntity(com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone)
    }

    def deletePhoneFromUser(token, userId, mobilePhoneId) {
        def response = methods.deletePhoneFromUser(token, userId, mobilePhoneId)
        assert(response.status == SC_NO_CONTENT)
        response
    }

    def addOTPDevice(token, userId, OTPDevice otpDevice) {
        def response = methods.addOTPDeviceToUser(token, userId, otpDevice)
        assert(response.status == SC_CREATED)
        response.getEntity(com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice)
    }

    def getOTPDevice(token, userId, deviceId) {
        def response = methods.getOTPDeviceFromUser(token, userId, deviceId)
        assert(response.status == SC_OK)
        response.getEntity(com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice)
    }

    def getOTPDevices(token, userId) {
        def response = methods.getOTPDevicesFromUser(token, userId)
        assert(response.status == SC_OK)
        response.getEntity(com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice)
    }

    def deleteOTPDeviceFromUser(token, userId, deviceId) {
        def response = methods.deleteOTPDeviceFromUser(token, userId, deviceId)
        assert(response.status == SC_NO_CONTENT)
    }

    def verifyOTPDevice(token, userId, deviceId, com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode verificationCode) {
        def response = methods.verifyOTPDevice(token, userId, deviceId, verificationCode)
        assert (response.status == HttpStatus.SC_NO_CONTENT)
    }

    def sendVerificationCodeToPhone(token, userId, mobilePhoneId) {
        def response = methods.sendVerificationCode(token, userId, mobilePhoneId)
        assert (response.status == SC_ACCEPTED)
    }

    def verifyPhone(token, userId, mobilePhoneId, com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode verificationCode) {
        def response = methods.verifyVerificationCode(token, userId, mobilePhoneId, verificationCode)
        assert (response.status == HttpStatus.SC_NO_CONTENT)
    }

    def updateMultiFactor(token, userId, MultiFactor settings) {
        def response = methods.updateMultiFactorSettings(token, userId, settings)
        assert (response.status == HttpStatus.SC_NO_CONTENT)
    }

    def updateMultiFactorDomainSettings(token, domainId, MultiFactorDomain settings) {
        def response = methods.updateMultiFactorDomainSettings(token, domainId, settings)
        assert (response.status == HttpStatus.SC_NO_CONTENT)
    }

    def deleteMultiFactor(token, userId) {
        def response = methods.deleteMultiFactor(token, userId)
        assert (response.status == HttpStatus.SC_NO_CONTENT)
    }

    def deleteUserQuietly(user, String token=getServiceAdminToken()) {
        if (user != null) {
            try {
                methods.destroyUser(token, user.getId())
            } catch (all) {
                //ignore
            }
        }
    }

    def createDomain(Domain domain) {
        def response = methods.addDomain(getServiceAdminToken(), domain)
        assert (response.status == SC_CREATED)
    }

    def addUserToDomain(token = getServiceAdminToken(), userId, domainId) {
        def response = methods.addUserToDomain(token, userId, domainId)
        assert (response.status == SC_NO_CONTENT)
    }

    def createTenant(Tenant tenant) {
        def response = methods.addTenant(getServiceAdminToken(), tenant)
        assert (response.status == SC_CREATED)
        response.getEntity(Tenant).value
    }

    def deleteTenant(String tenantId) {
        def response = methods.deleteTenant(getServiceAdminToken(), tenantId)
        assert (response.status == SC_NO_CONTENT)
    }

    def getTenant(String tenantId) {
        def response = methods.getTenant(getServiceAdminToken(), tenantId)
        assert (response.status == SC_OK)
        response.getEntity(Tenant).value
    }

    def updateTenant(String tenantId, boolean enabled) {
        def tenant = getTenant(tenantId)
        tenant.enabled = enabled
        def response = methods.updateTenant(getServiceAdminToken(), tenantId, tenant)
        assert (response.status == SC_OK)
        response.getEntity(Tenant).value
    }

    def addTenantToDomain(String domainId, String tenantId) {
        def response = methods.addTenantToDomain(getServiceAdminToken(), domainId, tenantId)
        assert (response.status == SC_NO_CONTENT)
    }

    def getEndpointsByDomain(String domainId) {
        def response = methods.getEndpointsByDomain(getServiceAdminToken(), domainId)
        assert (response.status == SC_OK)
        response.getEntity(EndpointList).value

    }

    def extractSessionIdFromWwwAuthenticateHeader(String headerValue) {
        def matcher = ( headerValue =~ DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE_VALUE_SESSIONID_REGEX )
        matcher[0][1]
    }

    def extractFactorFromWwwAuthenticateHeader(String headerValue) {
        def matcher = ( headerValue =~ DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE_VALUE_SESSIONID_REGEX )
        matcher[0][2]
    }

    def listDevices(user, token=getToken(user.username)) {
        def response = methods.listDevices(token, user.id)
        assert (response.status = SC_OK)
        response.getEntity(MobilePhones)
    }

    def getNastTenant(String domainId){
        return NAST_TENANT_PREFIX.concat(domainId)
    }

    def getEndpointsForToken(String token) {
        def response = methods.getEndpointsForToken(getServiceAdminToken(), token)
        assert (response.status == SC_OK)
        response.getEntity(EndpointList).value
    }

    def boolean checkUsersMFAFlag(ClientResponse usersResponse, String username, Boolean test) {
        String body = usersResponse.getEntity(String.class)
        def slurper;
        if (usersResponse.getType() == MediaType.APPLICATION_XML_TYPE) {
            def root = new XmlSlurper(false, true).parseText(body)
            slurper = root.user.findAll({ it -> it.getProperty('@username') == username })[0]
        } else {
            def root = new JsonSlurper().parseText(body)
            slurper = root.users.findAll({ it -> it.username == username })[0]
        }
        return checkUserMFAFlagSlurper(slurper, usersResponse.getType(), test)
    }

    def boolean checkUserMFAFlag(ClientResponse userResponse, Boolean test) {
        String body = userResponse.getEntity(String.class)
        def slurper;
        if (userResponse.getType() == MediaType.APPLICATION_XML_TYPE) {
            slurper = new XmlSlurper(false, true).parseText(body)
        } else {
            slurper = new JsonSlurper().parseText(body).user
        }
        return checkUserMFAFlagSlurper(slurper, userResponse.getType(), test)
    }

    def boolean checkUserMFAFlagSlurper(def slurper, MediaType mediaType, Boolean test) {
        if (slurper == null) {
            return false;
        } else if (mediaType == MediaType.APPLICATION_XML_TYPE) {
            return slurper.getProperty('@rax-auth:multiFactorEnabled') == test
        } else {
            return slurper.'RAX-AUTH:multiFactorEnabled' == test
        }
        return false
    }


    def MobilePhone setUpAndEnableUserForMultiFactorSMS(String userToken, User user) {
        MobilePhone phone = addVerifiedMobilePhoneToUser(userToken, user)
        updateMultiFactor(userToken, user.id, factory.createMultiFactorSettings(true))
        return phone
    }

    def OTPDevice setUpAndEnableUserForMultiFactorOTP(String userToken, User user) {
        OTPDevice device = addVerifiedOTPDeviceToUser(userToken, user)
        updateMultiFactor(userToken, user.id, factory.createMultiFactorSettings(true))
        return device
    }

    def MobilePhone addVerifiedMobilePhoneToUser(String userToken, User user) {
        MobilePhone phone = addMobilePhoneToUser(userToken, user)
        sendVerificationCodeToPhone(userToken, user.id, phone.id)
        def constantVerificationCode = factory.createVerificationCode(MFA_DEFAULT_PIN);
        verifyPhone(userToken, user.id, phone.id, constantVerificationCode)
        return phone
    }

    def OTPDevice addVerifiedOTPDeviceToUser(String userToken, User user) {
        OTPDevice device = addOtpDeviceToUser(userToken, user)
        verifyOTPDevice(userToken, user.id, device.id, getOTPVerificationCodeForDevice(device))
        return device
    }

    def MobilePhone addMobilePhoneToUser(String userToken, User user) {
        return addPhone(userToken, user.id)
    }


    def OTPDevice addOtpDeviceToUser(String userToken, User user) {
        OTPDevice device = new OTPDevice()
        device.setName("test-" + UUID.randomUUID().toString().replaceAll("-", ""))
        return addOTPDevice(userToken, user.id, device)
    }

    def VerificationCode getOTPVerificationCodeForDevice(OTPDevice device) {
        final VerificationCode verificationCode = new VerificationCode()
        verificationCode.code = getOTPCodeForDevice(device)
        return verificationCode
    }

    def getOTPCodeForDevice(OTPDevice device) {
        def secret = Base32.decode(URLEncodedUtils.parse(new URI(device.getKeyUri()), "UTF-8").find { it.name == 'secret' }.value)
        return otpHelper.TOTP(secret)
    }

    def String authenticateWithOTPDevice(User user, OTPDevice otpDevice) {
        //get MFA OTP token
        def response = methods.authenticate(user.username, DEFAULT_PASSWORD)
        String wwwHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = extractSessionIdFromWwwAuthenticateHeader(wwwHeader)
        def passcode = getOTPCodeForDevice(otpDevice)
        def mfaAuthResponse = methods.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, passcode)
        Token token = mfaAuthResponse.getEntity(AuthenticateResponse).value.token
        def userAdminToken = token.id
        return userAdminToken
    }
}
