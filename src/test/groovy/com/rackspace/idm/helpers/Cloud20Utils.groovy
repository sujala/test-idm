package com.rackspace.idm.helpers

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhones
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspace.idm.api.resource.cloud.v20.DefaultMultiFactorCloud20Service
import com.rackspace.idm.domain.entity.MobilePhone
import com.sun.jersey.api.client.GenericType
import org.apache.http.HttpStatus
import org.apache.xml.resolver.apps.resolver
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA

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

import static com.rackspace.idm.Constants.*
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

    def createUser(token, username=testUtils.getRandomUUID(), domainId=null) {
        def response = methods.createUser(token, factory.createUserForCreate(username, "display", "${username}@email.com", true, null, domainId, DEFAULT_PASSWORD))

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
        testUtils.getRandomUUID("domain")
    }

    def updateDomain(domainId, domain, String token=getServiceAdminToken()) {
        def response = methods.updateDomain(token, domainId, domain)
        assert (response.status == SC_OK)
    }

    def deleteDomain(domainId) {
        if (domainId == null) {
            return
        }
        def response = methods.deleteDomain(getServiceAdminToken(), domainId)
        assert (response.status == SC_NO_CONTENT)
    }

    def getServiceAdminToken() {
        if (serviceAdminToken == null) {
            serviceAdminToken = getToken(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PASSWORD)
        }
        return serviceAdminToken
    }

    def getIdentityAdminToken() {
        getToken(IDENTITY_ADMIN_USERNAME, IDENTITY_ADMIN_PASSWORD)
    }

    def createIdentityAdmin() {
        def serviceAdminToken = getServiceAdminToken()
        return createUser(serviceAdminToken, testUtils.getRandomUUID("identityAdmin"))
    }

    def createUserAdmin(domainId) {
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

    def impersonateWithRacker(user) {
        def auth = authenticateRacker(RACKER, RACKER_PASSWORD)
        def response = methods.impersonate(auth.token.id, user)
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

    def updateUser(user) {
        def response = methods.updateUser(getServiceAdminToken(), user.id, user)

        assert (response.status == SC_OK)

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

    def revokeToken(token) {
        def response = methods.revokeUserToken(getServiceAdminToken(), token)
        assert (response.status == SC_NO_CONTENT)
    }

    def listRoles(token, serviceId, marker, limit) {
        def response = methods.listRoles(token, serviceId, marker, limit)
        assert (response.status == SC_OK)
        response.getEntity(RoleList).value
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

    def createRole(service=null) {
        def roleName = testUtils.getRandomUUID("role")
        def role = factory.createRole(roleName)
        if(service != null){
            role.serviceId = service.id
        }
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

    def createEndpointTemplate(EndpointTemplate endpointTemplate) {
        def response = methods.addEndpointTemplate(getServiceAdminToken(), endpointTemplate)
        assert (response.status == SC_CREATED)
        response.getEntity(EndpointTemplate).value
    }

    def createEndpointTemplate(global=false, tenantAlias=null, type="compute", region="ORD", id=testUtils.getRandomIntegerString(), publicUrl=testUtils.getRandomUUID("http://"), name=testUtils.getRandomUUID("name")) {
        def endpointTemplate =v1Factory.createEndpointTemplate(id, type, publicUrl, name).with {
            it.global = global
            it.region = region
            it.tenantAlias = tenantAlias
            it
        }
        createEndpointTemplate(endpointTemplate)
    }

    def deleteEndpointTemplate(endpointTemplate) {
        def response = methods.deleteEndpointTemplate(getServiceAdminToken(), endpointTemplate.id.toString())
        assert (response.status == SC_NO_CONTENT)
    }

    def authenticateUser(String username, String password) {
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

    def addApiKeyToUser(User user) {
        def credentials = v1Factory.createApiKeyCredentials(user.username, testUtils.getRandomIntegerString())
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

    def getUserById(String id, String token=getServiceAdminToken()){
        def response = methods.getUserById(token, id)
        assert (response.status == SC_OK)
        response.getEntity(User).value
    }

    def getUserByName(String username, String token=getServiceAdminToken()){
        def response = methods.getUserByName(token, username)
        assert (response.status == SC_OK)
        response.getEntity(User).value
    }

    def getUsersByEmail(String email, String token=getServiceAdminToken()){
        def response = methods.getUsersByEmail(token, email)
        assert (response.status == SC_OK)
        List<User> users = response.getEntity(UserList).value.user
        users
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
        assert (response.status == SC_OK)
        response.getEntity(Domain).value
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

    def listDevices(user, token=getToken(user.username)) {
        def response = methods.listDevices(token, user.id)
        assert (response.status = SC_OK)
        response.getEntity(MobilePhones)
    }
}
