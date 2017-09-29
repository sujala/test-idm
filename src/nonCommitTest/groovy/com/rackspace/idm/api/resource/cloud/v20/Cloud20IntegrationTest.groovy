package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.config.IdentityConfig

import com.rackspace.idm.domain.dao.impl.LdapConnectionPools
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.TokenRevocationService
import com.rackspace.idm.util.JSONReaderForRoles
import com.rackspace.idm.validation.Validator20
import com.unboundid.ldap.sdk.Modification
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.ldap.sdk.persist.LDAPPersister
import org.apache.commons.configuration.Configuration
import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.joda.time.Seconds
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.MediaType

class Cloud20IntegrationTest extends RootIntegrationTest {
    Logger LOG = Logger.getLogger(Cloud20IntegrationTest.class)

    @Autowired(required = false) LdapConnectionPools connPools
    @Autowired Configuration config
    @Autowired ApplicationService applicationService

    @Autowired
    @Qualifier("tokenRevocationService")
    TokenRevocationService tokenRevocationService;

    @Shared JAXBObjectFactories objFactories;

    @Shared int MAX_TRIES = 20

    @Shared def serviceAdminToken
    @Shared def identityAdminToken
    @Shared def userAdminToken
    @Shared def userAdminTwoToken
    @Shared def defaultUserToken
    @Shared def defaultUserManageRoleToken
    @Shared def serviceAdmin

    @Shared def identityAdmin
    @Shared def userAdmin
    @Shared def userAdminTwo
    @Shared def defaultUserOtherDomain
    @Shared def defaultUser
    @Shared def defaultUserTwo
    @Shared def defaultUserThree
    @Shared def defaultUserForAdminTwo
    @Shared def defaultUserWithManageRole
    @Shared def defaultUserWithManageRole2
    @Shared def defaultUserForProductRole
    @Shared def testUser
    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def sharedRole
    @Shared def sharedRoleTwo
    @Shared def productRole
    @Shared def propagatingRole
    @Shared def tenant

    @Shared def emptyDomainId
    @Shared def testDomainId
    @Shared def testDomainId2
    @Shared def defaultRegion
    @Shared def endpointTemplateId

    def randomness = UUID.randomUUID()
    @Shared def groupLocation
    @Shared def group
    @Shared Region sharedRegion

    static def USER_MANAGE_ROLE_ID = "7"
    static def DEFAULT_PASSWORD = "Password1"
    static def DEFAULT_APIKEY = "0123456789"

    @Shared REFRESH_WINDOW_HOURS
    @Shared CLOUD_CLIENT_ID
    @Shared BASE_DN = "o=rackspace,dc=rackspace,dc=com"
    @Shared SCOPE = SearchScope.SUB

    @Shared def USER_FOR_AUTH
    @Shared def USER_FOR_AUTH_ID
    @Shared def USER_FOR_AUTH_PWD

    @Shared def defaultUserRoleId = "2"
    @Shared def userAdminRoleId = "3"
    @Shared def userManageRoleId = "7"
    @Shared def identityAdminRoleId = "1"
    @Shared def serviceAdminRoleId = "4"

    @Shared def cloudAuthClientId

    @Shared def randomMosso

    @Shared def racker = "test.racker"
    @Shared def rackerPassword = "password"
    @Shared def rackerRole = "Racker"
    @Shared def rackerRoleName = "team-cloud-identity"
    @Shared def rackerToken
    @Shared def randomSuffix

    @Shared JSONReaderForRoles readerForRoles = new JSONReaderForRoles()


    def setupSpec() {
        sharedRandom = ("$sharedRandomness").replace('-',"")
        testDomainId = "domain1$sharedRandom"
        testDomainId2 = "domain2$sharedRandom"
        emptyDomainId = "domain3$sharedRandom"

        Random randomNumber = new Random()
        randomMosso = 10000000 + randomNumber.nextInt(1000000)

        this.objFactories = new JAXBObjectFactories()
        serviceAdminToken = cloud20.authenticatePassword("authQE", "Auth1234").getEntity(AuthenticateResponse).value.token.id
        serviceAdmin = cloud20.getUserByName(serviceAdminToken, "authQE").getEntity(User).value

        identityAdmin = cloud20.getUserByName(serviceAdminToken, "auth").getEntity(User).value

        identityAdminToken = cloud20.authenticatePassword("auth", "auth123").getEntity(AuthenticateResponse).value.token.id

        cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("admin$sharedRandom", "display", "email@email.com", true, null, null, "Password1"))
        testUser = cloud20.getUserByName(serviceAdminToken, "admin$sharedRandom").getEntity(User).value
        USER_FOR_AUTH = testUser.username
        USER_FOR_AUTH_ID = testUser.id
        USER_FOR_AUTH_PWD = "Password1"

        endpointTemplateId = "100001"
        cloud20.addEndpointTemplate(serviceAdminToken, v1Factory.createEndpointTemplate(endpointTemplateId, "compute", "http://public.url", "cloudServers"))

        defaultRegion = v1Factory.createRegion("ORD", true, true)
        cloud20.createRegion(serviceAdminToken, defaultRegion)
        cloud20.updateRegion(serviceAdminToken, defaultRegion.name, defaultRegion)

