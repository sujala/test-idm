package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.DefaultUserService
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.RandomStringUtils
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.DEFAULT_PASSWORD

class CreateUserIntegrationTest extends RootIntegrationTest {

    static final String IDM_CLIENT_ID = "idm.clientId"
    static final String CLOUD_AUTH_CLIENT_ID = "cloudAuth.clientId"

    @Shared def identityAdminToken

    @Autowired def ScopeAccessService scopeAccessService
    @Autowired def TenantService tenantService
    @Autowired def UserService userService
    @Autowired def Configuration config
    @Autowired def EndpointService endpointService

    def setup() {
        identityAdminToken = utils.getIdentityAdminToken()
        staticIdmConfiguration.reset()
    }

    def "creating user with null enabled attribute creates an enabled user"() {
        given:
        def v20Username = "v20Username" + testUtils.getRandomUUID()
        def v20User = v2Factory.createUserForCreate(v20Username, "displayName", "testemail@rackspace.com", null, "ORD", testUtils.getRandomUUID(), "Password1")
        def randomMosso = 10000000 + new Random().nextInt(1000000)
        def v11Username = "v11Username" + testUtils.getRandomUUID()
        def v11User = v1Factory.createUser(v11Username, "1234567890", randomMosso, null, null)

        when: "creating the user in v2.0"
        cloud20.createUser(identityAdminToken, v20User)
        def v20CreatedUser = utils.getUserByName(v20Username)

        then: "the user is enabled"
        v20CreatedUser.enabled == true

        when: "creating the user in v1.1"
        cloud11.createUser(v11User)
        def v11CreatedUser = utils.getUserByName(v11Username)

        then: "the user is enabled"
        v11CreatedUser.enabled == true

        cleanup:
        utils.deleteUser(v20CreatedUser)
        utils.deleteUser(v11CreatedUser)
    }

    def "scope access is created when a user is created"() {
        given:
        def v20Username = "v20Username" + testUtils.getRandomUUID()
        def v20User = v2Factory.createUserForCreate(v20Username, "displayName", "testemail@rackspace.com", true, "ORD", testUtils.getRandomUUID(), "Password1")
        def randomMosso = 10000000 + new Random().nextInt(1000000)
        def v11Username = "v11Username" + testUtils.getRandomUUID()
        def v11User = v1Factory.createUser(v11Username, "1234567890", randomMosso, null, true)

        when:
        cloud20.createUser(identityAdminToken, v20User)
        def v20UserEntity = userService.getUser(v20Username)
        cloud11.createUser(v11User)
        def v11UserEntity = userService.getUser(v11Username)

        then:
        scopeAccessService.getScopeAccessesForUserByClientId(v20UserEntity, config.getString(IDM_CLIENT_ID)) != null
        scopeAccessService.getScopeAccessesForUserByClientId(v20UserEntity, config.getString(CLOUD_AUTH_CLIENT_ID)) != null
        scopeAccessService.getScopeAccessesForUserByClientId(v11UserEntity, config.getString(IDM_CLIENT_ID)) != null
        scopeAccessService.getScopeAccessesForUserByClientId(v11UserEntity, config.getString(CLOUD_AUTH_CLIENT_ID)) != null

        cleanup:
        cloud11.deleteUser(v11Username)
        cloud20.deleteUser(identityAdminToken, v20UserEntity.id)
    }

    def "tenants ARE NOT created for create user v2.0 calls"() {
        given:
        def username = "v20Username" + testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = v2Factory.createUserForCreate(username, "displayName", "testemail@rackspace.com", true, "ORD", domainId, "Password1")
        cloud20.createUser(identityAdminToken, user)
        def userEntity = userService.getUser(username)

        when:
        def tenantsResponse = cloud20.getDomainTenants(identityAdminToken, domainId)

        then:
        tenantsResponse.status == 404

        cleanup:
        cloud20.deleteUser(identityAdminToken, userEntity.id)
    }

    def "Only user-admin role is added for create user v2.0 calls when secretQA is not provided (ie not a create user in one call)"() {
        given:
        def username = "v20Username" + testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = v2Factory.createUserForCreate(username, "displayName", "testemail@rackspace.com", true, "ORD", domainId, "Password1")
        cloud20.createUser(identityAdminToken, user)
        def userEntity = userService.getUser(username)

        when:
        def roles = cloud20.listUserGlobalRoles(identityAdminToken, userEntity.id).getEntity(org.openstack.docs.identity.api.v2.RoleList).value

        then:
        roles.role.size == 1
        roles.role[0].name == "identity:user-admin"

        cleanup:
        cloud20.deleteUser(identityAdminToken, userEntity.id)
    }

