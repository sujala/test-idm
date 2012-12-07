package com.rackspace.idm.api.resource.cloud.v20;


import com.rackspace.docs.identity.api.ext.rax_auth.v1.Question
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Questions
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Region
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import com.sun.jersey.core.util.MultivaluedMapImpl
import spock.lang.Shared
import spock.lang.Specification

import javax.xml.namespace.QName

import org.openstack.docs.identity.api.v2.*

import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.ensureGrizzlyStarted
import static javax.ws.rs.core.MediaType.APPLICATION_XML
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies
import org.springframework.beans.factory.annotation.Autowired
import com.rackspace.idm.domain.service.ScopeAccessService
import org.apache.commons.configuration.Configuration
import org.springframework.test.context.ContextConfiguration
import com.rackspace.idm.domain.dao.impl.LdapConnectionPools
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.persist.LDAPPersister
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.entity.ScopeAccess
import com.unboundid.ldap.sdk.Modification
import com.unboundid.ldap.sdk.ModificationType
import org.joda.time.DateTime
import static javax.ws.rs.core.MediaType.MEDIA_TYPE_WILDCARD
import com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQA
import com.rackspace.docs.identity.api.ext.rax_auth.v1.SecretQAs

@ContextConfiguration(locations = "classpath:app-config.xml")
class Cloud20IntegrationTest extends Specification {
    @Autowired LdapConnectionPools connPools
    @Autowired Configuration config

    @Shared WebResource resource
    @Shared JAXBObjectFactories objFactories;

    @Shared def path = "cloud/v2.0/"
    @Shared def serviceAdminToken
    @Shared def identityAdminToken
    @Shared def userAdminToken
    @Shared def userAdminTwoToken
    @Shared def defaultUserToken
    @Shared def serviceAdmin

    @Shared def identityAdmin
    @Shared def userAdmin
    @Shared def userAdminTwo
    @Shared def defaultUser
    @Shared def defaultUserTwo
    @Shared def defaultUserThree
    @Shared def defaultUserForAdminTwo
    @Shared def testUser
    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom
    @Shared def sharedRole
    @Shared def sharedRoleTwo
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

