package com.rackspace.idm.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA
import com.rackspace.idm.Constants
import com.rackspace.idm.api.resource.cloud.v20.DefaultMultiFactorCloud20Service
import com.rackspace.idm.domain.config.IdmProperty
import com.rackspace.idm.domain.config.IdmPropertyList
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.entity.IdentityPropertyValueType
import com.rackspace.idm.domain.entity.PasswordPolicy
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.util.OTPHelper
import com.rackspace.idm.util.SamlUnmarshaller
import com.sun.jersey.api.client.ClientResponse
import com.unboundid.util.Base32
import groovy.json.JsonSlurper
import org.apache.commons.codec.binary.StringUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.opensaml.saml.saml2.core.LogoutResponse
import org.opensaml.security.credential.Credential
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import testHelpers.Cloud20Methods
import testHelpers.DevOpsMethods
import testHelpers.V1Factory
import testHelpers.V2Factory
import testHelpers.saml.SamlFactory

import javax.annotation.PostConstruct
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap

import static com.rackspace.idm.Constants.*
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE
import static org.apache.http.HttpStatus.*

@Component
class Cloud20Utils {
    private static final Logger LOG = Logger.getLogger(Cloud20Utils.class)

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

    @Autowired
    SamlUnmarshaller samlUnmarshaller

    @Autowired
    DevOpsMethods devOpsMethods

    @Autowired
    UserService userService

    @Autowired
    TenantService tenantService

    @Autowired
    ApplicationService applicationService

    @Autowired
    TenantRoleDao tenantRoleDao

    @PostConstruct
    def init() {
        methods.init()
    }