    def "tenants ARE created for create user v2.0 calls when secretQA is populated and caller is identity:admin"() {
        given:
        def username = "v20Username" + testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = v2Factory.createUser(username, "displayName", "testemail@rackspace.com", true, "ORD", domainId, "Password1")
        def secretQA = v2Factory.createSecretQA("question", "answer")
        user.secretQA = secretQA
        cloud20.createUser(identityAdminToken, user)
        def userEntity = userService.getUser(username)

        when:
        def tenants = cloud20.getDomainTenants(identityAdminToken, domainId).getEntity(Tenants).value

        then:
        !tenants.tenant.isEmpty()

        cleanup:
        cloud20.deleteUser(identityAdminToken, userEntity.id)
        cloud20.deleteTenant(identityAdminToken, tenants.tenant[0].id)
        cloud20.deleteTenant(identityAdminToken, tenants.tenant[1].id)
        cloud20.deleteDomain(identityAdminToken, domainId)
    }

    /**
     * Pick and set a random prefix on the expected property value. Per nast tenant naming convention we know the name
     * should be prefix+domainId (e.g. MossoFS_12345 where "MossoFS_" is the prefix, and 12345 is the domain
     *
     * @return
     */
    def "NAST tenant for v2.0 user call is prefixed with the value of the configuration property nast.tenant.prefix"() {
        def randomPrefix = RandomStringUtils.randomAscii(10)
        staticIdmConfiguration.setProperty(DefaultUserService.NAST_TENANT_PREFIX_PROP_NAME, randomPrefix)

        when: "create user in v20 one user call"
        def username = "v20Username" + testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = v2Factory.createUser(username, "displayName", "testemail@rackspace.com", true, "ORD", domainId, "Password1")
        def secretQA = v2Factory.createSecretQA("question", "answer")
        user.secretQA = secretQA
        cloud20.createUser(identityAdminToken, user)
        def userEntity = userService.getUser(username)
        Tenants tenants = cloud20.getDomainTenants(identityAdminToken, domainId).getEntity(Tenants).value

        then: "nast tenant is prefixed with property value"
        !tenants.tenant.isEmpty()
        tenants.tenant.find({t -> t.id.equals(randomPrefix+domainId) && userEntity.getNastId() == t.id}) != null

        when: "Create user in v11"
        def v11username = "v11Username" + testUtils.getRandomUUID()
        def v11MossoId = testUtils.getRandomInteger()
        def v11user = v1Factory.createUser(v11username, "apiKey", v11MossoId)
        cloud11.createUser(v11user)
        def v11UserEntity = userService.getUser(v11username)
        Tenants v11tenants = cloud20.getDomainTenants(identityAdminToken, v11MossoId as String).getEntity(Tenants).value

        then: "nast tenant is prefixed with property value"
        !v11tenants.tenant.isEmpty()
        v11tenants.tenant.find({t -> t.id.equals(randomPrefix+(v11MossoId as String)) && v11UserEntity.getNastId() == t.id}) != null

        cleanup:
        cloud20.deleteUser(identityAdminToken, userEntity.id)
        cloud20.deleteUser(identityAdminToken, v11UserEntity.id)
        cloud20.deleteTenant(identityAdminToken, tenants.tenant[0].id)
        cloud20.deleteTenant(identityAdminToken, tenants.tenant[1].id)
        cloud20.deleteTenant(identityAdminToken, v11tenants.tenant[0].id)
        cloud20.deleteTenant(identityAdminToken, v11tenants.tenant[1].id)
        cloud20.deleteDomain(identityAdminToken, domainId)
        cloud20.deleteDomain(identityAdminToken, v11MossoId as String)
    }


    def "tenants ARE NOT created for create user v2.0 calls when secretQA NOT populated and caller is identity:admin"() {
        given:
        def username = "v20Username" + testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = v2Factory.createUser(username, "displayName", "testemail@rackspace.com", true, "ORD", domainId, "Password1")
        cloud20.createUser(identityAdminToken, user)
        def userEntity = userService.getUser(username)

        when:
        def tenantsResponse = cloud20.getDomainTenants(identityAdminToken, domainId)

        then:
        tenantsResponse.status == 404

        cleanup:
        cloud20.deleteUser(identityAdminToken, userEntity.id)
        cloud20.deleteDomain(identityAdminToken, domainId)
    }

