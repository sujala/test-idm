package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.FactorTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserMultiFactorEnforcementLevelEnum
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.DomainDao
import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.DefaultUserService
import groovy.json.JsonSlurper
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.RoleList
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.IdmAssert

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.DEFAULT_PASSWORD

class CreateUserIntegrationTest extends RootIntegrationTest {

    @Shared def identityAdminToken

    @Autowired def ScopeAccessService scopeAccessService
    @Autowired def TenantService tenantService
    @Autowired def UserService userService
    @Autowired def Configuration config
    @Autowired def EndpointService endpointService
    @Autowired def DomainDao domainDao

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
        staticIdmConfiguration.setProperty(IdentityConfig.NAST_TENANT_PREFIX_PROP, randomPrefix)

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
        try { tenants.tenant.find({t -> t.id.equals(randomPrefix+domainId) && userEntity.getNastId() == t.id}) != null } catch (Exception e) {}

        when: "Create user in v11"
        def v11username = "v11Username" + testUtils.getRandomUUID()
        def v11MossoId = testUtils.getRandomInteger()
        def v11user = v1Factory.createUser(v11username, "apiKey", v11MossoId)
        cloud11.createUser(v11user)
        def v11UserEntity = userService.getUser(v11username)
        Tenants v11tenants = cloud20.getDomainTenants(identityAdminToken, v11MossoId as String).getEntity(Tenants).value