    String getToken(username, password=DEFAULT_PASSWORD) {
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

    AuthenticateResponse authenticate(String username, password=DEFAULT_PASSWORD, String applyRcnRoles = null) {
        def response = methods.authenticatePassword(username, password, applyRcnRoles)
        assert (response.status == SC_OK)
        return response.getEntity(AuthenticateResponse).value
    }

    AuthenticateResponse authenticateApplyRcnRoles(String username, String applyRcnRolesParam = "true", password=DEFAULT_PASSWORD) {
        def response = methods.authenticatePassword(username, password, applyRcnRolesParam)
        assert (response.status == SC_OK)
        return response.getEntity(AuthenticateResponse).value
    }

    AuthenticateResponse authenticate(User user, password=DEFAULT_PASSWORD, String applyRcnRoles = null) {
        return authenticate(user.username, password, applyRcnRoles)
    }

    def authenticateApplyRcnRoles(User user, String applyRcnRolesParam = "true", password=DEFAULT_PASSWORD) {
        def response = methods.authenticatePassword(user.username, password, applyRcnRolesParam)
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

    AuthenticateResponse authenticateTokenWithTenant(token, tenantId, applyRcnRoles=false) {
        def response = methods.authenticateTokenAndTenant(token, tenantId, applyRcnRoles)
        assert(response.status == 200)
        return response.getEntity(AuthenticateResponse).value
    }

    AuthenticateResponse authenticateTokenAndDelegationAgreement(token, delegationAgreementid, MediaType mediaType) {
        def response = methods.authenticateTokenAndDelegationAgreement(token, delegationAgreementid, mediaType)
        assert(response.status == 200)
        AuthenticateResponse delegateSubUserAuthResponse
        if (mediaType == MediaType.APPLICATION_JSON_TYPE) {
            delegateSubUserAuthResponse = response.getEntity(AuthenticateResponse)
        } else {
            delegateSubUserAuthResponse = response.getEntity(AuthenticateResponse).value
        }
        return delegateSubUserAuthResponse
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

    EndpointList listEndpointsForToken(token, authToken=getServiceAdminToken(), applyRcnRoles=false) {
        def response = methods.listEndpointsForToken(authToken, token, applyRcnRoles)
        assert response.status == 200
        def entity = response.getEntity(EndpointList).value
        assert entity != null
        return entity
    }

    User createUser(token, username=testUtils.getRandomUUID(), domainId=null, defaultRegion=null) {
        def response = methods.createUser(token, factory.createUserForCreate(username, "display", "${username}@rackspace.com", true, defaultRegion, domainId, DEFAULT_PASSWORD))

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

    /**
     * Creates a standard cloud account w/ mosso and nast tenants.
     *
     * @param domainId
     * @return
     */
    User createCloudAccount(identityAdminToken = getIdentityAdminToken(), int domainId = testUtils.getRandomInteger()) {
        createUserWithTenants(identityAdminToken, testUtils.getRandomUUID("userAdmin"), String.valueOf(domainId))
    }

    User createUserWithTenants(token=getIdentityAdminToken(), username=testUtils.getRandomUUID(), domainId=RandomStringUtils.randomNumeric(6)) {
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

    def deleteRoleOnUser(user, roleId, token=getServiceAdminToken()) {
        def response = methods.deleteApplicationRoleOnUser(token, roleId, user.id)
        assert (response.status == SC_NO_CONTENT)
    }

    def deleteUser(user) {
        def response = methods.deleteUser(getServiceAdminToken(), user.id)
        assert (response.status == SC_NO_CONTENT)
    }

    def createDomain() {
        testUtils.getRandomIntegerString()
    }

    def createDomainEntity(domainId = testUtils.getRandomUUID("domain")) {
        def domainEntity = factory.createDomain(domainId, testUtils.getRandomUUID("domain"))
        createDomain(domainEntity)
    }

    def updateDomain(domainId, domain, String token=getServiceAdminToken()) {
        def response = methods.updateDomain(token, domainId, domain)
        assert (response.status == SC_OK)
    }

    def domainRcnSwitch(domainId, rcn, String token=getServiceAdminToken()) {
        def response = methods.domainRcnSwitch(token, domainId, rcn)
        assert (response.status == SC_NO_CONTENT)
    }

    def disableDomain(domainId) {
        def domainToUpdate = v1Factory.createDomain().with {
            it.id = domainId
            it.name = domainId
            it.enabled = false
            it
        }
        updateDomain(domainId, domainToUpdate)
    }

    def deleteDomain(domainId) {
        if (domainId == null) {
            return
        }
        // Domains must be disabled before they can be deleted.
        disableDomain(domainId)

        def response = methods.deleteDomain(getServiceAdminToken(), domainId)
        assert (response.status == SC_NO_CONTENT)
    }

    Domain getDomain(domainId, String token=getServiceAdminToken()) {
        def response = methods.getDomain(token, domainId)
        assert (response.status == SC_OK)

        def entity = response.getEntity(Domain)
        assert (entity != null)
        return entity
    }

    Tenants listDomainTenants(String domainId, String token=getServiceAdminToken()) {
        def response = methods.getDomainTenants(token, domainId)
        assert response.status == SC_OK

        response.getEntity(Tenants).value
    }

    def updateDomainPasswordPolicy(String domainId, PasswordPolicy passwordPolicy, String token=getServiceAdminToken()) {
        def response = methods.updateDomainPasswordPolicy(token, domainId, passwordPolicy)
        assert (response.status == SC_OK)

        def policyAsString = response.getEntity(String)
        if (org.apache.commons.lang.StringUtils.isNotEmpty(policyAsString)) {
            return PasswordPolicy.fromJson(policyAsString)
        }
        return null
    }

    def updateDomainPasswordPolicy(String domainId, String passwordPolicyDuration="P90DT6H30M5S", Integer passwordHistoryRestriction = null, String token=getServiceAdminToken()) {
        updateDomainPasswordPolicy(domainId, new PasswordPolicy(passwordPolicyDuration, passwordHistoryRestriction), token)
    }

    PasswordPolicy getDomainPasswordPolicy(String domainId, token=getServiceAdminToken()) {
        def response = methods.getDomainPasswordPolicy(token, domainId)
        assert (response.status == SC_OK)

        def policyAsString = response.getEntity(String)
        if (org.apache.commons.lang.StringUtils.isNotEmpty()) {
            return PasswordPolicy.fromJson(policyAsString)
        }
        return null
    }

    def deleteDomainPasswordPolicy(String domainId, token=getServiceAdminToken()) {
        def response = methods.deleteDomainPasswordPolicy(token, domainId)
        assert (response.status == SC_NO_CONTENT)
    }

    def changeUserPassword(String username, String currentPassword, String newPassword) {
        def response = methods.changeUserPassword(username, currentPassword, newPassword)
        assert (response.status == SC_NO_CONTENT)
    }

    def deleteTestDomainQuietly(domainId) {
        try {
            if (domainId == null) {
                return
            }
            def saToken = getServiceAdminToken()
            Domain domain = getDomain(domainId, saToken)

            if (domain.isEnabled()) {
                domain.setEnabled(false)
                updateDomain(domainId, domain, saToken)
            }
            def response = methods.deleteDomain(getServiceAdminToken(), domainId)
        } catch (Exception ex) {
            //eat
        }
    }


    String getServiceAdminToken() {
        return getToken(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PASSWORD)
    }

    /**
     * Deprecated. Removing the caching of tokens because causing sporadic failures as we don't handle retries on
     * failure.
     *
     * @return
     */
    def resetServiceAdminToken() {
        // No-Op
    }

    String getIdentityAdminToken() {
        getToken(IDENTITY_ADMIN_USERNAME, IDENTITY_ADMIN_PASSWORD)
    }

    def createServiceAdmin() {
        def serviceAdmin = this.createIdentityAdmin()
        def serviceAdminEntity = userService.getUser(serviceAdmin.username)
        tenantRoleDao.deleteTenantRoleForUser(serviceAdminEntity, tenantService.getTenantRoleForUserById(serviceAdminEntity, Constants.IDENTITY_ADMIN_ROLE_ID))
        def serviceAdminClientRole = applicationService.getClientRoleById(Constants.SERVICE_ADMIN_ROLE_ID)
        TenantRole role = new TenantRole()
        role.setClientId(serviceAdminClientRole.getClientId())
        role.setName(serviceAdminClientRole.getName())
        role.setRoleRsId(serviceAdminClientRole.getId())
        tenantService.addTenantRoleToUser(serviceAdminEntity, role)
        return serviceAdmin
    }

    def deleteServiceAdmin(serviceAdmin) {
        def serviceAdminEntity = userService.getUserById(serviceAdmin.id)
        userService.deleteUser(serviceAdminEntity)
    }

    def createIdentityAdmin() {
        def serviceAdminToken = getServiceAdminToken()
        return createUser(serviceAdminToken, testUtils.getRandomUUID("identityAdmin"))
    }

    def createIdentityProviderManager() {
        def identityAdmin = createIdentityAdmin()
        addRoleToUser(identityAdmin, Constants.IDENTITY_PROVIDER_MANAGER_ROLE_ID)
        return identityAdmin
    }

    def createIdentityProvider(token = getServiceAdminToken(), IdentityProviderFederationTypeEnum providerType = IdentityProviderFederationTypeEnum.DOMAIN, def approvedDomainId = null) {
        def idp = factory.createIdentityProvider(testUtils.getRandomUUID(), testUtils.getRandomUUID("My IDP - "), testUtils.getRandomUUID("http://example.com/"), providerType)
        if (approvedDomainId != null) {
            idp.approvedDomainIds = new ApprovedDomainIds().with {
                it.approvedDomainId = [approvedDomainId]
                it
            }
            idp.approvedDomainGroup = null
        }
        def response = methods.createIdentityProvider(token, idp)
        assert (response.status == SC_CREATED)
        return response.getEntity(IdentityProvider)
    }

    def createIdentityProvider(token, IdentityProvider idp) {
        def response = methods.createIdentityProvider(token, idp)
        assert (response.status == SC_CREATED)
        return response.getEntity(IdentityProvider)
    }

    def createIdentityProviderWithCred(token = getServiceAdminToken(), IdentityProviderFederationTypeEnum type, Credential cred) {
        def response = methods.createIdentityProviderWithCred(token, type, cred)
        assert (response.status == SC_CREATED)
        return response.getEntity(IdentityProvider)
    }

    def getIdentityProvider(token, idpId) {
        def response = methods.getIdentityProvider(token, idpId)
        assert (response.status == SC_OK)
        return response.getEntity(IdentityProvider)
    }

    def deleteIdentityProvider(idp) {
        def response = methods.deleteIdentityProvider(getServiceAdminToken(), idp.id)
        assert (response.status == SC_NO_CONTENT)
    }

    def deleteIdentityProviderQuietly(token, idpId) {
        def response = methods.deleteIdentityProvider(token, idpId)
    }

    def createUserAdminWithTenantsAndRole(domainId, rolename, tenantId) {
        def identityAdmin = createIdentityAdmin()

        def identityAdminToken = getToken(identityAdmin.username)

        def userAdmin = createUserWithTenantsAndRole(identityAdminToken, testUtils.getRandomUUID("userAdmin"), domainId, rolename, tenantId)

        return [userAdmin, [identityAdmin, userAdmin].asList()]
    }

    def createUserAdminWithTenants(domainId = createDomain()) {
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

    User createUserAdminWithoutIdentityAdmin(domainId=testUtils.getRandomIntegerString()) {
        return createUser(this.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), domainId)
    }

    /**
     * Creates a generic user admin account without any tenants. Uses standard identity admin account.
     * @param domainId
     * @return
     */
    def createGenericUserAdmin(domainId=testUtils.getRandomIntegerString()) {
        return createUser(getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), domainId)
    }

    def createDefaultUser(domainId) {
        def identityAdmin = createIdentityAdmin()

        def identityAdminToken = getToken(identityAdmin.username)

        def userAdmin = createUser(identityAdminToken, testUtils.getRandomUUID("userAdmin"), domainId)

        def userAdminToken  = getToken(userAdmin.username)

        def defaultUser = createUser(userAdminToken, testUtils.getRandomUUID("defaultUser"), domainId)

        return [defaultUser, [defaultUser, userAdmin, identityAdmin].asList()]
    }

    def logoutFederatedUser(username, idpUri = DEFAULT_IDP_URI, samlProducer = null) {
        def logoutRequest
        if (samlProducer) {
            logoutRequest = new SamlFactory().generateLogoutRequestEncodedForSamlProducer(idpUri, username, samlProducer)
        } else {
            logoutRequest = new SamlFactory().generateLogoutRequestEncoded(idpUri, username)
        }
        def response = methods.federatedLogout(logoutRequest);
        def responseStr = response.getEntity(String.class)

        if (response.status != HttpStatus.SC_OK) {
            def unencodedRequest = org.apache.xml.security.utils.Base64.decode(logoutRequest)
            LOG.error(String.format("Failed to logout fed user. Logout Request: '%s', Logout response: '%s'", unencodedRequest, responseStr))
        }

        assert response.status == HttpStatus.SC_OK
        LogoutResponse logoutResponse = samlUnmarshaller.unmarshallLogoutRespone(StringUtils.getBytesUtf8(responseStr))
        return logoutResponse
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

    ImpersonationResponse impersonate(String token, User user, Integer expireTime = 10800) {
        def response = methods.impersonate(token, user, expireTime)
        assert (response.status == SC_OK)
        response.getEntity(ImpersonationResponse)
    }

    ImpersonationResponse impersonateWithRacker(User user, expireTime = 10800) {
        def auth = authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD)
        def response = methods.impersonate(auth.token.id, user, expireTime)
        assert (response.status == SC_OK)
        response.getEntity(ImpersonationResponse)
    }

    String getImpersonationTokenWithRacker(User user, expireTime = 10800) {
        impersonateWithRacker(user, expireTime).token.id
    }

    String getImpersonatedToken(User impersonator, User user) {
        impersonate(getToken(impersonator.username, DEFAULT_PASSWORD), user).token.id
    }

    String getImpersonatedTokenWithToken(String token, User user) {
        impersonate(token, user).token.id
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

    AuthenticateResponse validateToken(token) {
        def response = methods.validateToken(getServiceAdminToken(), token)
        assert (response.status == SC_OK)
        response.getEntity(AuthenticateResponse).value
    }

    def validateTokenApplyRcnRoles(token, String applyRcnRolesParam = "true") {
        def response = methods.validateTokenApplyRcnRoles(getServiceAdminToken(), token, applyRcnRolesParam)
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

    def getRole(roleId, token=getServiceAdminToken()) {
        def response = methods.getRole(token, roleId)
        assert (response.status == SC_OK)
        response.getEntity(Role).value
    }

    def getRoleByName(roleName, token=getServiceAdminToken()) {
        def response = methods.listRoles(token, null, null, null, roleName)
        assert (response.status == SC_OK)
        def roles = response.getEntity(RoleList).value
        assert roles.role.size() == 1
        roles.role[0]
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

    RoleList listRolesForUserOnTenant(User user, Tenant tenant, String token=getServiceAdminToken(), boolean applyRcnRoles = false) {
        def response = methods.listRolesForUserOnTenant(token, tenant.id, user.id)
        assert response.status == SC_OK
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
            it.roleType = RoleTypeEnum.PROPAGATE
            it.serviceId = service == null ? null : service.id
            it.administratorRole="identity:admin"
            it
        }
        createRole(role)
    }

    Role createRole(service=null, roleName=testUtils.getRandomUUID("role"), String administratorRole = "identity:user-manage") {
        def role = factory.createRole(roleName).with {
            it.administratorRole = administratorRole
            it
        }
        if(service != null){
            role.serviceId = service.id
        }
        createRole(role)
    }

    Role createRole(Role role) {
        def response = methods.createRole(getServiceAdminToken(), role)
        assert (response.status == SC_CREATED)
        response.getEntity(Role).value
    }

    def deleteRole(role) {
        def response = methods.deleteRole(getServiceAdminToken(), role.id)
        assert (response.status == SC_NO_CONTENT)
    }

    def deleteRoleQuietly(role) {
        try {
            methods.deleteRole(getServiceAdminToken(), role.id)
        } catch (Exception ex) {
            //eat. deleting quietly
        }
    }

    Tenant createTenantInDomain(String domainId) {
        createTenant(testUtils.getRandomUUID("tenant"), true, testUtils.getRandomUUID("tenant"), domainId)
    }

    Tenant createTenant(name=testUtils.getRandomUUID("tenant"), enabled=true, displayName=testUtils.getRandomUUID("tenant"), domainId=null) {
        def tenant = factory.createTenant(name, displayName, enabled, domainId)
        def response = methods.addTenant(getServiceAdminToken(), tenant)
        assert (response.status == SC_CREATED)
        response.getEntity(Tenant).value
    }

    def createTenantWithTypes(name=testUtils.getRandomUUID("tenant"), Collection<String> tenantTypes = null) {
        def tenant = factory.createTenant(name, name, tenantTypes)
        def response = methods.addTenant(getServiceAdminToken(), tenant)
        assert (response.status == SC_CREATED)
        response.getEntity(Tenant).value
    }

    TenantType createTenantType(name=testUtils.getRandomUUID("type")[0..15], description="description") {
        def tenantType = factory.createTenantType(name, description)
        def response = methods.addTenantType(getServiceAdminToken(), tenantType)
        assert (response.status == SC_CREATED)
        response.getEntity(TenantType)
    }

    def deleteTenant(def tenant) {
        def response = methods.deleteTenant(getServiceAdminToken(), tenant.id)
        assert (response.status == SC_NO_CONTENT)
    }

    def deleteTenantQuietly(def tenant) {
        try {
            def response = methods.deleteTenant(getServiceAdminToken(), tenant.id)
            assert (response.status == SC_NO_CONTENT)
        } catch (Exception ex) {
            //ignore
        }
    }

    def deleteTenantById(String tenantId) {
        def response = methods.deleteTenant(getServiceAdminToken(), tenantId)
        assert (response.status == SC_NO_CONTENT)
    }

    def addRoleToUserOnTenant(user, tenant, roleId=MOSSO_ROLE_ID) {
        def response = methods.addRoleToUserOnTenant(getServiceAdminToken(), tenant.id, user.id, roleId)
        assert (response.status == SC_OK)
    }

    def deleteRoleFromUserOnTenant(user, tenant, roleId) {
        def response = methods.deleteRoleFromUserOnTenant(getServiceAdminToken(), tenant.id, user.id, roleId)
        assert (response.status == SC_NO_CONTENT)
    }

    def addRoleToUserOnTenantId(user, tenantId, roleId=MOSSO_ROLE_ID) {
        def response = methods.addRoleToUserOnTenant(getServiceAdminToken(), tenantId, user.id, roleId)
        assert (response.status == SC_OK)
    }

    EndpointTemplate createEndpointTemplate(EndpointTemplate endpointTemplate) {
        def response = methods.addEndpointTemplate(getServiceAdminToken(), endpointTemplate)
        assert (response.status == SC_CREATED)
        response.getEntity(EndpointTemplate).value
    }

    EndpointTemplate updateEndpointTemplate(EndpointTemplate endpointTemplate, String endpointId) {
        def response = methods.updateEndpointTemplate(getServiceAdminToken(), endpointId, endpointTemplate)
        assert (response.status == SC_OK)
        response.getEntity(EndpointTemplate).value
    }

    def createAndUpdateEndpointTemplate(EndpointTemplate endpointTemplate, String endpointId) {
        createEndpointTemplate(endpointTemplate)
        updateEndpointTemplate(endpointTemplate, endpointId)
    }

    EndpointTemplate createEndpointTemplate(global=false, tenantAlias=null, enabled=true, type="compute", region="ORD", id=testUtils.getRandomIntegerString(), publicUrl=testUtils.getRandomUUID("http://"), name="cloudServers") {
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

    def getEndpointTemplate(def endpointTemplateId) {
        return methods.getEndpointTemplate(getServiceAdminToken(), "" + endpointTemplateId).getEntity(EndpointTemplate).value
    }

    def deleteEndpointTemplate(endpointTemplate) {
        def response = methods.deleteEndpointTemplate(getServiceAdminToken(), endpointTemplate.id.toString())
        assert (response.status == SC_NO_CONTENT)
    }

    def disableAndDeleteEndpointTemplate(endpointTemplateId){
        def endpointTemplate = new EndpointTemplate().with {
            it.id = endpointTemplateId as int
            it.enabled = false
            it
        }
        updateEndpointTemplate(endpointTemplate, endpointTemplateId)
        deleteEndpointTemplate(endpointTemplate)
    }

    def addEndpointTemplateToTenant(tenantId, int endpointTemplateId) {
        def endpointTemplate = new EndpointTemplate().with {
            it.id = endpointTemplateId
            it
        }
        def response = methods.addEndpoint(this.getServiceAdminToken(), tenantId, endpointTemplate)
        assert (response.status == SC_OK)
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

    User getUserByIdReturnUser(String id, String token=getServiceAdminToken(), MediaType mediaType = APPLICATION_XML_TYPE){
        def response = methods.getUserById(token, id, mediaType)
        assert (response.status == SC_OK)
        def entity = response.getEntity(User)
        if (mediaType == APPLICATION_XML_TYPE) {
            return entity.value
        } else {
            return entity
        }
    }

    User getUserByName(String username, String token=getServiceAdminToken(), MediaType mediaType = APPLICATION_XML_TYPE){
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

    def listUsers(String token=getServiceAdminToken(), MediaType mediaType = MediaType.APPLICATION_XML_TYPE){
        def response = methods.listUsers(token, mediaType)
        assert (response.status == SC_OK)
        def userList = response.getEntity(UserList)
        if (mediaType == APPLICATION_XML_TYPE) {
            userList = userList.value
        }
        userList.user
    }

    UserList listUsersWithTenant(String tenantId, String token = getServiceAdminToken()) {
        def response = methods.listUsersWithTenantId(token, tenantId)
        assert response.status == SC_OK
        response.getEntity(UserList).value
    }

    UserList listUsersWithTenantAndRole(String tenantId, String roleId, String token = getServiceAdminToken()) {
        def response = methods.listUsersWithTenantIdAndRole(token, tenantId, roleId)
        assert response.status == SC_OK
        response.getEntity(UserList).value
    }

    def addUserToGroupWithId(String groupId, User user, String token=getServiceAdminToken()) {
        def response = methods.addUserToGroup(token, groupId, user.id)
        assert (response.status == SC_NO_CONTENT)
    }

    def addUserToGroup(Group group, User user, String token=getServiceAdminToken()) {
        def response = methods.addUserToGroup(token, group.id, user.id)
        assert (response.status == SC_NO_CONTENT)
    }

    def removeUserFromGroup(Group group, User user, String token=getServiceAdminToken()) {
        def response = methods.removeUserFromGroup(token, group.id, user.id)
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
        if (settings.isEnabled()) {
            /*
            put this in here because when enable MFA, all PWD based tokens are disabled. Due to only second level precision,
            any MFA sessionId's generated as restricted AE tokens within the same second would be considered invalid. So sleep
             till the next second
             */
            System.sleep(1001-new DateTime().getMillisOfSecond())
        }
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

    def deleteUsersQuietly(List<User> users, String token=getServiceAdminToken()) {
        users.each {
            try {
                methods.destroyUser(token, it.getId())
            } catch (all) {
                //ignore
            }
        }
    }

    def createDomain(Domain domain) {
        def response = methods.addDomain(getServiceAdminToken(), domain)
        assert (response.status == SC_CREATED)
        response.getEntity(Domain)
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

    def deleteTenantType(String name) {
        def response = methods.deleteTenantType(getServiceAdminToken(), name)
        assert (response.status == SC_NO_CONTENT)
    }

    def deleteTenantTypeIgnoreError(String name) {
        def response = methods.deleteTenantType(getServiceAdminToken(), name)
    }

    def getTenant(String tenantId) {
        def response = methods.getTenant(getServiceAdminToken(), tenantId)
        assert (response.status == SC_OK)
        response.getEntity(Tenant).value
    }

    def listTenantsForToken(token, boolean applyRcnRoles = false) {
        def response = methods.listTenants(token, applyRcnRoles)
        assert (response.status == SC_OK)
        response.getEntity(Tenants).value
    }

    def updateTenant(String tenantId, boolean enabled = true, tenantName = null) {
        def tenant = getTenant(tenantId)
        tenant.enabled = enabled
        tenant.name = tenantName
        def response = methods.updateTenant(getServiceAdminToken(), tenantId, tenant)
        assert (response.status == SC_OK)
        response.getEntity(Tenant).value
    }

    def addTenantToDomain(String domainId, String tenantId) {
        def response = methods.addTenantToDomain(getServiceAdminToken(), domainId, tenantId)
        assert (response.status == SC_NO_CONTENT)
    }

    def deleteTenantFromDomain(String domainId, String tenantId) {
        def response = methods.deleteTenantFromDomain(getServiceAdminToken(), domainId, tenantId)
        assert (response.status == SC_NO_CONTENT)
    }

    def getEndpointsByDomain(String domainId) {
        def response = methods.getEndpointsByDomain(getServiceAdminToken(), domainId)
        assert (response.status == SC_OK)
        response.getEntity(EndpointList).value

    }

    def createDelegationAgreement(String token, DelegationAgreement delegationAgreement) {
        def response = methods.createDelegationAgreement(token, delegationAgreement)
        assert (response.status == SC_CREATED)
        response.getEntity(DelegationAgreement)
    }

    def getDelegationAgreement(String token, String delegationAgreementId) {
        def response = methods.getDelegationAgreement(token, delegationAgreementId)
        assert (response.status == SC_OK)
        response.getEntity(DelegationAgreement).value
    }

    def extractSessionIdFromFirstWwwAuthenticateHeader(MultivaluedMap<String, String> headers) {
        return extractSessionIdFromWwwAuthenticateHeader(headers.getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE));
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

    def getNastTenant(domainId) {
        return NAST_TENANT_PREFIX.concat("" + domainId)
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

    def addEndpointTemplateAssignmentRule(token, rule) {
        def response = methods.addEndpointAssignmentRule(token, rule)
        assert response.status == SC_CREATED
        return response.getEntity(rule.class)
    }

    def createIdentityProperty(name = testUtils.getRandomUUID("propName"), value = testUtils.getRandomUUID("propValue"), valueType = IdentityPropertyValueType.STRING.getTypeName(), reloadable = true, searchable = true) {
        def idmProperty = factory.createIdentityProperty(name, value, valueType)
        def response = devOpsMethods.createIdentityProperty(getIdentityAdminToken(), idmProperty)
        assert response.status == SC_CREATED
        response.getEntity(IdentityProperty)
    }

    def deleteQuestion(question, String token = getIdentityAdminToken()) {
        def response = methods.deleteQuestion(token, question.id)
        assert response.status == SC_NO_CONTENT
    }

    def deleteIdentityProperty(propId, token = getIdentityAdminToken()) {
        def response = devOpsMethods.deleteIdentityProperty(token, propId)
    }

    IdmProperty getIdentityPropertyByName(String propName, token = getIdentityAdminToken()) {
        def getPropsResponse = devOpsMethods.getIdmProps(getIdentityAdminToken(), propName)
        getPropsResponse.getEntity(IdmPropertyList).properties.first()
    }

    def createFederatedUser(String domainId, mediaType = APPLICATION_XML_TYPE) {
        AuthenticateResponse samlAuthResponse = createFederatedUserForAuthResponse(domainId, mediaType)
        def user = samlAuthResponse.user
        return user
    }
  
    def createFederatedUserForAuthResponse(String domainId, mediaType = APPLICATION_XML_TYPE) {
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def username = testUtils.getRandomUUID("samlUser")
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null);
        def samlResponse = methods.samlAuthenticate(samlAssertion, mediaType)
        def samlAuthResponse = samlResponse.getEntity(AuthenticateResponse)
        return mediaType == APPLICATION_XML_TYPE ? samlAuthResponse.value : samlAuthResponse
    }

    def createUserGroup(String domainId = testUtils.getRandomUUID(), String name = testUtils.getRandomUUID(), String token = getIdentityAdminToken()) {
        def response = methods.createUserGroup(token, factory.createUserGroup(domainId, name))
        assert response.status == SC_CREATED
        return response.getEntity(UserGroup)
    }

    def grantRoleAssignmentsOnUserGroup(UserGroup group, RoleAssignments roleAssignments, token = getIdentityAdminToken()) {
        def response = methods.grantRoleAssignmentsOnUserGroup(token, group, roleAssignments)
        assert response.status == SC_OK
        return response.getEntity(RoleAssignments)
    }

    def getUserGroup(String groupId, String domainId, String token = getIdentityAdminToken()) {
        def userGroup = new UserGroup().with {
            it.domainId = domainId
            it.id = groupId
            it
        }
        def response = methods.getUserGroup(token, userGroup)
        assert response.status == 200
        return response.getEntity(UserGroup)
    }

    RoleAssignments listRoleAssignmentsOnUserGroup(UserGroup group, String token = getServiceAdminToken()) {
        def response = methods.listRoleAssignmentsOnUserGroup(token, group)
        assert response.status == 200
        return response.getEntity(RoleAssignments)
    }

    def deleteUserGroup(UserGroup group, String token = getIdentityAdminToken()) {
        def response = methods.deleteUserGroup(token, group)
        assert response.status == SC_NO_CONTENT
    }

    def revokeRoleFromUserGroup(UserGroup group, String roleId, String token = getIdentityAdminToken()) {
        def response = methods.revokeRoleAssignmentFromUserGroup(token, group, roleId)
        assert response.status == SC_NO_CONTENT
    }

    def addUserToUserGroup(String userId, UserGroup userGroup, String token = getIdentityAdminToken()) {
        def response = methods.addUserToUserGroup(token, userGroup.domainId, userGroup.id, userId)
        assert response.status == SC_NO_CONTENT
    }

    def removeUserFromUserGroup(String userId, UserGroup userGroup, String token = getIdentityAdminToken()) {
        def response = methods.removeUserFromUserGroup(token, userGroup.domainId, userGroup.id, userId)
        assert response.status == SC_NO_CONTENT
    }
}