    def "BadRequestException is thrown when secretQA is populated and caller is identity:admin and domainId is NOT an integer"() {
        given:
        def username = "v20Username" + testUtils.getRandomUUID()
        def domainId = utils.createDomain() + "letters"
        def user = v2Factory.createUser(username, "displayName", "testemail@rackspace.com", true, "ORD", domainId, "Password1")
        def secretQA = v2Factory.createSecretQA("question", "answer")
        user.secretQA = secretQA

        when:
        def response = cloud20.createUser(identityAdminToken, user)

        then:
        response.status == 400

        cleanup:
        cloud20.deleteDomain(identityAdminToken, domainId)
    }

    def "tenants ARE created for create user v1.1 calls"() {
        given:
        def username = "v20Username" + testUtils.getRandomUUID()
        def randomMosso = 10000000 + new Random().nextInt(1000000)
        def v11User = v1Factory.createUser(username, "1234567890", randomMosso, null, true)
        cloud11.createUser(v11User)
        def userEntity = userService.getUser(username)

        when:
        def tenants = cloud20.getDomainTenants(identityAdminToken, userEntity.domainId).getEntity(Tenants).value

        then:
        !tenants.tenant.isEmpty()

        cleanup:
        cloud11.deleteUser(username)
    }

    def "Allow more than one userAdmin per domain" () {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def adminToken  = utils.getToken(identityAdmin.username)

        when:
        def user = v2Factory.createUserForCreate(testUtils.getRandomUUID(), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD)
        def userAdmin2Response = cloud20.createUser(adminToken, user)

        then:
        userAdmin2Response.status == 201

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteUser(userAdmin2Response.getEntity(User).value)
        utils.deleteDomain(domainId)
    }

    def "response from create user with secret QA returns secret QA"() {
        given:
        def username = "user" + testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def question = "What is the meaning?"
        def answer = "That is the wrong question"
        def group = utils.createGroup()
        def userRequest = v2Factory.createUserForCreate(username, username, "john.smith@example.org", true, "DFW", domainId,
                "securePassword2", ["rbacRole1"].asList(), [group.name].asList(), question, answer)

        when:
        def response = cloud20.createUser(identityAdminToken, userRequest)

        then:
        def user = response.getEntity(User).value
        user.secretQA.question.equals(question)
        user.secretQA.answer.equals(answer)

        cleanup:
        cloud20.deleteUser(identityAdminToken, user.id)
        utils.deleteGroup(group)
    }

    def "response from get user does not include secret QA"() {
        given:
        def username = "user" + testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def question = "What is the meaning?"
        def answer = "That is the wrong question"
        def group = utils.createGroup()
        def userRequest = v2Factory.createUserForCreate(username, username, "john.smith@example.org", true, "DFW", domainId,
                "securePassword2", ["rbacRole1"].asList(), [group.name].asList(), question, answer)
        cloud20.createUser(identityAdminToken, userRequest).getEntity(User).value

        when:
        def user = cloud20.getUserByName(identityAdminToken, username).getEntity(User).value

        then:
        user.secretQA == null

        cleanup:
        cloud20.deleteUser(identityAdminToken, user.id)
        utils.deleteGroup(group)
    }

