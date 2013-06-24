package com.rackspace.idm.api.resource.cloud.v20;


import com.rackspace.docs.identity.api.ext.rax_auth.v1.Question
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Questions
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Region
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.core.util.MultivaluedMapImpl
import org.apache.commons.lang.StringUtils
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.V2Factory

import javax.xml.namespace.QName

import org.openstack.docs.identity.api.v2.*

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static javax.ws.rs.core.MediaType.APPLICATION_XML
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
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
import com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA
import com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQAs
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest

import static com.rackspace.idm.RaxAuthConstants.*

@ContextConfiguration(locations = "classpath:app-config.xml")
class Cloud20IntegrationTest extends Specification {
    @Autowired LdapConnectionPools connPools
    @Autowired Configuration config
    @Autowired DefaultCloud20Service cloud20Service

    @Shared WebResource resource
    @Shared JAXBObjectFactories objFactories;
    @Shared V2Factory v2Factory

    @Shared def path20 = "cloud/v2.0/"
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
    static def X_AUTH_TOKEN = "X-Auth-Token"
    @Shared def groupLocation
    @Shared def group
    @Shared Region sharedRegion

    static def USER_MANAGE_ROLE_ID = "7"
    static def RAX_GRPADM= "RAX-GRPADM"
    static def RAX_AUTH = "RAX-AUTH"
    static def OS_KSCATALOG = "OS-KSCATALOG"
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

    @Shared def testIdentityRoleId = "testIdentityRoleForDelete"
    @Shared def testIdentityRole
    @Shared def cloudAuthClientId