        //User Admin
        def createResponse = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate("userAdmin1$sharedRandom", "display", "test@rackspace.com", true, null, testDomainId, "Password1"))
        userAdmin = cloud20.getUserByName(identityAdminToken, "userAdmin1$sharedRandom").getEntity(User).value
        cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate("userAdmin2$sharedRandom", "display", "test@rackspace.com", true, null, emptyDomainId, "Password1"))
        userAdminTwo = cloud20.getUserByName(identityAdminToken, "userAdmin2$sharedRandom").getEntity(User).value

        userAdminToken = cloud20.authenticatePassword("userAdmin1$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id
        userAdminTwoToken = cloud20.authenticatePassword("userAdmin2$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id

        //Racker
        rackerToken = cloud20.authenticateRacker(racker, rackerPassword).getEntity(AuthenticateResponse).value.token.id

        // Default Users
        cloud20.createUser(userAdminToken, v2Factory.createUserForCreate("defaultUser1$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUser = cloud20.getUserByName(userAdminToken, "defaultUser1$sharedRandom").getEntity(User).value
        cloud20.createUser(userAdminToken, v2Factory.createUserForCreate("defaultUser2$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserTwo = cloud20.getUserByName(userAdminToken, "defaultUser2$sharedRandom").getEntity(User).value
        cloud20.createUser(userAdminToken, v2Factory.createUserForCreate("defaultUser3$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserThree = cloud20.getUserByName(userAdminToken, "defaultUser3$sharedRandom").getEntity(User).value
        cloud20.createUser(userAdminTwoToken, v2Factory.createUserForCreate("defaultUser4$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserForAdminTwo = cloud20.getUserByName(userAdminTwoToken, "defaultUser4$sharedRandom").getEntity(User).value
        defaultUserToken = cloud20.authenticatePassword("defaultUser1$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id
        cloud20.createUser(userAdminToken, v2Factory.createUserForCreate("defaultUserWithManageRole$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserWithManageRole = cloud20.getUserByName(userAdminToken, "defaultUserWithManageRole$sharedRandom").getEntity(User).value
        defaultUserManageRoleToken = cloud20.authenticate("defaultUserWithManageRole$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id

        cloud20.createUser(userAdminToken, v2Factory.createUserForCreate("defaultUserForProductRole$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserForProductRole = cloud20.getUserByName(userAdminToken, "defaultUserForProductRole$sharedRandom").getEntity(User).value

        cloud20.createUser(userAdminTwoToken, v2Factory.createUserForCreate("otherDomainUser$sharedRandom", "display", "test@rackspace.com", true, null, emptyDomainId, "Password1"))
        defaultUserOtherDomain = cloud20.getUserByName(userAdminTwoToken, "otherDomainUser$sharedRandom").getEntity(User).value

        cloud20.createUser(userAdminToken, v2Factory.createUserForCreate("defaultUserWithManageRole2$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserWithManageRole2 = cloud20.getUserByName(userAdminToken, "defaultUserWithManageRole2$sharedRandom").getEntity(User).value

        defaultUserToken = cloud20.authenticatePassword("defaultUser1$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id

        //create group
        def createGroupResponse = cloud20.createGroup(serviceAdminToken, v1Factory.createGroup("group$sharedRandom", "this is a group"))
        groupLocation = createGroupResponse.location
        def getGroupResponse = cloud20.getGroup(serviceAdminToken, groupLocation)
        group = getGroupResponse.getEntity(Group).value

        def createRegionResponse = cloud20.createRegion(serviceAdminToken, v1Factory.createRegion("region$sharedRandom", true, false))
        def getRegionResponse = cloud20.getRegion(serviceAdminToken, "region$sharedRandom")
        sharedRegion = getRegionResponse.getEntity(Region)

        //create role
        Role createRole = v2Factory.createRole()
        createRole.serviceId = "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        createRole.name = "sharedRole1$sharedRandom"
        def responseRole = cloud20.createRole(identityAdminToken, createRole)
        sharedRole = responseRole.getEntity(Role).value

        Role createRole2 = v2Factory.createRole()
        createRole2.serviceId = "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        createRole2.name = "sharedRole2$sharedRandom"
        def responseRole2 = cloud20.createRole(identityAdminToken, createRole2)
        sharedRoleTwo = responseRole2.getEntity(Role).value

        //create product role
        Role createProductRole = v2Factory.createRole().with {it.administratorRole = IdentityUserTypeEnum.USER_MANAGER.roleName; it}
        createProductRole.serviceId = "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        createProductRole.name = "productRole2$sharedRandom"
        def responseProductRole = cloud20.createRole(identityAdminToken, createProductRole)
        productRole = responseProductRole.getEntity(Role).value

        def role = v2Factory.createRole(true).with {
            it.name = "propagatingRole$sharedRandom"
            it.otherAttributes = null
            return it
        }
        def responsePropagateRole = cloud20.createRole(identityAdminToken, role)
        propagatingRole = responsePropagateRole.getEntity(Role).value

        if (tenant == null) {
            def random = UUID.randomUUID().toString().replace("-", "")
            def tenantForCreate = v2Factory.createTenant("tenant$random", "tenant$random", true)
            def tenantResponse = cloud20.addTenant(serviceAdminToken, tenantForCreate)
            tenant = tenantResponse.getEntity(Tenant).value
        }

        //Add role to identity-admin and default-users
       cloud20.addApplicationRoleToUser(serviceAdminToken, sharedRole.getId(), identityAdmin.getId())
        cloud20.addApplicationRoleToUser(serviceAdminToken, sharedRole.getId(), defaultUser.getId())
        cloud20.addApplicationRoleToUser(serviceAdminToken, sharedRole.getId(), defaultUserTwo.getId())
        cloud20.addApplicationRoleToUser(serviceAdminToken, sharedRole.getId(), defaultUserThree.getId())

        //testIdentityRole = getRole(serviceAdminToken, testIdentityRoleId).getEntity(Role).value
    }

    def cleanup() {
        staticIdmConfiguration.reset()
        reloadableConfiguration.reset()
    }

    def createClientRole(sharedRandom, weight) {
        def productRole = v2Factory.createRole()
        productRole.serviceId = "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        productRole.name = "productRole$sharedRandom"
        def daoProductRole = new ClientRole().with {
            it.clientId = productRole.serviceId
            it.name = productRole.name
            it.rsWeight = weight
            it
        }
        applicationService.addClientRole(daoProductRole);
        productRole.setId(daoProductRole.getId())
        return productRole
    }

    def deleteClientRole(roleId) {
        def clientRole = applicationService.getClientRoleById(roleId)
        applicationService.deleteClientRole(clientRole)
    }

    def setup() {
        tokenRevocationService.revokeAllTokensForEndUser(USER_FOR_AUTH_ID)
        setConfigValues()
        cloudAuthClientId = config.getString("cloudAuth.clientId")
        randomSuffix = UUID.randomUUID().toString().replace('-',"")
    }

    def cleanupSpec() {
        cloud20.deleteGroup(serviceAdminToken, group.getId())
        cloud20.deleteRegion(serviceAdminToken, sharedRegion.getName())

        cloud20.deleteRole(serviceAdminToken, sharedRole.getId())
        cloud20.deleteRole(serviceAdminToken, sharedRoleTwo.getId())
        cloud20.deleteRole(serviceAdminToken, productRole.getId())
        cloud20.deleteRole(serviceAdminToken, propagatingRole.getId())

        cloud20.destroyUser(serviceAdminToken, defaultUser.getId())
        cloud20.destroyUser(serviceAdminToken, defaultUserTwo.getId())
        cloud20.destroyUser(serviceAdminToken, defaultUserThree.getId())
        cloud20.destroyUser(serviceAdminToken, defaultUserForAdminTwo.getId())
        cloud20.destroyUser(serviceAdminToken, defaultUserWithManageRole.getId())
        cloud20.destroyUser(serviceAdminToken, defaultUserWithManageRole2.getId())
        cloud20.destroyUser(serviceAdminToken, defaultUserForProductRole.getId())

        cloud20.destroyUser(serviceAdminToken, defaultUserOtherDomain.getId())

        cloud20.destroyUser(serviceAdminToken, userAdmin.getId())
        cloud20.destroyUser(serviceAdminToken, userAdminTwo.getId())

        cloud20.destroyUser(serviceAdminToken, testUser.getId())

        //TODO: DELETE RAX_AUTH_DOMAINS
        cloud20.deleteEndpointTemplate(serviceAdminToken, endpointTemplateId)
    }

    def "authenticating where total access tokens remains unchanged"() {
        when:
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, "UUID")
        def scopeAccessOne = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        def scopeAccessTwo = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        def allUsersScopeAccessAfter = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=UserScopeAccess)(userRsId=$USER_FOR_AUTH_ID))", "*")

        then:
        scopeAccessOne.token.id.equals(scopeAccessTwo.token.id)
        allUsersScopeAccessAfter.entryCount <= 2

        cleanup:
        staticIdmConfiguration.reset()
    }

    def "authenticating where token is within refresh window adds new token"() {
        when:
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, "UUID")
        def scopeAccessOne = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        setTokenInRefreshWindow(USER_FOR_AUTH, scopeAccessOne.token.id)

        def allUsersScopeAccessBefore = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=UserScopeAccess)(userRsId=$USER_FOR_AUTH_ID))", "*")

        def scopeAccessTwo = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        def allUsersScopeAccessAfter = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=UserScopeAccess)(userRsId=$USER_FOR_AUTH_ID))", "*")

        then:
        allUsersScopeAccessBefore.getEntryCount() + 1 == allUsersScopeAccessAfter.getEntryCount()
        !scopeAccessOne.token.id.equals(scopeAccessTwo.token.id)

        cleanup:
        staticIdmConfiguration.reset()
    }

    def "authenticating where token is valid returns existing token"() {
        when:
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, "UUID")
        def scopeAccessOne = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        def scopeAccessTwo = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        then:
        scopeAccessOne.token.id.equals(scopeAccessTwo.token.id)

        cleanup:
        staticIdmConfiguration.reset()
    }

    def "authenticate with two valid tokens"() {
        when:
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, "UUID")
        def firstScopeAccess = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        setTokenInRefreshWindow(USER_FOR_AUTH, firstScopeAccess.token.id)
        def secondScopeAccess = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        def thirdScopeAccess = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        then:
        secondScopeAccess.token.id.equals(thirdScopeAccess.token.id)

        cleanup:
        staticIdmConfiguration.reset()
    }

    def "authenticating token in refresh window with 2 existing tokens deletes existing expired token"() {
        when:
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_PROVISIONED_TOKEN_FORMAT, "UUID")
        def firstScopeAccess = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        setTokenInRefreshWindow(USER_FOR_AUTH, firstScopeAccess.token.id)
        def secondScopeAccess = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        tokenRevocationService.revokeToken(firstScopeAccess.token.id)
        setTokenInRefreshWindow(USER_FOR_AUTH, secondScopeAccess.token.id)
        def thirdScopeAccess = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        def allUsersScopeAccessAfter = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=scopeAccess)(userRsId=authQE))", "*")

        then:
        !thirdScopeAccess.token.id.equals(secondScopeAccess.token.id)
        for (entry in allUsersScopeAccessAfter.searchEntries) {
            assert(!entry.DN.contains("$firstScopeAccess.token.id"))
        }

        cleanup:
        staticIdmConfiguration.reset()
    }

    def "authenticating with token and tenantName"() {
        given:
        def mossoId = testUtils.getRandomInteger()
        def username = testUtils.getRandomUUID("user")
        def apiKey = "0987654231"
        def user = v1Factory.createUser(username, apiKey, mossoId)
        def createdUser = cloud11.createUser(user).getEntity(com.rackspacecloud.docs.auth.api.v1.User)
        def scopeAccess = cloud20.authenticateApiKey(username, apiKey).getEntity(AuthenticateResponse).value
        def authRequestContent = v2Factory.createAuthenticationRequest(scopeAccess.token.id, null, mossoId.toString())

        when:
        def authResponse = cloud20.authenticate(authRequestContent)

        then:
        authResponse.status == 200

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), createdUser.id)
    }

    def 'Create User with Blank ID'() {
        when:
        def random = ("$randomness").replace('-', "")
        def user = v2Factory.createUserForCreate("bob" + random, "displayName", "test@rackspace.com", true, "ORD", null, "Password1")
        user.id = ""
        def response = cloud20.createUser(serviceAdminToken, user)
        def getUserResponse = cloud20.getUser(serviceAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User).value

        then:
        response.status == 201

        cleanup:
        cloud20.deleteUser(serviceAdminToken, userEntity.getId())
    }

    def 'Update User with no username'() {
        when:
        def random = ("$randomness").replace('-', "")
        def user = v2Factory.createUserForCreate("bob" + random, "displayName", "test@rackspace.com", true, "ORD", null, "Password1")
        def response = cloud20.createUser(serviceAdminToken, user)
        def getUserResponse = cloud20.getUser(serviceAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User).value

        def userForUpdate = v2Factory.createUserForUpdate(null, null, null, null, true, null, null)
        def updateUserResponse = cloud20.updateUser(serviceAdminToken, userEntity.getId(), userForUpdate)

        then:
        response.status == 201
        updateUserResponse.status == 200

        cleanup:
        cloud20.deleteUser(serviceAdminToken, userEntity.getId())
    }

    def 'user created by user-admin or user-manage gets region from that user'() {
        when:
        //Create user by user-admin
        def random = ("$randomness").replace('-', "")
        def user = v2Factory.createUserForCreate("bob" + random, "displayName", "test@rackspace.com", true, null, "someDomain", "Password1")
        def response = cloud20.createUser(userAdminToken, user)
        //Get user
        def getUserResponse = cloud20.getUser(serviceAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User).value

        //Add user-manage role to user
        def addRole = cloud20.addUserRole(userAdminToken, userEntity.getId(), USER_MANAGE_ROLE_ID)

        def newUserToken = cloud20.authenticatePassword(userEntity.username, "Password1").getEntity(AuthenticateResponse).value.token.id

        //Add another user by user-manage
        def user2 = v2Factory.createUserForCreate("steve" + random, "displayName", "test@rackspace.com", true, null, "someDomain", "Password1")
        def response2 = cloud20.createUser(newUserToken, user2)
        //Get user
        def getUserResponse2 = cloud20.getUser(serviceAdminToken, response2.location)
        def userEntity2 = getUserResponse2.getEntity(User).value

        //Delete users
        def deleteResponses = cloud20.deleteUser(serviceAdminToken, userEntity.getId())
        def deleteResponses2 = cloud20.deleteUser(serviceAdminToken, userEntity2.getId())

        then:
        response.status == 201
        response.location != null
        userEntity.defaultRegion == userAdmin.defaultRegion
        userEntity2.defaultRegion == userAdmin.defaultRegion
        deleteResponses.status == 204
    }

    def 'update user returns all user info including domain and region'() {
        when:
        //Create user
        def random = ("$randomness").replace('-', "")
        def user = v2Factory.createUserForCreate("bob" + random, "displayName", "test@rackspace.com", true, "ORD", "someDomain$randomSuffix", "Password1")
        def response = cloud20.createUser(identityAdminToken, user)
        //Get user
        def getUserResponse = cloud20.getUser(serviceAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User).value
        //Update user
        def userForUpdate = v2Factory.createUserForUpdate(null, null, "Bob", "test@rackspace.com", false, null, null)
        def updateUserResponse = cloud20.updateUser(serviceAdminToken, userEntity.getId(), userForUpdate)
        def updateUser = updateUserResponse.getEntity(User).value
        //Delete user
        def deleteResponses = cloud20.deleteUser(serviceAdminToken, userEntity.getId())

        then:
        response.status == 201
        response.location != null
        updateUserResponse.status == 200
        updateUser.defaultRegion == "ORD"
        updateUser.domainId == "someDomain$randomSuffix"
        deleteResponses.status == 204
    }

    def 'User CRUD'() {
        when:
        //Create user
        def random = ("$randomness").replace('-', "")
        def user = v2Factory.createUserForCreate("bob" + random, "displayName", "test@rackspace.com", true, "ORD", null, "Password1")
        def response = cloud20.createUser(serviceAdminToken, user)

        //Get user
        def getUserResponse = cloud20.getUser(serviceAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User).value
        //Get user by id
        def getUserByIdResponse = cloud20.getUserById(serviceAdminToken, userEntity.getId())
        def getUserByNameResponse = cloud20.getUserByName(serviceAdminToken, userEntity.getUsername())
        def getUserByEmailResponse = cloud20.getUsersByEmail(serviceAdminToken, userEntity.getEmail())
        //Update User
        def userForUpdate = v2Factory.createUserForUpdate(null, null, "Bob", "test@rackspace.com", false, null, null)
        def updateUserResponse = cloud20.updateUser(serviceAdminToken, userEntity.getId(), userForUpdate)
        //Delete user
        def deleteResponses = cloud20.deleteUser(serviceAdminToken, userEntity.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        getUserByIdResponse.status == 200
        getUserByNameResponse.status == 200
        getUserByEmailResponse.status == 200
        updateUserResponse.status == 200
        deleteResponses.status == 204
    }

    def "username should be able to be re-used after being deleted"() {
        when:
        //Create user
        def random = ("$randomness").replace('-', "")
        def user = v2Factory.createUserForCreate("bob" + random, "displayName", "test@rackspace.com", true, "ORD", null, "Password1")
        def response = cloud20.createUser(serviceAdminToken, user)

        //Get user
        def getUserResponse = cloud20.getUser(serviceAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User).value

        //Delete user
        def deleteResponse = cloud20.deleteUser(serviceAdminToken, userEntity.getId())

        //Recreate user
        def recreateResponse = cloud20.createUser(serviceAdminToken, user)

        //Get user
        def getRecreateUserResponse = cloud20.getUser(serviceAdminToken, recreateResponse.location)
        def recreateUserEntity = getRecreateUserResponse.getEntity(User).value

        //Delete user again for cleanup
        def deleteResponseAgain = cloud20.deleteUser(serviceAdminToken, recreateUserEntity.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        deleteResponse.status == 204
        recreateResponse.status == 201
        recreateResponse.location != null
        recreateUserEntity.username == userEntity.username
        deleteResponseAgain.status == 204
    }

    def "user-admin should be able to assign & remove role of weight 1000 to sub-user"() {
        when:
        def productRole = createClientRole(sharedRandom, 1000)
        def response = cloud20.addApplicationRoleToUser(userAdminToken, productRole.id, defaultUser.id)
        def response2 = cloud20.deleteApplicationRoleFromUser(userAdminToken, productRole.id, defaultUser.id)
        deleteClientRole(productRole.id)

        then:
        response.status == 200
        response2.status == 204
    }

    def "user-admin should NOT be able to assign & remove role of weight 1000 to sub-user outside of his domain"() {
        when:
        def productRole = createClientRole(sharedRandom, 1000)
        def response = cloud20.addApplicationRoleToUser(userAdminToken, productRole.id, defaultUserOtherDomain.id)
        def response2 = cloud20.deleteApplicationRoleFromUser(userAdminToken, productRole.id, defaultUserOtherDomain.id)
        deleteClientRole(productRole.id)

        then:
        response.status == 403
        response2.status == 403
    }

    def "User-Admin should not be able to assign himself role"() {
        when:
        def productRole = createClientRole(sharedRandom, 1000)
        def response = cloud20.addApplicationRoleToUser(userAdminToken, productRole.id, userAdmin.id)
        deleteClientRole(productRole.id)

        then:
        response.status == 403
    }

    def "User-Admin should not be able to remove role from himself"() {
        when:
        def productRole = createClientRole(sharedRandom, 1000)
        def response = cloud20.deleteApplicationRoleFromUser(userAdminToken, productRole.id, userAdmin.id)
        deleteClientRole(productRole.id)

        then:
        response.status == 403
    }

    def "user-manage should be able to assign & remove role of weight 1000 to sub-users"() {
        when:
        def productRole = createClientRole(sharedRandom, 1000)
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())
        def response = cloud20.addApplicationRoleToUser(defaultUserManageRoleToken, productRole.id, defaultUser.id)
        def response2 = cloud20.deleteApplicationRoleFromUser(defaultUserManageRoleToken, productRole.id, defaultUser.id)
        deleteClientRole(productRole.id)

        then:
        response.status == 200
        response2.status == 204

        cleanup:
        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())
    }

    def "user-manage should NOT be able to assign & remove role of weight 1000 to sub-users outside of his domain"() {
        when:
        def productRole = createClientRole(sharedRandom, 1000)
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())
        def response = cloud20.addApplicationRoleToUser(defaultUserManageRoleToken, productRole.id, defaultUserOtherDomain.id)
        def response2 = cloud20.deleteApplicationRoleFromUser(defaultUserManageRoleToken, productRole.id, defaultUserOtherDomain.id)
        deleteClientRole(productRole.id)


        then:
        response.status == 403
        response2.status == 403

        cleanup:
        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())
    }

    def 'User-manage role CRUD'() {
        when:
        //Create user
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        def random = ("$randomness").replace('-', "")
        def user = v2Factory.createUserForCreate("somename" + random, "displayName", "test@rackspace.com", true, "ORD", null, "Password1")
        def response = cloud20.createUser(defaultUserManageRoleToken, user)

        //Get user
        def getUserResponse = cloud20.getUser(defaultUserManageRoleToken, response.location)
        def userEntity = getUserResponse.getEntity(User).value

        //Update User
        def userForUpdate = v2Factory.createUserForUpdate(null, null, "Bob", "test@rackspace.com", false, null, null)
        def updateUserResponse = cloud20.updateUser(defaultUserManageRoleToken, userEntity.getId(), userForUpdate)

        //Delete user
        def deleteResponses = cloud20.deleteUser(defaultUserManageRoleToken, userEntity.getId())

        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        updateUserResponse.status == 200
        deleteResponses.status == 204
    }

    def "roles that CAN list roles"() {

        when:
        //Create user
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        def listForServiceAdmin = cloud20.listRoles(serviceAdminToken, null, null, null)
        def listForIdentityAdmin = cloud20.listRoles(identityAdminToken, null, null, null)
        def listForUserAdmin = cloud20.listRoles(userAdminToken, null, null, null)
        def listForUserManage = cloud20.listRoles(defaultUserManageRoleToken, null, null, null)

        then:
        listForServiceAdmin.status == 200
        listForIdentityAdmin.status == 200
        listForUserAdmin.status == 200
        listForUserManage.status == 200

        cleanup:
        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())
    }

    def "roles that CANNOT list roles"() {

        when:
        def listForDefaultUser = cloud20.listRoles(defaultUserToken, null, null, null)

        then:
        listForDefaultUser.status == 403

        cleanup:
        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())
    }

    def "user admin able to list and query for created role"() {
        def role = v2Factory.createRole("userAdminGlobalRole$sharedRandom", null).with {it.administratorRole = IdentityUserTypeEnum.USER_MANAGER.roleName; it}

        when:
        def createdRole = cloud20.createRole(identityAdminToken, role).getEntity(Role).value
        def roleListResponse = cloud20.listRoles(userAdminToken, null, "0", "500")
        def roleResponse = cloud20.getRole(userAdminToken, createdRole.id)

        then:
        roleListResponse.status == 200
        roleListResponse.getEntity(RoleList).value.role.id.contains(createdRole.id)
        roleResponse.status == 200

        cleanup:
        cloud20.deleteRole(identityAdminToken, createdRole.id)
    }

    def "User manage retrieve user by name"() {
        given:
        def random = ("$randomness").replace('-', "")
        def userName = "somename$random"

        when:
        //Create user
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        def user = v2Factory.createUserForCreate(userName, "displayName", "test@rackspace.com", true, "ORD", null, "Password1")
        def response = cloud20.createUser(defaultUserManageRoleToken, user)

        //Get user
        def getUserResponse = cloud20.getUserByName(defaultUserManageRoleToken, userName)
        def userEntity = getUserResponse.getEntity(User).value

        //Delete user
        def deleteResponses = cloud20.deleteUser(defaultUserManageRoleToken, userEntity.getId())

        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        deleteResponses.status == 204
    }

    def "User manage retrieve user by email"() {
        given:
        def random = ("$randomness").replace('-', "")
        def userName = "somename$random"
        def userEmail = "test123@rackspace.com"

        when:
        //Create user
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        def user = v2Factory.createUserForCreate(userName, "displayName", userEmail, true, "ORD", null, "Password1")
        def response = cloud20.createUser(defaultUserManageRoleToken, user)

        //Get user
        def getUserByEmailResponse = cloud20.getUsersByEmail(defaultUserManageRoleToken, userEmail)
        def getUserResponse = cloud20.getUser(defaultUserManageRoleToken, response.location)
        def userEntity = getUserResponse.getEntity(User).value

        //Delete user
        def deleteResponses = cloud20.deleteUser(defaultUserManageRoleToken, userEntity.getId())

        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        then:
        response.status == 201
        response.location != null
        getUserByEmailResponse.status == 200
        getUserResponse.status == 200
        deleteResponses.status == 204
    }

    def "delete user api key"() {
        given:
        def random = ("$randomness").replace('-', "")
        def userName = "somename$random"

        when:
        //Create user
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        def user = v2Factory.createUserForCreate(userName, "displayName", "test@rackspace.com", true, "ORD", null, "Password1")
        def response = cloud20.createUser(defaultUserManageRoleToken, user)

        //Get user
        def getUserResponse = cloud20.getUserByName(defaultUserManageRoleToken, userName)
        def userEntity = getUserResponse.getEntity(User).value

        //Get apiKey
        def createApiKey = cloud20.addApiKeyToUser(serviceAdminToken, userEntity.getId(), v2Factory.createApiKeyCredentials(userName, "bananas"))
        def getApiKeyResponse = cloud20.getUserApiKey(defaultUserManageRoleToken, userEntity.getId())

        //Delete apiKey
        def deleteApiKeyResponse = cloud20.deleteUserApiKey(serviceAdminToken, userEntity.getId())

        //Get apiKey Again should be Not Found
        def getApiKeyResponse404 = cloud20.getUserApiKey(defaultUserManageRoleToken, userEntity.getId())

        //Delete user
        def deleteResponses = cloud20.deleteUser(defaultUserManageRoleToken, userEntity.getId())

        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        createApiKey.status == 200
        getApiKeyResponse.status == 200
        deleteApiKeyResponse.status == 204
        getApiKeyResponse404.status == 404
        deleteResponses.status == 204
    }

    def "user admins should be able to delete API keys for User-Manage, Default User"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def userAdminToken = utils.getToken(userAdmin.username, DEFAULT_PASSWORD)

        when: "Themselves"
        utils.addApiKeyToUser(userAdmin)
        def getApiKeyResponse = cloud20.getUserApiKey(userAdminToken, userAdmin.id)
        def deleteApiKeyResponse = cloud20.deleteUserApiKey(userAdminToken, userAdmin.id)

        then:
        // Note: All users can view and delete API Key Credentials from themselves EXCEPT
        //       for identity:user-admins. They can only retrieve their API key (not delete it).
        getApiKeyResponse.status == 200
        deleteApiKeyResponse.status == 403

        when: "User-Manage"
        utils.addApiKeyToUser(userManage)
        getApiKeyResponse = cloud20.getUserApiKey(userAdminToken, userManage.id)
        deleteApiKeyResponse = cloud20.deleteUserApiKey(userAdminToken, userManage.id)

        then:
        getApiKeyResponse.status == 200
        deleteApiKeyResponse.status == 204

        when: "Default User"
        utils.addApiKeyToUser(defaultUser)
        getApiKeyResponse = cloud20.getUserApiKey(userAdminToken, defaultUser.id)
        deleteApiKeyResponse = cloud20.deleteUserApiKey(userAdminToken, defaultUser.id)

        then:
        getApiKeyResponse.status == 200
        deleteApiKeyResponse.status == 204

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "user managed should be able to delete API keys for Themselves, Default User"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def userManageToken = utils.getToken(userManage.username, DEFAULT_PASSWORD)

        when: "User Admin"
        utils.addApiKeyToUser(userAdmin)
        def getApiKeyResponse = cloud20.getUserApiKey(userManageToken, userAdmin.id)
        def deleteApiKeyResponse = cloud20.deleteUserApiKey(userManageToken, userAdmin.id)

        then:
        getApiKeyResponse.status == 403
        deleteApiKeyResponse.status == 403

        when: "Themselves"
        utils.addApiKeyToUser(userManage)
        getApiKeyResponse = cloud20.getUserApiKey(userManageToken, userManage.id)
        deleteApiKeyResponse = cloud20.deleteUserApiKey(userManageToken, userManage.id)

        then:
        getApiKeyResponse.status == 200
        deleteApiKeyResponse.status == 204

        when: "Default User"
        utils.addApiKeyToUser(defaultUser)
        getApiKeyResponse = cloud20.getUserApiKey(userManageToken, defaultUser.id)
        deleteApiKeyResponse = cloud20.deleteUserApiKey(userManageToken, defaultUser.id)

        then:
        getApiKeyResponse.status == 200
        deleteApiKeyResponse.status == 204

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "default users should be able to delete API keys for Themselves"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def defaultUserToken = utils.getToken(defaultUser.username, DEFAULT_PASSWORD)

        when: "User Admin"
        utils.addApiKeyToUser(userAdmin)
        def getApiKeyResponse = cloud20.getUserApiKey(defaultUserToken, userAdmin.id)
        def deleteApiKeyResponse = cloud20.deleteUserApiKey(defaultUserToken, userAdmin.id)

        then:
        getApiKeyResponse.status == 403
        deleteApiKeyResponse.status == 403

        when: "User Manage"
        utils.addApiKeyToUser(userManage)
        getApiKeyResponse = cloud20.getUserApiKey(defaultUserToken, userManage.id)
        deleteApiKeyResponse = cloud20.deleteUserApiKey(defaultUserToken, userManage.id)

        then:
        getApiKeyResponse.status == 403
        deleteApiKeyResponse.status == 403

        when: "Themselves"
        utils.addApiKeyToUser(defaultUser)
        getApiKeyResponse = cloud20.getUserApiKey(defaultUserToken, defaultUser.id)
        deleteApiKeyResponse = cloud20.deleteUserApiKey(defaultUserToken, defaultUser.id)

        then:
        getApiKeyResponse.status == 200
        deleteApiKeyResponse.status == 204

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "user manage get user's api key" () {
        given:
        def random = ("$randomness").replace('-', "")
        def userName = "somename$random"

        when:
        //Create user
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        def user = v2Factory.createUserForCreate(userName, "displayName", "test@rackspace.com", true, "ORD", null, "Password1")
        def response = cloud20.createUser(defaultUserManageRoleToken, user)

        //Get user
        def getUserResponse = cloud20.getUserByName(defaultUserManageRoleToken, userName)
        def userEntity = getUserResponse.getEntity(User).value

        //Get apiKey
        def createApiKey = cloud20.addApiKeyToUser(serviceAdminToken, userEntity.getId(), v2Factory.createApiKeyCredentials(userName, "bananas"))
        def getApiKeyResponse = cloud20.getUserApiKey(defaultUserManageRoleToken, userEntity.getId())

        //Delete user
        def deleteResponses = cloud20.deleteUser(defaultUserManageRoleToken, userEntity.getId())

        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        createApiKey.status == 200
        getApiKeyResponse.status == 200
        deleteResponses.status == 204
    }

    def "user-manage cannot update/delete another user with user-manage"() {
        when:
        //Create user
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        def random = ("$randomness").replace('-', "")
        def user = v2Factory.createUserForCreate("somename" + random, "displayName", "test@rackspace.com", true, "ORD", null, "Password1")
        def response = cloud20.createUser(defaultUserManageRoleToken, user)

        //Get user
        def getUserResponse = cloud20.getUser(serviceAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User).value
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, userEntity.getId())

        //Delete user
        def deleteResponses = cloud20.deleteUser(defaultUserManageRoleToken, userEntity.getId())
        //Hard delete user
        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, userEntity.getId())
        def actualDelete = cloud20.deleteUser(defaultUserManageRoleToken, userEntity.getId())

        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        deleteResponses.status == 401
        actualDelete.status == 204
    }

    def "user-manage cannot be added to user admin"() {
        when:
        //Create user
        def random = ("$randomness").replace('-', "")
        def user = v2Factory.createUserForCreate("somename" + random, "displayName", "test@rackspace.com", true, "ORD", "domain$random", "Password1")
        def response = cloud20.createUser(identityAdminToken, user)

        //Get user
        def getUserResponse = cloud20.getUser(identityAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User).value

        def addUserManageRole = cloud20.addApplicationRoleToUser(identityAdminToken, USER_MANAGE_ROLE_ID, userEntity.getId())

        def getUserRole = cloud20.getUserApplicationRole(identityAdminToken, USER_MANAGE_ROLE_ID, userEntity.getId())

        //Hard delete user
        cloud20.deleteApplicationRoleFromUser(identityAdminToken, USER_MANAGE_ROLE_ID, userEntity.getId())
        def actualDelete = cloud20.deleteUser(identityAdminToken, userEntity.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        addUserManageRole.status == 400
        getUserRole.status == 404
        actualDelete.status == 204
    }

    def "user-manage cannot update user admin" () {
        when:
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        //Update user
        def updateUserAdminResponse = cloud20.updateUser(defaultUserManageRoleToken, userAdmin.getId(), userAdmin)

        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        then:
        updateUserAdminResponse.status == 403
    }

    def "user-manage cannot get user admin's api key" () {
        when:
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        def getUserAdminApi = cloud20.getUserApiKey(defaultUserManageRoleToken, userAdmin.getId())

        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        then:
        getUserAdminApi.status == 403
    }

    def "getUserApiKey returns 404 when given invalid user Id"() {
        when:
        def getUserAdminApi = cloud20.getUserApiKey(defaultUserManageRoleToken, "invalidUserId")

        then:
        getUserAdminApi.status == 404
    }

    def "a user can be retrieved by email"() {
        when:
        def createUser = v2Factory.createUserForCreate("user1$sharedRandom", "user1$sharedRandom", email, true, "ORD", null, "Password1")
        def createdUser = cloud20.createUser(serviceAdminToken, createUser).getEntity(User).value
        cloud20.destroyUser(serviceAdminToken, createdUser.getId())
        def response = cloud20.getUsersByEmail(serviceAdminToken, email)

        then:
        response.status == expected

        where:
        expected | email
        200      | "user1$sharedRandom@email.com"
        200      | "$sharedRandom@random.com"
    }

    def "a list of users can be retrieved by email"() {
        when:
        def user1 = v2Factory.createUserForCreate("user10$sharedRandom", "user1$sharedRandom", email, true, "ORD", null, "Password1")
        def user2 = v2Factory.createUserForCreate("user20$sharedRandom", "user2$sharedRandom", email, true, "ORD", null, "Password1")
        def createdUser1 = cloud20.createUser(serviceAdminToken, user1).getEntity(User).value
        def createUser2 = cloud20.createUser(serviceAdminToken, user2).getEntity(User).value
        def response = cloud20.getUsersByEmail(serviceAdminToken, email)
        cloud20.destroyUser(serviceAdminToken, createdUser1.getId())
        cloud20.destroyUser(serviceAdminToken, createUser2.getId())

        then:
        response.status == expected

        where:
        expected | email
        200      | "user$sharedRandom@email.com"
        200      | "$sharedRandom@random.com"
    }

    def "operations on non-existent users return 'not found'"() {
        expect:
        response.status == 404

        where:
        response << [
                cloud20.getUserById(serviceAdminToken, "badId"),
                cloud20.getUserByName(serviceAdminToken, "badName"),
                cloud20.updateUser(serviceAdminToken, "badId", new User()),
                cloud20.deleteUser(serviceAdminToken, "badId"),
                cloud20.addCredential(serviceAdminToken, "badId", v2Factory.createPasswordCredentialsBase("someUser", "SomePassword1")),
                cloud20.addCredential(serviceAdminToken, "badId", v2Factory.createApiKeyCredentials("someUser", "someApiKey1"))
        ]
    }

    @Unroll
    def "invalid operations on create/update user returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("!@#What", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("What!@#", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("one name", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate(null, "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("a$sharedRandom", "display", "junk!@#", true, "ORD", null, "Password1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("z$sharedRandom", "display", "   ", true, "ORD", null, "Password1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("c$sharedRandom", "display", "test@rackspace.com", true, "ORD", null, "Pop1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("d$sharedRandom", "display", "test@rackspace.com", true, "ORD", null, "longpassword1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("e$sharedRandom", "display", "test@rackspace.com", true, "ORD", null, "Longpassword")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("f$sharedRandom", "displ:ay", "test@rackspace.com", true, "ORD", "someId", "Longpassword")),
                cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate("g$sharedRandom", "display", "test@rackspace.com", true, "ORD", null, "Longpassword1")),
                cloud20.updateUser(identityAdminToken, defaultUser.getId(), v2Factory.createUserForUpdate(null, null, null, null, true, "HAHAHAHA", "Password1"))
        ]
    }

    def 'operations with invalid tokens'() {
        expect:
        response.status == 401

        where:
        response << [
                cloud20.createUser("invalidToken", v2Factory.createUserForCreate("someName", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                cloud20.createUser(null, v2Factory.createUserForCreate("someName", "display", "test@rackspace.com", true, "ORD", null, "Password1")),  \
                cloud20.getUserById("invalidToken", "badId"),
                cloud20.getUserById(null, "badId"),
                cloud20.getUserByName("invalidToken", "badId"),
                cloud20.getUserByName(null, "badId"),
                cloud20.updateUser("invalidToken", "badId", new User()),
                cloud20.updateUser(null, "badId", new User()),
                cloud20.deleteUser("invalidToken", "badId"),
                cloud20.deleteUser(null, "badId"),
                cloud20.listUsers("invalidToken"),
                cloud20.listUsers(null)
        ]

    }

    @Unroll
    def 'forbidden operations for users'() {
        expect:
        response.status == 403

        where:
        response << [
                cloud20.createUser(defaultUserToken, v2Factory.createUserForCreate("someName", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                cloud20.updateUser(defaultUserToken, userAdmin.getId(), v2Factory.createUserForUpdate(null, "someOtherName", "someOtherDisplay", "some@rackspace.com", true, "ORD", "SomeOtherPassword1")),
                cloud20.getUserByName(defaultUserToken, userAdmin.getUsername()),
                cloud20.getUserByName(defaultUserToken, identityAdmin.getUsername()),
                cloud20.getUserByName(defaultUserToken, serviceAdmin.getUsername()),
                cloud20.getUserByName(userAdminToken, identityAdmin.getUsername()),
                cloud20.getUserByName(userAdminToken, serviceAdmin.getUsername()),
                cloud20.createGroup(defaultUserToken, v1Factory.createGroup()),
                cloud20.updateGroup(defaultUserToken, group.getId(), v1Factory.createGroup()),
                cloud20.deleteGroup(defaultUserToken, group.getId()),
                cloud20.getGroup(defaultUserToken, groupLocation),
                cloud20.getGroups(defaultUserToken),
                cloud20.createRegion(defaultUserToken, v1Factory.createRegion()),
                cloud20.updateRegion(defaultUserToken, sharedRegion.getName(), sharedRegion),
                cloud20.deleteRegion(defaultUserToken, sharedRegion.getName()),
                cloud20.getRegion(defaultUserToken, sharedRegion.getName()),
                cloud20.createQuestion(defaultUserToken, v1Factory.createQuestion()),
                cloud20.updateQuestion(defaultUserToken, "id", v1Factory.createQuestion()),
                cloud20.deleteQuestion(defaultUserToken, "id"),
        ]
    }

    @Unroll
    def 'forbidden but obfuscated operations for users'() {
        expect:
        // [B-82794] Modify [Get User] API Error Message
        response.status == 404

        where:
        response << [
                cloud20.getUserById(defaultUserToken, userAdmin.getId()),
                cloud20.getUserById(defaultUserToken, identityAdmin.getId()),
                cloud20.getUserById(defaultUserToken, serviceAdmin.getId()),
                cloud20.getUserById(userAdminToken, identityAdmin.getId()),
                cloud20.getUserById(userAdminToken, serviceAdmin.getId())
        ]
    }

    def 'valid operations on retrieving users'() {
        expect:
        response.status == 200

        where:
        response << [
                cloud20.listUsers(serviceAdminToken),
                cloud20.listUsers(identityAdminToken),
                cloud20.listUsers(userAdminToken),
                cloud20.listUsers(defaultUserToken),
                cloud20.getUserById(defaultUserToken, defaultUser.getId()),
                cloud20.getUserById(userAdminToken, defaultUser.getId()),
                cloud20.getUserById(userAdminToken, userAdmin.getId()),
                cloud20.getUserById(identityAdminToken, userAdmin.getId()),
                cloud20.getUserById(serviceAdminToken, userAdmin.getId()),
                cloud20.getUserByName(defaultUserToken, defaultUser.getUsername()),
                cloud20.getUserByName(userAdminToken, defaultUser.getUsername()),
                cloud20.getUserByName(userAdminToken, userAdmin.getUsername()),
                cloud20.getUserByName(identityAdminToken, userAdmin.getUsername()),
                cloud20.getUserByName(serviceAdminToken, userAdmin.getUsername())
        ]

    }

    def "Group CRUD"() {
        when:
        def random = ((String) UUID.randomUUID()).replace("-", "")
        def createGroupResponse = cloud20.createGroup(serviceAdminToken, v1Factory.createGroup("group$random", "this is a group"))

        def getGroupResponse = cloud20.getGroup(serviceAdminToken, createGroupResponse.location)
        def groupEntity = getGroupResponse.getEntity(Group)
        def getGroupByNameResponse = cloud20.getGroupByName(serviceAdminToken, groupEntity.value.name)
        def groupId = groupEntity.value.id

        def getGroupsResponse = cloud20.getGroups(serviceAdminToken)
        def groupsEntity = getGroupsResponse.getEntity(Groups)

        def updateGroupResponse = cloud20.updateGroup(serviceAdminToken, groupId, v1Factory.createGroup("group$random", "updated group"))

        def deleteGroupResponse = cloud20.deleteGroup(serviceAdminToken, groupId)


        then:
        createGroupResponse.status == 201
        createGroupResponse.location != null
        getGroupResponse.status == 200
        getGroupByNameResponse.status == 200
        getGroupsResponse.status == 200
        groupsEntity.value.getGroup().size() > 0
        updateGroupResponse.status == 200
        deleteGroupResponse.status == 204
    }

    def "Group Assignment CRUD for serviceAdmin modifying identityAdmin"() {
        when:
        def addUserToGroupResponse = cloud20.addUserToGroup(serviceAdminToken, group.getId(), identityAdmin.getId())

        def listGroupsForUserResponse = cloud20.listGroupsForUser(serviceAdminToken, identityAdmin.getId())
        def groups = listGroupsForUserResponse.getEntity(Groups).value

        def getUsersFromGroupResponse = cloud20.getUsersFromGroup(serviceAdminToken, group.getId())
        def users = getUsersFromGroupResponse.getEntity(UserList).value

        def removeUserFromGroupRespone = cloud20.removeUserFromGroup(serviceAdminToken, group.getId(), identityAdmin.getId())

        then:
        addUserToGroupResponse.status == 204

        listGroupsForUserResponse.status == 200
        groups.getGroup().size() == 1
        getUsersFromGroupResponse.status == 200
        users.getUser().size() == 1

        removeUserFromGroupRespone.status == 204
    }

    def "Group Assignment CRUD for identityAdmin modifying userAdmin"() {
        when:
        def addUserToGroupResponse = cloud20.addUserToGroup(identityAdminToken, group.getId(), userAdmin.getId())

        def listGroupsForUserResponse = cloud20.listGroupsForUser(identityAdminToken, userAdmin.getId())
        def groups = listGroupsForUserResponse.getEntity(Groups).value

        def getUsersFromGroupResponse = cloud20.getUsersFromGroup(identityAdminToken, group.getId())
        def users = getUsersFromGroupResponse.getEntity(UserList).value

        def removeUserFromGroupRespone = cloud20.removeUserFromGroup(identityAdminToken, group.getId(), userAdmin.getId())

        then:
        addUserToGroupResponse.status == 204

        listGroupsForUserResponse.status == 200
        groups.getGroup().size() >= 1
        getUsersFromGroupResponse.status == 200
        users.getUser().size() >= 1

        removeUserFromGroupRespone.status == 204
    }

    def "create user adds user to the group"() {
        given:
        def tempUserAdmin = "tempUserAdmin$sharedRandom"

        when:
        cloud20.addUserToGroup(identityAdminToken, group.getId(), userAdmin.getId())

        def getUsersFromGroupResponse1 = cloud20.getUsersFromGroup(identityAdminToken, group.getId())
        UserList users1 = getUsersFromGroupResponse1.getEntity(UserList).value

        cloud20.createUser(userAdminToken, v2Factory.createUserForCreate(tempUserAdmin, "display", "test@rackspace.com", true, null, testDomainId, "Password1"))
        def tempUser = cloud20.getUserByName(userAdminToken, tempUserAdmin).getEntity(User).value

        def getUsersFromGroupResponse2 = cloud20.getUsersFromGroup(identityAdminToken, group.getId())
        UserList users2 = getUsersFromGroupResponse2.getEntity(UserList).value

        cloud20.removeUserFromGroup(identityAdminToken, group.getId(), userAdmin.getId())

        cloud20.deleteUser(userAdminToken, tempUser.getId())

        then:
        users1.user.size() + 1 == users2.user.size()
        users2.user.findAll({it.username == tempUserAdmin}).size() == 1
    }

    @Unroll
    def "invalid operations on create/update group returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                cloud20.createGroup(serviceAdminToken, v1Factory.createGroup(null, "this is a group")),
                cloud20.createGroup(serviceAdminToken, v1Factory.createGroup("", "this is a group")),
                cloud20.createGroup(serviceAdminToken, v1Factory.createGroup("group", null)),
                cloud20.updateGroup(serviceAdminToken, group.getId(), v1Factory.createGroup(null, "this is a group")),
                cloud20.updateGroup(serviceAdminToken, group.getId(), v1Factory.createGroup("", "this is a group")),
                cloud20.updateGroup(serviceAdminToken, group.getId(), v1Factory.createGroup("group", null)),
                cloud20.addUserToGroup(serviceAdminToken, group.getId(), defaultUser.getId()),
                cloud20.removeUserFromGroup(serviceAdminToken, group.getId(), defaultUser.getId()),
        ]
    }

    def "update group with invalid group Id returns 404"() {
        when:
        def response = cloud20.updateGroup(serviceAdminToken, "invalidGroupId", v1Factory.createGroup("group name", "group description"))

        then:
        response.status == 404
    }

    def "invalid operations on get/create/update group returns 'not found'"() {
        expect:
        response.status == 404

        where:
        response << [
                cloud20.addUserToGroup(serviceAdminToken, group.getId(), "doesnotexist"),
                cloud20.addUserToGroup(serviceAdminToken, "doesnotexist", defaultUser.getId()),
                cloud20.getGroupById(serviceAdminToken, "doesnotexist"),
        ]
    }

    def "update region name is not allowed"() {
        given:
        Region region1 = v1Factory.createRegion("somename", false, false)

        when:
        def updateRegionResponse = cloud20.updateRegion(serviceAdminToken, sharedRegion.getName(), region1)

        then:
        updateRegionResponse.status == 400
    }

    def "region crud"() {
        given:
        def random = ("$randomness").replace('-', "")
        def regionName = "region${random}"
        Region region1 = v1Factory.createRegion(regionName, false, false)
        Region region2 = v1Factory.createRegion(regionName, true, false)

        when:
        def createRegionResponse = cloud20.createRegion(serviceAdminToken, region1)
        def getRegionResponse = cloud20.getRegion(serviceAdminToken, regionName)
        Region createdRegion = getRegionResponse.getEntity(Region)

        def updateRegionResponse = cloud20.updateRegion(serviceAdminToken, regionName, region2)
        def getUpdatedRegionResponse = cloud20.getRegion(serviceAdminToken, regionName)
        Region updatedRegion = getUpdatedRegionResponse.getEntity(Region)

        def getRegionsResponse = cloud20.getRegions(serviceAdminToken)
        Regions regions = getRegionsResponse.getEntity(Regions)

        def deleteRegionResponse = cloud20.deleteRegion(serviceAdminToken, regionName)
        def getDeletedRegionResponse = cloud20.getRegion(serviceAdminToken, regionName)


        then:
        createRegionResponse.status == 201
        createRegionResponse.location != null
        getRegionResponse.status == 200
        region1.name.equals(createdRegion.name)
        region1.enabled.equals(createdRegion.enabled)
        region1.isDefault.equals(createdRegion.isDefault)
        updateRegionResponse.status == 204
        region2.name.equals(updatedRegion.name)
        region2.enabled.equals(updatedRegion.enabled)
        region2.isDefault.equals(updatedRegion.isDefault)
        getRegionsResponse.status == 200
        regions.region.size() > 0
        deleteRegionResponse.status == 204
        getDeletedRegionResponse.status == 404
    }

    def "invalid operations on region returns 'not found'"() {
        expect:
        response.status == 404

        where:
        response << [
                cloud20.updateRegion(serviceAdminToken, "notfound", v1Factory.createRegion()),
                cloud20.deleteRegion(serviceAdminToken, "notfound"),
                cloud20.getRegion(serviceAdminToken, "notfound"),
        ]
    }

    def "invalid operations on create regions returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                cloud20.createRegion(serviceAdminToken, v1Factory.createRegion(null, true, false)),
        ]
    }

    def "create region that already exists returns conflict"() {
        when:
        def createRegionResponse = cloud20.createRegion(serviceAdminToken, sharedRegion)

        then:
        createRegionResponse.status == 409
    }

    def "listUsersWithRole called by default-user returns forbidden"() {
        when:
        def response = cloud20.listUsersWithRole(defaultUserToken, "1")

        then:
        response.status == 403
    }

    @Unroll
    def "listUsersWithRole called by admin with invalid roleId returns not found"() {
        given: "Invalid role id"
        def roleId = "invalid"

        when: "List users with role"
        def response = cloud20.listUsersWithRole(serviceAdminToken, roleId, acceptContentType)

        then:
        def expectedErrorMsg = String.format(DefaultCloud20Service.ROLE_ID_NOT_FOUND_ERROR_MESSAGE, roleId)
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, expectedErrorMsg)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Assert add role to user on tenant with invalid role id returns not found"() {
        given: "Invalid role id"
        def roleId = "invalid"

        when: "Add role to user on tenant"
        def response = cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, userAdmin.id, roleId, acceptContentType)

        then: "Assert not found"
        def expectedErrorMsg = String.format(DefaultCloud20Service.ROLE_ID_NOT_FOUND_ERROR_MESSAGE, roleId)
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, expectedErrorMsg)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def "listUsersWithRole called by admins returns success"() {
        expect:
        response.status == 200

        where:
        response << [
                cloud20.listUsersWithRole(identityAdminToken, sharedRole.getId()),
                cloud20.listUsersWithRole(serviceAdminToken, sharedRole.getId()),
                cloud20.listUsersWithRole(userAdminToken, sharedRole.getId())
        ]
    }

    def "listUsersWithRole empty list returns"() {
        when:
        def response = cloud20.listUsersWithRole(userAdminTwoToken, sharedRole.getId())

        then:
        response.status == 200
        response.getEntity(UserList).value.user.size == 0
        response.headers.getFirst("Link") == null
    }

    def "listUsersWithRole non empty list"() {
        when:
        def userAdminResponse = cloud20.listUsersWithRole(userAdminToken, sharedRole.getId())
        def serviceAdminResponse = cloud20.listUsersWithRole(serviceAdminToken, sharedRole.getId())
        def identityAdminResponse = cloud20.listUsersWithRole(identityAdminToken, sharedRole.getId())

        then:
        userAdminResponse.status == 200
        def userAdminResponseObj = userAdminResponse.getEntity(UserList).value
        userAdminResponseObj.user.size() == 3
        serviceAdminResponse.status == 200
        def serviceAdminResponseObj = serviceAdminResponse.getEntity(UserList).value
        serviceAdminResponseObj.user.size == 4
        identityAdminResponse.status == 200
        def identityAdminResponseObj = identityAdminResponse.getEntity(UserList).value
        identityAdminResponseObj.user.size == 4
    }

    def "listUsersWithRole pages results"() {
        when:
        def userAdminResponse1 = cloud20.listUsersWithRole(userAdminToken, sharedRole.getId(), "0", "1")
        def userAdminResponse2 = cloud20.listUsersWithRole(userAdminToken, sharedRole.getId(), "1", "1")
        def userAdminResponse3 = cloud20.listUsersWithRole(userAdminToken, sharedRole.getId(), "2", "1")
        def serviceAdminResponse1 = cloud20.listUsersWithRole(serviceAdminToken, sharedRole.getId(), "1", "2")
        def serviceAdminResponse2 = cloud20.listUsersWithRole(serviceAdminToken, sharedRole.getId(), "0", "2")
        def serviceAdminResponse3 = cloud20.listUsersWithRole(serviceAdminToken, sharedRole.getId(), "2", "2")
        def serviceAdminResponse4 = cloud20.listUsersWithRole(serviceAdminToken, sharedRole.getId(), "3", "4")

        then:
        userAdminResponse1.getEntity(UserList).value.user.size == 1
        userAdminResponse2.getEntity(UserList).value.user.size == 1
        userAdminResponse3.getEntity(UserList).value.user.size == 1
        serviceAdminResponse1.getEntity(UserList).value.user.size == 2
        serviceAdminResponse2.getEntity(UserList).value.user.size == 2
        serviceAdminResponse3.getEntity(UserList).value.user.size == 2
        serviceAdminResponse4.getEntity(UserList).value.user.size == 1

        def serviceAdminHeaders = serviceAdminResponse3.headers
        serviceAdminHeaders.getFirst("Link") != null

        def userAdminHeaders = userAdminResponse2.headers
        userAdminHeaders.getFirst("Link") != null
    }

    def "listUsersWithRole returns bad request with invalid paging paramters"() {
        expect:
        response.status == 400

        where:
        response << [
                cloud20.listUsersWithRole(serviceAdminToken, sharedRole.getId(), "-1", "5"),
                cloud20.listUsersWithRole(serviceAdminToken, sharedRole.getId(), "5", "-1"),
                cloud20.listUsersWithRole(serviceAdminToken, sharedRole.getId(), "-1", "-1")
        ]
    }

    def "listUsersWithRole returns invalid request when trying to list more users than size limit"() {
        given:
        def domains = []
        def users = []
        def role = utils.createRole()
        staticIdmConfiguration.setProperty(IdentityConfig.LIST_USERS_BY_ROLE_LIMIT_NAME, 2)
        def userSizeLimit = config.getInt(IdentityConfig.LIST_USERS_BY_ROLE_LIMIT_NAME)
        def identityAdmin = utils.createIdentityAdmin()
        def identityAdminToken = utils.getToken(identityAdmin.username, DEFAULT_PASSWORD)
        for(i in 0..userSizeLimit) {
            def domain = utils.createDomain()
            domains << domain
            def user = utils.createUser(identityAdminToken, testUtils.getRandomUUID("userAdmin"), domain)
            users << user
            utils.addRoleToUser(user, role.id)
        }

        when:
        def response = cloud20.listUsersWithRole(utils.getServiceAdminToken(), role.id)

        then:
        response.status == 400

        cleanup:
        for(user in users) {
            utils.deleteUser(user)
        }
        for(domain in domains) {
            utils.deleteDomain(domain)
        }
        utils.deleteRole(role)
    }

    def "listUsersWithRole offset greater than result set length returns 200 with empty list"() {
        expect:
        response.status == 200
        response.getEntity(UserList).value.user.size == 0

        where:
        response << [
                cloud20.listUsersWithRole(serviceAdminToken, sharedRole.getId(), "100", "10"),
                cloud20.listUsersWithRole(userAdminToken, sharedRole.getId(), "100", "10")
        ]
    }

    def "listUsersWithRole role assigned to no one"() {
        when:
        def responseOne = cloud20.listUsersWithRole(serviceAdminToken, sharedRoleTwo.getId())
        def responseTwo = cloud20.listUsersWithRole(serviceAdminToken, sharedRoleTwo.getId(), "0", "10")
        def responseThree = cloud20.listUsersWithRole(identityAdminToken, sharedRoleTwo.getId())
        def responseFour = cloud20.listUsersWithRole(identityAdminToken, sharedRoleTwo.getId(), "0", "10")
        def responseFive = cloud20.listUsersWithRole(userAdminToken, sharedRoleTwo.getId())
        def responseSix = cloud20.listUsersWithRole(userAdminToken, sharedRoleTwo.getId(), "0", "10")

        then:
        responseOne.getEntity(UserList).value.user.size == 0
        responseTwo.getEntity(UserList).value.user.size == 0
        responseThree.getEntity(UserList).value.user.size == 0
        responseFour.getEntity(UserList).value.user.size == 0
        responseFive.getEntity(UserList).value.user.size == 0
        responseSix.getEntity(UserList).value.user.size == 0
    }

    def "question crud"() {
        given:
        def question1 = v1Factory.createQuestion(null, "question")
        def question2 = v1Factory.createQuestion(null, "question changed")

        when:
        def createResponse = cloud20.createQuestion(serviceAdminToken, question1)
        def getCreateResponse = cloud20.getQuestionFromLocation(serviceAdminToken, createResponse.location)
        def createEntity = getCreateResponse.getEntity(Question)
        question2.id = createEntity.id

        def updateResponse = cloud20.updateQuestion(serviceAdminToken, createEntity.id, question2)
        def getUpdateResponse = cloud20.getQuestion(serviceAdminToken, createEntity.id)
        def updateEntity = getUpdateResponse.getEntity(Question)

        def deleteResponse = cloud20.deleteQuestion(serviceAdminToken, createEntity.id)
        def getDeleteResponse = cloud20.getQuestion(serviceAdminToken, createEntity.id)

        def getQuestionResponse = cloud20.getQuestions(serviceAdminToken)
        def questions = getQuestionResponse.getEntity(Questions)

        then:
        createResponse.status == 201
        createResponse.location != null
        createEntity.id != null
        question1.question == createEntity.question

        updateResponse.status == 204
        updateEntity.id == question2.id
        updateEntity.question == question2.question

        deleteResponse.status == 204
        getDeleteResponse.status == 404

        getQuestionResponse.status == 200
        questions != null
    }

    @Unroll
    def "Test feature flag 'rsid.uuid.questions.enabled' = #flag"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.USE_UUID_IDS_FOR_NEW_QUESTION_ENABLED_PROP, flag)
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        Question question = v1Factory.createQuestion(null, "question")

        when: "Create and get question"
        def response = cloud20.createQuestion(utils.getServiceAdminToken(), question)
        def getResponse = cloud20.getQuestionFromLocation(utils.getServiceAdminToken(), response.location)
        def questionEntity = getResponse.getEntity(Question)

        then: "Assert question id"
        response.status == HttpStatus.SC_CREATED
        if (flag) {
            // 32 length UUID without dashes
            testUtils.assertStringPattern("[a-zA-Z0-9]{32}", questionEntity.id)
        } else {
            testUtils.assertStringPattern("\\d+", questionEntity.id)
        }

        when: "Add secretQA to user"
        def answer = testUtils.getRandomUUID("Answer")
        def secretQA = v1Factory.createSecretQA(questionEntity.id, answer)
        response = cloud20.createSecretQA(utils.getServiceAdminToken(), userAdmin.id, secretQA)

        then:
        response.status == HttpStatus.SC_OK

        when: "Get secretQA for user"
        response = cloud20.getSecretQA(utils.getServiceAdminToken(), userAdmin.id)
        def secretQAEntity = response.getEntity(SecretQA)

        then: "Assert correct answer"
        response.status == HttpStatus.SC_OK
        secretQAEntity.answer == answer

        cleanup:
        utils.deleteQuestion(questionEntity)
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        reloadableConfiguration.reset()

        where:
        flag << [true, false]
    }

    def "invalid operations on question returns 'not found'"() {
        expect:
        response.status == 404

        where:
        response << [
                cloud20.updateQuestion(serviceAdminToken, "notfound", v1Factory.createQuestion("notfound", "question")),
                cloud20.deleteQuestion(serviceAdminToken, "notfound"),
                cloud20.getQuestion(serviceAdminToken, "notfound"),
        ]
    }

    def "invalid operations on question returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                cloud20.updateQuestion(serviceAdminToken, "ids", v1Factory.createQuestion("dontmatch", "question")),
                cloud20.updateQuestion(serviceAdminToken, "id", v1Factory.createQuestion("id", null)),
                cloud20.createQuestion(serviceAdminToken, v1Factory.createQuestion("id", null)),
        ]
    }

    def "listUsers returns forbidden (invalid token)"() {
        expect:
        response.status == 401

        where:
        response << [
                cloud20.listUsers(""),
                cloud20.listUsers("1")
        ]
    }

    def "listUsers returns default user"() {
        when:
        def users = cloud20.listUsers(defaultUserToken).getEntity(UserList).value.user

        then:
        users[0].username.equals(defaultUser.username)
    }

    def "listUsers caller is user-admin returns users from domain"() {
        when:
        def users = cloud20.listUsers(userAdminToken).getEntity(UserList).value.user

        then:
        users.size() == 7
    }

    def "listUsers caller is user-manage returns users from domain"() {
        when:
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())
        def users = cloud20.listUsers(defaultUserManageRoleToken).getEntity(UserList).value.user

        then:
        users.size() == 6

        cleanup:
        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())
    }

    def "listUsers caller is identity-admin or higher returns paged results"() {
        expect:
        response.status == 200
        response.headers.getFirst("Link") != null

        where:
        response << [
                cloud20.listUsers(identityAdminToken),
                cloud20.listUsers(identityAdminToken, "0", "10"),
                cloud20.listUsers(identityAdminToken, "15", "10"),
                cloud20.listUsers(serviceAdminToken),
                cloud20.listUsers(serviceAdminToken, "0", "10"),
                cloud20.listUsers(serviceAdminToken, "15", "10"),
        ]
    }

    def "listUsers throws bad request"() {
        expect:
        response.status == 400

        where:
        response << [
                cloud20.listUsers(serviceAdminToken, "0", "-1"),
                cloud20.listUsers(serviceAdminToken, "-1", "10"),
                cloud20.listUsers(serviceAdminToken, "offset", "10"),
                cloud20.listUsers(serviceAdminToken, "0", "limit"),
        ]
    }

    def "listUsers returns 200 and empty list when offset exceedes result set size"() {
        expect:
        response.status == 200
        response.getEntity(UserList).value.user.size == 0

        where:
        response << [
                cloud20.listUsers(serviceAdminToken, "100000000", "25"),
                cloud20.listUsers(identityAdminToken, "10000000", "50"),
        ]
    }

    def "create and delete applicationRole with insufficient priveleges"() {
        expect:
        response.status == 403

        where:
        response << [
                cloud20.addApplicationRoleToUser(defaultUserToken, sharedRole.getId(), defaultUserForAdminTwo.getId()),
                cloud20.addApplicationRoleToUser(userAdminToken, sharedRole.getId(), defaultUserForAdminTwo.getId()),
                cloud20.deleteApplicationRoleFromUser(defaultUserToken, sharedRole.getId(), defaultUser.getId()),
                cloud20.deleteApplicationRoleFromUser(userAdminToken, sharedRole.getId(), defaultUserForAdminTwo.getId()),
                //deleteApplicationRoleFromUser(userAdminToken, sharedRole.getId(), defaultUser.getId()),
        ]
    }

    def "adding identity:* to user with identity:* role And deleting own identity:* role return forbidden"() {
        expect:
        response.status == 403

        where:
        response << [
                cloud20.addApplicationRoleToUser(serviceAdminToken, "1", defaultUser.getId()),
                cloud20.addApplicationRoleToUser(serviceAdminToken, "1", userAdmin.getId()),
                cloud20.addApplicationRoleToUser(serviceAdminToken, "1", identityAdmin.getId()),
                cloud20.addApplicationRoleToUser(serviceAdminToken, "1", serviceAdmin.getId()),
                cloud20.deleteApplicationRoleFromUser(userAdminToken, "3", userAdmin.getId()),
                cloud20.deleteApplicationRoleFromUser(identityAdminToken, "1", identityAdmin.getId()),
                cloud20.deleteApplicationRoleFromUser(serviceAdminToken, "4", serviceAdmin.getId())
        ]
    }

    def "application role assignment" (){
        when:
        def responseAddRole1 = cloud20.addApplicationRoleToUser(identityAdminToken, sharedRoleTwo.getId(), userAdmin.getId())
        def responseAddRole2 = cloud20.addApplicationRoleToUser(serviceAdminToken, sharedRoleTwo.getId(), identityAdmin.getId())
        def responseDeleteRole1 = cloud20.deleteApplicationRoleFromUser(identityAdminToken, sharedRoleTwo.getId(), userAdmin.getId())
        def responseDeleteRole2 = cloud20.deleteApplicationRoleFromUser(serviceAdminToken, sharedRoleTwo.getId(), identityAdmin.getId())

        then:
        responseAddRole1.status == 200
        responseAddRole2.status == 200
        responseDeleteRole1.status == 204
        responseDeleteRole2.status == 204
    }

    def "deleting role from user without role returns not found (404)"() {
        given:
        cloud20.deleteApplicationRoleFromUser(identityAdminToken, sharedRoleTwo.getId(), userAdmin.getId())

        when:
        def response = cloud20.deleteApplicationRoleFromUser(identityAdminToken, sharedRoleTwo.getId(), userAdmin.getId())

        then:
        response.status == 404
    }

    def "adding to and deleting roles from user on tenant return 403"() {
        expect:
        response.status == 403

        where:
        response << [
                cloud20.deleteRoleFromUserOnTenant(userAdminToken, tenant.id, defaultUserForAdminTwo.getId(), sharedRole.getId()),
                cloud20.deleteRoleFromUserOnTenant(userAdminToken, tenant.id, defaultUser.getId(), sharedRole.getId()),
                cloud20.addRoleToUserOnTenant(userAdminToken, tenant.id, defaultUserForAdminTwo.getId(), sharedRole.getId()),
                cloud20.addRoleToUserOnTenant(userAdminToken, tenant.id, defaultUser.getId(), sharedRole.getId())
        ]
    }

    def "Identity User type roles can not be granted to a user on a tenant"() {
        expect:
        response.status == 403

        where:
        response << [
                cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), defaultUserRoleId),
                cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), userManageRoleId),
                cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), userAdminRoleId),
                cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), identityAdminRoleId),
                cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), serviceAdminRoleId)
        ]
    }

    def "Identity product roles that are not user type roles can be added to a user on a tenant"() {
        Role identityProductRole = utils.createRole(v2Factory.createRole("identity:" + getRandomUUID()))

        expect:
        cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), identityProductRole.id).status == HttpStatus.SC_OK

        cleanup:
        cloud20.deleteRoleFromUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), identityProductRole.id)
        cloud20.deleteRole(serviceAdminToken, identityProductRole.id)
    }


    def "Tenant role assignment"() {
        when:
        def response1 = cloud20.addRoleToUserOnTenant(identityAdminToken, tenant.id, userAdmin.getId(), sharedRoleTwo.id)
        def response2 = cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, identityAdmin.getId(), sharedRoleTwo.id)
        def response3 = cloud20.deleteRoleFromUserOnTenant(identityAdminToken, tenant.id, userAdmin.getId(), sharedRoleTwo.id)
        def response4 = cloud20.deleteRoleFromUserOnTenant(serviceAdminToken, tenant.id, identityAdmin.getId(), sharedRoleTwo.id)

        then:
        response1.status == 200
        response2.status == 200
        response3.status == 204
        response4.status == 204
    }

    def "delete role returns 403"() {
        expect:
        response.status == 403

        where:
        response << [
                cloud20.deleteRole(userAdminToken, sharedRoleTwo.id),
                cloud20.deleteRole(identityAdminToken, identityAdminRoleId),
                cloud20.deleteRole(identityAdminToken, userAdminRoleId),
                cloud20.deleteRole(serviceAdminToken, identityAdminRoleId),
                cloud20.deleteRole(serviceAdminToken, userAdminRoleId)
        ]
    }

    def "Create createSecretQA and get createSecretQA"() {
        when:
        def response = cloud20.createSecretQA(serviceAdminToken,defaultUser.getId(), v1Factory.createSecretQA("1","answer"))
        def createSecretQAResponse = cloud20.getSecretQAs(serviceAdminToken, defaultUser.getId()).getEntity(SecretQAs)

        then:
        response.status == 200
        createSecretQAResponse.secretqa.get(0).answer == "answer"
    }

    def "Create/Get createSecretQA returns 403"() {
        expect:
        response.status == 403

        where:
        response << [
                cloud20.createSecretQA(defaultUserToken, serviceAdmin.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.createSecretQA(defaultUserToken, identityAdmin.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.createSecretQA(defaultUserToken, userAdmin.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.createSecretQA(userAdminToken, serviceAdmin.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.createSecretQA(userAdminToken, identityAdmin.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.createSecretQA(userAdminToken, userAdminTwo.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.createSecretQA(userAdminToken, defaultUserForAdminTwo.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.getSecretQAs(defaultUserToken, serviceAdmin.getId()),
                cloud20.getSecretQAs(defaultUserToken, identityAdmin.getId()),
                cloud20.getSecretQAs(userAdminToken, serviceAdmin.getId()),
                cloud20.getSecretQAs(userAdminToken, identityAdmin.getId()),
                cloud20.getSecretQAs(userAdminToken, userAdminTwo.getId()),
                cloud20.getSecretQAs(userAdminToken, defaultUserForAdminTwo.getId())
        ]

    }

    def "Create/Get createSecretQA returns 401"() {
        expect:
        response.status == 401

        where:
        response << [
                cloud20.createSecretQA("", defaultUser.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.createSecretQA("", defaultUser.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.createSecretQA(null, defaultUser.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.createSecretQA(null, defaultUser.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.getSecretQAs("", serviceAdmin.getId()),
                cloud20.getSecretQAs("", identityAdmin.getId()),
                cloud20.getSecretQAs(null, serviceAdmin.getId()),
                cloud20.getSecretQAs(null, identityAdmin.getId()),
        ]

    }

    def "Create/Get createSecretQA returns 400"() {
        expect:
        response.status == 400

        where:
        response << [
                cloud20.createSecretQA(serviceAdminToken, defaultUser.getId(), v1Factory.createSecretQA(null, "answer")),
                cloud20.createSecretQA(serviceAdminToken, defaultUser.getId(), v1Factory.createSecretQA("1", null))
        ]

    }

    def "Create/Get createSecretQA returns 404"() {
        expect:
        response.status == 404

        where:
        response << [
                cloud20.createSecretQA(serviceAdminToken, "badId", v1Factory.createSecretQA("1", "answer")),
                cloud20.createSecretQA(serviceAdminToken, defaultUser.getId(), v1Factory.createSecretQA("badId", "answer")),
                cloud20.getSecretQAs(serviceAdminToken, "badId")
        ]

    }

    def "Create/Get createSecretQA returns 200"() {
        expect:
        response.status == 200

        where:
        response << [
                cloud20.createSecretQA(serviceAdminToken, defaultUser.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.createSecretQA(identityAdminToken, defaultUser.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.createSecretQA(userAdminToken, defaultUser.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.createSecretQA(defaultUserToken, defaultUser.getId(), v1Factory.createSecretQA("1", "answer")),
                cloud20.getSecretQAs(serviceAdminToken, defaultUser.getId()),
                cloud20.getSecretQAs(identityAdminToken, defaultUser.getId()),
                cloud20.getSecretQAs(userAdminToken, defaultUser.getId()),
                cloud20.getSecretQAs(defaultUserToken, defaultUser.getId()),
        ]
    }

    def "listRoles returns success"() {
        expect:
        response.status == 200

        where:
        response << [
                cloud20.listRoles(userAdminToken, cloudAuthClientId, null, null),
                cloud20.listRoles(identityAdminToken, cloudAuthClientId, null, null),
                cloud20.listRoles(serviceAdminToken, cloudAuthClientId, null, null),
                cloud20.listRoles(userAdminToken, null, null, null),
                cloud20.listRoles(identityAdminToken, null, null, null),
                cloud20.listRoles(serviceAdminToken, null, null, null)
        ]
    }

    def "listRoles returns valid link headers"() {
        given:
        def response = cloud20.listRoles(serviceAdminToken, null, "2", "1")
        def queryParams = parseLinks(response.headers.get("link"))

        when:
        def first_response = cloud20.listRoles(serviceAdminToken, null, queryParams["first"][0], queryParams["first"][1])
        def last_response = cloud20.listRoles(serviceAdminToken, null, queryParams["last"][0], queryParams["last"][1])
        def prev_response = cloud20.listRoles(serviceAdminToken, null, queryParams["prev"][0], queryParams["prev"][1])
        def next_response = cloud20.listRoles(serviceAdminToken, null, queryParams["next"][0], queryParams["next"][1])

        then:
        first_response.status == 200
        last_response.status == 200
        prev_response.status == 200
        next_response.status == 200
    }

    def "listRoles returns forbidden"() {
        expect:
        response.status == 403

        where:
        response << [
                cloud20.listRoles(defaultUserToken, null, null, null),
                cloud20.listRoles(defaultUserToken, cloudAuthClientId, null, null)
        ]
    }

    def "listRoles returns not authorized"() {
        expect:
        response.status == 401

        where:
        response << [
                cloud20.listRoles("token", null, null, null),
                cloud20.listRoles("token", cloudAuthClientId, null, null)
        ]
    }

    def "Can iterate through paged roles"() {
        when:
        Map<Integer, Integer> results = followPages()

        then:
        for (result in results) {
            assert(result.value == 200)
        }
    }

    def followPages() {
        int page = 0
        def response = cloud20.listRoles(serviceAdminToken, null, "0", "3")
        def queryParams = parseLinks(response.headers.get("link"))
        Map<Integer, Integer> responseStatus = new HashMap<Integer, Integer>()

        while (queryParams.containsKey("next")) {
            page++
            if (page > MAX_TRIES) {
                break
            }
            response = cloud20.listRoles(serviceAdminToken, null, queryParams["next"][0], queryParams["next"][1])
            queryParams = parseLinks(response.headers.get("link"))
            responseStatus.put(page, response.status)
        }
        return responseStatus
    }

    def parseLinks(List<String> header) {
        List<String> links = header[0].split(",")
        Map<String, String[]> params = new HashMap<String, String[]>()
        setLinkParams(links, params)
        return params
    }

    def "updateCredentials with valid passwords should be able to authenticate"() {
        given:
        String username = "userUpdateCred" + sharedRandom
        def response = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", "someDomain$randomSuffix", "Password1"))
        User user = response.getEntity(User).value
        String password = "Password1~!@#\$%^&*_#\$%^% <>?:\"^(%)'"
        PasswordCredentialsBase creds = new PasswordCredentialsBase().with {
            it.username = username
            it.password = password
            return it
        }

        when:
        def updateCreds = cloud20.updateCredentials(identityAdminToken, user.id, creds)
        String updatePassword = updateCreds.getEntity(PasswordCredentialsBase).value.password
        def authenticate = cloud20.authenticatePassword(user.username,updatePassword)

        then:
        updateCreds.status == 200
        authenticate.status == 200
    }

    def "updateCredentials with invalid password should return BadRequestException"() {
        given:
        String username = "userUpdateCred2" + sharedRandom
        def response = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", "someDomain$randomSuffix", "Password1"))
        User user = response.getEntity(User).value
        String password = "Password1~!@א"
        PasswordCredentialsBase creds = new PasswordCredentialsBase().with {
            it.username = username
            it.password = password
            return it
        }

        when:
        def updateCreds = cloud20.updateCredentials(identityAdminToken, user.id, creds)

        then:
        updateCreds.status == 400
    }

    def "Disabling a user should return 404 when its token gets validated"() {
        given:
        String username = "disabledUser" + sharedRandom
        def response = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", "someDomain$randomSuffix", "Password1"))
        User user = response.getEntity(User).value

        when:
        def authRequest = cloud20.authenticatePassword(username, "Password1")
        String token = authRequest.getEntity(AuthenticateResponse).value.token.id
        def userForUpdate = v2Factory.createUserForUpdate(null, username, null, "test@rackspace.com", false, null, null)
        def updateUserResponse = cloud20.updateUser(serviceAdminToken, user.id, userForUpdate)
        def validateResponse = cloud20.validateToken(serviceAdminToken, token)
        def deleteResponses = cloud20.deleteUser(serviceAdminToken, user.id)

        then:
        authRequest.status == 200
        token != null
        updateUserResponse.status == 200
        validateResponse.status == 404
        deleteResponses.status == 204
    }

    def "Disable a userAdmin disables his subUsers"() {
        given:
        def domain = "someDomain$sharedRandom"
        def adminUsername = "userAdminDisableSub$sharedRandom"
        def username = "userSub$sharedRandom"
        def password = "Password1"
        def userAdminForCreate = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(adminUsername, "displayName", "someEmail@rackspace.com", true, "ORD", domain, password))
        User userAdmin = userAdminForCreate.getEntity(User).value

        def userAdminToken = cloud20.authenticatePassword(adminUsername, password).getEntity(AuthenticateResponse).value.token.id
        def createUserForCreate = cloud20.createUser(userAdminToken, v2Factory.createUserForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", domain, "Password1"))

        when:
        userAdmin.enabled = false;
        def updateUserResponse = cloud20.updateUser(identityAdminToken, userAdmin.id, userAdmin)
        def getUserResponse = cloud20.getUser(serviceAdminToken, createUserForCreate.location)
        User user = getUserResponse.getEntity(User).value

        then:
        updateUserResponse.status == 200
        user.enabled == false

        cleanup:
        utils.deleteUserQuietly(user, serviceAdminToken)
        utils.deleteUserQuietly(userAdmin, serviceAdminToken)
    }

    def "default user one cannot get default user two's admins"() {
        when:
        def response = cloud20.getAdminsForUser(defaultUserToken, defaultUserTwo.id)

        then:
        response.status == 403
    }

    def "default user gets his admins"() {
        when:
        def response = cloud20.getAdminsForUser(defaultUserToken, defaultUser.id)

        then:
        response.status == 200
        def users = response.getEntity(UserList).value
        users.getUser().size != 0
    }

    def "if user has no domain then an empty list is returned for his admins"() {
        when:
        def response = cloud20.getAdminsForUser(identityAdminToken, identityAdmin.id)

        then:
        response.status == 200
        def users = response.getEntity(UserList).value
        users.getUser().size == 0
    }

    def "Adding a group to user-admin also adds the group to its sub-users"(){
        given:
        String username = "groupUserAdmin" + sharedRandom
        String domainId = "myGroupDomain" + sharedRandom
        String subUsername = "groupDefaultUser" + sharedRandom


        when:
        def userAdminForCreate = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, "displayName", "email@rackspace.com", true, "ORD", domainId, "Password1"))
        def userAdmin = userAdminForCreate.getEntity(User).value
        def authRequest = cloud20.authenticatePassword(username, "Password1")
        String token = authRequest.getEntity(AuthenticateResponse).value.token.id
        def defaultUserForCreate = cloud20.createUser(token, v2Factory.createUserForCreate(subUsername, "displayName", "email@rackspace.com", true, "ORD", null, "Password1"))
        def defaultUser = defaultUserForCreate.getEntity(User).value
        def groupName = "myGroup" + sharedRandom
        def groupResponse = cloud20.createGroup(serviceAdminToken, v1Factory.createGroup(groupName,groupName))
        def group = groupResponse.getEntity(Group)
        def addUserToGroupResponse = cloud20.addUserToGroup(serviceAdminToken, group.value.id, userAdmin.id)
        //Get groups
        def getUserAdminGroups = cloud20.listGroupsForUser(serviceAdminToken, userAdmin.id)
        def userAdminGroups = getUserAdminGroups.getEntity(Groups)
        def getDefaultUserGroups = cloud20.listGroupsForUser(serviceAdminToken, defaultUser.id)
        def defaultUserGroups = getDefaultUserGroups.getEntity(Groups)
        //Delete Group
        def deleteUserAdminGroupResponse = cloud20.removeUserFromGroup(serviceAdminToken, group.value.id,userAdmin.id)
        //Get users with group deleted
        def getUserAdminDeletedGroup = cloud20.listGroupsForUser(serviceAdminToken, userAdmin.id)
        def userAdminDeletedGroup = getUserAdminDeletedGroup.getEntity(Groups)
        def getDefaultUserDeletedGroup = cloud20.listGroupsForUser(serviceAdminToken, defaultUser.id)
        def defaultUserDeletedGroup = getDefaultUserDeletedGroup.getEntity(Groups)

        //Clean data
        def deleteResponses = cloud20.deleteUser(serviceAdminToken, defaultUser.id)
        def deleteAdminResponses = cloud20.deleteUser(serviceAdminToken, userAdmin.id)
        def deleteGroupResponse = cloud20.deleteGroup(serviceAdminToken, group.value.id)
        utils.disableDomain(domainId)
        def deleteDomainResponse = cloud20.deleteDomain(serviceAdminToken, domainId)

        then:
        userAdminForCreate.status == 201
        authRequest.status == 200
        token != null
        defaultUserForCreate.status == 201
        addUserToGroupResponse.status == 204

        userAdminGroups.value.group.get(0).name == groupName
        defaultUserGroups.value.group.get(0).name == groupName

        deleteUserAdminGroupResponse.status == 204
        getUserAdminDeletedGroup.status == 200
        getDefaultUserDeletedGroup.status == 200

        userAdminDeletedGroup.value.group.get(0).name == "Default"
        defaultUserDeletedGroup.value.group.get(0).name == "Default"

        deleteResponses.status == 204
        deleteAdminResponses.status == 204
        deleteGroupResponse.status == 204
        deleteDomainResponse.status == 204
    }

    def "Trying to delete group with enabled users causes BadRequest"(){
        given:
        String username = "groupUserAdmin" + getRandomUUID()
        String domainId = "myGroupDomain" + getRandomUUID()

        def userAdminForCreate = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, "displayName", "email@rackspace.com", true, "ORD", domainId, "Password1"))
        def userAdmin = userAdminForCreate.getEntity(User).value

        def groupName =  getRandomUUID()
        def groupResponse = cloud20.createGroup(serviceAdminToken, v1Factory.createGroup(groupName,groupName))
        def group = groupResponse.getEntity(Group).value
        def addUserToGroupResponse = cloud20.addUserToGroup(serviceAdminToken, group.id, userAdmin.id)

        when:
        //Delete Group with enabled user
        def deleteGroupResponse = cloud20.deleteGroup(serviceAdminToken, group.id)

        then:
        deleteGroupResponse.status == HttpServletResponse.SC_BAD_REQUEST

        when: "delete user-admin"
        def deleteAdminResponses = cloud20.deleteUser(serviceAdminToken, userAdmin.id)
        def deleteGroupResponseAfterAdminDeleted = cloud20.deleteGroup(serviceAdminToken, group.id)

        then:
        deleteGroupResponseAfterAdminDeleted.status == HttpServletResponse.SC_NO_CONTENT
    }

    def "authenticate returns password authentication type in response"() {
        when:
        def response = cloud20.authenticatePassword("admin$sharedRandom", "Password1")
        def authBy = response.getEntity(AuthenticateResponse).value.token.authenticatedBy.credential[0]

        then:
        response.status == 200
        authBy == GlobalConstants.AUTHENTICATED_BY_PASSWORD
    }

    @Ignore
    def "authenticate returns apiKey authentication type in response"() {
        when:
        def response = cloud20.getUserApiKey(serviceAdminToken, userAdmin.getId()).getEntity(com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials).value
        def authResp = cloud20.authenticateApiKey(userAdmin.name, response.apiKey)

        then:
        authResp.getEntity(AuthenticateResponse).getToken().getAny().contains("rax-auth:authenticatedBy")
    }

    def "remove product roles throws 400 when type not rbac"() {
        when:
        def removeRolesResponse = cloud20.deleteUserProductRoles(serviceAdminToken, defaultUserForProductRole.id, "NOTRBAC")

        then:
        removeRolesResponse.status == 400
    }

    def "remove product roles removes all 1000 weight roles"() {
        when:
        def addRoleResponse = cloud20.addApplicationRoleToUser(userAdminToken, productRole.id, defaultUserForProductRole.id)
        def listRolesResponse = cloud20.listUserGlobalRoles(serviceAdminToken, defaultUserForProductRole.id)
        def roleList = listRolesResponse.getEntity(RoleList).value
        def removeRolesResponse = cloud20.deleteUserProductRoles(serviceAdminToken, defaultUserForProductRole.id, "rbac")
        def relistRolesResponse = cloud20.listUserGlobalRoles(serviceAdminToken, defaultUserForProductRole.id)
        def roleList2 = relistRolesResponse.getEntity(RoleList).value

        then:
        addRoleResponse.status == 200
        listRolesResponse.status == 200
        roleList.role.size() == 2

        def roleId1 = roleList.role.get(0).id
        def roleId2 = roleList.role.get(1).id

        def success = false

        if (roleId1.equals(productRole.id)) {
            success = roleId2.equals(defaultUserRoleId)
        } else if (roleId1.equals(defaultUserRoleId)) {
            success = roleId2.equals(productRole.id)
        }
        success == true
        removeRolesResponse.status == 204
        relistRolesResponse.status == 200

        def roleId3 = roleList2.role.get(0).id

        roleList2.role.size() == 1
        roleId3.equals(defaultUserRoleId)
    }

    def "validate returns password authentication type in response"() {
        when:
        def response = cloud20.validateToken(serviceAdminToken, userAdminToken)
        def authBy = response.getEntity(AuthenticateResponse).value.token.authenticatedBy.credential[0]

        then:
        response.status == 200
        authBy == GlobalConstants.AUTHENTICATED_BY_PASSWORD
    }

    def "Identity-Admin should not be allowd to delete a service"() {
        given:
        def serviceName = "someTestApplication" + sharedRandom
        def serviceType = "identity"
        def service = v1Factory.createService(null, serviceName, serviceType)

        when:
        def createServiceResponse = cloud20.createService(serviceAdminToken, service)
        def serviceEntity = createServiceResponse.getEntity(Service)
        def deleteServiceIdentityAdminTokenResponse = cloud20.deleteService(identityAdminToken, serviceEntity.id)
        def deleteServiceResponse = cloud20.deleteService(serviceAdminToken, serviceEntity.id)

        then:
        createServiceResponse.status == 201
        serviceEntity.name == serviceName
        serviceEntity.type == serviceType
        deleteServiceIdentityAdminTokenResponse.status == 403
        deleteServiceResponse.status == 204
    }

    def "Service admin should be the only one to add roles in CI/Foundation"() {
        when:
        String roleName = "someServiceRole" + sharedRandom
        def result = cloud20.createRole((String)token, v2Factory.createRole(roleName, serviceId))
        if (result.status == 201){
            cloud20.deleteRole(serviceAdminToken, result.getEntity(Role).value.id)
        }

        then:
        result.status == expectedResult

        where:
        token                | serviceId                                  | expectedResult
        identityAdminToken   | "bde1268ebabeeabb70a0e702a4626977c331d5c4" | 201
        identityAdminToken   | "18e7a7032733486cd32f472d7bd58f709ac0d221" | 201
        userAdminToken       | "bde1268ebabeeabb70a0e702a4626977c331d5c4" | 403
        userAdminToken       | "18e7a7032733486cd32f472d7bd58f709ac0d221" | 403
        defaultUserToken     | "bde1268ebabeeabb70a0e702a4626977c331d5c4" | 403
        defaultUserToken     | "18e7a7032733486cd32f472d7bd58f709ac0d221" | 403
        serviceAdminToken    | "bde1268ebabeeabb70a0e702a4626977c331d5c4" | 201
        serviceAdminToken    | "18e7a7032733486cd32f472d7bd58f709ac0d221" | 201
        //Used when global roles are in its own application
//        identityAdminToken   | "bde1268ebabeeabb70a0e702a4626977c331d5c4" | 403
//        identityAdminToken   | "18e7a7032733486cd32f472d7bd58f709ac0d221" | 403
    }

    def "Creating a role without specifying the serviceId should create it under IdentityGlobalRoles"() {
        given:
        String roleName = "identityGlobalRole" + sharedRandom

        when:
        def identityAdminResponse = cloud20.createRole(identityAdminToken, v2Factory.createRole(roleName, null))
        def deleteResponse = cloud20.deleteRole(serviceAdminToken, identityAdminResponse.getEntity(Role).value.id)

        then:
        identityAdminResponse.status == 201
        deleteResponse.status == 204
    }

    def "authenticate token and verify entropy"() {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "user$random"
        def user = cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", null, password)).getEntity(User).value
        defaultExpirationSeconds = config.getInt("token.cloudAuthExpirationSeconds")
        entropy = config.getDouble("token.entropy")

        when:
        def startTime = new DateTime()
        def expOne = authAndExpire(username, password)
        def expTwo = authAndExpire(username, password)
        def expThree = authAndExpire(username, password)
        def endTime = new DateTime()

        def range = getRange(defaultExpirationSeconds, startTime, endTime)
        cloud20.destroyUser(serviceAdminToken, user.id)

        then:
        expOne >= range.get("min")
        expOne <= range.get("max")
        expTwo >= range.get("min")
        expTwo <= range.get("max")
        expThree >= range.get("min")
        expThree <= range.get("max")
    }

    def "Impersonation call with a expiration time provided does not apply entropy" () {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "impersionationUser$random"


        when:
        def user = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "impersonationUserDomain$random", password)).getEntity(User).value
        DateTime date = new DateTime().plusHours(3)
        def impersonatedToken = cloud20.impersonate(identityAdminToken, user, 10800).getEntity(ImpersonationResponse.class)
        cloud20.destroyUser(serviceAdminToken, user.getId())

        then:
        impersonatedToken.token != null
        def expireTime = impersonatedToken.token.expires.toGregorianCalendar().getTime()
        def diff = Math.abs(Seconds.secondsBetween(new DateTime(date.toDate()), new DateTime(expireTime)).seconds)
        diff <= 2

    }

    def "Impersonation call with a expiration time not provided does apply entropy" () {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "impersionationUser$random"

        when:
        def user = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "impersonationUserDomain$random", password)).getEntity(User).value
        DateTime date = new DateTime().plusHours(3)
        def impersonatedToken = cloud20.impersonate(identityAdminToken, user, null).getEntity(ImpersonationResponse.class)
        cloud20.destroyUser(serviceAdminToken, user.getId())

        then:
        impersonatedToken.token != null
        def expireTime = impersonatedToken.token.expires.toGregorianCalendar().getTime()
        def diff = Math.abs(Seconds.secondsBetween(new DateTime(date.toDate()), new DateTime(expireTime)).seconds)
        diff <= 108 //1 % of 3 hours

    }

    def "Create tenant with negative tenantId" () {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "negativeTenantUser$random"
        def tenant = v2Factory.createTenant("-754612$random", "-754612$random")
        def role = v2Factory.createRole("roleName$random", "a45b14e394a57e3fd4e45d59ff3693ead204998b")
        def endpointId = testUtils.getRandomInteger().toString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointId, "compute", "http://bananas.com", "cloudServers")

        when:
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "negtenDomain$random", password)).getEntity(User).value
        def addTenant = cloud20.addTenant(identityAdminToken, tenant).getEntity(Tenant).value
        def createRole = cloud20.createRole(identityAdminToken, role).getEntity(Role).value
        def createEndpointTemplate = cloud20.addEndpointTemplate(identityAdminToken, endpointTemplate).getEntity(EndpointTemplate).value

        def addRoleToUserOnTenant = cloud20.addRoleToUserOnTenant(identityAdminToken, addTenant.id, createUser.id, createRole.id)
        def addEndpointToTenant = cloud20.addEndpoint(identityAdminToken, addTenant.id, endpointTemplate)

        cloud20.destroyUser(serviceAdminToken, createUser.id)
        cloud20.deleteTenant(serviceAdminToken, addTenant.id)
        cloud20.deleteRole(serviceAdminToken, createRole.id)
        cloud20.deleteEndpointTemplate(serviceAdminToken, createEndpointTemplate.id.toString())


        then:
        createUser != null
        addTenant != null
        createRole != null
        createEndpointTemplate != null
        addRoleToUserOnTenant.status == 200
        addEndpointToTenant.status == 200
    }

    def "Do not have duplicate endpoints if user has two roles on the same tenant" () {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def endpointCount = 0
        def username = "deDupeUser$random"
        def tenant = v2Factory.createTenant("754612$random", "754612$random")
        def role = v2Factory.createRole("dupeRole1$random", "a45b14e394a57e3fd4e45d59ff3693ead204998b")
        def role2 = v2Factory.createRole("dupeRole2$random", "a45b14e394a57e3fd4e45d59ff3693ead204998b")
        def endpointId = testUtils.getRandomInteger().toString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointId, "compute", "http://bananas.com", "cloudServers")

        when:
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "deDupeDomain$random", password)).getEntity(User).value
        def addTenant = cloud20.addTenant(identityAdminToken, tenant).getEntity(Tenant).value
        def createRole = cloud20.createRole(identityAdminToken, role).getEntity(Role).value
        def createRole2 = cloud20.createRole(identityAdminToken, role2).getEntity(Role).value
        def createEndpointTemplate = cloud20.addEndpointTemplate(identityAdminToken, endpointTemplate).getEntity(EndpointTemplate).value

        def addRoleToUserOnTenant = cloud20.addRoleToUserOnTenant(identityAdminToken, addTenant.id, createUser.id, createRole.id)
        def addRole2ToUserOnTenant = cloud20.addRoleToUserOnTenant(identityAdminToken, addTenant.id, createUser.id, createRole2.id)
        def addEndpointToTenant = cloud20.addEndpoint(identityAdminToken, addTenant.id, endpointTemplate)

        def authResponse = cloud20.authenticatePassword(username, password).getEntity(AuthenticateResponse).value

        cloud20.destroyUser(serviceAdminToken, createUser.id)
        cloud20.deleteTenant(serviceAdminToken, addTenant.id)
        cloud20.deleteRole(serviceAdminToken, createRole.id)
        cloud20.deleteRole(serviceAdminToken, createRole2.id)
        cloud20.deleteEndpointTemplate(serviceAdminToken, createEndpointTemplate.id.toString())

        then:
        for (List publicUrls : authResponse.serviceCatalog.service.endpoint.publicURL) {
            if (publicUrls.contains(endpointTemplate.publicURL + "/" + addTenant.id)) {
                endpointCount++
            }
        }

        createUser != null
        addTenant != null
        createRole != null
        createEndpointTemplate != null
        addRoleToUserOnTenant.status == 200
        addRole2ToUserOnTenant.status == 200
        addEndpointToTenant.status == 200
        endpointCount == 1
    }

    def "Default User should be allow to update himself" () {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "defaultUserUpdate$random"

        when:
        def createUser = cloud20.createUser(userAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "deDupeDomain$random", password)).getEntity(User).value
        def token = cloud20.authenticatePassword(username, password).getEntity(AuthenticateResponse).value.token.id
        def updateUser = cloud20.updateUser(token, createUser.id, v2Factory.createUserForCreate(username, "display name", "other@email.email", true, "DFW", null, null)).getEntity(User).value

        cloud20.destroyUser(serviceAdminToken, createUser.id)

        then:
        createUser != null
        token != null
        updateUser != null
        updateUser.displayName == "display name"
        updateUser.email == "other@email.email"
    }

    def "When user admin creates sub-user both should be returned in list users by tenant call"() {
        given:
        def username = "usernsdfg43$sharedRandom"

        def adminUsername1 = "userAdmin3$sharedRandom"
        com.rackspacecloud.docs.auth.api.v1.User cloud11User = v1Factory.createUser(adminUsername1, "1234567890", randomMosso, null, true)
        cloud11.createUser(cloud11User)
        User userAdmin1 = cloud20.getUserByName(identityAdminToken, adminUsername1).getEntity(User).value

        def userAdminAuthResponse = cloud20.authenticateApiKey(adminUsername1, "1234567890").getEntity(AuthenticateResponse).value
        def userAdminToken = userAdminAuthResponse.token.id
        def userAdminTenant = userAdminAuthResponse.token.tenant.id
        def subUserForCreate = cloud20.createUser(userAdminToken, v2Factory.createUserForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", null, "Password1"))
        User subUser1 = subUserForCreate.getEntity(User).value
        def getUserResponse = cloud11.getUserByName(username)
        com.rackspacecloud.docs.auth.api.v1.User userEntity = getUserResponse.getEntity(com.rackspacecloud.docs.auth.api.v1.User)

        when:
        def listUsersByTenant = cloud20.listUsersWithTenantId(identityAdminToken, userAdminTenant).getEntity(UserList).value

        then:
        listUsersByTenant.user.size() == 2
        listUsersByTenant.user.id.contains(subUser1.id)
        listUsersByTenant.user.id.contains(userAdmin1.id)

        cleanup:
        cloud20.destroyUser(serviceAdminToken, userAdmin1.id)
        cloud20.destroyUser(serviceAdminToken, subUser1.id)
        cloud20.deleteTenant(serviceAdminToken, userAdminTenant)
        cloud20.deleteTenant(serviceAdminToken, userEntity.nastId)

    }

    def "Get user by domainId"() {
        given:
        def username = "userkljk$sharedRandom"
        def mossoId = getRandomNumber(1000000,2000000)

        def adminUsername1 = "someUserAdmin$sharedRandom"
        com.rackspacecloud.docs.auth.api.v1.User cloud11User = v1Factory.createUser(adminUsername1, "1234567890", mossoId, null, true)
        cloud11.createUser(cloud11User)
        User userAdmin = cloud20.getUserByName(identityAdminToken, adminUsername1).getEntity(User).value

        def userAdminAuthResponse = cloud20.authenticateApiKey(adminUsername1, "1234567890").getEntity(AuthenticateResponse).value
        def userAdminToken = userAdminAuthResponse.token.id
        def userAdminTenant = userAdminAuthResponse.token.tenant.id


        def subUserForCreate = cloud20.createUser(userAdminToken, v2Factory.createUserForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", null, "Password1"))
        User subUser = subUserForCreate.getEntity(User).value
        def getUserResponse = cloud11.getUserByName(username)
        com.rackspacecloud.docs.auth.api.v1.User userEntity = getUserResponse.getEntity(com.rackspacecloud.docs.auth.api.v1.User)

        when:
        def getUsersByDomainId = cloud20.getUsersByDomainId(identityAdminToken, userAdmin.domainId).getEntity(UserList).value

        then:
        getUsersByDomainId.user.size() == 2
        getUsersByDomainId.user.username.contains(userAdmin.username)
        getUsersByDomainId.user.username.contains(subUser.username)

        cleanup:
        utils.deleteUserQuietly(subUser, serviceAdminToken)
        utils.deleteUserQuietly(userAdmin, serviceAdminToken)
        cloud20.deleteTenant(serviceAdminToken, userAdminTenant)
        cloud20.deleteTenant(serviceAdminToken, userEntity.nastId)
    }

    def "List users by tenant id should display sub users" () {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "listUserByTenant$random"
        def subUsername = "subListUserByTenant$random"
        def tenantId = testUtils.getRandomUUID()
        def tenant = v2Factory.createTenant(tenantId, tenantId)
        def role = v2Factory.createRole("listUsersByTenantRole$random", "a45b14e394a57e3fd4e45d59ff3693ead204998b").with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }

        when:
        def addTenant = cloud20.addTenant(identityAdminToken, tenant).getEntity(Tenant).value
        def createRole = cloud20.createRole(identityAdminToken, role).getEntity(Role).value
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "listUserOnTenantDomain$random", password)).getEntity(User).value
        cloud20.addRoleToUserOnTenant(identityAdminToken, addTenant.id, createUser.id, createRole.id)
        def token = cloud20.authenticatePassword(username, password).getEntity(AuthenticateResponse).value.token.id
        def createSubUser = cloud20.createUser(token, v2Factory.createUserForCreate(subUsername, subUsername, "email@email.email", true, "DFW", null, password)).getEntity(User).value
        def listUsersByTenant = cloud20.listUsersWithTenantId(identityAdminToken, addTenant.id).getEntity(UserList).value

        cloud20.destroyUser(serviceAdminToken, createSubUser.id)
        cloud20.destroyUser(serviceAdminToken, createUser.id)
        cloud20.deleteTenant(serviceAdminToken, addTenant.id)
        cloud20.deleteRole(serviceAdminToken, createRole.id)

        then:
        createUser != null
        token != null
        createSubUser != null
        listUsersByTenant.user.size() == 2
        //this if/else is necessary due to difference in the order in which openldap and ca return the results
        boolean matched = false
        if (listUsersByTenant.user[0].id == createUser.id) {
            listUsersByTenant.user[1].id == createSubUser.id
            matched = true
        }
        else if (listUsersByTenant.user[1].id == createUser.id) {
            listUsersByTenant.user[0].id == createSubUser.id
            matched = true
        }

        matched == true
    }

    def "Add role to user on tenant using identity:admin token should return 200" () {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "addRoleToUser$random"
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId)
        def role = v2Factory.createRole("addRoleToUserTenantRole$random", "a45b14e394a57e3fd4e45d59ff3693ead204998b")

        when:
        def addTenant = cloud20.addTenant(identityAdminToken, tenant).getEntity(Tenant).value
        def createRole = cloud20.createRole(identityAdminToken, role).getEntity(Role).value
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "domain$random", password)).getEntity(User).value
        def addRoleResponse = cloud20.addRoleToUserOnTenant(identityAdminToken, addTenant.id, createUser.id, createRole.id)

        cloud20.destroyUser(serviceAdminToken, createUser.id)
        cloud20.deleteTenant(serviceAdminToken, addTenant.id)
        cloud20.deleteRole(serviceAdminToken, createRole.id)

        then:
        createUser != null
        addRoleResponse.status == 200
    }

    def "List users for tenant should return subUsers" () {
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "listUserByTenant$random"
        def subUser1name = "sub1ListUserByTenant$random"
        def subUser2name = "sub2ListUserByTenant$random"
        def tenantId = "tenant$random"
        def tenant = v2Factory.createTenant(tenantId, tenantId)
        def role = v2Factory.createRole("listUsersByTenantRole$random", "a45b14e394a57e3fd4e45d59ff3693ead204998b").with {
            it.roleType = RoleTypeEnum.PROPAGATE
            it
        }

        when:
        def addTenant = cloud20.addTenant(identityAdminToken, tenant).getEntity(Tenant).value
        def createRole = cloud20.createRole(identityAdminToken, role).getEntity(Role).value
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "listUserOnTenantDomain$random", password)).getEntity(User).value
        def token = cloud20.authenticatePassword(username, password).getEntity(AuthenticateResponse).value.token.id
        def createSub1User = cloud20.createUser(token, v2Factory.createUserForCreate(subUser1name, subUser1name, "email@email.email", true, "DFW", null, password)).getEntity(User).value
        def createSub2User = cloud20.createUser(token, v2Factory.createUserForCreate(subUser2name, subUser2name, "email@email.email", true, "DFW", null, password)).getEntity(User).value
        cloud20.addRoleToUserOnTenant(identityAdminToken, addTenant.id, createSub1User.id, createRole.id)
        cloud20.addRoleToUserOnTenant(identityAdminToken, addTenant.id, createSub2User.id, createRole.id)
        UserList listUsersByTenant = cloud20.listUsersWithTenantId(identityAdminToken, addTenant.id).getEntity(UserList).value

        cloud20.destroyUser(serviceAdminToken, createSub1User.id)
        cloud20.destroyUser(serviceAdminToken, createSub2User.id)
        cloud20.destroyUser(serviceAdminToken, createUser.id)
        cloud20.deleteTenant(serviceAdminToken, addTenant.id)
        cloud20.deleteRole(serviceAdminToken, createRole.id)

        then:
        createUser != null
        token != null
        createSub1User != null
        listUsersByTenant.user.size() == 2
        listUsersByTenant.user.username.contains(createSub1User.username)
        listUsersByTenant.user.username.contains(createSub2User.username)
    }

    def "Authenticate Response returns user roles" () {
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "listUserByTenant$random"
        def tenantId = getRandomNumber(7000000, 8000000)
        def tenant = v2Factory.createTenant(tenantId.toString(), tenantId.toString())
        def role = v2Factory.createRole("listUsersByTenantRole$random", "a45b14e394a57e3fd4e45d59ff3693ead204998b")

        when:
        def addTenant = cloud20.addTenant(identityAdminToken, tenant).getEntity(Tenant).value
        def createRole = cloud20.createRole(identityAdminToken, role).getEntity(Role).value
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "listUserOnTenantDomain$random", password)).getEntity(User).value
        cloud20.addRoleToUserOnTenant(identityAdminToken, addTenant.id, createUser.id, createRole.id)
        AuthenticateResponse authResponse = cloud20.authenticatePassword(username, password).getEntity(AuthenticateResponse).value

        then:
        createUser != null
        authResponse.user.roles != null
        authResponse.user.roles.role.size() == 2
        authResponse.user.roles.role.name.contains(role.name)
        authResponse.user.roles.role.name.contains("identity:user-admin")

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createUser.id)
        cloud20.deleteTenant(serviceAdminToken, addTenant.id)
        cloud20.deleteRole(serviceAdminToken, createRole.id)

    }

    def "Authenticate Racker"(){
        when:
        def rackerAuth = cloud20.authenticateRacker(racker, rackerPassword)
        def response = rackerAuth.getEntity(AuthenticateResponse).value

        then:
        rackerAuth.status == 200
        response.token != null
        response.user.roles.role.name.contains(rackerRole)
        response.user.roles.role.name.contains(rackerRoleName)
    }

    def "Impersonate user with racker token"(){
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "userForImpersonation$random"

        when:
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "domain$username", password)).getEntity(User).value
        def rackerAuth = cloud20.authenticateRacker(Constants.RACKER_IMPERSONATE, Constants.RACKER_IMPERSONATE_PASSWORD)
        AuthenticateResponse response = rackerAuth.getEntity(AuthenticateResponse).value
        def token = response.token.id
        def userForImpersonation = new User().with {
            it.username = createUser.username
            it
        }
        def impersonationResponse = cloud20.impersonate(token, userForImpersonation)
        ImpersonationResponse ir = impersonationResponse.getEntity(ImpersonationResponse)
        def impersonatedToken = ir.token.id

        def validateResponse = cloud20.validateToken(identityAdminToken, impersonatedToken)

        then:
        rackerAuth.status == 200
        response.token != null
        impersonationResponse.status == 200
        ir != null
        ir.token != null
        ir.token.id != null

        validateResponse.status == 200
        def validateEntity = validateResponse.getEntity(AuthenticateResponse).value
        validateEntity.any.attributes.nodes[0].value.contains(Constants.RACKER_IMPERSONATE)

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createUser.id)
    }

    def "Impersonate user with identity admin token"(){
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "userForImpersonation$random"

        when:
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "domain$username", password)).getEntity(User).value
        def userForImpersonation = new User().with {
            it.username = createUser.username
            it
        }
        def impersonationResponse = cloud20.impersonate(identityAdminToken, userForImpersonation)
        ImpersonationResponse impersonationEntity = impersonationResponse.getEntity(ImpersonationResponse)
        def impersonatedToken = impersonationEntity.token.id

        def validateResponse = cloud20.validateToken(identityAdminToken, impersonatedToken)

        then:
        impersonationResponse.status == 200
        impersonationEntity != null
        impersonationEntity.token != null
        impersonationEntity.token.id != null

        def validateEntity = validateResponse.getEntity(AuthenticateResponse).value
        validateEntity.any.attributes.nodes[0].value.contains("auth")

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createUser.id)
    }

    @Unroll
    def "racker token returns 403 when making admin calls" () {
        given:
        rackerToken = cloud20.authenticateRacker(Constants.RACKER, Constants.RACKER_PASSWORD).getEntity(AuthenticateResponse).value.token.id

        expect:
        response.status == 403

        where:
        response << [
                cloud20.listRoles(rackerToken, null, null, null),
                cloud20.addApiKeyToUser(rackerToken, "id", new ApiKeyCredentials()),
                cloud20.addTenant(rackerToken, new Tenant()),
                cloud20.addUserToGroup(rackerToken, "groupId", "userId"),
                cloud20.addEndpoint(rackerToken, "tenantId", new EndpointTemplate()),
                cloud20.createUser(rackerToken, new User())
        ]
    }

    def "validate racker token" (){
        given:
        rackerToken = cloud20.authenticateRacker(racker, rackerPassword).getEntity(AuthenticateResponse).value.token.id

        when:
        def response = cloud20.validateToken(identityAdminToken, rackerToken).getEntity(AuthenticateResponse).value

        then:
        response.token != null
        response.token.id != null
        response.user != null
        response.user.id == racker
        response.user.roles.role.name.contains(rackerRole)
        response.user.roles.role.name.contains(rackerRoleName)
    }

    def "List credentials should not return passwordCredentials"() {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "listCredentialUser$random"

        when:
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "listUserOnTenantDomain$random", password)).getEntity(User).value
        def listCredResponse = cloud20.listCredentials(serviceAdminToken, createUser.id).getEntity(CredentialListType).value

        then:
        createUser != null
        listCredResponse != null
        ((CredentialListType) listCredResponse).credential.size() == 0

        cleanup:
        cloud20.destroyUser(identityAdminToken, createUser.id)
    }

    def "Create user should auto generate an apikey which will allows new user to authenticate" () {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "userApiKey$random"

        when:
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "Domain$username", password)).getEntity(User).value
        String listCredResponse = cloud20.listCredentials(serviceAdminToken, createUser.id).getEntity(String)
        def apiKey = cloud11.getUserByName(username).getEntity(com.rackspacecloud.docs.auth.api.v1.User).key
        def authenticate = cloud20.authenticateApiKey(username, apiKey)

        then:
        createUser != null
        listCredResponse != null
        assert (listCredResponse.contains(JSONConstants.API_KEY_CREDENTIALS))
        assert (!listCredResponse.contains(JSONConstants.PASSWORD_CREDENTIALS))
        apiKey != null
        authenticate.status == 200

        cleanup:
        cloud20.destroyUser(identityAdminToken, createUser.id)

    }

    def "Create admin user with complete payload" () {
        given:
        def domain = utils.createDomain()
        def username1 = "username" + (long)(Math.random() * 100000)
        def agroup = utils.createGroup()
        def user = v2Factory.createUserForCreate(username1, username1, "john.smith@example.org", true, "DFW", domain,
                                                 "securePassword2", ["rbacRole1"].asList(), [agroup.name].asList(), "What is the meaning?", "That is the wrong question")

        when:
        def result = cloud20.createUser(identityAdminToken, user)

        then:
        result.status == 201

        cleanup:
        utils.deleteUserQuietly(result.getEntity(User).value, serviceAdminToken)
        cloud20.deleteGroup(serviceAdminToken, agroup.id)
    }

    def "List credentials should not return allow an identity admin to list service admin credentials"() {
        when:
        def listCredResponse = cloud20.listCredentials(identityAdminToken, serviceAdmin.id)

        then:
        listCredResponse != null
        listCredResponse.status == 403
    }

    def "List Groups with invalid name - returns 404" () {
        when:
        def listGroupsResponse = cloud20.listGroupsForUser(serviceAdminToken, "badUserId")
        then:
        listGroupsResponse.status == 404
    }

    def "updating user does not modify user groups"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def userForUpdate = new UserForCreate().with {
            it.id = userAdmin.id
            it.password = DEFAULT_PASSWORD
            it
        }
        def createGroupResponse = cloud20.createGroup(serviceAdminToken, v1Factory.createGroup("group1$sharedRandom", "this is my group"))
        groupLocation = createGroupResponse.location
        def getGroupResponse = cloud20.getGroup(serviceAdminToken, groupLocation)
        group = getGroupResponse.getEntity(Group).value
        cloud20.addUserToGroup(serviceAdminToken, group.id, userAdmin.id)
        def groupsBeforeUpdateResponse = cloud20.listGroupsForUser(utils.getServiceAdminToken(), userAdmin.id)
        def groupsBeforeUpdate = groupsBeforeUpdateResponse.getEntity(Groups).value

        when:
        utils.updateUser(userForUpdate)
        def groupsAfterUpdateResponse = cloud20.listGroupsForUser(utils.getServiceAdminToken(), userAdmin.id)
        def groupsAfterUpdate = groupsAfterUpdateResponse.getEntity(Groups).value

        then:
        for(Group group : groupsBeforeUpdate.group) {
            assert(groupsAfterUpdate.group.id.contains(group.id))
        }

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
    }

    def "Updating user's secretQA twice with same data - returns 200" () {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "updateUserSecretQA$random"
        def secretQA = v1Factory.createSecretQA("1", "Somewhere over the rainbow!")
        def updateSecreatQA = v1Factory.createRaxKsQaSecretQA()

        when:
        def user = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "updateUserSecretQADomain$random", password)).getEntity(User).value
        def createSecretQAResponse = cloud20.createSecretQA(identityAdminToken, user.id, secretQA)
        def updateSecretQAResponse = cloud20.updateSecretQA(identityAdminToken, user.id, updateSecreatQA)
        def updateSecretQAResponse2 = cloud20.updateSecretQA(identityAdminToken, user.id, updateSecreatQA)
        def getSecretQAResponse = cloud20.getSecretQA(identityAdminToken, user.id)
        def secretQAEntity = getSecretQAResponse.getEntity(SecretQA)

        then:
        createSecretQAResponse.status == 200
        updateSecretQAResponse.status == 200
        updateSecretQAResponse2.status == 200
        getSecretQAResponse.status == 200
        secretQAEntity.answer == updateSecreatQA.answer
        secretQAEntity.question == updateSecreatQA.question

        cleanup:
        cloud20.destroyUser(serviceAdminToken, user.id)
    }

    def "List credentials for subUser using userAdmin within same domain returns 200"() {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "listCredentialUser$random"
        def subUsername = "listCredentialSubUser$random"

        when:
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "listUserOnTenantDomain$random", password)).getEntity(User).value
        String userAdminToken = cloud20.authenticate(username, password).getEntity(AuthenticateResponse).value.token.id
        def createSubUser = cloud20.createUser(userAdminToken, v2Factory.createUserForCreate(subUsername, subUsername, "email@email.email", true, "DFW", null, password)).getEntity(User).value
        def listCredResponse = cloud20.listCredentials(userAdminToken, createSubUser.id)

        then:
        createUser != null
        userAdminToken != null
        createSubUser != null
        listCredResponse != null
        listCredResponse.status == 200

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createUser.id)
        cloud20.destroyUser(serviceAdminToken, createSubUser.id)
    }

    def "Default user should not be allow to retrieve users by email unless its promoted to user-manage"() {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "userByEmail$random"
        def subUsername = "subUserByEmail$random"
        def email = "testEmail@rackspace.com"

        when:
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, email, true, "DFW", username.concat("Domain"), password)).getEntity(User).value
        String userAdminToken = cloud20.authenticate(username, password).getEntity(AuthenticateResponse).value.token.id
        def createSubUser = cloud20.createUser(userAdminToken, v2Factory.createUserForCreate(subUsername, subUsername, email, true, "DFW", null, password)).getEntity(User).value
        String defaultUserToken = cloud20.authenticate(subUsername, password).getEntity(AuthenticateResponse).value.token.id
        def getUsersByEmail = cloud20.getUsersByEmail(defaultUserToken, email)
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, createSubUser.id)
        def userManageToken = cloud20.authenticate(subUsername, password).getEntity(AuthenticateResponse).value.token.id
        def getUsersByEmailUserManage = cloud20.getUsersByEmail(userManageToken, email)
        def usersByEmailUserManage = getUsersByEmailUserManage.getEntity(UserList)

        then:
        createUser != null
        userAdminToken != null
        createSubUser != null
        defaultUserToken != null
        getUsersByEmail.status == 403
        userManageToken != null
        getUsersByEmailUserManage.status == 200
        usersByEmailUserManage.value.user.size() == 2

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createUser.id)
        cloud20.destroyUser(serviceAdminToken, createSubUser.id)
        cloud20.deleteDomain(serviceAdminToken, createUser.domainId)
    }

    def "[CIDMDEV-5357] get user admin by tenant id"() {
        given:
        def nastId = "983452"
        def mossoId = "StagingUS_ed361067-aebd-45b7-92a3-353edacb0e09"
        def response, userObject

        when:
        response = cloud20.getUserByTenantId(serviceAdminToken, nastId)
        userObject = response.getEntity(User).value

        then:
        response.status == 200
        userObject != null
        userObject.otherAttributes != null
        userObject.otherAttributes.values().contains(nastId)
        userObject.otherAttributes.values().contains(mossoId)

        when:
        response = cloud20.getUserByTenantId(serviceAdminToken, mossoId)
        userObject = response.getEntity(User).value

        then:
        response.status == 200
        userObject != null
        userObject.otherAttributes != null
        userObject.otherAttributes.values().contains(nastId)
        userObject.otherAttributes.values().contains(mossoId)

        when:
        response = cloud20.getUserByTenantId(userAdminToken, nastId)

        then:
        response.status == 403
    }

    def "Update a users domainId to null or a blank value should not update the domain" () {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "updateDomainSpacesUser$random"
        def email = "test@rackspace.com"
        def domainId = username.concat("Domain")

        def updateUser = new User().with {
            it.username = username
            it.domainId = domainValue
            it.enabled = true
            it
        }

        when:
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, email, true, "DFW", domainId, password)).getEntity(User).value
        def updateUserResponse = cloud20.updateUser(identityAdminToken, createUser.id, updateUser)
        def updateUserObject = updateUserResponse.getEntity(User).value

        then:
        createUser != null
        updateUserResponse.status == status
        updateUserObject.domainId == domainId

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createUser.id)
        cloud20.deleteDomain(serviceAdminToken, domainId)

        where:
        domainValue| status
        "    "     | 200
        ""         | 200
        null       | 200
    }

    def "Update a users domainId to an invalid domain returns doesn't change domain" () {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "updateDomainSpacesUser$random"
        def email = "test@rackspace.com"
        def domainId = username.concat("Domain")

        def updateUser = new User().with {
            it.username = username
            it.domainId = "badDomain"
            it.enabled = true
            it
        }

        when:
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, email, true, "DFW", domainId, password)).getEntity(User).value
        def updateUserResponse = cloud20.updateUser(identityAdminToken, createUser.id, updateUser)

        then:
        createUser != null
        updateUserResponse.status == 200
        updateUserResponse.getEntity(User).value.domainId == domainId

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createUser.id)
        cloud20.deleteDomain(serviceAdminToken, domainId)
    }

    def "racker revoke token should disable rackers token"() {
        when:
        def rackerAuth = cloud20.authenticateRacker(racker, rackerPassword)
        assert (rackerAuth.status == 200)

        def token = rackerAuth.getEntity(AuthenticateResponse).value.token.id
        def validateResponse = cloud20.validateToken(serviceAdminToken, token)
        assert (validateResponse.status == 200)

        def revokeResponse = cloud20.revokeUserToken(serviceAdminToken, token)
        assert (revokeResponse.status == 204)

        def failedValidateResponse = cloud20.validateToken(serviceAdminToken, token)

        then:
        failedValidateResponse.status == 404
    }

    def "auth with apikey returns apikey as the type of authentication"() {
        given:
        def username = "username" + getRandomUUID()

        when:
        def createUser = cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate(username, "display", "$username@email.com", true, null, null, DEFAULT_PASSWORD))
        assert (createUser.status == 201)

        def getUserResponse = cloud20.getUserByName(identityAdminToken, username)
        assert (getUserResponse.status == 200)
        def userEntity = getUserResponse.getEntity(User).value

        def createApiKey = cloud20.addApiKeyToUser(serviceAdminToken, userEntity.getId(), v2Factory.createApiKeyCredentials(username, DEFAULT_APIKEY))
        assert (createApiKey.status == 200)

        def authApikeyResponse = cloud20.authenticateApiKey(username, DEFAULT_APIKEY)
        assert (authApikeyResponse.status == 200)
        AuthenticateResponse apiKeyResponse = authApikeyResponse.getEntity(AuthenticateResponse).value
        def validateApikeyResponse = cloud20.validateToken(serviceAdminToken, apiKeyResponse.token.id)

        def authPasswordResponse = cloud20.authenticatePassword(username, DEFAULT_PASSWORD)
        assert (authPasswordResponse.status == 200)
        AuthenticateResponse passwordResponse = authPasswordResponse.getEntity(AuthenticateResponse).value
        def validatePasswordResponse = cloud20.validateToken(serviceAdminToken, passwordResponse.token.id)

        then: "validate token created with apikey states authenticated by apikey"
        validateApikeyResponse.status == 200
        validateApikeyResponse.getEntity(AuthenticateResponse).value.token.authenticatedBy.credential.contains(GlobalConstants.AUTHENTICATED_BY_APIKEY)

        then: "validate password token states authenticated by password since it was created by a password"
        validatePasswordResponse.status == 200
        validatePasswordResponse.getEntity(AuthenticateResponse).value.token.authenticatedBy.credential.contains(GlobalConstants.AUTHENTICATED_BY_PASSWORD)

        then: "Tokens are two separate tokens"
        passwordResponse.token.id != apiKeyResponse.token.id

        cleanup:
        cloud20.deleteUser(serviceAdminToken, userEntity.getId())
    }

    def "Delete user's apiKey"(){
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "userApiKey$random"
        def email = "test@rackspace.com"
        com.rackspacecloud.docs.auth.api.v1.User user = new com.rackspacecloud.docs.auth.api.v1.User().with {
            it.key = "key"
            it
        }

        when:
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, email, true, "DFW", username.concat("Domain"), password)).getEntity(User).value
        def setApiKey = cloud11.setUserKey(createUser.username, user)
        def userObject = setApiKey.getEntity(com.rackspacecloud.docs.auth.api.v1.User)
        def deleteApiKey = cloud20.deleteUserApiKey(identityAdminToken, createUser.id)
        def getApiKey = cloud20.getUserApiKey(identityAdminToken, createUser.id)

        then:
        createUser != null
        setApiKey.status == 200
        userObject.key == "key"
        deleteApiKey.status == 204
        getApiKey.status == 404


        cleanup:
        cloud20.destroyUser(serviceAdminToken, createUser.id)
        cloud20.deleteDomain(serviceAdminToken, createUser.domainId)
    }

    def "Add user to group within a valid domain" () {
        given:
        def username = "addGrpToUser$sharedRandom"
        def groupName = "validGroup$sharedRandom"
        def group = v1Factory.createGroup(groupName, "desc")
        def password = "Password1"
        def email = "test@rackspace.com"
        def domainId = username.concat("Domain")

        when:
        def createGroup = cloud20.createGroup(identityAdminToken, group).getEntity(Group).value
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, email, true, "DFW", domainId, password)).getEntity(User).value
        def addGroupToUser = cloud20.addUserToGroup(identityAdminToken, createGroup.id, createUser.id)
        def usersFromGroup = cloud20.getUsersFromGroup(identityAdminToken, createGroup.id).getEntity(UserList).value

        then:
        createGroup != null
        createGroup.name == groupName
        createUser != null
        createUser.username == username
        addGroupToUser.status == 204
        usersFromGroup.user.id.contains(createUser.id)

        cleanup:
        cloud20.destroyUser(serviceAdminToken, createUser.id)
        cloud20.deleteDomain(serviceAdminToken, domainId)
        cloud20.deleteGroup(serviceAdminToken, createGroup.id)
    }

    def "identity admin should be able to update a group"() {
        given:
        def groupName = getRandomUUID("group")
        def groupDesc = "this is a group"
        def updatedGroupDesc = "updated group"

        when:
        def createGroupResponse = cloud20.createGroup(identityAdminToken, v1Factory.createGroup(groupName, groupDesc))
        assert (createGroupResponse.status == 201)

        def getGroupResponse = cloud20.getGroup(identityAdminToken, createGroupResponse.location)
        assert (getGroupResponse.status == 200)
        Group groupEntity = getGroupResponse.getEntity(Group).value
        def groupId = groupEntity.id

        def updateGroupResponse = cloud20.updateGroup(identityAdminToken, groupId, v1Factory.createGroup(groupName, updatedGroupDesc))
        assert (updateGroupResponse.status == 200)

        getGroupResponse = cloud20.getGroup(identityAdminToken, createGroupResponse.location)
        assert (getGroupResponse.status == 200)
        Group updatedGroupEntity = getGroupResponse.getEntity(Group).value

        then:
        groupEntity.description == groupDesc
        updateGroupResponse.status == 200
        updatedGroupEntity.description == updatedGroupDesc

        cleanup:
        cloud20.deleteGroup(identityAdminToken, groupId)
    }

    @Unroll
    def "saml api call returns 400 when given an empty payload"() {
        given:
        org.opensaml.saml.saml2.core.Response res = null;

        when:
        def response = cloud20.samlAuthenticate(res)

        then:
        assert response.status == HttpStatus.SC_BAD_REQUEST
    }

    def "user can validate his own token (B-80571:TK-165775)"() {
        given:
        def username = "username" + getRandomUUID()

        def createUser = cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate(username, "display", "$username@email.com", true, null, null, DEFAULT_PASSWORD))
        assert (createUser.status == 201)

        def getUserResponse = cloud20.getUserByName(identityAdminToken, username)
        assert (getUserResponse.status == 200)
        def userEntity = getUserResponse.getEntity(User).value

        def authPasswordResponse = cloud20.authenticatePassword(username, DEFAULT_PASSWORD)
        assert (authPasswordResponse.status == 200)

        def authToken = authPasswordResponse.getEntity(AuthenticateResponse).value.token.id

        def username2 = "username" + getRandomUUID()

        createUser = cloud20.createUser(authToken, v2Factory.createUserForCreate(username2, "display", "$username@email.com", true, null, userEntity.domainId, DEFAULT_PASSWORD))
        assert (createUser.status == 201)

        getUserResponse = cloud20.getUserByName(identityAdminToken, username2)
        assert (getUserResponse.status == 200)
        def userEntity2 = getUserResponse.getEntity(User).value

        authPasswordResponse = cloud20.authenticatePassword(username2, DEFAULT_PASSWORD)
        assert (authPasswordResponse.status == 200)

        authToken = authPasswordResponse.getEntity(AuthenticateResponse).value.token.id

        def validateResponse

        when: "user validates his own token"
        validateResponse = cloud20.validateToken(authToken, authToken)

        then: "validate token is validated"
        validateResponse.status == 200
        validateResponse.getEntity(AuthenticateResponse).value.user.name == username2

        cleanup:
        cloud20.deleteUser(serviceAdminToken, userEntity2.getId())
        cloud20.deleteUser(serviceAdminToken, userEntity.getId())
    }

    @Unroll
    def "include endpoints based on endpoint rules: content-type=#contentType; useEndpointRules=#useEndpointRules" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_PROP, useEndpointRules)

        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "username$random"
        def tenantType = "cloud"
        def tenant = v2Factory.createTenant("12345$random", "12345$random", [tenantType])
        def role = v2Factory.createRole("roleName$random", "a45b14e394a57e3fd4e45d59ff3693ead204998b")
        def endpointId = testUtils.getRandomInteger().toString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointId, "compute", "http://bananas.$random", "cloudServers")
        def endpointRule = v2Factory.createTenantTypeEndpointRule(tenantType, [endpointId])

        when:
        def createUser = getEntity(cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(
            username, username, "email@email.email", true, "DFW", "domain$random", password), contentType, contentType), User)

        def addTenant = getEntity(cloud20.addTenant(identityAdminToken, tenant, contentType, contentType), Tenant)
        def createRole = getEntity(cloud20.createRole(identityAdminToken, role, contentType, contentType), Role)
        def createEndpointTemplate = getEntity(cloud20.addEndpointTemplate(identityAdminToken, endpointTemplate, contentType, contentType), EndpointTemplate)
        def createEndpointRule = cloud20.addEndpointAssignmentRule(identityAdminToken, endpointRule, contentType, contentType).getEntity(TenantTypeEndpointRule)
        def addRoleToUserOnTenant = cloud20.addRoleToUserOnTenant(identityAdminToken, addTenant.id, createUser.id, createRole.id, contentType)

        AuthenticateResponse authenticateResponse = getEntity(cloud20.authenticate(username, password, contentType, contentType), AuthenticateResponse)
        def service = authenticateResponse.serviceCatalog.service.find {it.name.equals("cloudServers")}
        def endpointList = getEntity(cloud20.listEndpointsForToken(identityAdminToken, authenticateResponse.token.id, false, contentType), EndpointList)

        cloud20.deleteEndpointAssignmentRule(serviceAdminToken, createEndpointRule.id)
        cloud20.destroyUser(serviceAdminToken, createUser.id)
        cloud20.deleteTenant(serviceAdminToken, addTenant.id)
        cloud20.deleteRole(serviceAdminToken, createRole.id)
        cloud20.deleteEndpointTemplate(serviceAdminToken, createEndpointTemplate.id.toString())

        then:
        createUser != null
        addTenant != null
        createRole != null
        createEndpointTemplate != null
        createEndpointRule != null
        addRoleToUserOnTenant.status == 200

        if (useEndpointRules) {
            service != null
            assert service.endpoint.find {it.publicURL.contains("bananas.$random")}
            endpointList.endpoint != null
            assert endpointList.endpoint.find {it.publicURL.contains("bananas.$random")}
        } else {
            service == null
            endpointList.endpoint == null
        }

        where:
        contentType                     | useEndpointRules
        MediaType.APPLICATION_XML_TYPE  | true
        MediaType.APPLICATION_XML_TYPE  | false
        MediaType.APPLICATION_JSON_TYPE | true
        MediaType.APPLICATION_JSON_TYPE | false
    }

    def "Authentication include endpoints using endpoint rules when only have implicit role." () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_PROP, true)

        def random = UUID.randomUUID().toString().replace("-", "")

        // Create the user & domain
        def domainId = "domain$random"
        def username = "username$random"
        def createUser = utils.createUser(identityAdminToken, username, domainId)

        // Add a tenant to the domain
        def tenantType = "cloud"
        def tenant = v2Factory.createTenant("12345$random", "12345$random", [tenantType]).with {it.domainId = domainId; it}
        def addTenant = utils.createTenant(tenant)

        // Create endpoint template and rule associating mosso tenant type to endpoint
        def endpointId = testUtils.getRandomInteger().toString()
        def publicUrl = "http://apple.$random"
        def createEndpointTemplate = utils.createEndpointTemplate(v1Factory.createEndpointTemplate(endpointId, "compute", publicUrl, "cloudServers"))
        def createEndpointRule = utils.addEndpointTemplateAssignmentRule(identityAdminToken, v2Factory.createTenantTypeEndpointRule(tenantType, [endpointId]))

        when: "Authenticate with auto-assignment enabled"
        AuthenticateResponse authenticateResponse = utils.authenticate(createUser)
        def service = authenticateResponse.serviceCatalog.service.find {it.name.equals("cloudServers")}

        then: "Then service catalog includes service"
        service != null
        service.endpoint.find {it.publicURL.startsWith(publicUrl)} != null

        cleanup:
        try {
            cloud20.deleteEndpointAssignmentRule(serviceAdminToken, createEndpointRule.id)
            cloud20.destroyUser(serviceAdminToken, createUser.id)
            cloud20.deleteTenant(serviceAdminToken, addTenant.id)
            cloud20.deleteEndpointTemplate(serviceAdminToken, createEndpointTemplate.id.toString())
        } catch (Exception ex) {
            //eat
        }
    }

    def "List endpoints for Token include endpoints using endpoint rules when only have implicit role." () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_PROP, true)

        def random = UUID.randomUUID().toString().replace("-", "")

        // Create the user & domain
        def domainId = "domain$random"
        def username = "username$random"
        def createUser = utils.createUser(identityAdminToken, username, domainId)

        // Add a tenant to the domain
        def tenantType = "cloud"
        def tenant = v2Factory.createTenant("12345$random", "12345$random", [tenantType]).with {it.domainId = domainId; it}
        def addTenant = utils.createTenant(tenant)

        // Create endpoint template and rule associating mosso tenant type to endpoint
        def endpointId = testUtils.getRandomInteger().toString()
        def publicUrl = "http://pear.$random"
        def createEndpointTemplate = utils.createEndpointTemplate(v1Factory.createEndpointTemplate(endpointId, "compute", publicUrl, "cloudServers"))
        def createEndpointRule = utils.addEndpointTemplateAssignmentRule(identityAdminToken, v2Factory.createTenantTypeEndpointRule(tenantType, [endpointId]))

        def uaToken = utils.getToken(username) // User-Admin token

        when: "List Endpoints for Token with auto-assignment enabled"
        def endpointList = utils.getEndpointsForToken(uaToken)

        then: "Then response includes endpoint"
        endpointList.endpoint != null
        endpointList.endpoint.find {it.publicURL.startsWith(publicUrl)} != null

        cleanup:
        try {
            cloud20.deleteEndpointAssignmentRule(serviceAdminToken, createEndpointRule.id)
            cloud20.destroyUser(serviceAdminToken, createUser.id)
            cloud20.deleteTenant(serviceAdminToken, addTenant.id)
            cloud20.deleteEndpointTemplate(serviceAdminToken, createEndpointTemplate.id.toString())
        } catch (Exception ex) {
            //eat
        }
    }

    def "List endpoints for Token returns empty list for rackers regardless of implicit role" () {
        given:
        AuthenticateResponse resp = utils.authenticateRacker(Constants.RACKER, Constants.RACKER_PASSWORD)
        def token = resp.token.id

        when: "List Endpoints for Token with auto-assignment"
        def endpointList = utils.getEndpointsForToken(token)

        then: "Then response does not include endpoint"
        endpointList.endpoint != null
        endpointList.endpoint.size() == 0
    }

    @Unroll
    def "duplicate endpoints removed based on endpoint rules: content-type=#contentType useEndpointRules=#useEndpointRules" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_INCLUDE_ENDPOINTS_BASED_ON_RULES_PROP, useEndpointRules)

        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "username$random"
        def tenantType = "cloud"
        def tenant = v2Factory.createTenant("12345$random", "12345$random", [tenantType])
        def role = v2Factory.createRole("roleName$random", "a45b14e394a57e3fd4e45d59ff3693ead204998b")
        def endpointId = testUtils.getRandomInteger().toString()
        def endpointTemplate = v1Factory.createEndpointTemplate(endpointId, "compute", "http://bananas.$random", "cloudServers")
        def endpointRule = v2Factory.createTenantTypeEndpointRule(tenantType, [endpointId])

        when:
        def createUser = getEntity(cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(
            username, username, "email@email.email", true, "DFW", "domain$random", password), contentType, contentType), User)

        def addTenant = getEntity(cloud20.addTenant(identityAdminToken, tenant, contentType, contentType), Tenant)
        def createRole = getEntity(cloud20.createRole(identityAdminToken, role, contentType, contentType), Role)
        def createEndpointTemplate = getEntity(cloud20.addEndpointTemplate(identityAdminToken, endpointTemplate, contentType, contentType), EndpointTemplate)
        def createEndpointRule = cloud20.addEndpointAssignmentRule(identityAdminToken, endpointRule, contentType, contentType).getEntity(TenantTypeEndpointRule)
        def addRoleToUserOnTenant = cloud20.addRoleToUserOnTenant(identityAdminToken, addTenant.id, createUser.id, createRole.id, contentType)

        //Includes both endpoint based on tenant and endpoint rule.  Duplicates should be removed.
        def addEndpointToTenant = cloud20.addEndpoint(identityAdminToken, addTenant.id, endpointTemplate, contentType, contentType)

        AuthenticateResponse authenticateResponse = getEntity(cloud20.authenticate(username, password, contentType, contentType), AuthenticateResponse)
        def service = authenticateResponse.serviceCatalog.service.find {it.name.equals("cloudServers")}
        def endpointList = getEntity(cloud20.listEndpointsForToken(identityAdminToken, authenticateResponse.token.id, false, contentType), EndpointList)

        cloud20.deleteEndpointAssignmentRule(serviceAdminToken, createEndpointRule.id)
        cloud20.destroyUser(serviceAdminToken, createUser.id)
        cloud20.deleteTenant(serviceAdminToken, addTenant.id)
        cloud20.deleteRole(serviceAdminToken, createRole.id)
        cloud20.deleteEndpointTemplate(serviceAdminToken, createEndpointTemplate.id.toString())

        then:
        createUser != null
        addTenant != null
        createRole != null
        createEndpointTemplate != null
        createEndpointRule != null
        addRoleToUserOnTenant.status == 200
        addEndpointToTenant.status == 200

        service != null
        assert service.endpoint.findAll {it.publicURL.contains("bananas.$random")}.size() == 1
        endpointList.endpoint != null
        assert endpointList.endpoint.findAll {it.publicURL.contains("bananas.$random")}.size() == 1

        where:
        contentType                     | useEndpointRules
        MediaType.APPLICATION_XML_TYPE  | true
        MediaType.APPLICATION_XML_TYPE  | false
        MediaType.APPLICATION_JSON_TYPE | true
        MediaType.APPLICATION_JSON_TYPE | false
    }

    @Unroll
    def "Add new optional attribute for role to specify how a role can be assigned: content-type=#contentType assignment=#assignment" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Role createRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it.assignment = assignment
            it
        }

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)
        def role = getEntity(response, Role)

        then:
        role.assignment == assignment

        cleanup:
        cloud20.deleteRole(identityAdminToken, role.id)

        where:
        contentType                     | assignment
        MediaType.APPLICATION_XML_TYPE  | RoleAssignmentEnum.GLOBAL
        MediaType.APPLICATION_JSON_TYPE | RoleAssignmentEnum.GLOBAL
        MediaType.APPLICATION_XML_TYPE  | RoleAssignmentEnum.TENANT
        MediaType.APPLICATION_JSON_TYPE | RoleAssignmentEnum.TENANT
        MediaType.APPLICATION_XML_TYPE  | RoleAssignmentEnum.BOTH
        MediaType.APPLICATION_JSON_TYPE | RoleAssignmentEnum.BOTH
    }

    @Unroll
    def "Default optional role assignment is both if specified as null or blank: content-type=#contentType assignment=#assignment" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        def createRole;

        if (contentType == MediaType.APPLICATION_JSON_TYPE) {
            createRole = """{"role":{"name":"${name}","serviceId":"${serviceId}","RAX-AUTH:assignment":${assignment}}}"""
        } else {
            createRole = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><ns3:role xmlns:ns1="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0" xmlns:ns2="http://www.w3.org/2005/Atom" xmlns:ns3="http://docs.openstack.org/identity/api/v2.0" xmlns:ns4="http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0" xmlns:ns5="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0" xmlns:ns6="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0" xmlns:ns7="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0" xmlns:ns8="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0" ns1:assignment="${assignment}" serviceId="${serviceId}" name="${name}"/>"""
        }

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)
        def role = getEntity(response, Role)

        then:
        role.assignment == RoleAssignmentEnum.BOTH

        cleanup:
        cloud20.deleteRole(identityAdminToken, role.id)

        where:
        contentType                     | assignment
        MediaType.APPLICATION_JSON_TYPE | '""'
        MediaType.APPLICATION_JSON_TYPE | null
        MediaType.APPLICATION_XML_TYPE  | ""
    }

    @Unroll
    def "Default optional role assignment to both if not specified: content-type=#contentType" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Role createRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it
        }

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)
        def role = getEntity(response, Role)

        then:
        role.assignment == RoleAssignmentEnum.BOTH

        cleanup:
        cloud20.deleteRole(identityAdminToken, role.id)

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "The assignment of 'global only' roles via global assignment. If attempted to be assigned to a user on a tenant, a 403 must be returned: content-type=#contentType" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Role createRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it.assignment = RoleAssignmentEnum.GLOBAL
            it
        }
        def createUser = v2Factory.createUserForCreate(getRandomUUID("user"), "displayName", "test@rackspace.com", true, "ORD", getRandomUUID("domain"), "Password1")
        def tenantId = getRandomNumber(7000000, 8000000)
        def createTenant = v2Factory.createTenant(tenantId.toString(), tenantId.toString())

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)
        def role = getEntity(response, Role)

        response = cloud20.createUser(identityAdminToken, createUser, contentType, contentType)
        def user = getEntity(response, User)

        response = cloud20.addTenant(identityAdminToken, createTenant, contentType, contentType)
        def tenant = getEntity(response, Tenant)

        response = cloud20.addRoleToUserOnTenant(identityAdminToken, tenant.id, user.id, role.id, contentType, contentType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultCloud20Service.ERROR_CANNOT_ASSIGN_GLOBAL_ONLY_ROLES_VIA_TENANT_ASSIGNMENT)

        cleanup:
        cloud20.deleteRole(identityAdminToken, role.id)
        cloud20.deleteTenant(identityAdminToken, tenant.id)
        cloud20.deleteUser(identityAdminToken, user.id)

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "The assignment of 'tenant only' roles via tenant assignment. If attempted to be assigned to a user globally, a 403 must be returned: content-type=#contentType" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Role createRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it.assignment = RoleAssignmentEnum.TENANT
            it
        }
        def createUser = v2Factory.createUserForCreate(getRandomUUID("user"), "displayName", "test@rackspace.com", true, "ORD", getRandomUUID("domain"), "Password1")
        def tenantId = getRandomNumber(7000000, 8000000)
        def createTenant = v2Factory.createTenant(tenantId.toString(), tenantId.toString())

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)
        def role = getEntity(response, Role)

        response = cloud20.createUser(identityAdminToken, createUser, contentType, contentType)
        def user = getEntity(response, User)

        response = cloud20.addTenant(identityAdminToken, createTenant, contentType, contentType)
        def tenant = getEntity(response, Tenant)

        response = cloud20.addUserRole(identityAdminToken, user.id, role.id, contentType, contentType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultCloud20Service.ERROR_CANNOT_ASSIGN_TENANT_ONLY_ROLES_VIA_GLOBAL_ASSIGNMENT)

        cleanup:
        cloud20.deleteRole(identityAdminToken, role.id)
        cloud20.deleteTenant(identityAdminToken, tenant.id)
        cloud20.deleteUser(identityAdminToken, user.id)

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Default optional role type is standard if specified as null or blank: content-type=#contentType roleType=#roleType" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        def createRole;

        if (contentType == MediaType.APPLICATION_JSON_TYPE) {
            createRole = """{"role":{"name":"${name}","serviceId":"${serviceId}","RAX-AUTH:roleType":${roleType}}}"""
        } else {
            createRole = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><ns3:role xmlns:ns1="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0" xmlns:ns2="http://www.w3.org/2005/Atom" xmlns:ns3="http://docs.openstack.org/identity/api/v2.0" xmlns:ns4="http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0" xmlns:ns5="http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0" xmlns:ns6="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0" xmlns:ns7="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0" xmlns:ns8="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0" ns1:roleType="${roleType}" serviceId="${serviceId}" name="${name}"/>"""
        }

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)
        def role = getEntity(response, Role)

        then:
        role.roleType == RoleTypeEnum.STANDARD

        cleanup:
        cloud20.deleteRole(identityAdminToken, role.id)

        where:
        contentType                     | roleType
        MediaType.APPLICATION_JSON_TYPE | '""'
        MediaType.APPLICATION_JSON_TYPE | null
        MediaType.APPLICATION_XML_TYPE  | ""
    }

    @Unroll
    def "'standard' is the default if type is not specified: content-type=#contentType" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Role createRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it
        }

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)
        def role = getEntity(response, Role)

        then:
        role.roleType == RoleTypeEnum.STANDARD

        cleanup:
        cloud20.deleteRole(identityAdminToken, role.id)

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Add ability for user to set 'tenantTypes' additional attribute on role creation: content-type=#contentType tenantTypes=#tenantTypes" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Types types = new Types().with {
            it.type = tenantTypes
            it
        }
        Role createRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it.roleType = RoleTypeEnum.RCN
            it.types = types
            it
        }

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)
        Role role = getEntity(response, Role)

        then:
        role.types.type as HashSet == tenantTypes as HashSet

        cleanup:
        cloud20.deleteRole(identityAdminToken, role.id)

        where:
        contentType                     | tenantTypes
        MediaType.APPLICATION_XML_TYPE  | ["cloud", "files"]
        MediaType.APPLICATION_JSON_TYPE | ["cloud", "files"]
    }

    @Unroll
    def "The specified tenant types must meet the standard tenant type validation with the exception that '*' is reserved, to represent all tenant types.: content-type=#contentType" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Types types = new Types().with {
            it.type = tenantTypes
            it
        }
        Role createRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it.roleType = RoleTypeEnum.RCN
            it.types = types
            it
        }

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)
        Role role = getEntity(response, Role)

        then:
        role.types.type as HashSet == tenantTypes as HashSet

        cleanup:
        cloud20.deleteRole(identityAdminToken, role.id)

        where:
        contentType                     | tenantTypes
        MediaType.APPLICATION_XML_TYPE  | ["*"]
        MediaType.APPLICATION_JSON_TYPE | ["*"]
    }

    @Unroll
    def "The response for create role and get role by ID must be updated to include 'standard' if no type is stored: content-type=#contentType" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Role createRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it
        }

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)
        Role createdRole = getEntity(response, Role)

        response = cloud20.getRole(identityAdminToken, createdRole.id)
        Role role = getEntity(response, Role)

        then:
        createdRole.roleType == RoleTypeEnum.STANDARD
        role.roleType == RoleTypeEnum.STANDARD

        then: "tenantTypes only if RCN was specified (attribute must not be returned if not an RCN role)"
        createdRole.types == null
        role.types == null

        cleanup:
        cloud20.deleteRole(identityAdminToken, role.id)

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "The provided tenant types must be automatically lower-cased prior to any validation: content-type=#contentType tenantTypes=#tenantTypes" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Types types = new Types().with {
            it.type = tenantTypes
            it
        }
        Role createRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it.roleType = RoleTypeEnum.RCN
            it.types = types
            it
        }

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)
        Role role = getEntity(response, Role)

        then:
        role.types.type as HashSet == tenantTypes.collect {it.toLowerCase()} as HashSet

        cleanup:
        cloud20.deleteRole(identityAdminToken, role.id)

        where:
        contentType                     | tenantTypes
        MediaType.APPLICATION_XML_TYPE  | ["Cloud", "FILES"]
        MediaType.APPLICATION_JSON_TYPE | ["Cloud", "FILES"]
    }

    @Unroll
    def "'rcn' as the type and assignment is not specified default to 'global' assignment: content-type=#contentType" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Types types = new Types().with {
            it.type = ["cloud", "files"]
            it
        }
        Role createRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it.roleType = RoleTypeEnum.RCN
            it.types = types
            it
        }

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)
        Role role = getEntity(response, Role)

        then: "default the assignment to 'global'"
        role.assignment == RoleAssignmentEnum.GLOBAL

        cleanup:
        cloud20.deleteRole(identityAdminToken, role.id)

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "role 'tenantTypes' required if type is 'rcn': content-type=#contentType" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Role createRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it.roleType = RoleTypeEnum.RCN
            it
        }

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, Validator20.ERROR_TENANT_REQUIRED_WHEN_TYPE_IS_RCN)

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "When 'rcn' type is specified the 'assignment' MUST be 'global': content-type=#contentType assignment=#assignment" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Role createRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it.assignment = assignment
            it.roleType = RoleTypeEnum.RCN
            it
        }

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, Validator20.ERROR_TENANT_RCN_ROLE_MUST_HAVE_GLOBAL_ASSIGNMENT)

        where:
        contentType                     | assignment
        MediaType.APPLICATION_XML_TYPE  | RoleAssignmentEnum.TENANT
        MediaType.APPLICATION_JSON_TYPE | RoleAssignmentEnum.BOTH
    }

    @Unroll
    def "A maximum of 16 unique tenant types can be assigned to any given role: content-type=#contentType" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Types types = new Types().with {
            it.type = ["type1", "type2", "type3", "type4", "type5", "type6", "type7", "type8", "type9", "type10", "type11", "type12", "type13", "type14", "type15", "type16", "type17"]
            it
        }
        Role createRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it.roleType = RoleTypeEnum.RCN
            it.types = types
            it
        }

        when:
        def response = cloud20.createRole(identityAdminToken, createRole, contentType, contentType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, Validator20.ERROR_TENANT_TYPE_CANNOT_EXCEED_MAXIMUM)

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Roles returned for list roles (/v2.0/OS-KSADM/roles) should include the attributes as applicable: content-type=#contentType" () {
        given:
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def rcnName = getRandomUUID("role")
        def standardName = getRandomUUID("role")
        Types types = new Types().with {
            it.type = ["cloud"]
            it
        }
        def rcnRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = rcnName
            it.roleType = RoleTypeEnum.RCN
            it.types = types
            it
        }

        def standardRole = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = standardName
            it
        }

        when:
        def response = cloud20.createRole(identityAdminToken, rcnRole, contentType, contentType)
        def createdRcnRole = getEntity(response, Role)

        response = cloud20.createRole(identityAdminToken, standardRole, contentType, contentType)
        def createdStandardRole = getEntity(response, Role)

        response = cloud20.listRoles(identityAdminToken, null, null, null, rcnName, contentType)
        RoleList roles = getEntity(response, RoleList)
        def retrievedRcnRole = roles.role.get(0)

        response = cloud20.listRoles(identityAdminToken, null, null, null, standardName, contentType)
        roles = getEntity(response, RoleList)
        def retrievedStandardRole =  roles.role.get(0)

        then: "Assignment must always be returned. When not set on the role, must return value 'both'"
        createdRcnRole.assignment == RoleAssignmentEnum.GLOBAL
        createdStandardRole.assignment == RoleAssignmentEnum.BOTH

        retrievedRcnRole.assignment == RoleAssignmentEnum.GLOBAL
        retrievedStandardRole.assignment == RoleAssignmentEnum.BOTH

        then: "type must always be returned. When not set on the role, must return value 'standard'"
        createdRcnRole.roleType == RoleTypeEnum.RCN
        createdStandardRole.roleType == RoleTypeEnum.STANDARD

        retrievedRcnRole.roleType == RoleTypeEnum.RCN
        retrievedStandardRole.roleType == RoleTypeEnum.STANDARD

        then: "Tenant Type must only be returned when type is 'rcn'"
        createdRcnRole.types.type as HashSet == ["cloud"] as HashSet
        createdStandardRole.types == null

        retrievedRcnRole.types.type as HashSet == ["cloud"] as HashSet
        retrievedStandardRole.types == null

        cleanup:
        utils.deleteRoleQuietly(createdRcnRole)
        utils.deleteRoleQuietly(createdStandardRole)

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "Changes to the above services as specified must be enabled via a feature flag 'feature.list.support.additional.role.properties': content-type=#contentType" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_LIST_SUPPORT_ADDITIONAL_ROLE_PROPERTIES_PROP, false)
        def serviceId = Constants.IDENTITY_SERVICE_ID
        def name = getRandomUUID("role")
        Types types = new Types().with {
            it.type = ["cloud"]
            it
        }
        def role = v2Factory.createRole().with {
            it.serviceId = serviceId
            it.name = name
            it.roleType = RoleTypeEnum.RCN
            it.types = types
            it
        }

        when:
        def response = cloud20.createRole(identityAdminToken, role, contentType, contentType)
        def createdRole = getEntity(response, Role)

        response = cloud20.listRoles(identityAdminToken, null, null, null, name, contentType)
        RoleList roles = getEntity(response, RoleList)
        def retrievedRole = roles.role.get(0)

        then: "Assignment must not be returned."
        createdRole.assignment == null
        retrievedRole.assignment == null

        then: "Type must not be returned."
        createdRole.roleType == null
        retrievedRole.roleType == null

        then: "Tenant Type must not be returned."
        createdRole.types == null
        retrievedRole.types == null

        cleanup:
        cloud20.deleteRole(identityAdminToken, createdRole.id)

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def getEntity(response, type) {
        if (type == RoleList && response.getType() == MediaType.APPLICATION_JSON_TYPE) {
            InputStream inputStream = IOUtils.toInputStream(response.getEntity(String))
            return readerForRoles.readFrom(Role, null, null, null, null, inputStream)
        }

        def entity = response.getEntity(type)

        if(response.getType() == MediaType.APPLICATION_XML_TYPE) {
            entity = entity.value
        }
        return entity
    }

    def authAndExpire(String username, String password) {
        Token token = cloud20.authenticatePassword(username, password).getEntity(AuthenticateResponse).value.token
        cloud20.revokeUserToken(token.id, token.id)
        return token.expires.toGregorianCalendar().getTime()
    }

    //Helper Methods
    def setConfigValues() {
        REFRESH_WINDOW_HOURS = config.getInt("token.refreshWindowHours")
        CLOUD_CLIENT_ID = config.getString("cloudAuth.clientId")
    }

    def setTokenInRefreshWindow(String uid, String accessToken) {
        def resultCloudAuthScopeAccess = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=scopeAccess)(userRsId=$USER_FOR_AUTH_ID)(accessToken=$accessToken))","*")
        for (SearchResultEntry entry in resultCloudAuthScopeAccess.getSearchEntries()) {
            def entity = LDAPPersister.getInstance(ScopeAccess.class)decode(entry)
            if (!entity.isAccessTokenWithinRefreshWindow(config.getInt("token.refreshWindowHours"))) {
                entity.accessTokenExp = new DateTime().plusHours(REFRESH_WINDOW_HOURS).minusHours(2).toDate()
                List<Modification> mods = LDAPPersister.getInstance(ScopeAccess.class).getModifications(entity, true)
                connPools.getAppConnPool().modify(entity.getUniqueId(), mods)
            }
        }
    }
}