        then: "nast tenant is prefixed with property value"
        !v11tenants.tenant.isEmpty()
        try { v11tenants.tenant.find({t -> t.id.equals(randomPrefix+(v11MossoId as String)) && v11UserEntity.getNastId() == t.id}) != null } catch (Exception e) {}

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
        def endpointTemplateResp = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), v1Factory.createEndpointTemplate(endpointTemplateId, "object-store", publicUrl, "cloudFiles", false, "ORD")).getEntity(EndpointTemplate).value
        def endpointTemplateEntity = endpointService.getBaseUrlById(endpointTemplateId)
        endpointTemplateEntity.def = true
        endpointService.updateBaseUrl(endpointTemplateEntity)

        when: "create a user with the new endpoint disabled and auth as that user"
        def user1DomainId = utils.createDomain()
        def createdUser1 = utils.createUserWithTenants(utils.getIdentityAdminToken(), testUtils.getRandomUUID("user-admin"), user1DomainId)
        def authUser1 = utils.authenticate(createdUser1)

        then: "the endpoint DOES NOT show up in the service catalog"
        authUser1.serviceCatalog.service.endpoint.flatten().publicURL.find({t -> t.startsWith(endpointTemplateEntity.publicUrl)}) == null

        when: "enable the endpoint and create a user"
        endpointTemplateEntity = endpointService.getBaseUrlById(endpointTemplateId)
        endpointTemplateEntity.enabled = true
        endpointService.updateBaseUrl(endpointTemplateEntity)
        def user2DomainId = utils.createDomain()
        def createdUser2 = utils.createUserWithTenants(utils.getIdentityAdminToken(), testUtils.getRandomUUID("user-admin"), user2DomainId)
        def authUser2 = utils.authenticate(createdUser2)

        then: "the endpoint DOES show up in the service catalog"
        authUser2.serviceCatalog.service.endpoint.flatten().publicURL.find({t -> t.startsWith(endpointTemplateEntity.publicUrl)}) != null

        cleanup:
        utils.deleteUsers(createdUser1, createdUser2)
        utils.deleteTenant(utils.getNastTenant(user1DomainId))
        utils.deleteTenant(utils.getNastTenant(user2DomainId))
        utils.disableAndDeleteEndpointTemplate(endpointTemplateId)
    }

    @Unroll
    def "test feature flag for respecting enabled property on default base URLs when assigning them to a new user (CIDMDEV-1248): useEnabled = #useEnabledFlag"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_BASE_URL_RESPECT_ENABLED_FLAG, useEnabledFlag)
        def endpointTemplateId = testUtils.getRandomInteger().toString()
        def publicUrl = testUtils.getRandomUUID("http://public/")
        def endpointTemplateResp = cloud20.addEndpointTemplate(utils.getServiceAdminToken(), v1Factory.createEndpointTemplate(endpointTemplateId, "object-store", publicUrl, "cloudFiles", false, "ORD")).getEntity(EndpointTemplate).value
        def endpointTemplateEntity = endpointService.getBaseUrlById(endpointTemplateId)
        endpointTemplateEntity.def = true
        endpointService.updateBaseUrl(endpointTemplateEntity)

        when: "create a user with the base URL enabled attribute set to false"
        def domainId = utils.createDomain()
        def createdUser = utils.createUserWithTenants(utils.getIdentityAdminToken(), testUtils.getRandomUUID("user-admin"), domainId)
        def authUser = utils.authenticate(createdUser)

        then:
        if(useEnabledFlag) {
            authUser.serviceCatalog.service.endpoint.flatten().publicURL.find({t -> t.startsWith(endpointTemplateEntity.publicUrl)}) == null
        } else {
            authUser.serviceCatalog.service.endpoint.flatten().publicURL.find({t -> t.startsWith(endpointTemplateEntity.publicUrl)}) != null
        }

        cleanup:
        utils.deleteUsers(createdUser)
        utils.deleteTenant(utils.getNastTenant(domainId))
        utils.disableAndDeleteEndpointTemplate(endpointTemplateId)
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
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RESTRICT_CREATE_USER_IN_DOMAIN_WITH_USERS_PROP, false)
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
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RESTRICT_CREATE_USER_IN_DOMAIN_WITH_USERS_PROP, false)
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
    @Ignore
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
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RESTRICT_CREATE_USER_IN_DOMAIN_WITH_USERS_PROP, false)
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

    @Unroll
    def "#userType setting contact ID on user on create, #userType, accept = #accept, request = #request"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RESTRICT_CREATE_USER_IN_DOMAIN_WITH_USERS_PROP, false)
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

    @Unroll
    def "create a user admin and sub-users using a non-numeric domain, accept = #accept, request = #request"() {
        given:
        def domainId = testUtils.getRandomUUID("domain")
        def userAdminUsername = testUtils.getRandomUUID("userAdmin")
        def defaultUserUsername = testUtils.getRandomUUID("defaultUser")
        def userManagerUsername = testUtils.getRandomUUID("userManager")
        def propRole = utils.createPropagatingRole()
        def nonPropRole = utils.createRole();
        def defaultUserRoles = v2Factory.createRoleList([v2Factory.createRole(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP)), nonPropRole].asList())
        def userManageUserRoles = v2Factory.createRoleList([v2Factory.createRole(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP)), v2Factory.createRole(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_USER_MANAGE_ROLE_NAME_PROP)), nonPropRole].asList())
        def userAdminForCreate = v2Factory.createUserForCreate(userAdminUsername, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.secretQA = v1Factory.createRaxKsQaSecretQA()
            it.roles = v2Factory.createRoleList([].asList() << propRole)
            it
        }
        def defaultUserForCreate = v2Factory.createUserForCreate(defaultUserUsername, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.secretQA = v1Factory.createRaxKsQaSecretQA()
            it.roles = defaultUserRoles
            it
        }
        def userManageForCreate = v2Factory.createUserForCreate(userManagerUsername, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.secretQA = v1Factory.createRaxKsQaSecretQA()
            it.roles = userManageUserRoles
            it
        }

        when: "create the user admin"
        def userAdminResponse = cloud20.createUser(utils.getIdentityAdminToken(), userAdminForCreate, request, accept)

        then:
        userAdminResponse.status == 201
        def userAdmin
        if(MediaType.APPLICATION_XML_TYPE == accept) {
            userAdmin = userAdminResponse.getEntity(User).value
            assert userAdmin.roles.role.name.contains(propRole.name)
            assert userAdmin.domainId == domainId
        } else {
            userAdmin = new JsonSlurper().parseText(userAdminResponse.getEntity(String)).user
            assert userAdmin['RAX-AUTH:domainId'] == domainId
            assert userAdmin.roles.name.contains(propRole.name)
            assert userAdmin.roles.name.contains(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_USER_ADMIN_ROLE_NAME_PROP))
        }

        when: "create the default user"
        def subUserResponse = cloud20.createUser(utils.getIdentityAdminToken(), defaultUserForCreate, request, accept)

        then:
        subUserResponse.status == 201
        def subUser
        if(MediaType.APPLICATION_XML_TYPE == accept) {
            subUser = subUserResponse.getEntity(User).value
            assert subUser.roles.role.name.contains(propRole.name)
            assert subUser.roles.role.name.contains(nonPropRole.name)
            assert subUser.roles.role.name.contains(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP))
            assert subUser.domainId == domainId
        } else {
            subUser = new JsonSlurper().parseText(subUserResponse.getEntity(String)).user
            assert subUser['RAX-AUTH:domainId'] == domainId
            assert subUser.roles.name.contains(propRole.name)
            assert subUser.roles.name.contains(nonPropRole.name)
            assert subUser.roles.name.contains(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP))
        }

        when: "create the user manager"
        def userManagerResponse = cloud20.createUser(utils.getIdentityAdminToken(), userManageForCreate, request, accept)

        then:
        userManagerResponse.status == 201
        def userManager
        if(MediaType.APPLICATION_XML_TYPE == accept) {
            userManager = userManagerResponse.getEntity(User).value
            assert userManager.roles.role.name.contains(propRole.name)
            assert userManager.roles.role.name.contains(nonPropRole.name)
            assert userManager.roles.role.name.contains(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP))
            assert userManager.roles.role.name.contains(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_USER_MANAGE_ROLE_NAME_PROP))
            assert userManager.domainId == domainId
        } else {
            userManager = new JsonSlurper().parseText(userManagerResponse.getEntity(String)).user
            assert userManager.roles.name.contains(propRole.name)
            assert userManager.roles.name.contains(nonPropRole.name)
            assert userManager.roles.name.contains(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP))
            assert userManager.roles.name.contains(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_USER_MANAGE_ROLE_NAME_PROP))
            assert userManager['RAX-AUTH:domainId'] == domainId
        }

        cleanup:
        utils.deleteUserQuietly(subUser)
        utils.deleteUserQuietly(userManager)
        utils.deleteUserQuietly(userAdmin)
        utils.deleteRoleQuietly(propRole)
        utils.deleteRoleQuietly(nonPropRole)

        where:
        accept                          | request
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    def "error returned when identity admin creating sub-user when specifying group"() {
        given:
        def domainId = testUtils.getRandomUUID("domain")
        def group = utils.createGroup()
        def userAdminUsername = testUtils.getRandomUUID("userAdmin")
        def defaultUserUsername = testUtils.getRandomUUID("defaultUser")
        def defaultUserRoles = v2Factory.createRoleList([v2Factory.createRole(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP))].asList())
        def userAdminForCreate = v2Factory.createUserForCreate(userAdminUsername, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.secretQA = v1Factory.createRaxKsQaSecretQA()
            it.groups = new Groups().with {
                it.group = [group].asList()
                it
            }
            it
        }
        def defaultUserForCreate = v2Factory.createUserForCreate(defaultUserUsername, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.secretQA = v1Factory.createRaxKsQaSecretQA()
            it.roles = defaultUserRoles
            it.groups = new Groups().with {
                it.group = [group].asList()
                it
            }
            it
        }

        when: "create the user admin"
        def userAdminResponse = cloud20.createUser(utils.getIdentityAdminToken(), userAdminForCreate, request, accept)

        then:
        userAdminResponse.status == 201
        def userAdmin
        if(MediaType.APPLICATION_XML_TYPE == accept) {
            userAdmin = userAdminResponse.getEntity(User).value
            assert userAdmin.domainId == domainId
            assert userAdmin.groups.group.name.contains(group.name)
            assert userAdmin.roles.role.name.contains(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_USER_ADMIN_ROLE_NAME_PROP))
        } else {
            userAdmin = new JsonSlurper().parseText(userAdminResponse.getEntity(String)).user
            assert userAdmin['RAX-AUTH:domainId'] == domainId
            assert userAdmin[JSONConstants.RAX_KSGRP_GROUPS].name.contains(group.name)
            assert userAdmin.roles.name.contains(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_USER_ADMIN_ROLE_NAME_PROP))
        }

        when: "create the default user"
        def subUserResponse = cloud20.createUser(utils.getIdentityAdminToken(), defaultUserForCreate, request, accept)

        then:
        subUserResponse.status == 400

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), userAdmin.id)
        utils.deleteGroup(group)

        where:
        accept                          | request
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "tenants MUST exist for roles when creating sub-users"() {
        given:
        def domainId = testUtils.getRandomUUID("domain")
        def tenantId = testUtils.getRandomUUID("tenantIdForSubUser")
        def role = utils.createRole();
        def defaultUserUsername = testUtils.getRandomUUID("defaultUser")
        def defaultUserRoles = v2Factory.createRoleList([v2Factory.createRole(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP))].asList())
        defaultUserRoles.role << v2Factory.createRole(role.name).with {
            it.tenantId = tenantId
            it
        }
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        users = users.reverse()
        def defaultUserForCreate = v2Factory.createUserForCreate(defaultUserUsername, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.roles = defaultUserRoles
            it
        }

        when: "try to create the default user w/o creating the tenant first"
        def createResponse1 = cloud20.createUser(utils.getIdentityAdminToken(), defaultUserForCreate)

        then: "error"
        createResponse1.status == 400
        createResponse1.getEntity(BadRequestFault).value.message == String.format(DefaultUserService.ERROR_MSG_TENANT_DOES_NOT_EXIST, tenantId)

        when: "create the tenant and try again"
        def tenant = utils.createTenant()
        defaultUserRoles.role[1].tenantId = tenant.id
        def createResponse2 = cloud20.createUser(utils.getIdentityAdminToken(), defaultUserForCreate)

        then: "success"
        createResponse2.status == 201
        def defaultUser = createResponse2.getEntity(User).value
        defaultUser.roles.role.tenantId.contains(tenant.id)

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), defaultUser.id)
        utils.deleteUsers(users)
        utils.deleteRole(role)
        utils.deleteTenant(tenant)
    }

    def "service admins cannot create identity admins with roles"() {
        given:
        def role = utils.createRole()
        def userRoles = v2Factory.createRoleList([v2Factory.createRole(Constants.IDENTITY_PROVIDER_MANAGER_ROLE_NAME)].asList())
        def identityAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("idmAdmin"), "display", "email@email.com", true, null, null, DEFAULT_PASSWORD).with {
            it.roles = userRoles
            it
        }

        when: "try to create the identity admin with an identity access role"
        def response = cloud20.createUser(utils.getServiceAdminToken(), identityAdminToCreate)

        then: "403 not authorized"
        response.status == 403

        when: "try to create the identity admin with an NON-identity access role"
        identityAdminToCreate.roles = v2Factory.createRoleList([v2Factory.createRole(role.name)].asList())
        response = cloud20.createUser(utils.getServiceAdminToken(), identityAdminToCreate)

        then: "403 not authorized"
        response.status == 403
    }

    def "service admins cannot create identity admins with groups"() {
        given:
        def group = utils.createGroup()
        def identityAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("idmAdmin"), "display", "email@email.com", true, null, null, DEFAULT_PASSWORD).with {
            it.groups = v2Factory.createGroups([group].asList())
            it
        }

        when: "try to create the identity admin with a group"
        def response = cloud20.createUser(utils.getServiceAdminToken(), identityAdminToCreate)

        then: "403 not authorized"
        response.status == 403
    }

    def "users cannot be created with multifactorEnabled"() {
        given:
        def domainId = utils.createDomain()
        def identityAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("idmAdmin"), "display", "email@email.com", true, null, null, DEFAULT_PASSWORD).with {
            it.multiFactorEnabled = true
            it
        }
        def userAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.multiFactorEnabled = true
            it
        }
        def defaultUserToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("defaultUser"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.multiFactorEnabled = true
            it
        }

        when: "try to create the identity admin with mfa enabled"
        def response = cloud20.createUser(utils.getServiceAdminToken(), identityAdminToCreate)

        then: "identity admin created but multifactor not enabled"
        response.status == 201
        def idmAdmin = response.getEntity(User).value
        idmAdmin.multiFactorEnabled == false

        when: "now verify that the user can authenticate (would be prevented if mfa was enabled)"
        def authResponse = cloud20.authenticate(idmAdmin.username, DEFAULT_PASSWORD)

        then: "success"
        authResponse.status == 200

        when: "try to create the user admin with mfa enabled"
        response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate)

        then: "user admin created but multifactor not enabled"
        response.status == 201
        def userAdmin = response.getEntity(User).value
        userAdmin.multiFactorEnabled == false

        when: "now verify that the user can authenticate (would be prevented if mfa was enabled)"
        authResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)

        then: "success"
        authResponse.status == 200

        when: "try to create the default user with mfa enabled"
        response = cloud20.createUser(utils.getToken(userAdmin.username), defaultUserToCreate)

        then: "default user created but multifactor not enabled"
        response.status == 201
        def defaultUser = response.getEntity(User).value
        defaultUser.multiFactorEnabled == false

        when: "now verify that the user can authenticate (would be prevented if mfa was enabled)"
        authResponse = cloud20.authenticate(defaultUser.username, DEFAULT_PASSWORD)

        then: "success"
        authResponse.status == 200

        cleanup:
        utils.deleteUsersQuietly([defaultUser, userAdmin, idmAdmin] as List<User>)
    }

    def "users cannot be created with multiFactorEnforcementLevel"() {
        given:
        def domainId = utils.createDomain()
        def identityAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("idmAdmin"), "display", "email@email.com", true, null, null, DEFAULT_PASSWORD).with {
            it.userMultiFactorEnforcementLevel = UserMultiFactorEnforcementLevelEnum.REQUIRED
            it
        }
        def userAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.userMultiFactorEnforcementLevel = UserMultiFactorEnforcementLevelEnum.REQUIRED
            it
        }
        def defaultUserToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("defaultUser"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.userMultiFactorEnforcementLevel = UserMultiFactorEnforcementLevelEnum.REQUIRED
            it
        }

        when: "try to create the identity admin with mfa enforcement level"
        def response = cloud20.createUser(utils.getServiceAdminToken(), identityAdminToCreate)

        then: "identity admin created but mfa enforcement level not set"
        response.status == 201
        def idmAdmin = response.getEntity(User).value
        idmAdmin.userMultiFactorEnforcementLevel == null

        when: "now try to auth as the user, this would be prevented if the mfa enforcement level was actually set"
        def authResponse = cloud20.authenticate(idmAdmin.username, DEFAULT_PASSWORD)

        then: "success"
        authResponse.status == 200

        when: "try to create the user admin with mfa enforcement level"
        response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate)

        then: "user admin created but mfa enforcement level not set"
        response.status == 201
        def userAdmin = response.getEntity(User).value
        userAdmin.userMultiFactorEnforcementLevel == null

        when: "now try to auth as the user, this would be prevented if the mfa enforcement level was actually set"
        authResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)

        then: "success"
        authResponse.status == 200

        when: "try to create the defualt user with mfa enforcement level"
        response = cloud20.createUser(utils.getToken(userAdmin.username), defaultUserToCreate)

        then: "default user created but mfa enforcement level not set"
        response.status == 201
        def defaultUser = response.getEntity(User).value
        defaultUser.userMultiFactorEnforcementLevel == null

        when: "now try to auth as the user, this would be prevented if the mfa enforcement level was actually set"
        authResponse = cloud20.authenticate(defaultUser.username, DEFAULT_PASSWORD)

        then: "success"
        authResponse.status == 200

        cleanup:
        utils.deleteUsersQuietly([defaultUser, userAdmin, idmAdmin] as List<User>)
    }

    def "users cannot be created with factorType"() {
        given:
        def domainId = utils.createDomain()
        def identityAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("idmAdmin"), "display", "email@email.com", true, null, null, DEFAULT_PASSWORD).with {
            it.factorType = FactorTypeEnum.OTP
            it
        }
        def userAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.factorType = FactorTypeEnum.OTP
            it
        }
        def defaultUserToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("defaultUser"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.factorType = FactorTypeEnum.OTP
            it
        }

        when: "try to create the identity admin with factor type"
        def response = cloud20.createUser(utils.getServiceAdminToken(), identityAdminToCreate)

        then: "identity admin created but mfa enforcement level not set"
        response.status == 201
        def idmAdmin = response.getEntity(User).value
        idmAdmin.factorType == null

        when:
        def authResponse = cloud20.authenticate(idmAdmin.username, DEFAULT_PASSWORD)

        then: "success"
        authResponse.status == 200

        when: "try to create the user admin with mfa factor type"
        response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate)

        then: "user admin created but mfa enforcement level not set"
        response.status == 201
        def userAdmin = response.getEntity(User).value
        userAdmin.factorType == null

        when:
        authResponse = cloud20.authenticate(userAdmin.username, DEFAULT_PASSWORD)

        then: "success"
        authResponse.status == 200

        when: "try to create the defualt user with mfa factor type"
        response = cloud20.createUser(utils.getToken(userAdmin.username), defaultUserToCreate)

        then: "default user created but mfa enforcement level not set"
        response.status == 201
        def defaultUser = response.getEntity(User).value
        defaultUser.factorType == null

        when:
        authResponse = cloud20.authenticate(defaultUser.username, DEFAULT_PASSWORD)

        then: "success"
        authResponse.status == 200

        cleanup:
        utils.deleteUsersQuietly([defaultUser, userAdmin, idmAdmin] as List<User>)
    }

    def "create user without specifying enabled creates an enabled user"() {
        when:
        def identityAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("idmAdmin"), "display", "email@email.com", null, null, null, DEFAULT_PASSWORD)
        def response = cloud20.createUser(utils.getServiceAdminToken(), identityAdminToCreate)

        then:
        response.status == 201
        def idmAdmin = response.getEntity(User).value
        idmAdmin.enabled == true

        cleanup:
        utils.deleteUserQuietly(idmAdmin)
    }

    def "can create user admin in one call if mosso or nast tenants already exist"() {
        given:
        def domainId = utils.createDomain()
        def domain = v2Factory.createDomain(domainId, domainId)
        def userAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.secretQA = v2Factory.createSecretQA()
            it
        }
        def mossoTenantId = domainId
        def nastTenantId = utils.getNastTenant(domainId)

        when: "create the mosso tenant and try to create the user in one-call"
        utils.createDomain(domain)
        utils.createTenant(mossoTenantId, true, mossoTenantId, domainId)
        def response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate)
        def user = response.getEntity(User).value

        then: "User created"
        response.status == 201

        when: "delete the tenants and user, create the nast tenant, and try to create the user again"
        utils.deleteTenant(mossoTenantId)
        utils.deleteTenant(nastTenantId)
        utils.deleteUser(user)
        utils.createTenant(nastTenantId, true, nastTenantId, domainId)
        response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate)

        then: "User created"
        response.status == 201
    }

    @Unroll
    def "when creating a user the response contains the mfa enabled attribute: request = #request, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def userAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD)

        when:
        def response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate, request, accept)

        then:
        response.status == 201
        def userResponse
        if (accept == MediaType.APPLICATION_XML_TYPE) {
            userResponse = response.getEntity(User).value
            assert userResponse.multiFactorEnabled == false
        } else {
            userResponse = new JsonSlurper().parseText(response.getEntity(String))['user']
            assert userResponse[JSONConstants.RAX_AUTH_MULTI_FACTOR_ENABLED] == false
        }

        where:
        request | accept
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "Can not create a new account in the default domain: useOneCall = #useOneCall"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RESTRICT_CREATE_USER_IN_DEFAULT_DOMAIN_PROP, true)
        def domainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId()
        def userAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            if (useOneCall) {
                it.secretQA = v2Factory.createSecretQA()
            }
            it
        }

        when: "try to create the user in one-call"
        def response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate)

        then: "forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultUserService.ERROR_MSG_NEW_ACCOUNT_IN_DEFAULT_DOMAIN)

        where:
        useOneCall  | _
        true        | _
        false       | _
    }

    @Unroll
    def "Can not create a new account in a disabled domain: useOneCall = #useOneCall"() {
        given:
        def domainId = utils.createDomain()
        def domain = v2Factory.createDomain(domainId, domainId).with {
            it.enabled = false
            it
        }
        def userAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            if (useOneCall) {
                it.secretQA = v2Factory.createSecretQA()
            }
            it
        }

        when: "try to create the user in one-call"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RESTRICT_CREATE_USER_IN_DISABLED_DOMAIN_PROP, true)
        utils.createDomain(domain)
        def response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate)

        then: "forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultUserService.ERROR_MSG_NEW_ACCOUNT_IN_DISABLED_DOMAIN)

        when: "update feature flag, and try to create the user again"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RESTRICT_CREATE_USER_IN_DISABLED_DOMAIN_PROP, false)
        response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate)
        def user = response.getEntity(User).value

        then: "User created"
        response.status == 201

        cleanup:
        utils.deleteUser(user)
        utils.deleteDomain(domainId)

        where:
        useOneCall  | _
        true        | _
        false       | _
    }

    @Unroll
    def "Can not create a new account in an existing domain with users: useOneCall = #useOneCall"() {
        given:
        def domainId = utils.createDomain()
        def domain = v2Factory.createDomain(domainId, domainId)
        def userAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.secretQA = v2Factory.createSecretQA()
            it
        }
        def userAdminToCreate2 = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            if (useOneCall) {
                it.secretQA = v2Factory.createSecretQA()
            }
            it
        }

        when: "try to create the user in one-call"
        utils.createDomain(domain)
        def response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate)
        def user = response.getEntity(User).value

        then: "User created"
        response.status == 201

        when: "try to create the user again"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RESTRICT_CREATE_USER_IN_DOMAIN_WITH_USERS_PROP, true)
        response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate2)

        then: "forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultUserService.ERROR_MSG_NEW_ACCOUNT_IN_DOMAIN_WITH_USERS)

        when: "allow and try to create the user again"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RESTRICT_CREATE_USER_IN_DOMAIN_WITH_USERS_PROP, false)
        response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate2)
        def user2 = response.getEntity(User).value

        then: "user created"
        response.status == 201

        cleanup:
        utils.deleteUser(user)
        utils.deleteUser(user2)
        utils.deleteDomain(domainId)

        where:
        useOneCall  | _
        true        | _
        false       | _
    }

    def "Can not create new account with an an existing tenant in a different domain"() {
        given:
        def domainId = utils.createDomain()
        def domain = v2Factory.createDomain(domainId, domainId)
        def userAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.secretQA = v2Factory.createSecretQA()
            it
        }
        def mossoTenantId = domainId

        when: "create the mosso tenant and try to create the user in one-call"
        utils.createDomain(domain)
        utils.createTenant(mossoTenantId, true, mossoTenantId)
        def response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate)

        then: "forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, DefaultUserService.ERROR_MSG_NEW_ACCOUNT_EXISTING_TENANT_DIFFERENT_DOMAIN)

        cleanup:
        utils.deleteDomain(domainId)
        utils.deleteTenant(domainId)
    }

    def "Multiple tenants can be specified within a call. The specified tenants can be a mixture of new tenants and existing tenants"() {
        given:
        def domainId = utils.createDomain()
        def domain = v2Factory.createDomain(domainId, domainId)
        def mossoTenantId = domainId
        def nastTenantId = utils.getNastTenant(domainId)
        def otherTenantId = getRandomUUID("other")
        def otherRoleId = getRandomUUID("other")
        def mossoRole = v2Factory.createRole("compute:default ").with {
            it.tenantId = mossoTenantId
            it
        }
        def nastRole = v2Factory.createRole("object-store:default").with {
            it.tenantId = nastTenantId
            it
        }
        def otherRole = v2Factory.createRole(otherRoleId)
        def otherTenantRole = v2Factory.createRole(otherRoleId).with {
            it.tenantId = otherTenantId
            it
        }
        def userAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.secretQA = v2Factory.createSecretQA()
            it.roles = new RoleList().with {
                it.role.add(mossoRole)
                it.role.add(nastRole)
                it.role.add(otherTenantRole)
                it
            }
            it
        }

        when: "create the mosso and nast tenant and the user"
        utils.createDomain(domain)
        utils.createTenant(mossoTenantId, true, mossoTenantId, domainId)
        utils.createTenant(nastTenantId, true, nastTenantId, domainId)
        def createdRole = cloud20.createRole(getIdentityAdminToken(), otherRole).getEntity(Role).value

        def response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate)
        User user = response.getEntity(User).value
        def authResponse = cloud20.authenticate(user.username, DEFAULT_PASSWORD)
        AuthenticateResponse authenticateResponse = authResponse.getEntity(AuthenticateResponse).value

        then: "User created"
        response.status == 201

        then: "tenant got created"
        def role = authenticateResponse.user.roles.role.find {it.name.equals(otherRole.name)}
        assert role != null
        assert role.tenantId == otherTenantId

        cleanup:
        utils.deleteUser(user)
        utils.deleteDomain(domainId)
        utils.deleteRole(createdRole)
    }

    def "Create user with existing tenants adds nast and mosso roles and tenant contains endpoints"() {
        given:
        def domainId = utils.createDomain()
        def domain = v2Factory.createDomain(domainId, domainId)
        def userAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.secretQA = v2Factory.createSecretQA()
            it
        }
        def mossoTenantId = domainId
        def nastTenantId = utils.getNastTenant(domainId)

        when: "create the mosso and nast tenant and the user"
        utils.createDomain(domain)
        utils.createTenant(mossoTenantId, true, mossoTenantId, domainId)
        utils.createTenant(nastTenantId, true, nastTenantId, domainId)

        def response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate)
        User user = response.getEntity(User).value
        def authResponse = cloud20.authenticate(user.username, DEFAULT_PASSWORD)
        AuthenticateResponse authenticateResponse = authResponse.getEntity(AuthenticateResponse).value

        then: "User created"
        response.status == 201

        then: "The user must be assigned the object-store:default role on the nast tenant"
        def nastRole = authenticateResponse.user.roles.role.find {it.name.equals("object-store:default")}
        assert nastRole != null
        assert nastRole.tenantId == nastTenantId

        then: "The user must be assigned the compute:default role on the mosso tenant"
        def mossoRole = authenticateResponse.user.roles.role.find {it.name.equals("compute:default")}
        assert mossoRole != null
        assert mossoRole.tenantId == mossoTenantId

        then: "All endpoints must be assigned to these tenants as if the tenants did not already exist"
        assert authenticateResponse.serviceCatalog.service.findAll{it.endpoint.findAll{it.tenantId.equals(mossoTenantId)}}
        assert authenticateResponse.serviceCatalog.service.findAll{it.endpoint.findAll{it.tenantId.equals(nastTenantId)}}

        cleanup:
        utils.deleteUser(user)
        utils.deleteDomain(domainId)
    }

    @Unroll
    def "create cloud account tenant sets the tenant type on the tenant = featureEnabled = #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_SET_DEFAULT_TENANT_TYPE_ON_CREATION_PROP, featureEnabled)
        def domainId = utils.createDomain()
        def userAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@example.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.secretQA = v2Factory.createSecretQA()
            it
        }

        when:
        def response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate)

        then: "user created successfully"
        response.status == 201

        when:
        def mossoTenant = utils.getTenant(domainId)

        then:
        if (featureEnabled) {
            assert mossoTenant.types.type[0] == GlobalConstants.TENANT_TYPE_CLOUD
        } else {
            assert mossoTenant.types == null
        }

        when:
        def nastTenant = utils.getTenant(userService.getNastTenantId(domainId))

        then:
        if (featureEnabled) {
            assert nastTenant.types.type[0] == GlobalConstants.TENANT_TYPE_FILES
        } else {
            assert nastTenant.types == null
        }

        cleanup:
        reloadableConfiguration.reset()
        utils.deleteUser(utils.getUserByName(userAdminToCreate.username))
        utils.deleteDomain(domainId)

        where:
        featureEnabled << [true, false]
    }

    @Unroll
    def "create user with a cloud tenant sets the tenant type on the tenant = featureEnabled = #featureEnabled, tenantType = #tenantType"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_SET_DEFAULT_TENANT_TYPE_ON_CREATION_PROP, featureEnabled)
        def domainId = utils.createDomain()
        def tenantName;
        switch (tenantType) {
            case Constants.TENANT_TYPE_CLOUD :
                tenantName = RandomStringUtils.randomNumeric(6)
                break;
            case Constants.TENANT_TYPE_FILES :
                tenantName = identityConfig.getStaticConfig().getNastTenantPrefix() + RandomStringUtils.randomAlphabetic(8)
                break;
            case Constants.TENANT_TYPE_MANAGED_HOSTING :
                tenantName = GlobalConstants.MANAGED_HOSTING_TENANT_PREFIX + RandomStringUtils.randomAlphabetic(8)
                break;
            case [Constants.TENANT_TYPE_FAWS, Constants.TENANT_TYPE_RCN] :
                tenantName = tenantType + ':' + RandomStringUtils.randomAlphabetic(8)
                break;
        }
        def role = utils.createRole()
        def tenantRole = v2Factory.createRole(role.name).with {
            it.tenantId = tenantName
            it
        }
        def userAdminToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@example.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.roles = new RoleList().with {
                it.role.add(tenantRole)
                it
            }
            it
        }

        when:
        def response = cloud20.createUser(utils.getIdentityAdminToken(), userAdminToCreate)

        then: "user created successfully"
        response.status == 201

        when:
        def createdTenant = utils.getTenant(tenantName)

        then:
        if (featureEnabled) {
            assert createdTenant.types.type[0] == tenantType
        } else {
            assert createdTenant.types == null
        }

        cleanup:
        reloadableConfiguration.reset()
        utils.deleteUser(utils.getUserByName(userAdminToCreate.username))
        utils.deleteDomain(domainId)
        utils.deleteRole(role)
        utils.deleteTenant(tenantName)

        where:
        [tenantType, featureEnabled] << [[Constants.TENANT_TYPE_CLOUD,
                                          Constants.TENANT_TYPE_FILES,
                                          Constants.TENANT_TYPE_MANAGED_HOSTING,
                                          Constants.TENANT_TYPE_FAWS,
                                          Constants.TENANT_TYPE_RCN],
                                         [false, true]].combinations()
    }

    @Unroll
    def "test create user does not set the type on an already exist tenant w/o a type, featureEnabled = #featureEnabled, tenantTypeCreated = #tenantTypeCreated"() {
        given:
        def userAdmin = utils.createUserWithTenants()
        def domainId = userAdmin.domainId
        def tenantTypeName = RandomStringUtils.randomAlphabetic(8).toLowerCase()
        def tenantName = "${tenantTypeName}:${RandomStringUtils.randomAlphabetic(8)}"
        utils.createTenant(tenantName)
        //create the tenant type after the role so the tenant does not get a type set
        utils.createTenantType(tenantTypeName)
        def role = utils.createRole()
        def tenantRole = v2Factory.createRole(role.name).with {
            it.tenantId = tenantName
            it
        }
        def defaultUserRoles = v2Factory.createRoleList([v2Factory.createRole(staticIdmConfiguration.getProperty(IdentityConfig.IDENTITY_DEFAULT_USER_ROLE_NAME_PROP)), tenantRole].asList())
        def defaultUserToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("user"), "display", "email@example.com", true, null, domainId, DEFAULT_PASSWORD).with {
            it.roles = defaultUserRoles
            it
        }

        when: "create a default user with a role on a tenant without a tenant type set"
        def response = cloud20.createUser(utils.getIdentityAdminToken(), defaultUserToCreate)

        then: "user created successfully"
        response.status == 201

        when:
        def tenant = utils.getTenant(tenantName)

        then:
        tenant.types == null

        cleanup:
        utils.deleteUser(utils.getUserByName(defaultUserToCreate.username))
        utils.deleteUser(userAdmin)
        utils.deleteDomain(domainId)
        utils.deleteRole(role)
        utils.deleteTenant(tenantName)
        utils.deleteTenantType(tenantTypeName)
    }

}