    def setupSpec() {
        sharedRandom = ("$sharedRandomness").replace('-',"")
        testDomainId = "domain1$sharedRandom"
        testDomainId2 = "domain2$sharedRandom"
        emptyDomainId = "domain3$sharedRandom"

        this.resource = ensureGrizzlyStarted("classpath:app-config.xml");
        this.objFactories = new JAXBObjectFactories()
        this.v2Factory = new V2Factory()
        serviceAdminToken = authenticatePasswordXML("authQE", "Auth1234").getEntity(AuthenticateResponse).value.token.id
        serviceAdmin = getUserByNameXML(serviceAdminToken, "authQE").getEntity(User)

        identityAdmin = getUserByNameXML(serviceAdminToken, "auth").getEntity(User)
        identityAdminToken = authenticatePasswordXML("auth", "auth123").getEntity(AuthenticateResponse).value.token.id

        createUserXML(serviceAdminToken, userForCreate("admin$sharedRandom", "display", "email@email.com", true, null, null, "Password1"))
        testUser = getUserByNameXML(serviceAdminToken, "admin$sharedRandom").getEntity(User)
        USER_FOR_AUTH = testUser.username
        USER_FOR_AUTH_PWD = "Password1"

        endpointTemplateId = "100001"
        addEndpointTemplateXML(serviceAdminToken, endpointTemplate(endpointTemplateId))
        def addPolicyResponse = addPolicyXML(serviceAdminToken, policy("name"))
        def getPolicyResponse = getPolicyXML(serviceAdminToken, addPolicyResponse.location)
        policyId = getPolicyResponse.getEntity(Policy).id as String


        defaultRegion = region("ORD", true, true)
        createRegionXML(serviceAdminToken, defaultRegion)
        updateRegionXML(serviceAdminToken, defaultRegion.name, defaultRegion)

        //User Admin
        def createResponse = createUserXML(identityAdminToken, userForCreate("userAdmin1$sharedRandom", "display", "test@rackspace.com", true, null, testDomainId, "Password1"))
        userAdmin = getUserByNameXML(identityAdminToken, "userAdmin1$sharedRandom").getEntity(User)
        createUserXML(identityAdminToken, userForCreate("userAdmin2$sharedRandom", "display", "test@rackspace.com", true, null, emptyDomainId, "Password1"))
        userAdminTwo = getUserByNameXML(identityAdminToken, "userAdmin2$sharedRandom").getEntity(User)

        userAdminToken = authenticatePasswordXML("userAdmin1$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id
        userAdminTwoToken = authenticatePasswordXML("userAdmin2$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id

        // Default Users
        createUserXML(userAdminToken, userForCreate("defaultUser1$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUser = getUserByNameXML(userAdminToken, "defaultUser1$sharedRandom").getEntity(User)
        createUserXML(userAdminToken, userForCreate("defaultUser2$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserTwo = getUserByNameXML(userAdminToken, "defaultUser2$sharedRandom").getEntity(User)
        createUserXML(userAdminToken, userForCreate("defaultUser3$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserThree = getUserByNameXML(userAdminToken, "defaultUser3$sharedRandom").getEntity(User)
        createUserXML(userAdminTwoToken, userForCreate("defaultUser4$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserForAdminTwo = getUserByNameXML(userAdminTwoToken, "defaultUser4$sharedRandom").getEntity(User)

        createUserXML(userAdminToken, userForCreate("defaultUserWithManageRole$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserWithManageRole = getUserByNameXML(userAdminToken, "defaultUserWithManageRole$sharedRandom").getEntity(User)
        defaultUserManageRoleToken = authenticatePasswordXML("defaultUserWithManageRole$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id

        createUserXML(userAdminToken, userForCreate("defaultUserWithManageRole2$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserWithManageRole2 = getUserByNameXML(userAdminToken, "defaultUserWithManageRole2$sharedRandom").getEntity(User)

        defaultUserToken = authenticatePasswordXML("defaultUser1$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id

        //create group
        def createGroupResponse = createGroupXML(serviceAdminToken, group("group$sharedRandom", "this is a group"))
        groupLocation = createGroupResponse.location
        def getGroupResponse = getGroupXML(serviceAdminToken, groupLocation)
        group = getGroupResponse.getEntity(Group).value

        def createRegionResponse = createRegionXML(serviceAdminToken, region("region$sharedRandom", true, false))
        def getRegionResponse = getRegionXML(serviceAdminToken, "region$sharedRandom")
        sharedRegion = getRegionResponse.getEntity(Region)

        //create role
        if (sharedRole == null) {
            def response = createRoleXML(serviceAdminToken, role())
            sharedRole = response.getEntity(Role).value
        }
        if (sharedRoleTwo == null) {
            def response = createRoleXML(serviceAdminToken, role())
            sharedRoleTwo = response.getEntity(Role).value
        }
        if (propagatingRole == null) {
            def role = v2Factory.createRole(true, 500).with {
                it.name = "propagatingRole$sharedRandom"
                return it
            }
            def response = createRoleXML(serviceAdminToken, role)
            propagatingRole = response.getEntity(Role).value
        }

        if (tenant == null) {
            def tenantForCreate = createTenant()
            def tenantResponse = addTenantXML(serviceAdminToken, tenantForCreate)
            tenant = tenantResponse.getEntity(Tenant).value
        }

        //Add role to identity-admin and default-users
        addApplicationRoleToUserXML(serviceAdminToken, sharedRole.getId(), identityAdmin.getId())
        addApplicationRoleToUserXML(serviceAdminToken, sharedRole.getId(), defaultUser.getId())
        addApplicationRoleToUserXML(serviceAdminToken, sharedRole.getId(), defaultUserTwo.getId())
        addApplicationRoleToUserXML(serviceAdminToken, sharedRole.getId(), defaultUserThree.getId())

        //testIdentityRole = getRoleXML(serviceAdminToken, testIdentityRoleId).getEntity(Role).value
    }

    def setup() {
        expireTokens(USER_FOR_AUTH, 12)
        setConfigValues()
        cloudAuthClientId = config.getString("cloudAuth.clientId")
    }

    def cleanupSpec() {
        deleteGroupXML(serviceAdminToken, group.getId())
        deleteRegionXML(serviceAdminToken, sharedRegion.getName())

        deleteRoleXML(serviceAdminToken, sharedRole.getId())
        deleteRoleXML(serviceAdminToken, sharedRoleTwo.getId())
        deleteRoleXML(serviceAdminToken, propagatingRole.getId())

        destroyUser(userAdmin.getId())
        destroyUser(userAdminTwo.getId())

        destroyUser(defaultUser.getId())
        destroyUser(defaultUserTwo.getId())
        destroyUser(defaultUserThree.getId())
        destroyUser(defaultUserForAdminTwo.getId())
        destroyUser(defaultUserWithManageRole.getId())
        destroyUser(defaultUserWithManageRole2.getId())

        destroyUser(testUser.getId())

        //TODO: DELETE DOMAINS
        deleteEndpointTemplateXML(serviceAdminToken, endpointTemplateId)
        deletePolicyXML(serviceAdminToken, policyId)
    }

    def "authenticating where total access tokens remains unchanged"() {
        when:
        def scopeAccessOne = authenticatePasswordXML(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        def scopeAccessTwo = authenticatePasswordXML(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        def allUsersScopeAccessAfter = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=UserScopeAccess)(uid=$USER_FOR_AUTH))", "*")

        then:
        scopeAccessOne.token.id.equals(scopeAccessTwo.token.id)
        allUsersScopeAccessAfter.entryCount <= 2
    }

    def "authenticating where token is within refresh window adds new token"() {
        when:
        def scopeAccessOne = authenticatePasswordXML(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        setTokenInRefreshWindow(USER_FOR_AUTH, scopeAccessOne.token.id)

        def allUsersScopeAccessBefore = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=UserScopeAccess)(uid=$USER_FOR_AUTH))", "*")

        def scopeAccessTwo = authenticatePasswordXML(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        def allUsersScopeAccessAfter = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=UserScopeAccess)(uid=$USER_FOR_AUTH))", "*")

        then:
        allUsersScopeAccessBefore.getEntryCount() + 1 == allUsersScopeAccessAfter.getEntryCount()
        !scopeAccessOne.token.id.equals(scopeAccessTwo.token.id)

    }

    def "authenticating where token is valid returns existing token"() {
        when:
        def scopeAccessOne = authenticatePasswordXML(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        def scopeAccessTwo = authenticatePasswordXML(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        then:
        scopeAccessOne.token.id.equals(scopeAccessTwo.token.id)
    }

    def "authenticate with two valid tokens"() {
        when:
        def firstScopeAccess = authenticatePasswordXML(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        setTokenInRefreshWindow(USER_FOR_AUTH, firstScopeAccess.token.id)
        def secondScopeAccess = authenticatePasswordXML(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        def thirdScopeAccess = authenticatePasswordXML(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        then:
        secondScopeAccess.token.id.equals(thirdScopeAccess.token.id)
    }

    def "authenticating token in refresh window with 2 existing tokens deletes existing expired token"() {
        when:
        def firstScopeAccess = authenticatePasswordXML(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        setTokenInRefreshWindow(USER_FOR_AUTH, firstScopeAccess.token.id)
        def secondScopeAccess = authenticatePasswordXML(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        expireToken(USER_FOR_AUTH, firstScopeAccess.token.id, 12)
        setTokenInRefreshWindow(USER_FOR_AUTH, secondScopeAccess.token.id)
        def thirdScopeAccess = authenticatePasswordXML(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

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
        def user = userForCreate("bob" + random, "displayName", "test@rackspace.com", true, "ORD", null, "Password1")
        def response = createUserXML(serviceAdminToken, user)
        //Get user
        def getUserResponse = getUserXML(serviceAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User)
        //Get user by id
        def getUserByIdResponse = getUserByIdXML(serviceAdminToken, userEntity.getId())
        def getUserByNameResponse = getUserByNameXML(serviceAdminToken, userEntity.getUsername())
        def getUserByEmailResponse = getUsersByEmailXML(serviceAdminToken, userEntity.getEmail())
        //Update User
        def userForUpdate = userForUpdate(null, "updatedBob" + random, "Bob", "test@rackspace.com", false, null, null)
        def updateUserResponse = updateUserXML(serviceAdminToken, userEntity.getId(), userForUpdate)
        //Delete user
        def deleteResponses = deleteUserXML(serviceAdminToken, userEntity.getId())
        //Hard delete user
        def hardDeleteResponses = hardDeleteUserXML(serviceAdminToken, userEntity.getId())

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

    def 'User-managed role CRUD'() {
        when:
        //Create user
        def random = ("$randomness").replace('-', "")
        def user = userForCreate("bob" + random, "displayName", "test@rackspace.com", true, "ORD", null, "Password1")
        def response = createUserXML(defaultUserManageRoleToken, user)
        //Get user
        def getUserResponse = getUserXML(defaultUserManageRoleToken, response.location)
        def userEntity = getUserResponse.getEntity(User)
        //Update User
        def userForUpdate = userForUpdate(null, "updatedBob" + random, "Bob", "test@rackspace.com", false, null, null)
        def updateUserResponse = updateUserXML(defaultUserManageRoleToken, userEntity.getId(), userForUpdate)
        //Delete user
        def deleteResponses = deleteUserXML(defaultUserManageRoleToken, userEntity.getId())
        //Delte user as service admin
        def deleteResponsesServiceAdmin = deleteUserXML(serviceAdminToken, userEntity.getId())
        //Hard delete user
        def hardDeleteResponses = hardDeleteUserXML(serviceAdminToken, userEntity.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        updateUserResponse.status == 200
        deleteResponses.status == 403
        deleteResponsesServiceAdmin.status == 403
        hardDeleteResponses.status == 204
    }

    def "a user can be retrieved by email"() {
        when:
        def createUser = userForCreate("user1$sharedRandom", "user1$sharedRandom", email, true, "ORD", null, "Password1")
        def createdUser = createUserXML(serviceAdminToken, createUser).getEntity(User)
        destroyUser(createdUser.getId())
        def response = getUsersByEmailXML(serviceAdminToken, email)

        then:
        response.status == expected

        where:
        expected | email
        200      | "user1$sharedRandom@email.com"
        200      | "$sharedRandom@random.com"
    }

    def "a list of users can be retrieved by email"() {
        when:
        def user1 = userForCreate("user1$sharedRandom", "user1$sharedRandom", email, true, "ORD", null, "Password1")
        def user2 = userForCreate("user2$sharedRandom", "user2$sharedRandom", email, true, "ORD", null, "Password1")
        def createdUser1 = createUserXML(serviceAdminToken, user1).getEntity(User)
        def createUser2 = createUserXML(serviceAdminToken, user2).getEntity(User)
        destroyUser(createdUser1.getId())
        destroyUser(createUser2.getId())
        def response = getUsersByEmailXML(serviceAdminToken, email)

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
                getUserByIdXML(serviceAdminToken, "badId"),
                getUserByNameXML(serviceAdminToken, "badName"),
                updateUserXML(serviceAdminToken, "badId", new User()),
                deleteUserXML(serviceAdminToken, "badId"),
                addCredentialXML(serviceAdminToken, "badId", getPasswordCredentials("someUser", "SomePassword1"))
        ]
    }

    def "invalid operations on create/update user returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                createUserXML(serviceAdminToken, userForCreate("!@#What", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUserXML(serviceAdminToken, userForCreate("What!@#", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                //createUserXML(serviceAdminToken, userForCreate("1one", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUserXML(serviceAdminToken, userForCreate("one name", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUserXML(serviceAdminToken, userForCreate("", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUserXML(serviceAdminToken, userForCreate(null, "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUserXML(serviceAdminToken, userForCreate("a$sharedRandom", "display", "junk!@#", true, "ORD", null, "Password1")),
                createUserXML(serviceAdminToken, userForCreate("z$sharedRandom", "display", "   ", true, "ORD", null, "Password1")),
                //createUserXML(serviceAdminToken, userForCreate("b$sharedRandom", "display", null, true, "ORD", null, "Password1")),
                createUserXML(serviceAdminToken, userForCreate("c$sharedRandom", "display", "test@rackspace.com", true, "ORD", null, "Pop1")),
                createUserXML(serviceAdminToken, userForCreate("d$sharedRandom", "display", "test@rackspace.com", true, "ORD", null, "longpassword1")),
                createUserXML(serviceAdminToken, userForCreate("e$sharedRandom", "display", "test@rackspace.com", true, "ORD", null, "Longpassword")),
                createUserXML(serviceAdminToken, userForCreate("f$sharedRandom", "displ:ay", "test@rackspace.com", true, "ORD", "someId", "Longpassword")),
                createUserXML(identityAdminToken, userForCreate("g$sharedRandom", "display", "test@rackspace.com", true, "ORD", null, "Longpassword1")),
                //updateUserXML(userAdminToken, defaultUser.getId(), userForUpdate("1", "someOtherName", "someOtherDisplay", "some@rackspace.com", true, "ORD", "SomeOtherPassword1")),
                updateUserXML(defaultUserToken, defaultUser.getId(), userForUpdate(null, "someOtherName", "someOtherDisplay", "some@rackspace.com", false, "ORD", "SomeOtherPassword1")),
                updateUserXML(identityAdminToken, defaultUser.getId(), userForUpdate(null, null, null, null, true, "HAHAHAHA", "Password1"))
        ]
    }

    def 'operations with invalid tokens'() {
        expect:
        response.status == 401

        where:
        response << [
                createUserXML("invalidToken", userForCreate("someName", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUserXML(null, userForCreate("someName", "display", "test@rackspace.com", true, "ORD", null, "Password1")),  \
                getUserByIdXML("invalidToken", "badId"),
                getUserByIdXML(null, "badId"),
                getUserByNameXML("invalidToken", "badId"),
                getUserByNameXML(null, "badId"),
                updateUserXML("invalidToken", "badId", new User()),
                updateUserXML(null, "badId", new User()),
                deleteUserXML("invalidToken", "badId"),
                deleteUserXML(null, "badId"),
                listUsersXML("invalidToken"),
                listUsersXML(null)
        ]

    }

    def 'forbidden operations for users'() {
        expect:
        response.status == 403

        where:
        response << [
                createUserXML(defaultUserToken, userForCreate("someName", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                updateUserXML(defaultUserToken, userAdmin.getId(), userForUpdate(null, "someOtherName", "someOtherDisplay", "some@rackspace.com", true, "ORD", "SomeOtherPassword1")),
                getUserByIdXML(defaultUserToken, userAdmin.getId()),
                getUserByIdXML(defaultUserToken, identityAdmin.getId()),
                getUserByIdXML(defaultUserToken, serviceAdmin.getId()),
                getUserByIdXML(userAdminToken, identityAdmin.getId()),
                getUserByIdXML(userAdminToken, serviceAdmin.getId()),
                getUserByNameXML(defaultUserToken, userAdmin.getUsername()),
                getUserByNameXML(defaultUserToken, identityAdmin.getUsername()),
                getUserByNameXML(defaultUserToken, serviceAdmin.getUsername()),
                getUserByNameXML(userAdminToken, identityAdmin.getUsername()),
                getUserByNameXML(userAdminToken, serviceAdmin.getUsername()),
                createGroupXML(defaultUserToken, group()),
                updateGroupXML(defaultUserToken, group.getId(), group()),
                deleteGroupXML(defaultUserToken, group.getId()),
                getGroupXML(defaultUserToken, groupLocation),
                getGroupsXML(defaultUserToken),
                createRegionXML(defaultUserToken, region()),
                updateRegionXML(defaultUserToken, sharedRegion.getName(), sharedRegion),
                deleteRegionXML(defaultUserToken, sharedRegion.getName()),
                getRegionXML(defaultUserToken, sharedRegion.getName()),
                createQuestionXML(defaultUserToken, question()),
                updateQuestionXML(defaultUserToken, "id", question()),
                deleteQuestionXML(defaultUserToken, "id"),
        ]
    }

    def 'valid operations on retrieving users'() {
        expect:
        response.status == 200

        where:
        response << [
                listUsersXML(serviceAdminToken),
                listUsersXML(identityAdminToken),
                listUsersXML(userAdminToken),
                listUsersXML(defaultUserToken),
                getUserByIdXML(defaultUserToken, defaultUser.getId()),
                getUserByIdXML(userAdminToken, defaultUser.getId()),
                getUserByIdXML(userAdminToken, userAdmin.getId()),
                getUserByIdXML(identityAdminToken, userAdmin.getId()),
                getUserByIdXML(serviceAdminToken, userAdmin.getId()),
                getUserByNameXML(defaultUserToken, defaultUser.getUsername()),
                getUserByNameXML(userAdminToken, defaultUser.getUsername()),
                getUserByNameXML(userAdminToken, userAdmin.getUsername()),
                getUserByNameXML(identityAdminToken, userAdmin.getUsername()),
                getUserByNameXML(serviceAdminToken, userAdmin.getUsername())
        ]

    }

    def "Group CRUD"() {
        when:
        def random = ((String) UUID.randomUUID()).replace("-", "")
        def createGroupResponse = createGroupXML(serviceAdminToken, group("group$random", "this is a group"))

        def getGroupResponse = getGroupXML(serviceAdminToken, createGroupResponse.location)
        def groupEntity = getGroupResponse.getEntity(Group)
        def getGroupByNameResponse = getGroupByNameXML(serviceAdminToken, groupEntity.value.name)
        def groupId = groupEntity.value.id

        def getGroupsResponse = getGroupsXML(serviceAdminToken)
        def groupsEntity = getGroupsResponse.getEntity(Groups)

        def updateGroupResponse = updateGroupXML(serviceAdminToken, groupId, group("group$random", "updated group"))

        def deleteGroupResponse = deleteGroupXML(serviceAdminToken, groupId)


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
        def addUserToGroupResponse = addUserToGroupXML(serviceAdminToken, group.getId(), identityAdmin.getId())

        def listGroupsForUserResponse = listGroupsForUserXML(serviceAdminToken, identityAdmin.getId())
        def groups = listGroupsForUserResponse.getEntity(Groups).value

        def getUsersFromGroupResponse = getUsersFromGroupXML(serviceAdminToken, group.getId())
        def users = getUsersFromGroupResponse.getEntity(UserList).value

        def removeUserFromGroupRespone = removeUserFromGroupXML(serviceAdminToken, group.getId(), identityAdmin.getId())

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
        def addUserToGroupResponse = addUserToGroupXML(identityAdminToken, group.getId(), userAdmin.getId())

        def listGroupsForUserResponse = listGroupsForUserXML(identityAdminToken, userAdmin.getId())
        def groups = listGroupsForUserResponse.getEntity(Groups).value

        def getUsersFromGroupResponse = getUsersFromGroupXML(identityAdminToken, group.getId())
        def users = getUsersFromGroupResponse.getEntity(UserList).value

        def removeUserFromGroupRespone = removeUserFromGroupXML(identityAdminToken, group.getId(), userAdmin.getId())

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
        addUserToGroupXML(identityAdminToken, group.getId(), userAdmin.getId())

        def getUsersFromGroupResponse1 = getUsersFromGroupXML(identityAdminToken, group.getId())
        UserList users1 = getUsersFromGroupResponse1.getEntity(UserList).value

        createUserXML(userAdminToken, userForCreate(tempUserAdmin, "display", "test@rackspace.com", true, null, testDomainId, "Password1"))
        def tempUser = getUserByNameXML(userAdminToken, tempUserAdmin).getEntity(User)

        def getUsersFromGroupResponse2 = getUsersFromGroupXML(identityAdminToken, group.getId())
        UserList users2 = getUsersFromGroupResponse2.getEntity(UserList).value

        removeUserFromGroupXML(identityAdminToken, group.getId(), userAdmin.getId())

        deleteUserXML(userAdminToken, tempUser.getId())

        then:
        users1.user.size() + 1 == users2.user.size()
        users2.user.findAll({it.username == tempUserAdmin}).size() == 1
    }

    def "invalid operations on create/update group returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                createGroupXML(serviceAdminToken, group(null, "this is a group")),
                createGroupXML(serviceAdminToken, group("", "this is a group")),
                createGroupXML(serviceAdminToken, group("group", null)),
                updateGroupXML(serviceAdminToken, group.getId(), group(null, "this is a group")),
                updateGroupXML(serviceAdminToken, group.getId(), group("", "this is a group")),
                updateGroupXML(serviceAdminToken, group.getId(), group("group", null)),
                addUserToGroupXML(serviceAdminToken, "doesnotexist", defaultUser.getId()),
                addUserToGroupXML(serviceAdminToken, group.getId(), defaultUser.getId()),
                removeUserFromGroupXML(serviceAdminToken, group.getId(), defaultUser.getId()),
        ]
    }

    def "invalid operations on create/update group returns 'not found'"() {
        expect:
        response.status == 404

        where:
        response << [
                addUserToGroupXML(serviceAdminToken, group.getId(), "doesnotexist"),
        ]
    }

    def "update region name is not allowed"() {
        given:
        Region region1 = region("somename", false, false)

        when:
        def updateRegionResponse = updateRegionXML(serviceAdminToken, sharedRegion.getName(), region1)

        then:
        updateRegionResponse.status == 400
    }

    def "region crud"() {
        given:
        def random = ("$randomness").replace('-', "")
        def regionName = "region${random}"
        Region region1 = region(regionName, false, false)
        Region region2 = region(regionName, true, false)

        when:
        def createRegionResponse = createRegionXML(serviceAdminToken, region1)
        def getRegionResponse = getRegionXML(serviceAdminToken, regionName)
        Region createdRegion = getRegionResponse.getEntity(Region)

        def updateRegionResponse = updateRegionXML(serviceAdminToken, regionName, region2)
        def getUpdatedRegionResponse = getRegionXML(serviceAdminToken, regionName)
        Region updatedRegion = getUpdatedRegionResponse.getEntity(Region)

        def getRegionsResponse = getRegionsXML(serviceAdminToken)
        Regions regions = getRegionsResponse.getEntity(Regions)

        def deleteRegionResponse = deleteRegionXML(serviceAdminToken, regionName)
        def getDeletedRegionResponse = getRegionXML(serviceAdminToken, regionName)


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
                updateRegionXML(serviceAdminToken, "notfound", region()),
                deleteRegionXML(serviceAdminToken, "notfound"),
                getRegionXML(serviceAdminToken, "notfound"),
        ]
    }

    def "invalid operations on create regions returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                createRegionXML(serviceAdminToken, region(null, true, false)),
        ]
    }

    def "create region that already exists returns conflict"() {
        when:
        def createRegionResponse = createRegionXML(serviceAdminToken, sharedRegion)

        then:
        createRegionResponse.status == 409
    }

    def "listUsersWithRole called by default-user returns forbidden"() {
        when:
        def response = listUsersWithRoleXML(defaultUserToken, "1")

        then:
        response.status == 403
    }

    def "listUsersWithRole called by admin invalid roleId returns not found"() {
        when:
        def response = listUsersWithRoleXML(serviceAdminToken, "-5")

        then:
        response.status == 404
    }

    def "listUsersWithRole called by admins returns success"() {
        expect:
        response.status == 200

        where:
        response << [
                listUsersWithRoleXML(identityAdminToken, sharedRole.getId()),
                listUsersWithRoleXML(serviceAdminToken, sharedRole.getId()),
                listUsersWithRoleXML(userAdminToken, sharedRole.getId())
        ]
    }

    def "listUsersWithRole empty list returns"() {
        when:
        def response = listUsersWithRoleXML(userAdminTwoToken, sharedRole.getId())

        then:
        response.status == 200
        response.getEntity(UserList).value.user.size == 0
        response.headers.getFirst("Link") == null
    }

    def "listUsersWithRole non empty list"() {
        when:
        def userAdminResponse = listUsersWithRoleXML(userAdminToken, sharedRole.getId())
        def serviceAdminResponse = listUsersWithRoleXML(serviceAdminToken, sharedRole.getId())
        def identityAdminResponse = listUsersWithRoleXML(identityAdminToken, sharedRole.getId())

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
        def userAdminResponse1 = listUsersWithRoleXML(userAdminToken, sharedRole.getId(), "0", "1")
        def userAdminResponse2 = listUsersWithRoleXML(userAdminToken, sharedRole.getId(), "1", "1")
        def userAdminResponse3 = listUsersWithRoleXML(userAdminToken, sharedRole.getId(), "2", "1")
        def serviceAdminResponse1 = listUsersWithRoleXML(serviceAdminToken, sharedRole.getId(), "1", "2")
        def serviceAdminResponse2 = listUsersWithRoleXML(serviceAdminToken, sharedRole.getId(), "0", "2")
        def serviceAdminResponse3 = listUsersWithRoleXML(serviceAdminToken, sharedRole.getId(), "2", "2")
        def serviceAdminResponse4 = listUsersWithRoleXML(serviceAdminToken, sharedRole.getId(), "3", "4")

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
                listUsersWithRoleXML(serviceAdminToken, sharedRole.getId(), "abC", "5"),
                listUsersWithRoleXML(serviceAdminToken, sharedRole.getId(), "5", "bCg"),
                listUsersWithRoleXML(serviceAdminToken, sharedRole.getId(), "abC", "asdf")
        ]
    }

    def "listUsersWithRole offset greater than result set length returns 200 with empty list"() {
        expect:
        response.status == 200
        response.getEntity(UserList).value.user.size == 0

        where:
        response << [
                listUsersWithRoleXML(serviceAdminToken, sharedRole.getId(), "100", "10"),
                listUsersWithRoleXML(userAdminToken, sharedRole.getId(), "100", "10")
        ]
    }

    def "listUsersWithRole role assigned to no one"() {
        when:
        def responseOne = listUsersWithRoleXML(serviceAdminToken, sharedRoleTwo.getId())
        def responseTwo = listUsersWithRoleXML(serviceAdminToken, sharedRoleTwo.getId(), "0", "10")
        def responseThree = listUsersWithRoleXML(identityAdminToken, sharedRoleTwo.getId())
        def responseFour = listUsersWithRoleXML(identityAdminToken, sharedRoleTwo.getId(), "0", "10")
        def responseFive = listUsersWithRoleXML(userAdminToken, sharedRoleTwo.getId())
        def responseSix = listUsersWithRoleXML(userAdminToken, sharedRoleTwo.getId(), "0", "10")

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
        def question1 = question(null, "question")
        def question2 = question(null, "question changed")

        when:
        def createResponse = createQuestionXML(serviceAdminToken, question1)
        def getCreateResponse = getQuestionFromLocationXML(serviceAdminToken, createResponse.location)
        def createEntity = getCreateResponse.getEntity(Question)
        question2.id = createEntity.id

        def updateResponse = updateQuestionXML(serviceAdminToken, createEntity.id, question2)
        def getUpdateResponse = getQuestionXML(serviceAdminToken, createEntity.id)
        def updateEntity = getUpdateResponse.getEntity(Question)

        def deleteResponse = deleteQuestionXML(serviceAdminToken, createEntity.id)
        def getDeleteResponse = getQuestionXML(serviceAdminToken, createEntity.id)

        def getQuestionResponse = getQuestionsXML(serviceAdminToken)
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
                updateQuestionXML(serviceAdminToken, "notfound", question("notfound", "question")),
                deleteQuestionXML(serviceAdminToken, "notfound"),
                getQuestionXML(serviceAdminToken, "notfound"),
        ]
    }

    def "invalid operations on question returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                updateQuestionXML(serviceAdminToken, "ids", question("dontmatch", "question")),
                updateQuestionXML(serviceAdminToken, "id", question("id", null)),
                createQuestionXML(serviceAdminToken, question("id", null)),
        ]
    }

    def "listUsers returns forbidden (invalid token)"() {
        expect:
        response.status == 401

        where:
        response << [
                listUsersXML(""),
                listUsersXML("1")
        ]
    }

    def "listUsers returns default user"() {
        when:
        def users = listUsersXML(defaultUserToken).getEntity(UserList).value.user

        then:
        users[0].username.equals(defaultUser.username)
    }

    def "listUsers caller is user-admin returns users from domain"() {
        when:
        def users = listUsersXML(userAdminToken).getEntity(UserList).value.user

        then:
        users.size() == 4
    }

    def "listUsers caller is identity-admin or higher returns paged results"() {
        expect:
        response.status == 200
        response.headers.getFirst("Link") != null

        where:
        response << [
                listUsersXML(identityAdminToken),
                listUsersXML(identityAdminToken, "0", "10"),
                listUsersXML(identityAdminToken, "15", "10"),
                listUsersXML(serviceAdminToken),
                listUsersXML(serviceAdminToken, "0", "10"),
                listUsersXML(serviceAdminToken, "15", "10"),
        ]
    }

    def "listUsers throws bad request"() {
        expect:
        response.status == 400

        where:
        response << [
                listUsersXML(serviceAdminToken, "0", "abc"),
                listUsersXML(serviceAdminToken, "abc", "10")
        ]
    }

    def "listUsers returns 200 and empty list when offset exceedes result set size"() {
        expect:
        response.status == 200
        response.getEntity(UserList).value.user.size == 0

        where:
        response << [
                listUsersXML(serviceAdminToken, "100000000", "25"),
                listUsersXML(identityAdminToken, "10000000", "50"),
        ]
    }

    def "create and delete applicationRole with insufficient priveleges"() {
        expect:
        response.status == 403

        where:
        response << [
                addApplicationRoleToUserXML(defaultUserToken, sharedRole.getId(), defaultUserForAdminTwo.getId()),
                addApplicationRoleToUserXML(userAdminToken, sharedRole.getId(), defaultUserForAdminTwo.getId()),
                deleteApplicationRoleFromUserXML(defaultUserToken, sharedRole.getId(), defaultUser.getId()),
                deleteApplicationRoleFromUserXML(userAdminToken, sharedRole.getId(), defaultUserForAdminTwo.getId()),
                //deleteApplicationRoleFromUserXML(userAdminToken, sharedRole.getId(), defaultUser.getId()),
        ]
    }

    def "adding identity:* to user with identity:* role And deleting own identity:* role return forbidden"() {
        expect:
        response.status == 403

        where:
        response << [
                addApplicationRoleToUserXML(serviceAdminToken, "1", defaultUser.getId()),
                addApplicationRoleToUserXML(serviceAdminToken, "1", userAdmin.getId()),
                addApplicationRoleToUserXML(serviceAdminToken, "1", identityAdmin.getId()),
                addApplicationRoleToUserXML(serviceAdminToken, "1", serviceAdmin.getId()),
                deleteApplicationRoleFromUserXML(userAdminToken, "3", userAdmin.getId()),
                deleteApplicationRoleFromUserXML(identityAdminToken, "1", identityAdmin.getId()),
                deleteApplicationRoleFromUserXML(serviceAdminToken, "4", serviceAdmin.getId())
        ]
    }

    def "add Application role to user succeeds"() {
        expect:
        response.status == 200

        where:
        response << [
                addApplicationRoleToUserXML(identityAdminToken, sharedRoleTwo.getId(), userAdmin.getId()),
                addApplicationRoleToUserXML(serviceAdminToken, sharedRoleTwo.getId(), identityAdmin.getId())
        ]
    }

    def "delete Application role from user succeeds"() {
        expect:
        response.status == 204

        where:
        response << [
                deleteApplicationRoleFromUserXML(identityAdminToken, sharedRoleTwo.getId(), userAdmin.getId()),
                deleteApplicationRoleFromUserXML(serviceAdminToken, sharedRoleTwo.getId(), identityAdmin.getId())
        ]
    }

    def "deleting role from user without role returns not found (404)"() {
        given:
        deleteApplicationRoleFromUserXML(identityAdminToken, sharedRoleTwo.getId(), userAdmin.getId())

        when:
        def response = deleteApplicationRoleFromUserXML(identityAdminToken, sharedRoleTwo.getId(), userAdmin.getId())

        then:
        response.status == 404
    }

    def "adding to and deleting roles from user on tenant return 403"() {
        expect:
        response.status == 403

        where:
        response << [
                deleteRoleFromUserOnTenantXML(userAdminToken, tenant.id, defaultUserForAdminTwo.getId(), sharedRole.getId()),
                deleteRoleFromUserOnTenantXML(userAdminToken, tenant.id, defaultUser.getId(), sharedRole.getId()),
                addRoleToUserOnTenantXML(userAdminToken, tenant.id, defaultUserForAdminTwo.getId(), sharedRole.getId()),
                addRoleToUserOnTenantXML(userAdminToken, tenant.id, defaultUser.getId(), sharedRole.getId())
        ]
    }

    def "adding identity:* roles to user on tenant returns 400"() {
        expect:
        response.status == 403

        where:
        response << [
                addRoleToUserOnTenantXML(serviceAdminToken, tenant.id, defaultUser.getId(), defaultUserRoleId),
                addRoleToUserOnTenantXML(serviceAdminToken, tenant.id, defaultUser.getId(), userAdminRoleId),
                addRoleToUserOnTenantXML(serviceAdminToken, tenant.id, defaultUser.getId(), identityAdminRoleId),
                addRoleToUserOnTenantXML(serviceAdminToken, tenant.id, defaultUser.getId(), serviceAdminRoleId)
        ]
    }

    def "adding roles to user on tenant succeeds"() {
        expect:
        response.status == 200

        where:
        response << [
                addRoleToUserOnTenantXML(identityAdminToken, tenant.id, userAdmin.getId(), sharedRoleTwo.id),
                addRoleToUserOnTenantXML(serviceAdminToken, tenant.id, identityAdmin.getId(), sharedRoleTwo.id)
        ]
    }

    def "deleting roles from user on tenant succeeds"() {
        expect:
        response.status == 204

        where:
        response << [
                deleteRoleFromUserOnTenantXML(identityAdminToken, tenant.id, userAdmin.getId(), sharedRoleTwo.id),
                deleteRoleFromUserOnTenantXML(serviceAdminToken, tenant.id, identityAdmin.getId(), sharedRoleTwo.id)
        ]
    }

    def "delete role returns 403"() {
        expect:
        response.status == 403

        where:
        response << [
                deleteRoleXML(userAdminToken, sharedRoleTwo.id),
                deleteRoleXML(identityAdminToken, identityAdminRoleId),
                deleteRoleXML(identityAdminToken, userAdminRoleId),
                deleteRoleXML(serviceAdminToken, identityAdminRoleId),
                deleteRoleXML(serviceAdminToken, userAdminRoleId)
        ]
    }

    def "add policy to endpoint without endpoint without policy returns 404"() {
        when:
        def response = addPolicyToEndpointTemplateXML(identityAdminToken, "111111", "111111")

        then:
        response.status == 404
    }

    def "add policy to endpoint with endpoint without policy returns 404"() {
        when:
        def response = addPolicyToEndpointTemplateXML(identityAdminToken, endpointTemplateId, "111111")

        then:
        response.status == 404
    }

    def "add policy to endpoint with endpoint with policy returns 204"() {
        when:
        def addResponse = addPolicyToEndpointTemplateXML(serviceAdminToken, endpointTemplateId, policyId)
        def getResponse = getPoliciesFromEndpointTemplateXML(serviceAdminToken, endpointTemplateId)
        def policies = getResponse.getEntity(Policies)
        def updateResponse = updatePoliciesForEndpointTemplateXML(serviceAdminToken, endpointTemplateId, policies)
        def deletePolicyResponse = deletePolicyXML(serviceAdminToken, policyId)
        def deleteResponse = deletePolicyToEndpointTemplateXML(serviceAdminToken, endpointTemplateId, policyId)

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
        Policy policy = policy("name")
        policies.policy.add(policy)
        def response = updatePoliciesForEndpointTemplateXML(serviceAdminToken, "111111", policies)

        then:
        response.status == 404
    }

    def "update policy to endpoint with endpoint without policy returns 404"() {
        when:
        Policies policies = new Policies()
        Policy policy = policy("name")
        policies.policy.add(policy)
        def response = updatePoliciesForEndpointTemplateXML(serviceAdminToken, endpointTemplateId, policies)

        then:
        response.status == 404
    }

    def "Create secretQA and get secretQA"() {
        when:
        def response = createSecretQAXML(serviceAdminToken,defaultUser.getId(), secretQA("1","answer"))
        def secretQAResponse = getSecretQAXML(serviceAdminToken, defaultUser.getId()).getEntity(SecretQAs)

        then:
        response.status == 200
        secretQAResponse.secretqa.get(0).answer == "answer"
    }

    def "Create/Get secretQA returns 403"() {
        expect:
        response.status == 403

        where:
        response << [
                createSecretQAXML(defaultUserToken, serviceAdmin.getId(), secretQA("1", "answer")),
                createSecretQAXML(defaultUserToken, identityAdmin.getId(), secretQA("1", "answer")),
                createSecretQAXML(defaultUserToken, userAdmin.getId(), secretQA("1", "answer")),
                createSecretQAXML(userAdminToken, serviceAdmin.getId(), secretQA("1", "answer")),
                createSecretQAXML(userAdminToken, identityAdmin.getId(), secretQA("1", "answer")),
                createSecretQAXML(userAdminToken, userAdminTwo.getId(), secretQA("1", "answer")),
                createSecretQAXML(userAdminToken, defaultUserForAdminTwo.getId(), secretQA("1", "answer")),
                getSecretQAXML(defaultUserToken, serviceAdmin.getId()),
                getSecretQAXML(defaultUserToken, identityAdmin.getId()),
                getSecretQAXML(userAdminToken, serviceAdmin.getId()),
                getSecretQAXML(userAdminToken, identityAdmin.getId()),
                getSecretQAXML(userAdminToken, userAdminTwo.getId()),
                getSecretQAXML(userAdminToken, defaultUserForAdminTwo.getId())

        ]

    }

    def "Create/Get secretQA returns 401"() {
        expect:
        response.status == 401

        where:
        response << [
                createSecretQAXML("", defaultUser.getId(), secretQA("1", "answer")),
                createSecretQAXML("", defaultUser.getId(), secretQA("1", "answer")),
                createSecretQAXML(null, defaultUser.getId(), secretQA("1", "answer")),
                createSecretQAXML(null, defaultUser.getId(), secretQA("1", "answer")),
                getSecretQAXML("", serviceAdmin.getId()),
                getSecretQAXML("", identityAdmin.getId()),
                getSecretQAXML(null, serviceAdmin.getId()),
                getSecretQAXML(null, identityAdmin.getId()),
        ]

    }

    def "Create/Get secretQA returns 400"() {
        expect:
        response.status == 400

        where:
        response << [
                createSecretQAXML(serviceAdminToken, defaultUser.getId(), secretQA(null, "answer")),
                createSecretQAXML(serviceAdminToken, defaultUser.getId(), secretQA("1", null))
        ]

    }

    def "Create/Get secretQA returns 404"() {
        expect:
        response.status == 404

        where:
        response << [
                createSecretQAXML(serviceAdminToken, "badId", secretQA("1", "answer")),
                createSecretQAXML(serviceAdminToken, defaultUser.getId(), secretQA("badId", "answer")),
                getSecretQAXML(serviceAdminToken, "badId")
        ]

    }

    def "Create/Get secretQA returns 200"() {
        expect:
        response.status == 200

        where:
        response << [
                createSecretQAXML(serviceAdminToken, defaultUser.getId(), secretQA("1", "answer")),
                createSecretQAXML(identityAdminToken, defaultUser.getId(), secretQA("1", "answer")),
                createSecretQAXML(userAdminToken, defaultUser.getId(), secretQA("1", "answer")),
                createSecretQAXML(defaultUserToken, defaultUser.getId(), secretQA("1", "answer")),
                getSecretQAXML(serviceAdminToken, defaultUser.getId()),
                getSecretQAXML(identityAdminToken, defaultUser.getId()),
                getSecretQAXML(userAdminToken, defaultUser.getId()),
                getSecretQAXML(defaultUserToken, defaultUser.getId()),
        ]
    }

    def "listRoles returns success"() {
        expect:
        response.status == 200

        where:
        response << [
                listRoles(userAdminToken, cloudAuthClientId, null, null),
                listRoles(identityAdminToken, cloudAuthClientId, null, null),
                listRoles(serviceAdminToken, cloudAuthClientId, null, null),
                listRoles(userAdminToken, null, null, null),
                listRoles(identityAdminToken, null, null, null),
                listRoles(serviceAdminToken, null, null, null)
        ]
    }

    def "listRoles returns valid link headers"() {
        given:
        def response = listRoles(serviceAdminToken, null, "2", "1")
        def queryParams = parseLinks(response.headers.get("link"))

        when:
        def first_response = listRoles(serviceAdminToken, null, queryParams["first"][0], queryParams["first"][1])
        def last_response = listRoles(serviceAdminToken, null, queryParams["last"][0], queryParams["last"][1])
        def prev_response = listRoles(serviceAdminToken, null, queryParams["prev"][0], queryParams["prev"][1])
        def next_response = listRoles(serviceAdminToken, null, queryParams["next"][0], queryParams["next"][1])

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
                listRoles(defaultUserToken, null, null, null),
                listRoles(defaultUserToken, cloudAuthClientId, null, null)
        ]
    }

    def "listRoles returns not authorized"() {
        expect:
        response.status == 401

        where:
        response << [
                listRoles("token", null, null, null),
                listRoles("token", cloudAuthClientId, null, null)
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
        def response = listRoles(serviceAdminToken, null, "0", "3")
        def queryParams = parseLinks(response.headers.get("link"))
        Map<Integer, Integer> responseStatus = new HashMap<Integer, Integer>()

        while (queryParams.containsKey("next")) {
            page++
            if (page > MAX_TRIES) {
                break
            }
            response = listRoles(serviceAdminToken, null, queryParams["next"][0], queryParams["next"][1])
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
        def userForCreate = createUserXML(identityAdminToken, userForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", "someDomain", "Password1"))
        User user = userForCreate.getEntity(User)
        String password = "Password1~!@#\$%^&*_#\$%^% <>?:\"^(%)'"
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername().with {
            it.username = username
            it.password = password
            return it
        }

        when:
        def updateCreds = updateCredentialsXML(identityAdminToken, user.id, creds)
        String updatePassword = updateCreds.getEntity(PasswordCredentialsRequiredUsername).value.password
        def authenticate = authenticatePasswordXML(user.username,updatePassword)

        then:
        updateCreds.status == 200
        authenticate.status == 200
    }

    def "updateCredentials with invalid password should return BadRequestException"() {
        given:
        String username = "userUpdateCred2" + sharedRandom
        def userForCreate = createUserXML(identityAdminToken, userForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", "someDomain", "Password1"))
        User user = userForCreate.getEntity(User)
        String password = "Password1~!@א"
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername().with {
            it.username = username
            it.password = password
            return it
        }

        when:
        def updateCreds = updateCredentialsXML(identityAdminToken, user.id, creds)

        then:
        updateCreds.status == 400
    }

    def "Disabling a user should return 404 when its token gets validated"() {
        given:
        String username = "disabledUser" + sharedRandom
        def userForCreate = createUserXML(identityAdminToken, userForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", "someDomain", "Password1"))
        User user = userForCreate.getEntity(User)

        when:
        def authRequest = authenticatePasswordXML(username, "Password1")
        String token = authRequest.getEntity(AuthenticateResponse).value.token.id
        def userForUpdate = userForUpdate(null, username, null, "test@rackspace.com", false, null, null)
        def updateUserResponse = updateUserXML(serviceAdminToken, user.id, userForUpdate)
        def validateResponse = validateTokenXML(serviceAdminToken, token)
        def deleteResponses = deleteUserXML(serviceAdminToken, user.id)
        def hardDeleteRespones = hardDeleteUserXML(serviceAdminToken, user.id)

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
        def userAdminForCreate = createUserXML(identityAdminToken, userForCreate(adminUsername, "displayName", "someEmail@rackspace.com", true, "ORD", domain, password))
        User userAdmin = userAdminForCreate.getEntity(User)

        def userAdminToken = authenticatePasswordXML(adminUsername, password).getEntity(AuthenticateResponse).value.token.id
        def userForCreate = createUserXML(userAdminToken, userForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", domain, "Password1"))

        when:
        userAdmin.enabled = false;
        def updateUserResponse = updateUserXML(identityAdminToken, userAdmin.id, userAdmin)
        def getUserResponse = getUserXML(serviceAdminToken, userForCreate.location)
        User user = getUserResponse.getEntity(User)
        destroyUser(user.id)
        destroyUser(userAdmin.id)

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
        def userAdminForCreate1 = createUserXML(identityAdminToken, userForCreate(adminUsername1, "displayName", "someEmail@rackspace.com", true, "ORD", domain, password))
        User userAdmin1 = userAdminForCreate1.getEntity(User)

        def adminUsername2 = "userAdmin4$sharedRandom"
        def userAdminForCreate2 = createUserXML(identityAdminToken, userForCreate(adminUsername2, "displayName", "someEmail@rackspace.com", true, "ORD", domain, password))
        User userAdmin2 = userAdminForCreate2.getEntity(User)

        def userAdminToken = authenticatePasswordXML(adminUsername1, password).getEntity(AuthenticateResponse).value.token.id
        def userForCreate = createUserXML(userAdminToken, userForCreate(username, "displayName", "someEmail@rackspace.com", true, "ORD", domain, "Password1"))

        when:
        userAdmin.enabled = false;
        def updateUserResponse = updateUserXML(identityAdminToken, userAdmin1.id, userAdmin1)
        def getUserResponse = getUserXML(serviceAdminToken, userForCreate.location)
        User user = getUserResponse.getEntity(User)
        destroyUser(user.id)
        destroyUser(userAdmin1.id)
        destroyUser(userAdmin2.id)

        then:
        updateUserResponse.status == 200
        user.enabled == true
    }

    def "default user one cannot get default user two's admins"() {
        when:
        def response = getAdminsForUserXML(defaultUserToken, defaultUserTwo.id)

        then:
        response.status == 403
    }

    def "default user gets his admins"() {
        when:
        def response = getAdminsForUserXML(defaultUserToken, defaultUser.id)

        then:
        response.status == 200
        def users = response.getEntity(UserList).value
        users.getUser().size != 0
    }

    def "if user has no domain then an empty list is returned for his admins"() {
        when:
        def response = getAdminsForUserXML(identityAdminToken, identityAdmin.id)

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
        def userAdminForCreate = createUserXML(identityAdminToken, userForCreate(username, "displayName", "email@rackspace.com", true, "ORD", domainId, "Password1"))
        def userAdmin = userAdminForCreate.getEntity(User)
        def authRequest = authenticatePasswordXML(username, "Password1")
        String token = authRequest.getEntity(AuthenticateResponse).value.token.id
        def defaultUserForCreate = createUserXML(token, userForCreate(subUsername, "displayName", "email@rackspace.com", true, "ORD", null, "Password1"))
        def defaultUser = defaultUserForCreate.getEntity(User)
        def groupName = "myGroup" + sharedRandom
        def groupResponse = createGroupXML(serviceAdminToken, group(groupName,groupName))
        def group = groupResponse.getEntity(Group)
        def addUserToGroupResponse = addUserToGroupXML(serviceAdminToken, group.value.id, userAdmin.id)
        //Get groups
        def getUserAdminGroups = listGroupsForUserXML(serviceAdminToken, userAdmin.id)
        def userAdminGroups = getUserAdminGroups.getEntity(Groups)
        def getDefaultUserGroups = listGroupsForUserXML(serviceAdminToken, defaultUser.id)
        def defaultUserGroups = getDefaultUserGroups.getEntity(Groups)
        //Delete Group
        def deleteUserAdminGroupResponse = removeUserFromGroupXML(serviceAdminToken, group.value.id,userAdmin.id)
        //Get users with group deleted
        def getUserAdminDeletedGroup = listGroupsForUserXML(serviceAdminToken, userAdmin.id)
        def userAdminDeletedGroup = getUserAdminDeletedGroup.getEntity(Groups)
        def getDefaultUserDeletedGroup = listGroupsForUserXML(serviceAdminToken, defaultUser.id)
        def defaultUserDeletedGroup = getDefaultUserDeletedGroup.getEntity(Groups)

        //Clean data
        def deleteResponses = deleteUserXML(serviceAdminToken, defaultUser.id)
        def deleteAdminResponses = deleteUserXML(serviceAdminToken, userAdmin.id)
        def hardDeleteRespones = hardDeleteUserXML(serviceAdminToken, defaultUser.id)
        def hardDeleteAdminRespones = hardDeleteUserXML(serviceAdminToken, userAdmin.id)
        def deleteGroupResponse = deleteGroupXML(serviceAdminToken, group.value.id)
        def deleteDomainResponse = deleteDomainXML(serviceAdminToken, domainId)

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
        def response = createRoleXML(serviceAdminToken, role)
        def createdRole = response.getEntity(Role).value
        deleteRoleXML(serviceAdminToken, createdRole.getId())

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
        def response = createRoleXML(token, role)
        if (response.status == 201) {
            deleteRoleXML(serviceAdminToken, response.getEntity(Role).value.getId())
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
        def response = createRoleXML(token, role)
        if (response.status == 201) {
            deleteRoleXML(serviceAdminToken, response.getEntity(Role).value.getId())
        }

        then:
        response.status == 403

        where:
        token              | weight
        identityAdminToken | 0
    }

    def "adding a role which propagates to a user admin adds the role to the sub users"() {
        when:
        def defaultUserResponse1 = getUserApplicationRoleXML(serviceAdminToken, propagatingRole.getId(), defaultUserForAdminTwo.getId())
        def userAdminResponse1 = getUserApplicationRoleXML(serviceAdminToken, propagatingRole.getId(), userAdminTwo.getId())

        def response = addApplicationRoleToUserXML(serviceAdminToken, propagatingRole.getId(), userAdminTwo.getId())

        def defaultUserResponse2 = getUserApplicationRoleXML(serviceAdminToken, propagatingRole.getId(), defaultUserForAdminTwo.getId())
        def userAdminResponse2 = getUserApplicationRoleXML(serviceAdminToken, propagatingRole.getId(), userAdminTwo.getId())

        then:
        response.status == 200
        defaultUserResponse1.status == 404
        userAdminResponse1.status == 404
        defaultUserResponse2.status == 200
        userAdminResponse2.status == 200
    }

    def "removing a propagating role from a user-admin removes the role from the sub users"() {
        given:
        addApplicationRoleToUserXML(serviceAdminToken, propagatingRole.getId(), userAdminTwo.getId())

        when:
        def defaultUserResponse1 = getUserApplicationRoleXML(serviceAdminToken, propagatingRole.getId(), defaultUserForAdminTwo.getId())
        def userAdminResponse1 = getUserApplicationRoleXML(serviceAdminToken, propagatingRole.getId(), userAdminTwo.getId())

        def response = deleteApplicationRoleFromUserXML(serviceAdminToken, propagatingRole.getId(), userAdminTwo.getId())

        def defaultUserResponse2 = getUserApplicationRoleXML(serviceAdminToken, propagatingRole.getId(), defaultUserForAdminTwo.getId())
        def userAdminResponse2 = getUserApplicationRoleXML(serviceAdminToken, propagatingRole.getId(), userAdminTwo.getId())

        then:
        defaultUserResponse1.status == 200
        userAdminResponse1.status == 200
        response.status == 204
        defaultUserResponse2.status == 404
        userAdminResponse2.status == 404
    }

    def "authenticate returns password authentication type in response"() {
        when:
        def response = authenticatePasswordXML("admin$sharedRandom", "Password1")
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
        def response = getUserApiKey(serviceAdminToken, userAdmin.getId()).getEntity(com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials).value
        def authResp = authenticateApiKeyXML(userAdmin.name, response.apiKey)

        then:
        authResp.getEntity(AuthenticateResponse).getToken().getAny().contains("rax-auth:authenticatedBy")
    }

    def "validate returns password authentication type in response"() {
        when:
        def response = validateTokenXML(serviceAdminToken, userAdminToken)
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
        def service = createService(serviceName, serviceType)

        when:
        def createServiceResponse = createServiceXML(serviceAdminToken, service)
        def serviceEntity = createServiceResponse.getEntity(Service)
        def deleteServiceIdentityAdminTokenResponse = deleteServiceXML(identityAdminToken, serviceEntity.id)
        def deleteServiceResponse = deleteServiceXML(serviceAdminToken, serviceEntity.id)

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
        def result = createRoleXML((String)token, createRole(roleName, serviceId))
        if (result.status == 201){
            deleteRoleXML(serviceAdminToken, result.getEntity(Role).value.id)
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
        def result = createRoleXML((String)token, createRole(roleName, serviceId))
        if (result.status == 201){
            deleteRoleXML(serviceAdminToken, result.getEntity(Role).value.id)
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
        def identityAdminResponse = createRoleXML(identityAdminToken, createRole(roleName, null))
        def deleteResponse = deleteRoleXML(serviceAdminToken, identityAdminResponse.getEntity(Role).value.id)

        then:
        identityAdminResponse.status == 201
        deleteResponse.status == 204
    }

    def "user-admin and manage role can add user manage role to default user"() {
        when:
        def addRoleResult = addApplicationRoleToUserXML(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())
        def addRoleResult2 = addApplicationRoleToUserXML(defaultUserManageRoleToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole2.getId())
        def deleteRoleResult = deleteApplicationRoleFromUserXML(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())
        def deleteRoleResult2 = deleteApplicationRoleFromUserXML(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole2.getId())

        then:
        addRoleResult.status == 200
        addRoleResult2.status == 200
        deleteRoleResult.status == 204
        deleteRoleResult2.status == 204
    }

    def "user-admin managed role cannot delete role from user-managed" () {
        when:
        def addRoleResult = addApplicationRoleToUserXML(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())
        def deleteRoleResult = deleteApplicationRoleFromUserXML(defaultUserManageRoleToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole2.getId())
        def deleteRoleResultAsServiceAdmin = deleteApplicationRoleFromUserXML(serviceAdminToken, USER_MANAGE_ROLE_ID, defaultUserWithManageRole.getId())

        then:
        addRoleResult.status == 200
        deleteRoleResult.status == 403
        deleteRoleResultAsServiceAdmin.status == 204
    }

    def destroyUser(userId) {
        def deleteResponses = deleteUserXML(serviceAdminToken, userId)
        def hardDeleteRespones = hardDeleteUserXML(serviceAdminToken, userId)
    }

    //Resource Calls
    def getUserApiKey(String token, String userId) {
        resource.path(path20).path("users").path(userId).path("OS-KSADM/credentials/RAX-KSKEY:apiKeyCredentials").accept(APPLICATION_XML).get(ClientResponse)
    }

    def createUserXML(String token, user) {
        resource.path(path20).path('users').header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).post(ClientResponse)
    }

    def getUserXML(String token, URI location) {
        resource.uri(location).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def listUsersXML(String token) {
        resource.path(path20).path("users").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def listUsersXML(String token, offset, limit) {
        resource.path(path20).path("users").queryParams(pageParams(offset, limit)).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getUserByIdXML(String token, String userId) {
        resource.path(path20).path('users').path(userId).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUserByNameXML(String token, String name) {
        resource.path(path20).path('users').queryParam("name", name).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUsersByEmailXML(String token, String email) {
        resource.path(path20).path('users').queryParam("email", email).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def updateUserXML(String token, String userId, user) {
        resource.path(path20).path('users').path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(user).post(ClientResponse)
    }

    def addCredentialXML(String token, String userId, credential) {
        resource.path(path20).path('users').path(userId).path('OS-KSADM').path('credentials').entity(credential).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).post(ClientResponse)
    }

    def deleteUserXML(String token, String userId) {
        resource.path(path20).path('users').path(userId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def hardDeleteUserXML(String token, String userId) {
        resource.path(path20).path('softDeleted').path('users').path(userId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def createGroupXML(String token, group) {
        resource.path(path20).path(RAX_GRPADM).path('groups').header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(group).post(ClientResponse)
    }

    def getGroupXML(String token, URI uri) {
        resource.uri(uri).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getGroupByNameXML(String token, String name) {
        resource.path(path20).path(RAX_GRPADM).path('groups').queryParam("name", name).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getGroupsXML(String token) {
        resource.path(path20).path(RAX_GRPADM).path('groups').accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def updateGroupXML(String token, String groupId, group) {
        resource.path(path20).path(RAX_GRPADM).path('groups').path(groupId).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(group).put(ClientResponse)
    }

    def deleteGroupXML(String  token, String groupId) {
        resource.path(path20).path(RAX_GRPADM).path('groups').path(groupId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addUserToGroupXML(String token, String groupId, String userId) {
        resource.path(path20).path(RAX_GRPADM).path('groups').path(groupId).path("users").path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }
    def removeUserFromGroupXML(String token, String groupId, String userId) {
        resource.path(path20).path(RAX_GRPADM).path('groups').path(groupId).path("users").path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def listGroupsForUserXML(String token, String userId) {
        resource.path(path20).path('users').path(userId).path("RAX-KSGRP").accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUsersFromGroupXML(String token, String groupId) {
        resource.path(path20).path(RAX_GRPADM).path('groups').path(groupId).path("users").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def authenticatePasswordXML(String username, String password) {
        authenticateXML(authenticateRequest(username, password, null))
    }

    def authenticateApiKeyXML(String username, String apiKey) {
        authenticateXML(authenticateRequest(username, null, apiKey))
    }

    def authenticateXML(request) {
        resource.path(path20).path('tokens').accept(APPLICATION_XML).type(APPLICATION_XML).entity(request).post(ClientResponse)
    }

    def createRegionXML(String token, region) {
        resource.path(path20).path(RAX_AUTH).path("regions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(region).post(ClientResponse)
    }

    def getRegionXML(String token, String regionId) {
        resource.path(path20).path(RAX_AUTH).path("regions").path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getRegionsXML(String token) {
        resource.path(path20).path(RAX_AUTH).path("regions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def updateRegionXML(String token, String regionId, region) {
        resource.path(path20).path(RAX_AUTH).path("regions").path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(region).put(ClientResponse)
    }

    def deleteRegionXML(String token, String regionId) {
        resource.path(path20).path(RAX_AUTH).path("regions").path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def listUsersWithRoleXML(String token, String roleId) {
        resource.path(path20).path("OS-KSADM/roles").path(roleId).path("RAX-AUTH/users").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def listUsersWithRoleXML(String token, String roleId, String offset, String limit) {
        resource.path(path20).path("OS-KSADM/roles").path(roleId).path("RAX-AUTH/users").queryParams(pageParams(offset, limit)).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createRoleXML(String token, Role role) {
        resource.path(path20).path("OS-KSADM/roles").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(role).post(ClientResponse)
    }

    def deleteRoleXML(String token, String roleId) {
        resource.path(path20).path("OS-KSADM/roles").path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addApplicationRoleToUserXML(String token, String roleId, String userId) {
        resource.path(path20).path("users").path(userId).path("roles/OS-KSADM").path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }

    def getUserApplicationRoleXML(String token, String roleId, String userId) {
        resource.path(path20).path("users").path(userId).path("roles/OS-KSADM").path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def deleteApplicationRoleFromUserXML(String token, String roleId, String userId) {
        resource.path(path20).path("users").path(userId).path("roles/OS-KSADM").path(roleId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def addRoleToUserOnTenantXML(String token, String tenantId, String userId, String roleId) {
        resource.path(path20).path("tenants").path(tenantId).path("users").path(userId)
                .path("roles").path("OS-KSADM").path(roleId)
                .header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }

    def deleteRoleFromUserOnTenantXML(String token, String tenantId, String userId, String roleId) {
        resource.path(path20).path("tenants").path(tenantId).path("users").path(userId)
                .path("roles").path("OS-KSADM").path(roleId)
                .header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def getRoleOnTenantForUserXML(String token, String tenantId, String userId, String roleId) {
        resource.path(path20).path("tenants").path(tenantId).path("users").path(userId)
                .path("roles").path("OS-KSADM").path(roleId)
                .header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def removeRoleFromUserXML(String token, String roleId, String userId) {
        resource.path(path20).path("users").path(userId).path("roles/OS-KSADM").path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete()
    }

    def createQuestionXML(String token, question) {
        resource.path(path20).path(RAX_AUTH).path("secretqa/questions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(question).post(ClientResponse)
    }

    def getQuestionXML(String token, questionId) {
        resource.path(path20).path(RAX_AUTH).path("secretqa/questions").path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def getQuestionFromLocationXML(String token, location) {
        resource.uri(location).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def getQuestionsXML(String token) {
        resource.path(path20).path(RAX_AUTH).path("secretqa/questions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def updateQuestionXML(String token, String questionId, question) {
        resource.path(path20).path(RAX_AUTH).path("secretqa/questions").path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(question).put(ClientResponse)
    }

    def deleteQuestionXML(String token, String questionId) {
        resource.path(path20).path(RAX_AUTH).path("secretqa/questions").path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).delete(ClientResponse)
    }

    def addEndpointTemplateXML(String token, endpointTemplate) {
        resource.path(path20).path(OS_KSCATALOG).path("endpointTemplates").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(endpointTemplate).post(ClientResponse)
    }

    def deleteEndpointTemplateXML(String token, endpointTemplateId) {
        resource.path(path20).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addPolicyXML(String token, policy) {
        resource.path(path20).path(RAX_AUTH).path("policies").header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(policy).post(ClientResponse)
    }

    def getPolicyXML(String token, location) {
        resource.uri(location).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def deletePolicyXML(String token, policyId) {
        resource.path(path20).path(RAX_AUTH).path("policies").path(policyId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addPolicyToEndpointTemplateXML(String token, endpointTemplateId, policyId) {
        resource.path(path20).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).path(RAX_AUTH).path("policies").path(policyId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).put(ClientResponse)
    }

    def deletePolicyToEndpointTemplateXML(String token, endpointTemplateId, policyId) {
        resource.path(path20).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).path(RAX_AUTH).path("policies").path(policyId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def getPoliciesFromEndpointTemplateXML(String token, endpointTemplateId) {
        resource.path(path20).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).path(RAX_AUTH).path("policies").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def updatePoliciesForEndpointTemplateXML(String token, endpointTemplateId, policies) {
        resource.path(path20).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).path(RAX_AUTH).path("policies").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(policies).put(ClientResponse)
    }

    def addTenantXML(String token, Tenant tenant) {
        resource.path(path20).path("tenants").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(tenant).post(ClientResponse)
    }

    def getSecretQAXML(String token, String userId){
        resource.path(path20).path('users').path(userId).path(RAX_AUTH).path('secretqas').header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createSecretQAXML(String token, String userId, secretqa){
        resource.path(path20).path('users').path(userId).path(RAX_AUTH).path('secretqas').header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(secretqa).post(ClientResponse)
    }

    def getRoleXML(String token, String roleId) {
        resource.path(path20).path('OSKD-ADM/roles').path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def validateTokenXML(String token, String validateToken){
        resource.path(path20).path("tokens").path(validateToken).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def deleteDomainXML(String token, String domainId) {
        resource.path(path20).path("RAX-AUTH").path("domains").path(domainId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def impersonateXML(String token, User user) {
        def request = new ImpersonationRequest().with {
            it.user = user
            it.expireInSeconds = 10800
        }
        resource.path(path20).path("RAX-AUTH/impersonation-tokens").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(request).post(ClientResponse)
    }

    def revokeUserTokenXML(String token, String tokenToRevoke) {
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

    def updateCredentialsXML(String token, String userId, creds) {
        resource.path(path20).path("users").path(userId).path("OS-KSADM").path("credentials").path(JSONConstants.PASSWORD_CREDENTIALS).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(creds).post(ClientResponse)
    }

    def getAdminsForUserXML(String token, String userId) {
        resource.path(path20).path("users").path(userId).path("RAX-AUTH").path("admins").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createServiceXML(String token, service) {
        resource.path(path20).path("OS-KSADM").path("services").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(service).post(ClientResponse)
    }

    def deleteServiceXML(String token, String serviceId) {
        resource.path(path20).path("OS-KSADM").path("services").path(serviceId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    //Helper Methods
    def getPasswordCredentials(String username, String password) {
        new PasswordCredentialsRequiredUsername().with {
            it.username = username
            it.password = password
            return it
        }
    }
    def getApiKeyCredentials(String username, String apiKey) {
        new ApiKeyCredentials().with {
            it.username = username
            it.apiKey = apiKey
            return it
        }
    }

    def authenticateRequest(String username, String password, String apiKey) {
        def objectFactory = new ObjectFactory()
        def credentialObj
        def credentials
        if (apiKey != null) {
            credentials = getApiKeyCredentials(username, apiKey)
            credentialObj = objectFactory.createCredential(credentials)
        } else {
            credentials = getPasswordCredentials(username, password)
            credentialObj = objectFactory.createPasswordCredentials(credentials)
        }


        new AuthenticationRequest().with {
            it.setCredential(credentialObj)
            return it
        }
    }

    def userForUpdate(String id, String username, String displayName, String email, Boolean enabled, String defaultRegion, String password) {
        new User().with {
            it.id = id
            it.username = username
            it.email = email
            it.enabled = enabled
            it.displayName = displayName
            if (password != null) {
                it.otherAttributes.put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"), password)
            }
            if (defaultRegion != null) {
                it.otherAttributes.put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion"), defaultRegion)
            }
            return it
        }
    }

    def userForCreate(String username, String displayName, String email, Boolean enabled, String defaultRegion, String domainId, String password) {
        new User().with {
            it.username = (username != null) ? username : null
            it.displayName = (displayName != null) ? displayName : null
            it.email = (email != null) ? email : null
            it.enabled = (enabled != null) ? enabled : null
            if (defaultRegion != null) {
                it.otherAttributes.put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion"), defaultRegion)
            }
            if (domainId != null) {
                it.otherAttributes.put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "domainId"), domainId)
            }
            if (password != null) {
                it.otherAttributes.put(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"), password)
            }
            return it
        }
    }

    def group(String name, String description) {
        new Group().with {
            it.name = name
            it.description = description
            return it
        }
    }

    def group() {
        return group("group", "description")
    }

    def region(String name, Boolean enabled, Boolean isDefault) {
        Region regionEntity = new Region().with {
            it.name = name
            it.enabled = enabled
            it.isDefault = isDefault
            return it
        }
    }

    def region(String name) {
        return region(name, true, false)
    }

    def region() {
        return region("name", true, false)
    }

    def role() {
        new Role().with {
            def random = ((String) UUID.randomUUID()).replace('-',"")
            it.name = "role$random"
            it.description = "Test Global Role"
            return it
        }
    }

    def pageParams(String offset, String limit) {
        new MultivaluedMapImpl().with {
            it.add("marker", offset)
            it.add("limit", limit)
            return it
        }
    }

    def question() {
        question("id", "question")
    }

    def question(String id, String question) {
        new Question().with {
            it.id = id
            it.question = question
            return it
        }
    }

    def endpointTemplate(endpointTemplateId) {
        new EndpointTemplate().with {
            it.id = endpointTemplateId as int
            it.type = "compute"
            it.publicURL = "http://public.url"
            return it
        }
    }

    def policy(name) {
        new Policy().with {
            it.name = name
            it.blob = "blob"
            it.type = "type"
            return it
        }
    }

    def createTenant() {
        new Tenant().with {
            it.name = "tenant$sharedRandom"
            it.displayName = "displayName"
            it.enabled = true
            return it
        }
    }

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

    def secretQA(String id, String answer) {
        new SecretQA().with {
            it.id = id
            it.answer = answer
            return it
        }
    }

    def createService(String name, String type) {
        new Service().with {
            it.name = name
            it.type = type
            return it
        }
    }

    def createRole(String name, String serviceId) {
        new Role().with {
            it.name = name
            it.serviceId = serviceId
            return it
        }
    }

    def setLinkParams(List<String> links, Map<String, String[]> params) {
        for (String link : links) {
            def first = getFirstLink(link)
            if (first) {
                params.put("first", first)
                continue
            }
            def last = getLastLink(link)
            if (last) {
                params.put("last", last)
                continue
            }
            def prev = getPrevLink(link)
            if (prev) {
                params.put("prev", prev)
                continue
            }
            def next = getNextLink(link)
            if (next) {
                params.put("next", next)
                continue
            }
        }
    }
    def getFirstLink(String linkString) {
        def pattern = /marker=(.*)&limit=(.*)>; rel="first"/
        def matcher = (linkString =~ pattern)
        if (matcher) {
            return [ matcher[0][1], matcher[0][2] ]
        }
        return null
    }

    def getLastLink(String linkString) {
        def pattern = /marker=(.*)&limit=(.*)>; rel="last"/
        def matcher = (linkString =~ pattern)
        if (matcher) {
            return [ matcher[0][1], matcher[0][2] ]
        }
        return null
    }

    def getPrevLink(String linkString) {
        def pattern = /marker=(.*)&limit=(.*)>; rel="prev"/
        def matcher = (linkString =~ pattern)
        if (matcher) {
            return [ matcher[0][1], matcher[0][2] ]
        }
        return null
    }

    def getNextLink(String linkString) {
        def pattern = /marker=(.*)&limit=(.*)>; rel="next"/
        def matcher = (linkString =~ pattern)
        if (matcher) {
            return [ matcher[0][1], matcher[0][2] ]
        }
        return null
    }
}