    def setupSpec() {
        sharedRandom = ("$sharedRandomness").replace('-',"")
        testDomainId = "domain1$sharedRandom"
        testDomainId2 = "domain2$sharedRandom"
        emptyDomainId = "domain3$sharedRandom"

        this.resource = ensureGrizzlyStarted("classpath:app-config.xml");
        this.objFactories = new JAXBObjectFactories()
        serviceAdminToken = authenticate("authQE", "Auth1234").getEntity(AuthenticateResponse).value.token.id
        serviceAdmin = getUserByName(serviceAdminToken, "authQE").getEntity(User)

        identityAdmin = getUserByName(serviceAdminToken, "auth").getEntity(User)
        identityAdminToken = authenticate("auth", "auth123").getEntity(AuthenticateResponse).value.token.id

        createUser(serviceAdminToken, userForCreate("admin$sharedRandom", "display", "email@email.com", true, null, null, "Password1"))
        testUser = getUserByName(serviceAdminToken, "admin$sharedRandom").getEntity(User)
        USER_FOR_AUTH = testUser.username
        USER_FOR_AUTH_PWD = "Password1"

        endpointTemplateId = "100001"
        addEndpointTemplate(serviceAdminToken, endpointTemplate(endpointTemplateId))
        def addPolicyResponse = addPolicy(serviceAdminToken, policy("name"))
        def getPolicyResponse = getPolicy(serviceAdminToken, addPolicyResponse.location)
        policyId = getPolicyResponse.getEntity(Policy).id as String


        defaultRegion = region("ORD", true, true)
        createRegion(serviceAdminToken, defaultRegion)
        updateRegion(serviceAdminToken, defaultRegion.name, defaultRegion)

        //User Admin
        def createResponse = createUser(identityAdminToken, userForCreate("userAdmin1$sharedRandom", "display", "test@rackspace.com", true, null, testDomainId, "Password1"))
        userAdmin = getUserByName(identityAdminToken, "userAdmin1$sharedRandom").getEntity(User)
        createUser(identityAdminToken, userForCreate("userAdmin2$sharedRandom", "display", "test@rackspace.com", true, null, emptyDomainId, "Password1"))
        userAdminTwo = getUserByName(identityAdminToken, "userAdmin2$sharedRandom").getEntity(User)

        userAdminToken = authenticate("userAdmin1$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id
        userAdminTwoToken = authenticate("userAdmin2$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id

        // Default Users
        createUser(userAdminToken, userForCreate("defaultUser1$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUser = getUserByName(userAdminToken, "defaultUser1$sharedRandom").getEntity(User)
        createUser(userAdminToken, userForCreate("defaultUser2$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserTwo = getUserByName(userAdminToken, "defaultUser2$sharedRandom").getEntity(User)
        createUser(userAdminToken, userForCreate("defaultUser3$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserThree = getUserByName(userAdminToken, "defaultUser3$sharedRandom").getEntity(User)
        createUser(userAdminTwoToken, userForCreate("defaultUser4$sharedRandom", "display", "test@rackspace.com", true, null, null, "Password1"))
        defaultUserForAdminTwo = getUserByName(userAdminTwoToken, "defaultUser4$sharedRandom").getEntity(User)

        defaultUserToken = authenticate("defaultUser1$sharedRandom", "Password1").getEntity(AuthenticateResponse).value.token.id

        //create group
        def createGroupResponse = createGroup(serviceAdminToken, group("group$sharedRandom", "this is a group"))
        groupLocation = createGroupResponse.location
        def getGroupResponse = getGroup(serviceAdminToken, groupLocation)
        group = getGroupResponse.getEntity(Group).value

        def createRegionResponse = createRegion(serviceAdminToken, region("region$sharedRandom", true, false))
        def getRegionResponse = getRegion(serviceAdminToken, "region$sharedRandom")
        sharedRegion = getRegionResponse.getEntity(Region)

        //create role
        if (sharedRole == null) {
            def roleResponse = createRole(serviceAdminToken, role())
            sharedRole = roleResponse.getEntity(Role).value
        }
        if (sharedRoleTwo == null) {
            def roleResponse2 = createRole(serviceAdminToken, role())
            sharedRoleTwo = roleResponse2.getEntity(Role).value
        }

        if (tenant == null) {
            def tenantForCreate = createTenant()
            def tenantResponse = addTenant(serviceAdminToken, tenantForCreate)
            tenant = tenantResponse.getEntity(Tenant).value
        }

        //Add role to identity-admin and default-users
        addApplicationRoleToUser(serviceAdminToken, sharedRole.getId(), identityAdmin.getId())
        addApplicationRoleToUser(serviceAdminToken, sharedRole.getId(), defaultUser.getId())
        addApplicationRoleToUser(serviceAdminToken, sharedRole.getId(), defaultUserTwo.getId())
        addApplicationRoleToUser(serviceAdminToken, sharedRole.getId(), defaultUserThree.getId())

        //testIdentityRole = getRole(serviceAdminToken, testIdentityRoleId).getEntity(Role).value

    }

    def setup() {
        expireTokens(USER_FOR_AUTH, 12)
        setConfigValues()
    }

    def cleanupSpec() {
        deleteGroup(serviceAdminToken, group.getId())
        deleteRegion(serviceAdminToken, sharedRegion.getName())

        deleteRole(serviceAdminToken, sharedRole.getId())
        deleteRole(serviceAdminToken, sharedRoleTwo.getId())

        deleteUser(serviceAdminToken, userAdmin.getId())
        deleteUser(serviceAdminToken, userAdminTwo.getId())

        deleteUser(serviceAdminToken, defaultUser.getId())
        deleteUser(serviceAdminToken, defaultUserTwo.getId())
        deleteUser(serviceAdminToken, defaultUserThree.getId())
        deleteUser(serviceAdminToken, defaultUserForAdminTwo.getId())

        deleteUser(serviceAdminToken, testUser.getId())

        //TODO: DELETE DOMAINS

        deleteEndpointTemplate(serviceAdminToken, endpointTemplateId)
        deletePolicy(serviceAdminToken, policyId)
    }

    def "authenticating where total access tokens remains unchanged"() {
        when:
        def scopeAccessOne = authenticate(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        def scopeAccessTwo = authenticate(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        def allUsersScopeAccessAfter = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=UserScopeAccess)(uid=$USER_FOR_AUTH))", "*")

        then:
        scopeAccessOne.token.id.equals(scopeAccessTwo.token.id)
        allUsersScopeAccessAfter.entryCount <= 2
    }

    def "authenticating where token is within refresh window adds new token"() {
        when:
        def scopeAccessOne = authenticate(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        setTokenInRefreshWindow(USER_FOR_AUTH, scopeAccessOne.token.id)

        def allUsersScopeAccessBefore = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=UserScopeAccess)(uid=$USER_FOR_AUTH))", "*")

        def scopeAccessTwo = authenticate(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        def allUsersScopeAccessAfter = connPools.getAppConnPool().search(BASE_DN, SCOPE, "(&(objectClass=UserScopeAccess)(uid=$USER_FOR_AUTH))", "*")

        then:
        allUsersScopeAccessBefore.getEntryCount() + 1 == allUsersScopeAccessAfter.getEntryCount()
        !scopeAccessOne.token.id.equals(scopeAccessTwo.token.id)

    }

    def "authenticating where token is valid returns existing token"() {
        when:
        def scopeAccessOne = authenticate(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        def scopeAccessTwo = authenticate(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        then:
        scopeAccessOne.token.id.equals(scopeAccessTwo.token.id)
    }

    def "authenticate with two valid tokens"() {
        when:
        def firstScopeAccess = authenticate(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        setTokenInRefreshWindow(USER_FOR_AUTH, firstScopeAccess.token.id)
        def secondScopeAccess = authenticate(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        def thirdScopeAccess = authenticate(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

        then:
        secondScopeAccess.token.id.equals(thirdScopeAccess.token.id)
    }

    def "authenticating token in refresh window with 2 existing tokens deletes existing expired token"() {
        when:
        def firstScopeAccess = authenticate(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        setTokenInRefreshWindow(USER_FOR_AUTH, firstScopeAccess.token.id)
        def secondScopeAccess = authenticate(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value
        expireToken(USER_FOR_AUTH, firstScopeAccess.token.id, 12)
        setTokenInRefreshWindow(USER_FOR_AUTH, secondScopeAccess.token.id)
        def thirdScopeAccess = authenticate(USER_FOR_AUTH, USER_FOR_AUTH_PWD).getEntity(AuthenticateResponse).value

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
        def response = createUser(serviceAdminToken, user)
        //Get user
        def getUserResponse = getUser(serviceAdminToken, response.location)
        def userEntity = getUserResponse.getEntity(User)
        //Get user by id
        def getUserByIdResponse = getUserById(serviceAdminToken, userEntity.getId())
        def getUserByNameResponse = getUserByName(serviceAdminToken, userEntity.getUsername())
        //Update User
        def userForUpdate = userForUpdate(null, "updatedBob" + random, "Bob", "test@rackspace.com", true, null, null)
        def updateUserResponse = updateUser(serviceAdminToken, userEntity.getId(), userForUpdate)
        //Delete user
        def deleteResponses = deleteUser(serviceAdminToken, userEntity.getId())
        //Hard delete user
        def hardDeleteResponses = hardDeleteUser(serviceAdminToken, userEntity.getId())

        then:
        response.status == 201
        response.location != null
        getUserResponse.status == 200
        getUserByIdResponse.status == 200
        getUserByNameResponse.status == 200
        updateUserResponse.status == 200
        deleteResponses.status == 204
        hardDeleteResponses.status == 204
    }

    def "operations on non-existent users return 'not found'"() {
        expect:
        response.status == 404

        where:
        response << [
                getUserById(serviceAdminToken, "badId"),
                getUserByName(serviceAdminToken, "badName"),
                updateUser(serviceAdminToken, "badId", new User()),
                deleteUser(serviceAdminToken, "badId"),
                addCredential(serviceAdminToken, "badId", getCredentials("someUser", "SomePassword1"))
        ]
    }

    def "invalid operations on create/update user returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                createUser(serviceAdminToken, userForCreate("!@#What", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                //createUser(serviceAdminToken, userForCreate("1one", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("one name", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate(null, "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "junk!@#", true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", null, true, "ORD", null, "Password1")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", null, "Pop1")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", null, "longpassword1")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", null, "Longpassword")),
                createUser(serviceAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", "someId", "Longpassword")),
                createUser(identityAdminToken, userForCreate("goodName", "display", "test@rackspace.com", true, "ORD", null, "Longpassword1")),
                //updateUser(userAdminToken, defaultUser.getId(), userForUpdate("1", "someOtherName", "someOtherDisplay", "some@rackspace.com", true, "ORD", "SomeOtherPassword1")),
                updateUser(defaultUserToken, defaultUser.getId(), userForUpdate(null, "someOtherName", "someOtherDisplay", "some@rackspace.com", false, "ORD", "SomeOtherPassword1")),
                updateUser(identityAdminToken, defaultUser.getId(), userForUpdate(null, null, null, null, true, "HAHAHAHA", "Password1"))
        ]
    }

    def 'operations with invalid tokens'() {
        expect:
        response.status == 401

        where:
        response << [
                createUser("invalidToken", userForCreate("someName", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                createUser(null, userForCreate("someName", "display", "test@rackspace.com", true, "ORD", null, "Password1")),  \
                getUserById("invalidToken", "badId"),
                getUserById(null, "badId"),
                getUserByName("invalidToken", "badId"),
                getUserByName(null, "badId"),
                updateUser("invalidToken", "badId", new User()),
                updateUser(null, "badId", new User()),
                deleteUser("invalidToken", "badId"),
                deleteUser(null, "badId"),
                listUsers("invalidToken"),
                listUsers(null)
        ]

    }

    def 'forbidden operations for users'() {
        expect:
        response.status == 403

        where:
        response << [
                createUser(defaultUserToken, userForCreate("someName", "display", "test@rackspace.com", true, "ORD", null, "Password1")),
                updateUser(defaultUserToken, userAdmin.getId(), userForUpdate(null, "someOtherName", "someOtherDisplay", "some@rackspace.com", true, "ORD", "SomeOtherPassword1")),
                getUserById(defaultUserToken, userAdmin.getId()),
                getUserById(defaultUserToken, identityAdmin.getId()),
                getUserById(defaultUserToken, serviceAdmin.getId()),
                getUserById(userAdminToken, identityAdmin.getId()),
                getUserById(userAdminToken, serviceAdmin.getId()),
                getUserByName(defaultUserToken, userAdmin.getUsername()),
                getUserByName(defaultUserToken, identityAdmin.getUsername()),
                getUserByName(defaultUserToken, serviceAdmin.getUsername()),
                getUserByName(userAdminToken, identityAdmin.getUsername()),
                getUserByName(userAdminToken, serviceAdmin.getUsername()),
                createGroup(defaultUserToken, group()),
                updateGroup(defaultUserToken, group.getId(), group()),
                deleteGroup(defaultUserToken, group.getId()),
                getGroup(defaultUserToken, groupLocation),
                getGroups(defaultUserToken),
                createRegion(defaultUserToken, region()),
                updateRegion(defaultUserToken, sharedRegion.getName(), sharedRegion),
                deleteRegion(defaultUserToken, sharedRegion.getName()),
                getRegion(defaultUserToken, sharedRegion.getName()),
                createQuestion(defaultUserToken, question()),
                updateQuestion(defaultUserToken, "id", question()),
                deleteQuestion(defaultUserToken, "id"),
        ]
    }

    def 'valid operations on retrieving users'() {
        expect:
        response.status == 200

        where:
        response << [
                listUsers(serviceAdminToken),
                listUsers(identityAdminToken),
                listUsers(userAdminToken),
                listUsers(defaultUserToken),
                getUserById(defaultUserToken, defaultUser.getId()),
                getUserById(userAdminToken, defaultUser.getId()),
                getUserById(userAdminToken, userAdmin.getId()),
                getUserById(identityAdminToken, userAdmin.getId()),
                getUserById(serviceAdminToken, userAdmin.getId()),
                getUserByName(defaultUserToken, defaultUser.getUsername()),
                getUserByName(userAdminToken, defaultUser.getUsername()),
                getUserByName(userAdminToken, userAdmin.getUsername()),
                getUserByName(identityAdminToken, userAdmin.getUsername()),
                getUserByName(serviceAdminToken, userAdmin.getUsername())
        ]

    }

    def "Group CRUD" () {
        when:
        def random = ("$randomness").replace('-', "")
        def createGroupResponse = createGroup(serviceAdminToken, group("group$random", "this is a group"))

        def getGroupResponse = getGroup(serviceAdminToken, createGroupResponse.location)
        def groupEntity = getGroupResponse.getEntity(Group)
        def groupId = groupEntity.value.id

        def getGroupsResponse = getGroups(serviceAdminToken)
        def groupsEntity = getGroupsResponse.getEntity(Groups)

        def updateGroupResponse = updateGroup(serviceAdminToken, groupId, group("group$random", "updated group"))

        def deleteGroupResponse = deleteGroup(serviceAdminToken, groupId)


        then:
        createGroupResponse.status == 201
        createGroupResponse.location != null
        getGroupResponse.status == 200
        getGroupsResponse.status == 200
        groupsEntity.value.getGroup().size() > 0
        updateGroupResponse.status == 200
        deleteGroupResponse.status == 204
    }

    def "Group Assignment CRUD" () {
        when:
        def addUserToGroupResponse = addUserToGroup(serviceAdminToken, group.getId(), defaultUser.getId())

        def listGroupsForUserResponse = listGroupsForUser(serviceAdminToken, defaultUser.getId())
        def groups = listGroupsForUserResponse.getEntity(Groups).value

        def getUsersFromGroupResponse = getUsersFromGroup(serviceAdminToken, group.getId())
        def users = getUsersFromGroupResponse.getEntity(UserList).value

        def removeUserFromGroupRespone = removeUserFromGroup(serviceAdminToken, group.getId(), defaultUser.getId())

        then:
        addUserToGroupResponse.status == 204

        listGroupsForUserResponse.status == 200
        groups.getGroup().size() == 1
        getUsersFromGroupResponse.status == 200
        users.getUser().size() == 1

        removeUserFromGroupRespone.status == 204
    }

    def "invalid operations on create/update group returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                createGroup(serviceAdminToken, group(null, "this is a group")),
                createGroup(serviceAdminToken, group("", "this is a group")),
                createGroup(serviceAdminToken, group("group", null)),
                updateGroup(serviceAdminToken, group.getId(), group(null, "this is a group")),
                updateGroup(serviceAdminToken, group.getId(), group("", "this is a group")),
                updateGroup(serviceAdminToken, group.getId(), group("group", null)),
                addUserToGroup(serviceAdminToken, "doesnotexist", defaultUser.getId()),
        ]
    }

    def "invalid operations on create/update group returns 'not found'"() {
        expect:
        response.status == 404

        where:
        response << [
                addUserToGroup(serviceAdminToken, group.getId(), "doesnotexist"),
        ]
    }

    def "update region name is not allowed"() {
        given:
        Region region1 = region("somename", false, false)

        when:
        def updateRegionResponse = updateRegion(serviceAdminToken, sharedRegion.getName(), region1)

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
        def createRegionResponse = createRegion(serviceAdminToken, region1)
        def getRegionResponse = getRegion(serviceAdminToken, regionName)
        Region createdRegion = getRegionResponse.getEntity(Region)

        def updateRegionResponse = updateRegion(serviceAdminToken, regionName, region2)
        def getUpdatedRegionResponse = getRegion(serviceAdminToken, regionName)
        Region updatedRegion = getUpdatedRegionResponse.getEntity(Region)

        def getRegionsResponse = getRegions(serviceAdminToken)
        Regions regions = getRegionsResponse.getEntity(Regions)

        def deleteRegionResponse = deleteRegion(serviceAdminToken, regionName)
        def getDeletedRegionResponse = getRegion(serviceAdminToken, regionName)


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
                updateRegion(serviceAdminToken, "notfound", region()),
                deleteRegion(serviceAdminToken, "notfound"),
                getRegion(serviceAdminToken, "notfound"),
        ]
    }

    def "invalid operations on create regions returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                createRegion(serviceAdminToken, region(null, true, false)),
        ]
    }

    def "create region that already exists returns conflict"() {
        when:
        def createRegionResponse = createRegion(serviceAdminToken, sharedRegion)

        then:
        createRegionResponse.status == 409
    }

    def "listUsersWithRole called by default-user returns forbidden"() {
        when:
        def response = listUsersWithRole(defaultUserToken, "1")

        then:
        response.status == 403
    }

    def "listUsersWithRole called by admin invalid roleId returns not found"() {
        when:
        def response = listUsersWithRole(serviceAdminToken, "-5")

        then:
        response.status == 404
    }

    def "listUsersWithRole called by admins returns success"() {
        expect:
        response.status == 200

        where:
        response << [
                listUsersWithRole(identityAdminToken, sharedRole.getId()),
                listUsersWithRole(serviceAdminToken, sharedRole.getId()),
                listUsersWithRole(userAdminToken, sharedRole.getId())
        ]
    }

    def "listUsersWithRole empty list returns"() {
        when:
        def response = listUsersWithRole(userAdminTwoToken, sharedRole.getId())

        then:
        response.status == 200
        response.getEntity(UserList).value.user.size == 0
        response.headers.getFirst("Link") == null
    }

    def "listUsersWithRole non empty list"() {
        when:
        def userAdminResponse = listUsersWithRole(userAdminToken, sharedRole.getId())
        def serviceAdminResponse = listUsersWithRole(serviceAdminToken, sharedRole.getId())
        def identityAdminResponse = listUsersWithRole(identityAdminToken, sharedRole.getId())

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
        def userAdminResponse1 = listUsersWithRole(userAdminToken, sharedRole.getId(), 0, 1)
        def userAdminResponse2 = listUsersWithRole(userAdminToken, sharedRole.getId(), 1, 1)
        def userAdminResponse3 = listUsersWithRole(userAdminToken, sharedRole.getId(), 2, 1)
        def serviceAdminResponse1 = listUsersWithRole(serviceAdminToken, sharedRole.getId(), 1, 2)
        def serviceAdminResponse2 = listUsersWithRole(serviceAdminToken, sharedRole.getId(), 0, 2)
        def serviceAdminResponse3 = listUsersWithRole(serviceAdminToken, sharedRole.getId(), 2, 2)
        def serviceAdminResponse4 = listUsersWithRole(serviceAdminToken, sharedRole.getId(), 3, 4)

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

    def "listUsersWithRole offset greater than result set length returns 400"() {
        when:
        def responseOne = listUsersWithRole(serviceAdminToken, sharedRole.getId(), 100, 10)
        def responseTwo = listUsersWithRole(userAdminToken, sharedRole.getId(), 100, 10)

        then:
        responseOne.status == 400
        responseTwo.status == 400
    }

    def "listUsersWithRole role assigned to no one"() {
        when:
        def responseOne = listUsersWithRole(serviceAdminToken, sharedRoleTwo.getId())
        def responseTwo = listUsersWithRole(serviceAdminToken, sharedRoleTwo.getId(), 0, 10)
        def responseThree = listUsersWithRole(identityAdminToken, sharedRoleTwo.getId())
        def responseFour = listUsersWithRole(identityAdminToken, sharedRoleTwo.getId(), 0, 10)
        def responseFive = listUsersWithRole(userAdminToken, sharedRoleTwo.getId())
        def responseSix = listUsersWithRole(userAdminToken, sharedRoleTwo.getId(), 0, 10)

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
        def createResponse = createQuestion(serviceAdminToken, question1)
        def getCreateResponse = getQuestionFromLocation(serviceAdminToken, createResponse.location)
        def createEntity = getCreateResponse.getEntity(Question)
        question2.id = createEntity.id

        def updateResponse = updateQuestion(serviceAdminToken, createEntity.id, question2)
        def getUpdateResponse = getQuestion(serviceAdminToken, createEntity.id)
        def updateEntity = getUpdateResponse.getEntity(Question)

        def deleteResponse = deleteQuestion(serviceAdminToken, createEntity.id)
        def getDeleteResponse = getQuestion(serviceAdminToken, createEntity.id)

        def getQuestionResponse = getQuestions(serviceAdminToken)
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
                updateQuestion(serviceAdminToken, "notfound", question("notfound", "question")),
                deleteQuestion(serviceAdminToken, "notfound"),
                getQuestion(serviceAdminToken, "notfound"),
        ]
    }

    def "invalid operations on question returns 'bad request'"() {
        expect:
        response.status == 400

        where:
        response << [
                updateQuestion(serviceAdminToken, "ids", question("dontmatch", "question")),
                updateQuestion(serviceAdminToken, "id", question("id", null)),
                createQuestion(serviceAdminToken, question("id", null)),
        ]
    }

    def "listUsers returns forbidden (invalid token)"() {
        expect:
        response.status == 401

        where:
        response << [
                listUsers(""),
                listUsers("1")
        ]
    }

    def "listUsers returns default user"() {
        when:
        def users = listUsers(defaultUserToken).getEntity(UserList).value.user

        then:
        users[0].username.equals(defaultUser.username)
    }

    def "listUsers caller is user-admin returns users from domain"() {
        when:
        def users = listUsers(userAdminToken).getEntity(UserList).value.user

        then:
        users.size() == 4
    }

    def "listUsers caller is identity-admin or higher returns paged results"() {
        expect:
        response.status == 200
        response.headers.getFirst("Link") != null

        where:
        response << [
                listUsers(identityAdminToken),
                listUsers(identityAdminToken, 0, 10),
                listUsers(identityAdminToken, 15, 10),
                listUsers(serviceAdminToken),
                listUsers(serviceAdminToken, 0, 10),
                listUsers(serviceAdminToken, 15, 10),
        ]
    }

    def "listUsers throws bad request"() {
        expect:
        response.status == 400

        where:
        response << [
                listUsers(serviceAdminToken, 100000000, 25),
                listUsers(identityAdminToken, 10000000, 50),
                listUsers(serviceAdminToken, 0, "abc"),
                listUsers(serviceAdminToken, "abc", 10)
        ]
    }

    def "create and delete applicationRole with insufficient priveleges"() {
        expect:
        response.status == 403

        where:
        response << [
                addApplicationRoleToUser(defaultUserToken, sharedRole.getId(), defaultUserForAdminTwo.getId()),
                addApplicationRoleToUser(userAdminToken, sharedRole.getId(), defaultUserForAdminTwo.getId()),
                addApplicationRoleToUser(userAdminToken, sharedRole.getId(), defaultUser.getId()),
                deleteApplicationRoleFromUser(defaultUserToken, sharedRole.getId(), defaultUser.getId()),
                deleteApplicationRoleFromUser(userAdminToken, sharedRole.getId(), defaultUserForAdminTwo.getId()),
                deleteApplicationRoleFromUser(userAdminToken, sharedRole.getId(), defaultUser.getId()),
        ]
    }

    def "adding identity:* to user with identity:* role And deleting own identity:* role return forbidden"() {
        expect:
        response.status == 403

        where:
        response << [
                addApplicationRoleToUser(serviceAdminToken, "1", defaultUser.getId()),
                addApplicationRoleToUser(serviceAdminToken, "1", userAdmin.getId()),
                addApplicationRoleToUser(serviceAdminToken, "1", identityAdmin.getId()),
                addApplicationRoleToUser(serviceAdminToken, "1", serviceAdmin.getId()),
                deleteApplicationRoleFromUser(userAdminToken, "3", userAdmin.getId()),
                deleteApplicationRoleFromUser(identityAdminToken, "1", identityAdmin.getId()),
                deleteApplicationRoleFromUser(serviceAdminToken, "4", serviceAdmin.getId())
        ]
    }

    def "add Application role to user succeeds"() {
        expect:
        response.status == 200

        where:
        response << [
                addApplicationRoleToUser(identityAdminToken, sharedRoleTwo.getId(), userAdmin.getId()),
                addApplicationRoleToUser(serviceAdminToken, sharedRoleTwo.getId(), identityAdmin.getId())
        ]
    }

    def "delete Application role from user succeeds"() {
        expect:
        response.status == 204

        where:
        response << [
                deleteApplicationRoleFromUser(identityAdminToken, sharedRoleTwo.getId(), userAdmin.getId()),
                deleteApplicationRoleFromUser(serviceAdminToken, sharedRoleTwo.getId(), identityAdmin.getId())
        ]
    }

    def "adding to and deleting roles from user on tenant return 403"() {
        expect:
        response.status == 403

        where:
        response << [
                deleteRoleFromUserOnTenant(userAdminToken, tenant.id, defaultUserForAdminTwo.getId(), sharedRole.getId()),
                deleteRoleFromUserOnTenant(userAdminToken, tenant.id, defaultUser.getId(), sharedRole.getId()),
                addRoleToUserOnTenant(userAdminToken, tenant.id, defaultUserForAdminTwo.getId(), sharedRole.getId()),
                addRoleToUserOnTenant(userAdminToken, tenant.id, defaultUser.getId(), sharedRole.getId())
        ]
    }

    def "adding identity:* roles to user on tenant returns 400"() {
        expect:
        response.status == 400

        where:
        response << [
                addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), defaultUserRoleId),
                addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), userAdminRoleId),
                addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), identityAdminRoleId),
                addRoleToUserOnTenant(serviceAdminToken, tenant.id, defaultUser.getId(), serviceAdminRoleId)
        ]
    }

    def "adding roles to user on tenant succeeds"() {
        expect:
        response.status == 200

        where:
        response << [
                addRoleToUserOnTenant(identityAdminToken, tenant.id, userAdmin.getId(), sharedRoleTwo.id),
                addRoleToUserOnTenant(serviceAdminToken, tenant.id, identityAdmin.getId(), sharedRoleTwo.id)
        ]
    }

    def "deleting roles from user on tenant succeeds"() {
        expect:
        response.status == 204

        where:
        response << [
                deleteRoleFromUserOnTenant(identityAdminToken, tenant.id, userAdmin.getId(), sharedRoleTwo.id),
                deleteRoleFromUserOnTenant(serviceAdminToken, tenant.id, identityAdmin.getId(), sharedRoleTwo.id)
        ]
    }

    def "add policy to endpoint without endpoint without policy returns 404" () {
        when:
        def response = addPolicyToEndpointTemplate(identityAdminToken, "111111", "111111")

        then:
        response.status == 404
    }

    def "add policy to endpoint with endpoint without policy returns 404" () {
        when:
        def response = addPolicyToEndpointTemplate(identityAdminToken, endpointTemplateId, "111111")

        then:
        response.status == 404
    }

    def "add policy to endpoint with endpoint with policy returns 204"() {
        when:
        def addResponse = addPolicyToEndpointTemplate(serviceAdminToken, endpointTemplateId, policyId)
        def getResponse = getPoliciesFromEndpointTemplate(serviceAdminToken, endpointTemplateId)
        def policies = getResponse.getEntity(Policies)
        def updateResponse = updatePoliciesForEndpointTemplate(serviceAdminToken, endpointTemplateId, policies)
        def deletePolicyResponse = deletePolicy(serviceAdminToken, policyId)
        def deleteResponse = deletePolicyToEndpointTemplate(serviceAdminToken, endpointTemplateId, policyId)

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
        def response = updatePoliciesForEndpointTemplate(serviceAdminToken, "111111", policies)

        then:
        response.status == 404
    }

    def "update policy to endpoint with endpoint without policy returns 404"() {
        when:
        Policies policies = new Policies()
        Policy policy = policy("name")
        policies.policy.add(policy)
        def response = updatePoliciesForEndpointTemplate(serviceAdminToken, endpointTemplateId, policies)

        then:
        response.status == 404
    }

    def "Create secretQA and get secretQA" () {
        when:
        def response = createSecretQA(serviceAdminToken,defaultUser.getId(), secretQA("1","answer"))
        def secretQAResponse = getSecretQA(serviceAdminToken, defaultUser.getId()).getEntity(SecretQAs)

        then:
        response.status == 200
        secretQAResponse.secretqa.get(0).answer == "answer"
    }

    def "Create/Get secretQA returns 403" () {
        expect:
        response.status == 403

        where:
        response << [
                createSecretQA(defaultUserToken, serviceAdmin.getId(), secretQA("1", "answer")),
                createSecretQA(defaultUserToken, identityAdmin.getId(), secretQA("1", "answer")),
                createSecretQA(defaultUserToken, userAdmin.getId(), secretQA("1", "answer")),
                createSecretQA(userAdminToken, serviceAdmin.getId(), secretQA("1", "answer")),
                createSecretQA(userAdminToken, identityAdmin.getId(), secretQA("1", "answer")),
                createSecretQA(userAdminToken, userAdminTwo.getId(), secretQA("1", "answer")),
                createSecretQA(userAdminToken, defaultUserForAdminTwo.getId(), secretQA("1", "answer")),
                getSecretQA(defaultUserToken, serviceAdmin.getId()),
                getSecretQA(defaultUserToken, identityAdmin.getId()),
                getSecretQA(userAdminToken, serviceAdmin.getId()),
                getSecretQA(userAdminToken, identityAdmin.getId()),
                getSecretQA(userAdminToken, userAdminTwo.getId()),
                getSecretQA(userAdminToken, defaultUserForAdminTwo.getId())

        ]

    }

    def "Create/Get secretQA returns 401" () {
        expect:
        response.status == 401

        where:
        response << [
                createSecretQA("", defaultUser.getId(), secretQA("1", "answer")),
                createSecretQA("", defaultUser.getId(), secretQA("1", "answer")),
                createSecretQA(null, defaultUser.getId(), secretQA("1", "answer")),
                createSecretQA(null, defaultUser.getId(), secretQA("1", "answer")),
                getSecretQA("", serviceAdmin.getId()),
                getSecretQA("", identityAdmin.getId()),
                getSecretQA(null, serviceAdmin.getId()),
                getSecretQA(null, identityAdmin.getId()),
        ]

    }

    def "Create/Get secretQA returns 400" () {
        expect:
        response.status == 400

        where:
        response << [
                createSecretQA(serviceAdminToken, defaultUser.getId(), secretQA(null, "answer")),
                createSecretQA(serviceAdminToken, defaultUser.getId(), secretQA("1", null))
        ]

    }

    def "Create/Get secretQA returns 404" () {
        expect:
        response.status == 404

        where:
        response << [
                createSecretQA(serviceAdminToken, "badId", secretQA("1", "answer")),
                createSecretQA(serviceAdminToken, defaultUser.getId(), secretQA("badId", "answer")),
                getSecretQA(serviceAdminToken, "badId")
        ]

    }

    def "Create/Get secretQA returns 200" () {
        expect:
        response.status == 200

        where:
        response << [
                createSecretQA(serviceAdminToken, defaultUser.getId(), secretQA("1", "answer")),
                createSecretQA(identityAdminToken, defaultUser.getId(), secretQA("1", "answer")),
                createSecretQA(userAdminToken, defaultUser.getId(), secretQA("1", "answer")),
                createSecretQA(defaultUserToken, defaultUser.getId(), secretQA("1", "answer")),
                getSecretQA(serviceAdminToken, defaultUser.getId()),
                getSecretQA(identityAdminToken, defaultUser.getId()),
                getSecretQA(userAdminToken, defaultUser.getId()),
                getSecretQA(defaultUserToken, defaultUser.getId()),
        ]

    }

    //Resource Calls
    def createUser(String token, user) {
        resource.path(path).path('users').header(X_AUTH_TOKEN, token).entity(user).post(ClientResponse)
    }

    def getUser(String token, URI location) {
        resource.uri(location).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def listUsers(String token) {
        resource.path(path).path("users").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def listUsers(String token, offset, limit) {
        resource.path(path).path("users").queryParams(pageParams(offset, limit)).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getUserById(String token, String userId) {
        resource.path(path).path('users').path(userId).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUserByName(String token, String name) {
        resource.path(path).path('users').queryParam("name", name).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def updateUser(String token, String userId, user) {
        resource.path(path).path('users').path(userId).header(X_AUTH_TOKEN, token).entity(user).post(ClientResponse)
    }

    def addCredential(String token, String userId, credential) {
        resource.path(path).path('users').path(userId).path('OS-KSADM').path('credentials').entity(credential).header(X_AUTH_TOKEN, token).post(ClientResponse)
    }

    def deleteUser(String token, String userId) {
        resource.path(path).path('users').path(userId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def hardDeleteUser(String token, String userId) {
        resource.path(path).path('softDeleted').path('users').path(userId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def createGroup(String token, group) {
        resource.path(path).path(RAX_GRPADM).path('groups').header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(group).post(ClientResponse)
    }

    def getGroup(String token, URI uri) {
        resource.uri(uri).accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getGroups(String token) {
        resource.path(path).path(RAX_GRPADM).path('groups').accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def updateGroup(String token, String groupId, group) {
        resource.path(path).path(RAX_GRPADM).path('groups').path(groupId).header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(group).put(ClientResponse)
    }

    def deleteGroup(String  token, String groupId) {
        resource.path(path).path(RAX_GRPADM).path('groups').path(groupId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addUserToGroup(String token, String groupId, String userId) {
        resource.path(path).path(RAX_GRPADM).path('groups').path(groupId).path("users").path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).put(ClientResponse)
    }
    def removeUserFromGroup(String token, String groupId, String userId) {
        resource.path(path).path(RAX_GRPADM).path('groups').path(groupId).path("users").path(userId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def listGroupsForUser(String token, String userId) {
        resource.path(path).path('users').path(userId).path("RAX-KSGRP").accept(APPLICATION_XML).header(X_AUTH_TOKEN, token).get(ClientResponse)
    }

    def getUsersFromGroup(String token, String groupId) {
        resource.path(path).path(RAX_GRPADM).path('groups').path(groupId).path("users").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def authenticate(username, password) {
        resource.path(path + 'tokens').accept(APPLICATION_XML).entity(authenticateRequest(username, password)).post(ClientResponse)
    }

    def createRegion(String token, region) {
        resource.path(path).path(RAX_AUTH).path("regions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(region).post(ClientResponse)
    }

    def getRegion(String token, String regionId) {
        resource.path(path).path(RAX_AUTH).path("regions").path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def getRegions(String token) {
        resource.path(path).path(RAX_AUTH).path("regions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def updateRegion(String token, String regionId, region) {
        resource.path(path).path(RAX_AUTH).path("regions").path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(region).put(ClientResponse)
    }

    def deleteRegion(String token, String regionId) {
        resource.path(path).path(RAX_AUTH).path("regions").path(regionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def listUsersWithRole(String token, String roleId) {
        resource.path(path).path("OS-KSADM/roles").path(roleId).path("RAX-AUTH/users").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def listUsersWithRole(String token, String roleId, int offset, int limit) {
        resource.path(path).path("OS-KSADM/roles").path(roleId).path("RAX-AUTH/users").queryParams(pageParams(offset, limit)).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createRole(String token, Role role) {
        resource.path(path).path("OS-KSADM/roles").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).entity(role).post(ClientResponse)
    }

    def deleteRole(String token, String roleId) {
        resource.path(path).path("OS-KSADM/roles").path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addApplicationRoleToUser(String token, String roleId, String userId) {
        resource.path(path).path("users").path(userId).path("roles/OS-KSADM").path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).put(ClientResponse)
    }

    def deleteApplicationRoleFromUser(String token, String roleId, String userId) {
        resource.path(path).path("users").path(userId).path("roles/OS-KSADM").path(roleId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def addRoleToUserOnTenant(String token, String tenantId, String userId, String roleId) {
        resource.path(path).path("tenants").path(tenantId).path("users").path(userId)
                .path("roles").path("OS-KSADM").path(roleId)
                .header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).put(ClientResponse)
    }

    def deleteRoleFromUserOnTenant(String token, String tenantId, String userId, String roleId) {
        resource.path(path).path("tenants").path(tenantId).path("users").path(userId)
                .path("roles").path("OS-KSADM").path(roleId)
                .header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def removeRoleFromUser(String token, String roleId, String userId) {
        resource.path(path).path("users").path(userId).path("roles/OS-KSADM").path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete()
    }

    def createQuestion(String token, question) {
        resource.path(path).path(RAX_AUTH).path("secretqa/questions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(question).post(ClientResponse)
    }

    def getQuestion(String token, questionId) {
        resource.path(path).path(RAX_AUTH).path("secretqa/questions").path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def getQuestionFromLocation(String token, location) {
        resource.uri(location).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def getQuestions(String token) {
        resource.path(path).path(RAX_AUTH).path("secretqa/questions").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).get(ClientResponse)
    }

    def updateQuestion(String token, String questionId, question) {
        resource.path(path).path(RAX_AUTH).path("secretqa/questions").path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(question).put(ClientResponse)
    }

    def deleteQuestion(String token, String questionId) {
        resource.path(path).path(RAX_AUTH).path("secretqa/questions").path(questionId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).delete(ClientResponse)
    }

    def addEndpointTemplate(String token, endpointTemplate) {
        resource.path(path).path(OS_KSCATALOG).path("endpointTemplates").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(endpointTemplate).post(ClientResponse)
    }

    def deleteEndpointTemplate(String token, endpointTemplateId) {
        resource.path(path).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addPolicy(String token, policy) {
        resource.path(path).path(RAX_AUTH).path("policies").header(X_AUTH_TOKEN, token).type(APPLICATION_XML).accept(APPLICATION_XML).entity(policy).post(ClientResponse)
    }

    def getPolicy(String token, location) {
        resource.uri(location).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def deletePolicy(String token, policyId) {
        resource.path(path).path(RAX_AUTH).path("policies").path(policyId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).delete(ClientResponse)
    }

    def addPolicyToEndpointTemplate(String token, endpointTemplateId, policyId) {
        resource.path(path).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).path(RAX_AUTH).path("policies").path(policyId).header(X_AUTH_TOKEN, token).put(ClientResponse)
    }

    def deletePolicyToEndpointTemplate(String token, endpointTemplateId, policyId) {
        resource.path(path).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).path(RAX_AUTH).path("policies").path(policyId).header(X_AUTH_TOKEN, token).delete(ClientResponse)
    }

    def getPoliciesFromEndpointTemplate(String token, endpointTemplateId) {
        resource.path(path).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).path(RAX_AUTH).path("policies").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def updatePoliciesForEndpointTemplate(String token, endpointTemplateId, policies) {
        resource.path(path).path(OS_KSCATALOG).path("endpointTemplates").path(endpointTemplateId).path(RAX_AUTH).path("policies").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(policies).put(ClientResponse)
    }

    def addTenant(String token, Tenant tenant) {
        resource.path(path).path("tenants").header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(tenant).post(ClientResponse)
    }

    def getSecretQA(String token, String userId){
        resource.path(path).path('users').path(userId).path(RAX_AUTH).path('secretqas').header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    def createSecretQA(String token, String userId, secretqa){
        resource.path(path).path('users').path(userId).path(RAX_AUTH).path('secretqas').header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).type(APPLICATION_XML).entity(secretqa).post(ClientResponse)
    }

    def getRole(String token, String roleId) {
        resource.path(path).path('OSKD-ADM/roles').path(roleId).header(X_AUTH_TOKEN, token).accept(APPLICATION_XML).get(ClientResponse)
    }

    //Helper Methods
    def getCredentials(String username, String password) {
        new PasswordCredentialsRequiredUsername().with {
            it.username = username
            it.password = password
            return it
        }
    }

    def authenticateRequest(String username, String password) {
        def credentials = getCredentials(username, password)

        def objectFactory = new ObjectFactory()

        new AuthenticationRequest().with {
            it.setCredential(objectFactory.createPasswordCredentials(credentials))
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

    def pageParams(offset, limit) {
        new MultivaluedMapImpl().with {
            it.add("marker", "$offset")
            it.add("limit", "$limit")
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
}
