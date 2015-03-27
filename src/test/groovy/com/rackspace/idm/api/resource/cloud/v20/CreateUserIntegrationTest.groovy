package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.DefaultUserService
import groovy.json.JsonSlurper
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.RandomStringUtils
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

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
        try { cloud20.deleteUser(identityAdminToken, userEntity.id) } catch (Exception e) {}
        try { cloud20.deleteUser(identityAdminToken, v11UserEntity.id) } catch (Exception e) {}
        try { cloud20.deleteTenant(identityAdminToken, tenants.tenant[0].id) } catch (Exception e) {}
        try { cloud20.deleteTenant(identityAdminToken, tenants.tenant[1].id) } catch (Exception e) {}
        try { cloud20.deleteTenant(identityAdminToken, v11tenants.tenant[0].id) } catch (Exception e) {}
        try { cloud20.deleteTenant(identityAdminToken, v11tenants.tenant[1].id) } catch (Exception e) {}
        try { cloud20.deleteDomain(identityAdminToken, domainId) } catch (Exception e) {}
        try { cloud20.deleteDomain(identityAdminToken, v11MossoId as String) } catch (Exception e) {}
    }

    def "disabled default tenants are not assigned to a user when making the create user in one call"() {
        given:
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def publicUrl = testUtils.getRandomUUID("http://public/")
        def endpointTemplateResp = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), v1Factory.createEndpointTemplate(endpointTemplateId, "object-store", publicUrl, "name", false, "ORD")).getEntity(EndpointTemplate).value
        def endpointTemplateEntity = endpointService.getBaseUrlById(endpointTemplateId)
        endpointTemplateEntity.def = true
        endpointService.updateBaseUrl(endpointTemplateEntity)

        when: "create a user with the new endpoint disabled and auth as that user"
        def createdUser1 = utils.createUserWithTenants(utils.getIdentityAdminToken(), testUtils.getRandomUUID("user-admin"), utils.createDomain())
        def authUser1 = utils.authenticate(createdUser1)

        then: "the endpoint DOES NOT show up in the service catalog"
        authUser1.serviceCatalog.service.endpoint.flatten().publicURL.find({t -> t.startsWith(endpointTemplateEntity.publicUrl)}) == null

        when: "enable the endpoint and create a user"
        endpointTemplateEntity = endpointService.getBaseUrlById(endpointTemplateId)
        endpointTemplateEntity.enabled = true
        endpointService.updateBaseUrl(endpointTemplateEntity)
        def createdUser2 = utils.createUserWithTenants(utils.getIdentityAdminToken(), testUtils.getRandomUUID("user-admin"), utils.createDomain())
        def authUser2 = utils.authenticate(createdUser2)

        then: "the endpoint DOES show up in the service catalog"
        authUser2.serviceCatalog.service.endpoint.flatten().publicURL.find({t -> t.startsWith(endpointTemplateEntity.publicUrl)}) != null

        cleanup:
        utils.deleteUsers(createdUser1, createdUser2)
        utils.deleteEndpointTemplate(endpointTemplateResp)
    }

    @Unroll
    def "test feature flag for respecting enabled property on default base URLs when assigning them to a new user (CIDMDEV-1248): useEnabled = #useEnabledFlag"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_BASE_URL_RESPECT_ENABLED_FLAG, useEnabledFlag)
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def publicUrl = testUtils.getRandomUUID("http://public/")
        def endpointTemplateResp = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), v1Factory.createEndpointTemplate(endpointTemplateId, "object-store", publicUrl, "name", false, "ORD")).getEntity(EndpointTemplate).value
        def endpointTemplateEntity = endpointService.getBaseUrlById(endpointTemplateId)
        endpointTemplateEntity.def = true
        endpointService.updateBaseUrl(endpointTemplateEntity)

        when: "create a user with the base URL enabled attribute set to false"
        def createdUser = utils.createUserWithTenants(utils.getIdentityAdminToken(), testUtils.getRandomUUID("user-admin"), utils.createDomain())
        def authUser = utils.authenticate(createdUser)

        then:
        if(useEnabledFlag) {
            authUser.serviceCatalog.service.endpoint.flatten().publicURL.find({t -> t.startsWith(endpointTemplateEntity.publicUrl)}) == null
        } else {
            authUser.serviceCatalog.service.endpoint.flatten().publicURL.find({t -> t.startsWith(endpointTemplateEntity.publicUrl)}) != null
        }

        cleanup:
        utils.deleteUsers(createdUser)
        utils.deleteEndpointTemplate(endpointTemplateResp)
        staticIdmConfiguration.reset()

        where:
        useEnabledFlag  | _
        true            | _
        false           | _
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

    def "create identity admin with no domain creates a domain for the user"() {
        given:
        def username = "identityAdmin" + testUtils.getRandomUUID()

        when:
        def user = utils.createUser(utils.getServiceAdminToken(), username)

        then:
        user.domainId != null

        cleanup:
        utils.deleteUser(user)
        utils.deleteDomain(user.domainId)
    }

    def "create identity admin with a domain ID creates the domain if it does not exist"() {
        given:
        def username = "identityAdmin" + testUtils.getRandomUUID()
        def domainId = testUtils.getRandomUUID()

        when: "load the domain to verify that it does not exist"
        def getDomainResponse = cloud20.getDomain(utils.getServiceAdminToken(), domainId)

        then: "the domain does not exist"
        getDomainResponse.status == 404

        when: "create the user with the domain ID"
        def user = utils.createUser(utils.getServiceAdminToken(), username, domainId)

        then: "the domain was added to the user"
        user.domainId == domainId
        def loadedUser = utils.getUserById(user.id, utils.getServiceAdminToken())
        loadedUser.domainId == domainId

        when: "load the domain again to verify that it was created (not just added to the user)"
        def domain = utils.getDomain(domainId)

        then: "the domain was created"
        domain.id == domainId

        cleanup:
        utils.deleteUser(user)
        utils.deleteDomain(domainId)
    }

    @Unroll
    def "identity admin can create default user, accept = #accept, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def username1 = testUtils.getRandomUUID("defaultUser")
        def username2 = testUtils.getRandomUUID("defaultUser")
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        users = users.reverse()
        def defaultUserRoles = v2Factory.createRoleList([v2Factory.createRole(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP))].asList())

        when:
        def userForCreate = v2Factory.createUserForCreate(username1, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.roles = defaultUserRoles
            it
        }
        def identityAdminResponse = cloud20.createUser(utils.getIdentityAdminToken(), userForCreate, request, accept)

        then:
        identityAdminResponse.status == 201
        def userResponse1
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            userResponse1 = identityAdminResponse.getEntity(User).value
            assert userResponse1.domainId == domainId
        } else {
            userResponse1 = new JsonSlurper().parseText(identityAdminResponse.getEntity(String)).user
            assert userResponse1['RAX-AUTH:domainId'] == domainId
        }

        when:
        userForCreate.username = username2
        userForCreate.roles = null
        def userAdminResponse = cloud20.createUser(utils.getToken(userAdmin.username), userForCreate, request, accept)

        then:
        userAdminResponse.status == 201
        def userResponse2
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            userResponse2 = userAdminResponse.getEntity(User).value
            assert userResponse2.domainId == domainId
        } else {
            userResponse2 = new JsonSlurper().parseText(userAdminResponse.getEntity(String)).user
            assert userResponse2['RAX-AUTH:domainId'] == domainId
        }

        when:
        def auth1 = utils.authenticateUser(userResponse1.username)
        def auth2 = utils.authenticateUser(userResponse2.username)

        then:
        def serviceCatalog1 = auth1.serviceCatalog.service.endpoint.flatten().publicURL
        def serviceCatalog2 = auth2.serviceCatalog.service.endpoint.flatten().publicURL
        serviceCatalog1 == serviceCatalog2
        auth1.user.defaultRegion == auth2.user.defaultRegion

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), userResponse1.id)
        cloud20.deleteUser(utils.getServiceAdminToken(), userResponse2.id)
        utils.deleteUsers(users)

        where:
        accept                          | request
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    def "a domain must exist for a sub-user to be created in it by identity admin"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("defaultUser")
        def defaultUserRoles = v2Factory.createRoleList([v2Factory.createRole(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP))].asList())
        def userForCreate = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.roles = defaultUserRoles
            it
        }

        when:
        def response = cloud20.createUser(utils.getIdentityAdminToken(), userForCreate)

        then:
        response.status == 400
    }

    def "a default user cannot be added by identity admin under disabled domain"() {
        given:
        def domainId = utils.createDomain()
        def username1 = testUtils.getRandomUUID("defaultUser")
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        users = users.reverse()
        def defaultUserRoles = v2Factory.createRoleList([v2Factory.createRole(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP))].asList())
        def userForCreate = v2Factory.createUserForCreate(username1, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.roles = defaultUserRoles
            it
        }
        utils.disableDomain(domainId)

        when:
        def response = cloud20.createUser(utils.getIdentityAdminToken(), userForCreate)

        then:
        response.status == 400

        cleanup:
        utils.deleteUsers(users)
    }

    def "a default user cannot be added by identity admin in domain with disabled user-admin"() {
        given:
        def domainId = utils.createDomain()
        def username1 = testUtils.getRandomUUID("defaultUser")
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        users = users.reverse()
        def defaultUserRoles = v2Factory.createRoleList([v2Factory.createRole(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP))].asList())
        def userForCreate = v2Factory.createUserForCreate(username1, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.roles = defaultUserRoles
            it
        }
        utils.disableUser(userAdmin)

        when:
        def response = cloud20.createUser(utils.getIdentityAdminToken(), userForCreate)

        then:
        response.status == 400

        cleanup:
        utils.deleteUsers(users)
    }

    def "a default user CAN be added by identity admin in domain with a disabled user-admin IF there is also an enabled user-admin"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_DOMAIN_RESTRICTED_ONE_USER_ADMIN_PROP, false)
        def domainId = utils.createDomain()
        def username1 = testUtils.getRandomUUID("defaultUser")
        def userAdmin1, users1
        (userAdmin1, users1) = utils.createUserAdminWithTenants(domainId)
        def userAdmin2, users2
        (userAdmin2, users2) = utils.createUserAdmin(domainId)
        users1 = users1.reverse()
        def defaultUserRoles = v2Factory.createRoleList([v2Factory.createRole(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP))].asList())
        def userForCreate = v2Factory.createUserForCreate(username1, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.roles = defaultUserRoles
            it
        }
        utils.disableUser(userAdmin1)

        when:
        def response = cloud20.createUser(utils.getIdentityAdminToken(), userForCreate)

        then:
        response.status == 201

        cleanup:
        utils.deleteUser(response.getEntity(User).value)
        utils.deleteUser(userAdmin2)
        utils.deleteUsers(users1)
        utils.deleteUsers(users2[0])
        staticIdmConfiguration.reset()
    }

    def "test feature flag for identity admin creating sub-users"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("defaultUser")
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        users = users.reverse()
        def defaultUserRoles = v2Factory.createRoleList([v2Factory.createRole(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP))].asList())
        def userForCreate = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.roles = defaultUserRoles
            it
        }

        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_IDENTITY_ADMIN_CREATE_SUBUSER_ENABLED_PROP, false)
        def userResponse = cloud20.createUser(utils.getIdentityAdminToken(), userForCreate)

        then:
        userResponse.status == 403

        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_IDENTITY_ADMIN_CREATE_SUBUSER_ENABLED_PROP, true)
        userResponse = cloud20.createUser(utils.getIdentityAdminToken(), userForCreate)

        then:
        userResponse.status == 201

        cleanup:
        reloadableConfiguration.reset()
        def userId = userResponse.getEntity(User).value.id
        cloud20.deleteUser(utils.getServiceAdminToken(), userId)
        utils.deleteUsers(users)
    }

    @Unroll
    def "#userType setting contact ID on user on create, #userType, accept = #accept, request = #request"() {
        given:
        def domainId = utils.createDomain()
        def contactId = testUtils.getRandomUUID("contactId")
        def username = testUtils.getRandomUUID("defaultUser")
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]
        def userForCreate = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.contactId = contactId
            it
        }

        when: "create the user"
        def token
        switch(userType) {
            case IdentityUserTypeEnum.SERVICE_ADMIN:
                token = utils.getServiceAdminToken()
                break
            case IdentityUserTypeEnum.IDENTITY_ADMIN:
                token = utils.getIdentityAdminToken()
                break
            case IdentityUserTypeEnum.USER_ADMIN:
                token = utils.getToken(userAdmin.username)
                break
            case IdentityUserTypeEnum.USER_MANAGER:
                token = utils.getToken(userManage.username)
                break
            case IdentityUserTypeEnum.DEFAULT_USER:
                token = utils.getToken(defaultUser.username)
                break
        }
        def userResponse = cloud20.createUser(token, userForCreate, request, accept)

        then: "verify the response from creating the user"
        userResponse.status == result
        def user
        if(MediaType.APPLICATION_XML_TYPE == accept) {
            user = userResponse.getEntity(User).value
            if(attributeSet) {
                assert user.contactId == contactId
            } else {
                assert user.contactId == null
            }
        } else {
            user = new JsonSlurper().parseText(userResponse.getEntity(String)).user
            if(attributeSet) {
                assert user['RAX-AUTH:contactId'] == contactId
            } else {
                assert !user.hasProperty('RAX-AUTH:contactId')
            }
        }

        and: "verify the contact ID value in the directory"
        def userEntity = userService.getUserById(user.id)
        if(attributeSet) {
            assert userEntity.contactId == contactId
        } else {
            assert userEntity.contactId == null
        }

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), user.id)
        utils.deleteUsers(users)

        where:
        userType                            | result | attributeSet | accept                          | request
        IdentityUserTypeEnum.SERVICE_ADMIN  | 201    | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | 201    | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | 201    | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | 201    | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

        IdentityUserTypeEnum.IDENTITY_ADMIN | 201    | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | 201    | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | 201    | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | 201    | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

        IdentityUserTypeEnum.USER_ADMIN     | 201    | false        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_ADMIN     | 201    | false        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.USER_ADMIN     | 201    | false        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_ADMIN     | 201    | false        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

        IdentityUserTypeEnum.USER_MANAGER   | 201    | false        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_MANAGER   | 201    | false        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.USER_MANAGER   | 201    | false        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_MANAGER   | 201    | false        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
        //not testing default users, default users are not allowed to make this call
    }

}