    def "Subuser can be created in domain with 2 existing user-admins" () {
        given:
        def userAdmin2Region = "DFW"
        def userAdmin3Region = "ORD"
        def domainId = utils.createDomain()
        def allUsers
        def userAdmin
        (userAdmin, allUsers) = utils.createUserAdmin(domainId)
        def identityAdmin = allUsers.get(0)
        def identityAdminToken  = utils.getToken(identityAdmin.username)

        def userAdmin2 = v2Factory.createUserForCreate(testUtils.getRandomUUID(), "display", "email@email.com", true, userAdmin2Region, domainId, DEFAULT_PASSWORD)
        def userAdmin2Response = cloud20.createUser(identityAdminToken, userAdmin2)
        def userAdmin2Entity = userAdmin2Response.getEntity(User).value;
        assert userAdmin2Response.status == 201
        def userAdmin2Token  = utils.getToken(userAdmin2Entity.username)

        def userAdmin3 = v2Factory.createUserForCreate(testUtils.getRandomUUID(), "display", "email@email.com", true, userAdmin3Region, domainId, DEFAULT_PASSWORD)
        def userAdmin3Response = cloud20.createUser(identityAdminToken, userAdmin3)
        def userAdmin3Entity = userAdmin3Response.getEntity(User).value;
        assert userAdmin3Response.status == 201
        def userAdmin3Token  = utils.getToken(userAdmin3Entity.username)

        when: "create a default user from user admin 2"
        def defaultUser = v2Factory.createUserForCreate(testUtils.getRandomUUID(), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD)
        def defaultUserResponse = cloud20.createUser(userAdmin2Token, defaultUser)
        assert defaultUserResponse.status == 201
        def defaultUserEntity = defaultUserResponse.getEntity(User).value;

        then: "default user created with user admin 2 region"
        defaultUserEntity != null
        defaultUserEntity.getDefaultRegion() == userAdmin2Region

        when: "create a default user from user admin 3"
        def defaultUser3 = v2Factory.createUserForCreate(testUtils.getRandomUUID(), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD)
        def defaultUser3Response = cloud20.createUser(userAdmin3Token, defaultUser3)
        assert defaultUser3Response.status == 201
        def defaultUser3Entity = defaultUser3Response.getEntity(User).value;

        then: "default user created with user admin 3 region"
        defaultUser3Entity != null
        defaultUser3Entity.getDefaultRegion() == userAdmin3Region

        cleanup:
        utils.deleteUser(defaultUserEntity)
        utils.deleteUser(defaultUser3Entity)
        utils.deleteUser(userAdmin2Entity)
        utils.deleteUser(userAdmin3Entity)
        utils.deleteUsers(allUsers)
        utils.deleteDomain(domainId)
    }

    def "Subuser can be created in domain with 1 existing user-admin" () {
        given:
        def domainId = utils.createDomain()
        def allUsers
        def userAdmin
        (userAdmin, allUsers) = utils.createUserAdmin(domainId)
        def userAdminToken  = utils.getToken(userAdmin.username)

        when: "create a default user"
        def defaultUser = v2Factory.createUserForCreate(testUtils.getRandomUUID(), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD)
        def defaultUserResponse = cloud20.createUser(userAdminToken, defaultUser)
        assert defaultUserResponse.status == 201

        then: "default user created"
        defaultUser != null

        cleanup:
        utils.deleteUser(defaultUserResponse.getEntity(User).value)
        utils.deleteUsers(allUsers)
        utils.deleteDomain(domainId)
    }

    def "when creating a user WITH a given password the response DOES NOT include the password given"() {
        given:
        def username = "user" + testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def group = utils.createGroup()
        def password = "securePassword2"
        def userRequest = v2Factory.createUserForCreate(username, username, "john.smith@example.org", true, "DFW", domainId,
                password, ["rbacRole1"].asList(), [group.name].asList(), "What is the meaning?", "That is the wrong question")

        when:
        def user = cloud20.createUser(identityAdminToken, userRequest).getEntity(User).value

        then:
        user.password == null

        cleanup:
        cloud20.deleteUser(identityAdminToken, user.id)
        utils.deleteGroup(group)
    }

    def "when creating a user WITHOUT a given password the response DOES include the password given"() {
        given:
        def username = "user" + testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def group = utils.createGroup()
        def userRequest = v2Factory.createUserForCreate(username, username, "john.smith@example.org", true, "DFW", domainId,
                null, ["rbacRole1"].asList(), [group.name].asList(), "What is the meaning?", "That is the wrong question")

        when:
        def user = cloud20.createUser(identityAdminToken, userRequest).getEntity(User).value

        then:
        user.password != null

        cleanup:
        cloud20.deleteUser(identityAdminToken, user.id)
        utils.deleteGroup(group)
    }

    @Unroll
    def "test feature flag for adding expired tokens on user create: addTokens = #addTokens"() {
        given:
        staticIdmConfiguration.setProperty(DefaultUserService.ADD_EXPIRED_TOKENS_ON_USER_CREATE_FEATURE_FLAG, addTokens)
        def domainId = utils.createDomain()
        def username = "user" + testUtils.getRandomUUID()

        when:
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)

        then:
        def userTokens = scopeAccessService.getScopeAccessListByUserId(user.id)
        if(addTokens) {
            assert userTokens.iterator().hasNext()
        } else {
            assert !userTokens.iterator().hasNext()
        }

        cleanup:
        utils.deleteUser(user)

        where:
        addTokens   | _
        true        | _
        false       | _
    }

}
