package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
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

    def setup() {
        identityAdminToken = utils.getIdentityAdminToken()
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
                "securePassword2", ["identity:user-manage"].asList(), [group.name].asList(), question, answer)

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
                "securePassword2", ["identity:user-manage"].asList(), [group.name].asList(), question, answer)
        cloud20.createUser(identityAdminToken, userRequest).getEntity(User).value

        when:
        def user = cloud20.getUserByName(identityAdminToken, username).getEntity(User).value

        then:
        user.secretQA == null

        cleanup:
        cloud20.deleteUser(identityAdminToken, user.id)
        utils.deleteGroup(group)
    }

}
