package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Question
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Questions
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Region
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import org.apache.commons.lang.StringUtils
import org.joda.time.Seconds
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll
import testHelpers.Cloud20Methods
import testHelpers.RootIntegrationTest
import testHelpers.V2Factory

import org.openstack.docs.identity.api.v2.*

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.configuration.Configuration
import org.springframework.test.context.ContextConfiguration
import com.rackspace.idm.domain.dao.impl.LdapConnectionPools
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.persist.LDAPPersister
import com.rackspace.idm.domain.entity.ScopeAccess
import com.unboundid.ldap.sdk.Modification
import org.joda.time.DateTime
import com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQAs

import static com.rackspace.idm.RaxAuthConstants.*

class Cloud20IntegrationTest extends RootIntegrationTest {
    @Autowired LdapConnectionPools connPools
    @Autowired Configuration config
    @Autowired DefaultCloud20Service cloud20Service

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
    @Shared def defaultUser
    @Shared def defaultUserTwo
    @Shared def defaultUserThree
    @Shared def defaultUserForAdminTwo
    @Shared def defaultUserWithManageRole
    @Shared def defaultUserWithManageRole2
    @Shared def testUser
    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def sharedRole
    @Shared def sharedRoleTwo
    @Shared def propagatingRole
    @Shared def tenant

    @Shared def emptyDomainId
    @Shared def testDomainId
    @Shared def testDomainId2
    @Shared def defaultRegion
    @Shared def endpointTemplateId
    @Shared def policyId

    def randomness = UUID.randomUUID()
    @Shared def groupLocation
    @Shared def group
    @Shared Region sharedRegion

    static def USER_MANAGE_ROLE_ID = "7"
    @Shared REFRESH_WINDOW_HOURS
    @Shared CLOUD_CLIENT_ID
    @Shared BASE_DN = "o=rackspace,dc=rackspace,dc=com"
    @Shared SCOPE = SearchScope.SUB
    
    @Shared def USER_FOR_AUTH
    @Shared def USER_FOR_AUTH_PWD

    @Shared def defaultUserRoleId = "2"
    @Shared def userAdminRoleId = "3"
    @Shared def identityAdminRoleId = "1"
    @Shared def serviceAdminRoleId = "4"

    @Shared def cloudAuthClientId

    def setupSpec() {
        sharedRandom = ("$sharedRandomness").replace('-',"")
        testDomainId = "domain1$sharedRandom"
        testDomainId2 = "domain2$sharedRandom"
        emptyDomainId = "domain3$sharedRandom"

        this.objFactories = new JAXBObjectFactories()
        serviceAdminToken = cloud20.authenticatePassword("authQE", "Auth1234").getEntity(AuthenticateResponse).value.token.id
        serviceAdmin = cloud20.getUserByName(serviceAdminToken, "authQE").getEntity(User)

        identityAdmin = cloud20.getUserByName(serviceAdminToken, "auth").getEntity(User)
        identityAdminToken = cloud20.authenticatePassword("auth", "auth123").getEntity(AuthenticateResponse).value.token.id

        cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("admin$sharedRandom", "display", "email@email.com", true, null, null, "Password1"))
        testUser = cloud20.getUserByName(serviceAdminToken, "admin$sharedRandom").getEntity(User)
        USER_FOR_AUTH = testUser.username
        USER_FOR_AUTH_PWD = "Password1"

        endpointTemplateId = "100001"
        cloud20.addEndpointTemplate(serviceAdminToken, v1Factory.createEndpointTemplate(endpointTemplateId, null, null))
        def addPolicyResponse = cloud20.addPolicy(serviceAdminToken, v1Factory.createPolicy("name", null, null))
        def getPolicyResponse = cloud20.getPolicy(serviceAdminToken, addPolicyResponse.location)
        policyId = getPolicyResponse.getEntity(Policy).id as String


        defaultRegion = v1Factory.createRegion("ORD", true, true)
        cloud20.createRegion(serviceAdminToken, defaultRegion)
        cloud20.updateRegion(serviceAdminToken, defaultRegion.name, defaultRegion)

        //User Admin
        def createResponse = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate("userAdmin1$sharedRandom", "display", "test@rackspace.com", true, null, testDomainId, "Password1"))
        userAdmin = cloud20.getUserByName(identityAdminToken, "userAdmin1$sharedRandom").getEntity(User)
        cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate("userAdmin2$sharedRandom", "display", "test@rackspace.com", true, null, emptyDomainId, "Password1"))
        userAdminTwo = cloud20.getUserByName(identityAdminToken, "userAdmin2$sharedRandom").getEntity(User)

        userAdminToken = cloud20.authenticatePassword("userAdmin1$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id
        userAdminTwoToken = cloud20.authenticatePassword("userAdmin2$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id

        // Default Users
        cloud20.createUser(userAdminToken, v2Factory.createUserForCreate("defaultUser1$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUser = cloud20.getUserByName(userAdminToken, "defaultUser1$sharedRandom").getEntity(User)
        cloud20.createUser(userAdminToken, v2Factory.createUserForCreate("defaultUser2$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserTwo = cloud20.getUserByName(userAdminToken, "defaultUser2$sharedRandom").getEntity(User)
        cloud20.createUser(userAdminToken, v2Factory.createUserForCreate("defaultUser3$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserThree = cloud20.getUserByName(userAdminToken, "defaultUser3$sharedRandom").getEntity(User)
        cloud20.createUser(userAdminTwoToken, v2Factory.createUserForCreate("defaultUser4$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserForAdminTwo = cloud20.getUserByName(userAdminTwoToken, "defaultUser4$sharedRandom").getEntity(User)
        defaultUserToken = cloud20.authenticatePassword("defaultUser1$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id
        cloud20.createUser(userAdminToken, v2Factory.createUserForCreate("defaultUserWithManageRole$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserWithManageRole = cloud20.getUserByName(userAdminToken, "defaultUserWithManageRole$sharedRandom").getEntity(User)
        defaultUserManageRoleToken = cloud20.authenticate("defaultUserWithManageRole$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id

        cloud20.createUser(userAdminToken, v2Factory.createUserForCreate("defaultUserWithManageRole2$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserWithManageRole2 = cloud20.getUserByName(userAdminToken, "defaultUserWithManageRole2$sharedRandom").getEntity(User)

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
        createRole.name = "sharedRole1"
        def responseRole = cloud20.createRole(serviceAdminToken, createRole)
        sharedRole = responseRole.getEntity(Role).value

        Role createRole2 = v2Factory.createRole()
        createRole2.serviceId = "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        createRole2.name = "sharedRole2"
        def responseRole2 = cloud20.createRole(serviceAdminToken, createRole2)
        sharedRoleTwo = responseRole2.getEntity(Role).value

        def role = v2Factory.createRole(true, 500).with {
            it.name = "propagatingRole$sharedRandom"
            return it
        }
        def responsePropagateRole = cloud20.createRole(serviceAdminToken, role)
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

    def setup() {
        expireTokens(USER_FOR_AUTH, 12)
        setConfigValues()
        cloudAuthClientId = config.getString("cloudAuth.clientId")
    }

    def cleanupSpec() {
        cloud20.deleteGroup(serviceAdminToken, group.getId())
        cloud20.deleteRegion(serviceAdminToken, sharedRegion.getName())

        cloud20.deleteRole(serviceAdminToken, sharedRole.getId())
        cloud20.deleteRole(serviceAdminToken, sharedRoleTwo.getId())
        cloud20.deleteRole(serviceAdminToken, propagatingRole.getId())

        cloud20.destroyUser(serviceAdminToken, userAdmin.getId())
        cloud20.destroyUser(serviceAdminToken, userAdminTwo.getId())

        cloud20.destroyUser(serviceAdminToken, defaultUser.getId())
        cloud20.destroyUser(serviceAdminToken, defaultUserTwo.getId())
        cloud20.destroyUser(serviceAdminToken, defaultUserThree.getId())
        cloud20.destroyUser(serviceAdminToken, defaultUserForAdminTwo.getId())
        cloud20.destroyUser(serviceAdminToken, defaultUserWithManageRole.getId())
        cloud20.destroyUser(serviceAdminToken, defaultUserWithManageRole2.getId())

        cloud20.destroyUser(serviceAdminToken, testUser.getId())

        //TODO: DELETE DOMAINS
        cloud20.deleteEndpointTemplate(serviceAdminToken, endpointTemplateId)
        cloud20.deletePolicy(serviceAdminToken, policyId)
    }

    def "authenticating where total access tokens remains unchanged"() {
        when:
        def scopeAccessOne = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        def scopeAccessTwo = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        def allUsersScopeAccessAfter = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=UserScopeAccess)(uid=$USER_FOR_AUTH))", "*")

        then:
        scopeAccessOne.token.id.equals(scopeAccessTwo.token.id)
        allUsersScopeAccessAfter.entryCount <= 2
    }

    def "authenticating where token is within refresh window adds new token"() {
        when:
        def scopeAccessOne = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        setTokenInRefreshWindow(USER_FOR_AUTH, scopeAccessOne.token.id)

        def allUsersScopeAccessBefore = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=UserScopeAccess)(uid=$USER_FOR_AUTH))", "*")

        def scopeAccessTwo = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        def allUsersScopeAccessAfter = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=UserScopeAccess)(uid=$USER_FOR_AUTH))", "*")

        then:
        allUsersScopeAccessBefore.getEntryCount() + 1 == allUsersScopeAccessAfter.getEntryCount()
        !scopeAccessOne.token.id.equals(scopeAccessTwo.token.id)

    }

    def "authenticating where token is valid returns existing token"() {
        when:
        def scopeAccessOne = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        def scopeAccessTwo = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        then:
        scopeAccessOne.token.id.equals(scopeAccessTwo.token.id)
    }

    def "authenticate with two valid tokens"() {
        when:
        def firstScopeAccess = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        setTokenInRefreshWindow(USER_FOR_AUTH, firstScopeAccess.token.id)
        def secondScopeAccess = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        def thirdScopeAccess = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        then:
        secondScopeAccess.token.id.equals(thirdScopeAccess.token.id)
    }

    def "authenticating token in refresh window with 2 existing tokens deletes existing expired token"() {
        when:
        def firstScopeAccess = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        setTokenInRefreshWindow(USER_FOR_AUTH, firstScopeAccess.token.id)
        def secondScopeAccess = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        expireToken(USER_FOR_AUTH, firstScopeAccess.token.id, 12)
        setTokenInRefreshWindow(USER_FOR_AUTH, secondScopeAccess.token.id)
        def thirdScopeAccess = cloud20.authenticatePassword(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        def allUsersScopeAccessAfter = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=scopeAccess)(uid=authQE))", "*")

        then:
        !thirdScopeAccess.token.id.equals(secondScopeAccess.token.id)
        for (entry in allUsersScopeAccessAfter.searchEntries) {
            assert(!entry.DN.contains("$firstScopeAccess.token.id"))
        }
    }

    def 'User CRUD'() {
        when:
        //Create user
        def random = ("$randomness").replace('-', "")
        def user = v2Factory.createUserForCreate("bob" + random, "displayName", "test@rackspace.com", true, "ORD", null, "Password1")
        def response = cloud20.createUser(serviceAdminToken, user)
        //Get user
        def getUserResponse = cloud20.getUser(serviceAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User)
        //Get user by id
        def getUserByIdResponse = cloud20.getUserById(serviceAdminToken, userEntity.getId())
        def getUserByNameResponse = cloud20.getUserByName(serviceAdminToken, userEntity.getUsername())
        def getUserByEmailResponse = cloud20.getUsersByEmail(serviceAdminToken, userEntity.getEmail())
        //Update User
        def userForUpdate = v2Factory.createUserForUpdate(null, "updatedBob" + random, "Bob", "test@rackspace.com", false, null, null)
        def updateUserResponse = cloud20.updateUser(serviceAdminToken, userEntity.getId(), userForUpdate)
        //Delete user
        def deleteResponses = cloud20.deleteUser(serviceAdminToken, userEntity.getId())
        //Hard delete user
        def hardDeleteResponses = cloud20.hardDeleteUser(serviceAdminToken, userEntity.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        getUserByIdResponse.status == 200
        getUserByNameResponse.status == 200
        getUserByEmailResponse.status == 200
        updateUserResponse.status == 200
        deleteResponses.status == 204
        hardDeleteResponses.status == 204
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
        def userEntity = getUserResponse.getEntity(User)

        //Update User
        def userForUpdate = v2Factory.createUserForUpdate(null, "updatedBob" + random, "Bob", "test@rackspace.com", false, null, null)
        def updateUserResponse = cloud20.updateUser(defaultUserManageRoleToken, userEntity.getId(), userForUpdate)

        //Delete user
        def deleteResponses = cloud20.deleteUser(defaultUserManageRoleToken, userEntity.getId())
        def hardDeleteResponse = cloud20.hardDeleteUser(serviceAdminToken, userEntity.getId())

        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        updateUserResponse.status == 200
        deleteResponses.status == 204
        hardDeleteResponse.status == 204
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
        def userEntity = getUserResponse.getEntity(User)

        //Delete user
        def deleteResponses = cloud20.deleteUser(defaultUserManageRoleToken, userEntity.getId())
        def hardDeleteResponse = cloud20.hardDeleteUser(serviceAdminToken, userEntity.getId())

        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        deleteResponses.status == 204
        hardDeleteResponse.status == 204
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
        def userEntity = getUserResponse.getEntity(User)

        //Delete user
        def deleteResponses = cloud20.deleteUser(defaultUserManageRoleToken, userEntity.getId())
        def hardDeleteResponse = cloud20.hardDeleteUser(serviceAdminToken, userEntity.getId())

        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        then:
        response.status == 201
        response.location != null
        getUserByEmailResponse.status == 200
        getUserResponse.status == 200
        deleteResponses.status == 204
        hardDeleteResponse.status == 204
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
        def userEntity = getUserResponse.getEntity(User)

        //Get apiKey
        def createApiKey = cloud20.addApiKeyToUser(serviceAdminToken, userEntity.getId(), v2Factory.createApiKeyCredentials(userName, "bananas"))
        def getApiKeyResponse = cloud20.getUserApiKey(defaultUserManageRoleToken, userEntity.getId())

        //Delete user
        def deleteResponses = cloud20.deleteUser(defaultUserManageRoleToken, userEntity.getId())
        def hardDeleteResponse = cloud20.hardDeleteUser(serviceAdminToken, userEntity.getId())

        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        createApiKey.status == 200
        getApiKeyResponse.status == 200
        deleteResponses.status == 204
        hardDeleteResponse.status == 204
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
        def userEntity = getUserResponse.getEntity(User)
        cloud20.addApplicationRoleToUser(serviceAdminToken, USER_MANAGE_ROLE_ID, userEntity.getId())

        //Delete user
        def deleteResponses = cloud20.deleteUser(defaultUserManageRoleToken, userEntity.getId())
        //Hard delete user
        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, userEntity.getId())
        def actualDelete = cloud20.deleteUser(defaultUserManageRoleToken, userEntity.getId())
        def hardDeleteResponses = cloud20.hardDeleteUser(serviceAdminToken, userEntity.getId())

        cloud20.deleteApplicationRoleFromUser(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        deleteResponses.status == 401
        actualDelete.status == 204
        hardDeleteResponses.status == 204
    }

    def "user-manage cannot be added to user admin"() {
        when:
        //Create user
        def random = ("$randomness").replace('-', "")
        def user = v2Factory.createUserForCreate("somename" + random, "displayName", "test@rackspace.com", true, "ORD", "domain$random", "Password1")
        def response = cloud20.createUser(identityAdminToken, user)

        //Get user
        def getUserResponse = cloud20.getUser(identityAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User)

        def addUserManageRole = cloud20.addApplicationRoleToUser(identityAdminToken, USER_MANAGE_ROLE_ID, userEntity.getId())

        def getUserRole = cloud20.getUserApplicationRole(identityAdminToken, USER_MANAGE_ROLE_ID, userEntity.getId())

        //Hard delete user
        cloud20.deleteApplicationRoleFromUser(identityAdminToken, USER_MANAGE_ROLE_ID, userEntity.getId())
        def actualDelete = cloud20.deleteUser(identityAdminToken, userEntity.getId())
        def hardDeleteResponses = cloud20.hardDeleteUser(serviceAdminToken, userEntity.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        addUserManageRole.status == 400
        getUserRole.status == 404
        actualDelete.status == 204
        hardDeleteResponses.status == 204
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

    def "a user can be retrieved by email"() {
        when:
        def createUser = v2Factory.createUserForCreate("user1$sharedRandom", "user1$sharedRandom", email, true, "ORD", null, "Password1")
        def createdUser = cloud20.createUser(serviceAdminToken, createUser).getEntity(User)
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
        def user1 = v2Factory.createUserForCreate("user1$sharedRandom", "user1$sharedRandom", email, true, "ORD", null, "Password1")
        def user2 = v2Factory.createUserForCreate("user2$sharedRandom", "user2$sharedRandom", email, true, "ORD", null, "Password1")
        def createdUser1 = cloud20.createUser(serviceAdminToken, user1).getEntity(User)
        def createUser2 = cloud20.createUser(serviceAdminToken, user2).getEntity(User)
        cloud20.destroyUser(serviceAdminToken, createdUser1.getId())
        cloud20.destroyUser(serviceAdminToken, createUser2.getId())
        def response = cloud20.getUsersByEmail(serviceAdminToken, email)

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
                cloud20.addCredential(serviceAdminToken, "badId", v2Factory.createPasswordCredentialsRequiredUsername("someUser", "SomePassword1"))
        ]
    }

    def "invalid operations on create/update user returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("!@#What", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("What!@#", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                //createUser(serviceAdminToken, v2Factory.createUserForCreate("1one", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("one name", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate(null, "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("a$sharedRandom", "display", "junk!@#", true, "ORD", null, "Password1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("z$sharedRandom", "display", "   ", true, "ORD", null, "Password1")),
                //createUser(serviceAdminToken, v2Factory.createUserForCreate("b$sharedRandom", "display", null, true, "ORD", null, "Password1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("c$sharedRandom", "display", "test@rackspace.com", true, "ORD", null, "Pop1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("d$sharedRandom", "display", "test@rackspace.com", true, "ORD", null, "longpassword1")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("e$sharedRandom", "display", "test@rackspace.com", true, "ORD", null, "Longpassword")),
                cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate("f$sharedRandom", "displ:ay", "test@rackspace.com", true, "ORD", "someId", "Longpassword")),
                cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate("g$sharedRandom", "display", "test@rackspace.com", true, "ORD", null, "Longpassword1")),
                //updateUser(userAdminToken, defaultUser.getId(), userForUpdate("1", "someOtherName", "someOtherDisplay", "some@rackspace.com", true, "ORD", "SomeOtherPassword1")),
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
                cloud20.getUserById(defaultUserToken, userAdmin.getId()),
                cloud20.getUserById(defaultUserToken, identityAdmin.getId()),
                cloud20.getUserById(defaultUserToken, serviceAdmin.getId()),
                cloud20.getUserById(userAdminToken, identityAdmin.getId()),
                cloud20.getUserById(userAdminToken, serviceAdmin.getId()),
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
        def tempUser = cloud20.getUserByName(userAdminToken, tempUserAdmin).getEntity(User)

        def getUsersFromGroupResponse2 = cloud20.getUsersFromGroup(identityAdminToken, group.getId())
        UserList users2 = getUsersFromGroupResponse2.getEntity(UserList).value

        cloud20.removeUserFromGroup(identityAdminToken, group.getId(), userAdmin.getId())

        cloud20.deleteUser(userAdminToken, tempUser.getId())

        then:
        users1.user.size() + 1 == users2.user.size()
        users2.user.findAll({it.username == tempUserAdmin}).size() == 1
    }

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
                cloud20.addUserToGroup(serviceAdminToken, "doesnotexist", defaultUser.getId()),
                cloud20.addUserToGroup(serviceAdminToken, group.getId(), defaultUser.getId()),
                cloud20.removeUserFromGroup(serviceAdminToken, group.getId(), defaultUser.getId()),
        ]
    }

    def "invalid operations on create/update group returns 'not found'"() {
        expect:
        response.status == 404

        where:
        response << [
                cloud20.addUserToGroup(serviceAdminToken, group.getId(), "doesnotexist"),
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

    def "listUsersWithRole called by admin invalid roleId returns not found"() {
        when:
        def response = cloud20.listUsersWithRole(serviceAdminToken, "-5")

        then:
        response.status == 404
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
                cloud20.listUsersWithRole(serviceAdminToken, sharedRole.getId(), "abC", "5"),
                cloud20.listUsersWithRole(serviceAdminToken, sharedRole.getId(), "5", "bCg"),
                cloud20.listUsersWithRole(serviceAdminToken, sharedRole.getId(), "abC", "asdf")
        ]
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
        users.size() == 6
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
                cloud20.listUsers(serviceAdminToken, "0", "abc"),
                cloud20.listUsers(serviceAdminToken, "abc", "10")
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

    def "adding identity:* roles to user on tenant returns 400"() {
        expect:
        response.status == 403

        where:
        response << [
                cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), defaultUserRoleId),
                cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), userAdminRoleId),
                cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), identityAdminRoleId),
                cloud20.addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), serviceAdminRoleId)
        ]
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

    def "add policy to endpoint without endpoint without policy returns 404"() {
        when:
        def response = cloud20.addPolicyToEndpointTemplate(identityAdminToken, "111111", "111111")

        then:
        response.status == 404
    }

    def "add policy to endpoint with endpoint without policy returns 404"() {
        when:
        def response = cloud20.addPolicyToEndpointTemplate(identityAdminToken, endpointTemplateId, "111111")

        then:
        response.status == 404
    }

    def "add policy to endpoint with endpoint with policy returns 204"() {
        when:
        def addResponse = cloud20.addPolicyToEndpointTemplate(serviceAdminToken, endpointTemplateId, policyId)
        def getResponse = cloud20.getPoliciesFromEndpointTemplate(serviceAdminToken, endpointTemplateId)
        def policies = getResponse.getEntity(Policies)
        def updateResponse = cloud20.updatePoliciesForEndpointTemplate(serviceAdminToken, endpointTemplateId, policies)
        def deletePolicyResponse = cloud20.deletePolicy(serviceAdminToken, policyId)
        def deleteResponse = cloud20.deletePolicyToEndpointTemplate(serviceAdminToken, endpointTemplateId, policyId)

        then:
        addResponse.status == 204
        policies.policy.size() == 1
        updateResponse.status == 204
        deletePolicyResponse.status == 400
        deleteResponse.status == 204
    }

    def "update policy to endpoint without endpoint without policy returns 404"() {
        when:
        Policies policies = new Policies()
        Policy policy = v1Factory.createPolicy("name", null, null)
        policies.policy.add(policy)
        def response = cloud20.updatePoliciesForEndpointTemplate(serviceAdminToken, "111111", policies)

        then:
        response.status == 404
    }

    def "update policy to endpoint with endpoint without policy returns 404"() {
        when:
        Policies policies = new Policies()
        Policy policy = v1Factory.createPolicy("name", null, null)
        policies.policy.add(policy)
        def response = cloud20.updatePoliciesForEndpointTemplate(serviceAdminToken, endpointTemplateId, policies)

        then:
        response.status == 404
    }

    def "Create createSecretQA and get createSecretQA"() {
        when:
        def response = cloud20.createSecretQA(serviceAdminToken,defaultUser.getId(), v1Factory.createSecretQA("1","answer"))
        def createSecretQAResponse = cloud20.getSecretQA(serviceAdminToken, defaultUser.getId()).getEntity(SecretQAs)

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
                cloud20.getSecretQA(defaultUserToken, serviceAdmin.getId()),
                cloud20.getSecretQA(defaultUserToken, identityAdmin.getId()),
                cloud20.getSecretQA(userAdminToken, serviceAdmin.getId()),
                cloud20.getSecretQA(userAdminToken, identityAdmin.getId()),
                cloud20.getSecretQA(userAdminToken, userAdminTwo.getId()),
                cloud20.getSecretQA(userAdminToken, defaultUserForAdminTwo.getId())
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
                cloud20.getSecretQA("", serviceAdmin.getId()),
                cloud20.getSecretQA("", identityAdmin.getId()),
                cloud20.getSecretQA(null, serviceAdmin.getId()),
                cloud20.getSecretQA(null, identityAdmin.getId()),
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
                cloud20.getSecretQA(serviceAdminToken, "badId")
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
                cloud20.getSecretQA(serviceAdminToken, defaultUser.getId()),
                cloud20.getSecretQA(identityAdminToken, defaultUser.getId()),
                cloud20.getSecretQA(userAdminToken, defaultUser.getId()),
                cloud20.getSecretQA(defaultUserToken, defaultUser.getId()),
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
        def response = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", "someDomain", "Password1"))
        User user = response.getEntity(User)
        String password = "Password1~!@#\$%^&*_#\$%^% <>?:\"^(%)'"
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername().with {
            it.username = username
            it.password = password
            return it
        }

        when:
        def updateCreds = cloud20.updateCredentials(identityAdminToken, user.id, creds)
        String updatePassword = updateCreds.getEntity(PasswordCredentialsRequiredUsername).value.password
        def authenticate = cloud20.authenticatePassword(user.username,updatePassword)

        then:
        updateCreds.status == 200
        authenticate.status == 200
    }

    def "updateCredentials with invalid password should return BadRequestException"() {
        given:
        String username = "userUpdateCred2" + sharedRandom
        def response = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", "someDomain", "Password1"))
        User user = response.getEntity(User)
        String password = "Password1~!@א"
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername().with {
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
        def response = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", "someDomain", "Password1"))
        User user = response.getEntity(User)

        when:
        def authRequest = cloud20.authenticatePassword(username, "Password1")
        String token = authRequest.getEntity(AuthenticateResponse).value.token.id
        def userForUpdate = v2Factory.createUserForUpdate(null, username, null, "test@rackspace.com", false, null, null)
        def updateUserResponse = cloud20.updateUser(serviceAdminToken, user.id, userForUpdate)
        def validateResponse = cloud20.validateToken(serviceAdminToken, token)
        def deleteResponses = cloud20.deleteUser(serviceAdminToken, user.id)
        def hardDeleteRespones = cloud20.hardDeleteUser(serviceAdminToken, user.id)

        then:
        authRequest.status == 200
        token != null
        updateUserResponse.status == 200
        validateResponse.status == 404
        deleteResponses.status == 204
        hardDeleteRespones.status == 204
    }

    def "Disable a userAdmin disables his subUsers"() {
        given:
        def domain = "someDomain$sharedRandom"
        def adminUsername = "userAdmin$sharedRandom"
        def username = "user$sharedRandom"
        def password = "Password1"
        def userAdminForCreate = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(adminUsername, "displayName", "someEmail@rackspace.com", true, "ORD", domain, password))
        User userAdmin = userAdminForCreate.getEntity(User)

        def userAdminToken = cloud20.authenticatePassword(adminUsername, password).getEntity(AuthenticateResponse).value.token.id
        def createUserForCreate = cloud20.createUser(userAdminToken, v2Factory.createUserForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", domain, "Password1"))

        when:
        userAdmin.enabled = false;
        def updateUserResponse = cloud20.updateUser(identityAdminToken, userAdmin.id, userAdmin)
        def getUserResponse = cloud20.getUser(serviceAdminToken, createUserForCreate.location)
        User user = getUserResponse.getEntity(User)
        cloud20.destroyUser(serviceAdminToken, user.id)
        cloud20.destroyUser(serviceAdminToken, userAdmin.id)

        then:
        updateUserResponse.status == 200
        user.enabled == false
    }

    def "Disable one of two user admins in domain does not disable subUsers"() {
        given:
        def domain = "someDomain$sharedRandom"
        def username = "user$sharedRandom"
        def password = "Password1"

        def adminUsername1 = "userAdmin3$sharedRandom"
        def userAdminForCreate1 = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(adminUsername1, "displayName", "someEmail@rackspace.com", true, "ORD", domain, password))
        User userAdmin1 = userAdminForCreate1.getEntity(User)

        def adminUsername2 = "userAdmin4$sharedRandom"
        def userAdminForCreate2 = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(adminUsername2, "displayName", "someEmail@rackspace.com", true, "ORD", domain, password))
        User userAdmin2 = userAdminForCreate2.getEntity(User)

        def userAdminToken = cloud20.authenticatePassword(adminUsername1, password).getEntity(AuthenticateResponse).value.token.id
        def subUserForCreate = cloud20.createUser(userAdminToken, v2Factory.createUserForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", domain, "Password1"))

        when:
        userAdmin.enabled = false;
        def updateUserResponse = cloud20.updateUser(identityAdminToken, userAdmin1.id, userAdmin1)
        def getUserResponse = cloud20.getUser(serviceAdminToken, subUserForCreate.location)
        User user = getUserResponse.getEntity(User)
        cloud20.destroyUser(serviceAdminToken, user.id)
        cloud20.destroyUser(serviceAdminToken, userAdmin1.id)
        cloud20.destroyUser(serviceAdminToken, userAdmin2.id)

        then:
        updateUserResponse.status == 200
        user.enabled == true
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
        def userAdmin = userAdminForCreate.getEntity(User)
        def authRequest = cloud20.authenticatePassword(username, "Password1")
        String token = authRequest.getEntity(AuthenticateResponse).value.token.id
        def defaultUserForCreate = cloud20.createUser(token, v2Factory.createUserForCreate(subUsername, "displayName", "email@rackspace.com", true, "ORD", null, "Password1"))
        def defaultUser = defaultUserForCreate.getEntity(User)
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
        def hardDeleteRespones = cloud20.hardDeleteUser(serviceAdminToken, defaultUser.id)
        def hardDeleteAdminRespones = cloud20.hardDeleteUser(serviceAdminToken, userAdmin.id)
        def deleteGroupResponse = cloud20.deleteGroup(serviceAdminToken, group.value.id)
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
        hardDeleteRespones.status == 204
        hardDeleteAdminRespones.status == 204
        deleteGroupResponse.status == 204
        deleteDomainResponse.status == 204
    }

    def "we can create a role when specifying weight and propagate values"() {
        when:
        def role = v2Factory.createRole(propagate, weight).with {
            it.name = "role$sharedRandom"
            return it
        }
        def response = cloud20.createRole(serviceAdminToken, role)
        def createdRole = response.getEntity(Role).value
        cloud20.deleteRole(serviceAdminToken, createdRole.getId())

        def propagateValue = null
        def weightValue = null

        if (createdRole.otherAttributes.containsKey(QNAME_PROPAGATE)) {
            propagateValue = createdRole.otherAttributes.get(QNAME_PROPAGATE).toBoolean()
        }
        if (createdRole.otherAttributes.containsKey(QNAME_WEIGHT)) {
            weightValue = createdRole.otherAttributes.get(QNAME_WEIGHT).toInteger()
        }

        then:
        propagateValue == expectedPropagate
        weightValue == expectedWeight

        where:
        weight  | propagate | expectedWeight | expectedPropagate
        null    | null      | 1000           | false
        100     | null      | 100            | false
        null    | true      | 1000           | true
        null    | false     | 1000           | false
        2000    | true      | 2000           | true
    }

    def "when specifying an invalid weight we receive a bad request"() {
        when:
        def role = v2Factory.createRole(null, weight)
        def response = cloud20.createRole(token, role)
        if (response.status == 201) {
            cloud20.deleteRole(serviceAdminToken, response.getEntity(Role).value.getId())
        }

        then:
        response.status == 400

        where:
        token              | weight
        identityAdminToken | 3
        serviceAdminToken  | 50
        serviceAdminToken  | 1234
    }

    def "we cannot specify a role weight which we cannot manage"() {
        when:
        def role = v2Factory.createRole(null, weight)
        def response = cloud20.createRole(token, role)
        if (response.status == 201) {
            cloud20.deleteRole(serviceAdminToken, response.getEntity(Role).value.getId())
        }

        then:
        response.status == 403

        where:
        token              | weight
        identityAdminToken | 0
    }

    def "adding a role which propagates to a user admin adds the role to the sub users"() {
        when:
        def defaultUserResponse1 = cloud20.getUserApplicationRole(serviceAdminToken, propagatingRole.getId(), defaultUserForAdminTwo.getId())
        def userAdminResponse1 = cloud20.getUserApplicationRole(serviceAdminToken, propagatingRole.getId(), userAdminTwo.getId())

        def response = cloud20.addApplicationRoleToUser(serviceAdminToken, propagatingRole.getId(), userAdminTwo.getId())

        def defaultUserResponse2 = cloud20.getUserApplicationRole(serviceAdminToken, propagatingRole.getId(), defaultUserForAdminTwo.getId())
        def userAdminResponse2 = cloud20.getUserApplicationRole(serviceAdminToken, propagatingRole.getId(), userAdminTwo.getId())

        then:
        response.status == 200
        defaultUserResponse1.status == 404
        userAdminResponse1.status == 404
        defaultUserResponse2.status == 200
        userAdminResponse2.status == 200
    }

    def "removing a propagating role from a user-admin removes the role from the sub users"() {
        given:
        cloud20.addApplicationRoleToUser(serviceAdminToken, propagatingRole.getId(), userAdminTwo.getId())

        when:
        def defaultUserResponse1 = cloud20.getUserApplicationRole(serviceAdminToken, propagatingRole.getId(), defaultUserForAdminTwo.getId())
        def userAdminResponse1 = cloud20.getUserApplicationRole(serviceAdminToken, propagatingRole.getId(), userAdminTwo.getId())

        def response = cloud20.deleteApplicationRoleFromUser(serviceAdminToken, propagatingRole.getId(), userAdminTwo.getId())

        def defaultUserResponse2 = cloud20.getUserApplicationRole(serviceAdminToken, propagatingRole.getId(), defaultUserForAdminTwo.getId())
        def userAdminResponse2 = cloud20.getUserApplicationRole(serviceAdminToken, propagatingRole.getId(), userAdminTwo.getId())

        then:
        defaultUserResponse1.status == 200
        userAdminResponse1.status == 200
        response.status == 204
        defaultUserResponse2.status == 404
        userAdminResponse2.status == 404
    }

    def "authenticate returns password authentication type in response"() {
        when:
        def response = cloud20.authenticatePassword("admin$sharedRandom", "Password1")
        def tokenAny = response.getEntity(AuthenticateResponse).value.token.any
        def attributes = []
        for (attr in tokenAny) {
            attributes.add(StringUtils.lowerCase(attr.name))
        }

        then:
        response.status == 200
        attributes.contains("rax-auth:authenticatedby")
    }

    @Ignore
    def "authenticate returns apiKey authentication type in response"() {
        when:
        def response = cloud20.getUserApiKey(serviceAdminToken, userAdmin.getId()).getEntity(com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials).value
        def authResp = cloud20.authenticateApiKey(userAdmin.name, response.apiKey)

        then:
        authResp.getEntity(AuthenticateResponse).getToken().getAny().contains("rax-auth:authenticatedBy")
    }

    def "validate returns password authentication type in response"() {
        when:
        def response = cloud20.validateToken(serviceAdminToken, userAdminToken)
        def blah = response.getEntity(AuthenticateResponse).value
        def tokenAny = blah.token.any
        def attributes = []
        for (attr in tokenAny) {
            attributes.add(StringUtils.lowerCase(attr.name))
        }

        then:
        response.status == 200
        attributes.contains("rax-auth:authenticatedby")
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

    def "Service admin should be the only one to add Identity roles"() {
        when:
        String roleName = "identity:someRole" + sharedRandom
        def result = cloud20.createRole((String)token, v2Factory.createRole(roleName, serviceId))
        if (result.status == 201){
            cloud20.deleteRole(serviceAdminToken, result.getEntity(Role).value.id)
        }

        then:
        result.status == expectedResult

        where:
        token                | serviceId                                  | expectedResult
        identityAdminToken   | "bde1268ebabeeabb70a0e702a4626977c331d5c4" | 403
        userAdminToken       | "bde1268ebabeeabb70a0e702a4626977c331d5c4" | 403
        defaultUserToken     | "bde1268ebabeeabb70a0e702a4626977c331d5c4" | 403
        serviceAdminToken    | "bde1268ebabeeabb70a0e702a4626977c331d5c4" | 201

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
        def user = cloud20.createUser(serviceAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", null, password)).getEntity(User)
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
        def user = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "impersonationUserDomain$random", password)).getEntity(User)
        DateTime date = new DateTime().plusHours(12)
        def impersonatedToken = cloud20.impersonate(identityAdminToken, user, 43200).getEntity(ImpersonationResponse.class)
        cloud20.destroyUser(serviceAdminToken, user.getId())

        then:
        impersonatedToken.token != null
        def expireTime = impersonatedToken.token.expires.toGregorianCalendar().getTime()
        def diff = Math.abs(Seconds.secondsBetween(new DateTime(date.toDate()), new DateTime(expireTime)).seconds)
        diff < 2

    }

    def "Impersonation call with a expiration time not provided does apply entropy" () {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "impersionationUser$random"

        when:
        def user = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "impersonationUserDomain$random", password)).getEntity(User)
        DateTime date = new DateTime().plusHours(3)
        def impersonatedToken = cloud20.impersonate(identityAdminToken, user, null).getEntity(ImpersonationResponse.class)
        cloud20.destroyUser(serviceAdminToken, user.getId())

        then:
        impersonatedToken.token != null
        def expireTime = impersonatedToken.token.expires.toGregorianCalendar().getTime()
        def diff = Math.abs(Seconds.secondsBetween(new DateTime(date.toDate()), new DateTime(expireTime)).seconds)
        diff < 108 //1 % of 3 hours

    }

    def "Create tenant with negative tenantId" () {
        given:
        def password = "Password1"
        def random = UUID.randomUUID().toString().replace("-", "")
        def username = "negativeTenantUser$random"
        def tenant = v2Factory.createTenant("-754612", "-754612")
        def role = v2Factory.createRole("roleName$random", "a45b14e394a57e3fd4e45d59ff3693ead204998b")
        def endpointTemplate = v1Factory.createEndpointTemplate("1658468", "compute", "http://bananas.com")

        when:
        def createUser = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate(username, username, "email@email.email", true, "DFW", "negtenDomain$random", password)).getEntity(User)
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

    def deleteUsersTokens(String uid) {
        def result = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=scopeAccess)(uid=$uid))")
        for (SearchResultEntry entry in result.getSearchEntries()) {
            connPools.getAppConnPool().delete(entry.getDN())
        }

    }

    def expireToken(String uid, String accessToken, int hoursOffset) {
        def resultCloudAuthScopeAccess = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=scopeAccess)(uid=$USER_FOR_AUTH)(clientId=$CLOUD_CLIENT_ID)(accessToken=$accessToken))","*")
        for (SearchResultEntry entry in resultCloudAuthScopeAccess.getSearchEntries()) {
            def entity = LDAPPersister.getInstance(ScopeAccess.class)decode(entry)
            if (!entity.isAccessTokenExpired(new DateTime())) {
                entity.accessTokenExp = new DateTime().minusHours(hoursOffset).toDate()
                List<Modification> mods = LDAPPersister.getInstance(ScopeAccess.class).getModifications(entity, true)
                connPools.getAppConnPool().modify(entity.getUniqueId(), mods)
            }
        }
    }

    def expireTokens(String uid, int hoursOffset) {
        def resultCloudAuthScopeAccess = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=scopeAccess)(uid=$USER_FOR_AUTH))","*")
        for (SearchResultEntry entry in resultCloudAuthScopeAccess.getSearchEntries()) {
            def entity = LDAPPersister.getInstance(ScopeAccess.class)decode(entry)
            if (!entity.isAccessTokenExpired(new DateTime())) {
                entity.accessTokenExp = new DateTime().minusHours(hoursOffset).toDate()
                List<Modification> mods = LDAPPersister.getInstance(ScopeAccess.class).getModifications(entity, true)
                connPools.getAppConnPool().modify(entity.getUniqueId(), mods)
            }
        }
    }

    def setTokenValid(String uid) {
        def resultCloudAuthScopeAccess = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=scopeAccess)(uid=$USER_FOR_AUTH)(clientId=$CLOUD_CLIENT_ID))","*")
        for (SearchResultEntry entry in resultCloudAuthScopeAccess.getSearchEntries()) {
            def entity = LDAPPersister.getInstance(ScopeAccess.class)decode(entry)
            if (!entity.isAccessTokenExpired(new DateTime())) {
                entity.accessTokenExp = new DateTime().plusHours(24).toDate()
                List<Modification> mods = LDAPPersister.getInstance(ScopeAccess.class).getModifications(entity, true)
                connPools.getAppConnPool().modify(entity.getUniqueId(), mods)
            }
        }
    }

    def setTokenInRefreshWindow(String uid, String accessToken) {
        def resultCloudAuthScopeAccess = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=scopeAccess)(uid=$USER_FOR_AUTH)(accessToken=$accessToken))","*")
        for (SearchResultEntry entry in resultCloudAuthScopeAccess.getSearchEntries()) {
            def entity = LDAPPersister.getInstance(ScopeAccess.class)decode(entry)
            if (!entity.isAccessTokenWithinRefreshWindow(config.getInt("token.refreshWindowHours"))) {
                entity.accessTokenExp = new DateTime().plusHours(REFRESH_WINDOW_HOURS).minusHours(2).toDate()
                List<Modification> mods = LDAPPersister.getInstance(ScopeAccess.class).getModifications(entity, true)
                connPools.getAppConnPool().modify(entity.getUniqueId(), mods)
            }
        }
    }

    def setTokensInRefreshWindow(String uid) {
        def resultCloudAuthScopeAccess = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=scopeAccess)(uid=$USER_FOR_AUTH)(clientId=$CLOUD_CLIENT_ID))","*")
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
