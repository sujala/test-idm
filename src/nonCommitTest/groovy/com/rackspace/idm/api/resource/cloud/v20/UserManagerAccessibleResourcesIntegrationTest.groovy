package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domains
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.apache.commons.lang3.StringUtils
import org.openstack.docs.identity.api.v2.*
import spock.lang.Shared
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory

import static com.rackspace.idm.Constants.*
import static org.apache.http.HttpStatus.*

class UserManagerAccessibleResourcesIntegrationTest extends RootIntegrationTest {

    @Shared
    String domainId

    @Shared
    User userAdmin, userManage, userManage2

    @Shared
    def fedUser

    @Shared
    String userManageToken, userAdminToken, identityAdminToken, fedUserToken

    @Shared
    Tenant tenant

    def setupSpec() {
        identityAdminToken = cloud20.authenticatePassword("auth", "auth123").getEntity(AuthenticateResponse).value.token.id

        def random = UUID.randomUUID()
        domainId = "domainId$random"

        // User Admin
        def response = cloud20.createUser(identityAdminToken, v2Factory.createUserForCreate("userAdmin$random", "display", "test@rackspace.com", true, null, domainId, DEFAULT_PASSWORD))
        assert response.status == SC_CREATED
        userAdmin = response.getEntity(User).value

        userAdminToken = cloud20.authenticatePassword(userAdmin.username, DEFAULT_PASSWORD).getEntity(AuthenticateResponse).value.token.id

        // Create user-manage
        response = cloud20.createUser(userAdminToken, v2Factory.createUserForCreate("userManage2$random", "display", "test@rackspace.com", true, null, domainId, DEFAULT_PASSWORD))
        assert response.status == SC_CREATED
        userManage = response.getEntity(User).value
        response = cloud20.addUserRole(identityAdminToken, userManage.id, USER_MANAGE_ROLE_ID)
        assert response.status == SC_OK

        userManageToken = cloud20.authenticatePassword(userManage.username, DEFAULT_PASSWORD).getEntity(AuthenticateResponse).value.token.id

        // Create tenant accessible to user-manager
        def tenantName = "testTenant$random"
        def tenantObj = v2Factory.createTenant(tenantName, tenantName, true, domainId)
        response = cloud20.addTenant(identityAdminToken, tenantObj)
        assert response.status == SC_CREATED

        tenant = response.getEntity(Tenant).value

    }

    def cleanupSpec() {
        cloud20.deleteUser(identityAdminToken, userAdmin.id)
        cloud20.deleteUser(identityAdminToken, userManage.id)
        cloud20.deleteTenant(identityAdminToken, tenant.id)
    }

    def setup() {
        // Create second userManager for testing
        userManage2 = utils.createUser(utils.getToken(userAdmin.username))
        utils.addRoleToUser(userManage2, USER_MANAGE_ROLE_ID)

        // Create federated user with user-manage role
        def fedRequest = utils.createFedRequest(userAdmin, DEFAULT_BROKER_IDP_URI, IDP_V2_DOMAIN_URI)
        fedRequest.roleNames = [IdentityUserTypeEnum.USER_MANAGER.roleName]
        def authResponse = utils.authenticateV2FederatedUser(fedRequest)
        fedUser = authResponse.user
        assert authResponse.user.roles.role.find {it.id == USER_MANAGE_ROLE_ID} != null

        fedUserToken = authResponse.token.id
    }

    def cleanup() {
        utils.deleteUserQuietly(userManage2)
        utils.deleteFederatedUserQuietly(fedUser.name)
    }

    def "user-manage can add/delete role to another user-manage in the same domain"() {
        when: "add role"
        def addResponse = cloud20.addUserRole(userManageToken, userManage2.id, ROLE_RBAC1_ID)

        then:
        addResponse.status == SC_OK

        when: "delete role"
        def deleteResponse = cloud20.deleteApplicationRoleFromUser(userManageToken, ROLE_RBAC1_ID, userManage2.id)

        then:
        deleteResponse.status == SC_NO_CONTENT

        when: "add role - federated user caller"
        addResponse = cloud20.addUserRole(fedUserToken, userManage2.id, ROLE_RBAC1_ID)

        then:
        addResponse.status == SC_OK

        when: "delete role - federated user caller"
        deleteResponse = cloud20.deleteApplicationRoleFromUser(fedUserToken, ROLE_RBAC1_ID, userManage2.id)

        then:
        deleteResponse.status == SC_NO_CONTENT
    }

    def "error check: user-manage CANNOT add/delete role to another user-manage in different domain"() {
        given:
        def domainIdOther = utils.createDomain()
        def identityAdminOther, userAdminOther, userManageOther, defaultUserOther
        (identityAdminOther, userAdminOther, userManageOther, defaultUserOther) = utils.createUsers(domainIdOther)
        def users = [defaultUserOther, userManageOther, userAdminOther, identityAdminOther]

        when: "add role"
        def addResponse = cloud20.addUserRole(userManageToken, userManageOther.id, ROLE_RBAC1_ID)

        then:
        addResponse.status == SC_FORBIDDEN

        when: "delete role"
        def deleteResponse = cloud20.deleteApplicationRoleFromUser(userManageToken, ROLE_RBAC1_ID, userManageOther.id)

        then:
        deleteResponse.status == SC_FORBIDDEN

        when: "add role - federated user caller"
        addResponse = cloud20.addUserRole(fedUserToken, userManageOther.id, ROLE_RBAC1_ID)

        then:
        addResponse.status == SC_FORBIDDEN

        when: "delete role - federated user caller"
        deleteResponse = cloud20.deleteApplicationRoleFromUser(fedUserToken, ROLE_RBAC1_ID, userManageOther.id)

        then:
        deleteResponse.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsersQuietly(users)
    }

    def "user-manage can add/delete role on tenant to another user-manage in the same domain"() {
        when: "add role to user on tenant"
        def response = cloud20.addRoleToUserOnTenant(userManageToken, tenant.id, userManage2.id, ROLE_RBAC1_ID)

        then:
        response.status == SC_OK

        when: "delete role from user on tenant"
        def deleteResponse = cloud20.deleteRoleFromUserOnTenant(userManageToken, tenant.id, userManage2.id, ROLE_RBAC1_ID)

        then:
        deleteResponse.status == SC_NO_CONTENT

        when: "add role to user on tenant - federated user caller"
        response = cloud20.addRoleToUserOnTenant(fedUserToken, tenant.id, userManage2.id, ROLE_RBAC1_ID)

        then:
        response.status == SC_OK

        when: "delete role from user on tenant - federated user caller"
        deleteResponse = cloud20.deleteRoleFromUserOnTenant(fedUserToken, tenant.id, userManage2.id, ROLE_RBAC1_ID)

        then:
        deleteResponse.status == SC_NO_CONTENT
    }

    def "error check: user-manage CANNOT add/delete role on tenant to another user-manage in the different domain"() {
        given:
        def domainIdOther = utils.createDomain()
        def identityAdminOther, userAdminOther, userManageOther, defaultUserOther
        (identityAdminOther, userAdminOther, userManageOther, defaultUserOther) = utils.createUsers(domainIdOther)
        def users = [defaultUserOther, userManageOther, userAdminOther, identityAdminOther]

        when: "add role to user on tenant"
        def response = cloud20.addRoleToUserOnTenant(userManageToken, tenant.id, userManageOther.id, ROLE_RBAC1_ID)

        then:
        response.status == SC_FORBIDDEN

        when: "delete role from user on tenant"
        def deleteResponse = cloud20.deleteRoleFromUserOnTenant(userManageToken, tenant.id, userManageOther.id, ROLE_RBAC1_ID)

        then:
        deleteResponse.status == SC_FORBIDDEN

        when: "add role to user on tenant - federated user caller"
        response = cloud20.addRoleToUserOnTenant(fedUserToken, tenant.id, userManageOther.id, ROLE_RBAC1_ID)

        then:
        response.status == SC_FORBIDDEN

        when: "delete role from user on tenant - federated user caller"
        deleteResponse = cloud20.deleteRoleFromUserOnTenant(fedUserToken, tenant.id, userManageOther.id, ROLE_RBAC1_ID)

        then:
        deleteResponse.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsersQuietly(users)
    }

    def "user-manage can update another user-manage in the same domain"() {
        given:
        def email = "anotherEmail@test.com"
        def userToUpdate = v2Factory.createUserForUpdate(null, null, null, email, true, null, null)

        when: "update user's email"
        def response = cloud20.updateUser(userManageToken, userManage2.id, userToUpdate)
        User entity = response.getEntity(User).value

        then:
        response.status == SC_OK
        entity.email == email

        when: "update user's email - federated user caller"
        response = cloud20.updateUser(fedUserToken, userManage2.id, userToUpdate)
        entity = response.getEntity(User).value

        then:
        response.status == SC_OK
        entity.email == email
    }

    def "error check: user-manage CANNOT update another user-manage in the different domain"() {
        given:
        def domainIdOther = utils.createDomain()
        def identityAdminOther, userAdminOther, userManageOther, defaultUserOther
        (identityAdminOther, userAdminOther, userManageOther, defaultUserOther) = utils.createUsers(domainIdOther)
        def users = [defaultUserOther, userManageOther, userAdminOther, identityAdminOther]

        def email = "anotherEmail@test.com"
        def userToUpdate = v2Factory.createUserForUpdate(null, null, null, email, true, null, null)

        when: "update user's email"
        def response = cloud20.updateUser(userManageToken, userManageOther.id, userToUpdate)

        then:
        response.status == SC_FORBIDDEN

        when: "update user's email - federated user caller"
        response = cloud20.updateUser(fedUserToken, userManageOther.id, userToUpdate)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsersQuietly(users)
    }

    def "user-manage can get/delete/reset another user-manage API key credentials in the same domain"() {
        given:
        utils.addApiKeyToUser(userManage2)

        when: "delete and get API key"
        def response = cloud20.deleteUserApiKey(userManageToken, userManage2.id)
        def getApiKeyResponse = cloud20.getUserApiKey(userManageToken, userManage2.id)

        then:
        response.status == SC_NO_CONTENT
        getApiKeyResponse.status == SC_NOT_FOUND

        when: "reset and get API key"
        def resetResponse = cloud20.resetUserApiKey(userManageToken, userManage2.id)
        ApiKeyCredentials resetCred = utils.getUserApiKey(userManage2)

        then:
        resetResponse.status == SC_OK
        assert StringUtils.isNotBlank(resetCred.apiKey)

        when: "delete and get API key - federated user caller"
        response = cloud20.deleteUserApiKey(fedUserToken, userManage2.id)
        getApiKeyResponse = cloud20.getUserApiKey(fedUserToken, userManage2.id)

        then:
        response.status == SC_NO_CONTENT
        getApiKeyResponse.status == SC_NOT_FOUND

        when: "reset and get API key - federated user caller"
        resetResponse = cloud20.resetUserApiKey(fedUserToken, userManage2.id)
        resetCred = utils.getUserApiKey(userManage2)

        then:
        resetResponse.status == SC_OK
        assert StringUtils.isNotBlank(resetCred.apiKey)
    }

    def "error check: user-manage CANNOT get/delete/reset another user-manage API key credentials in a different domain"() {
        given:
        def domainIdOther = utils.createDomain()
        def identityAdminOther, userAdminOther, userManageOther, defaultUserOther
        (identityAdminOther, userAdminOther, userManageOther, defaultUserOther) = utils.createUsers(domainIdOther)
        def users = [defaultUserOther, userManageOther, userAdminOther, identityAdminOther]
        utils.addApiKeyToUser(userManage2)

        when: "get API key"
        def getApiKeyResponse = cloud20.getUserApiKey(userManageToken, userManageOther.id)

        then:
        getApiKeyResponse.status == SC_FORBIDDEN

        when: "delete API key"
        def response = cloud20.deleteUserApiKey(userManageToken, userManageOther.id)

        then:
        response.status == SC_FORBIDDEN

        when: "reset API key"
        def resetResponse = cloud20.resetUserApiKey(userManageToken, userManageOther.id)

        then:
        resetResponse.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsersQuietly(users)
    }

    def "federated user with user-manager can view user-managers within same domain"() {
        when: "get user by id - self"
        def response = cloud20.getUserById(fedUserToken, fedUser.id)
        User user = response.getEntity(User).value

        then:
        response.status == SC_OK

        user.id == fedUser.id

        when: "get user by id - other user-manager"
        response = cloud20.getUserById(fedUserToken, userManage.id)
        user = response.getEntity(User).value

        then:
        response.status == SC_OK

        user.id == userManage.id

        when: "get user by name"
        response = cloud20.getUserByName(fedUserToken, userManage.username)
        user = response.getEntity(User).value

        then:
        response.status == SC_OK

        user.id == userManage.id

        when: "get user by email"
        response = cloud20.getUsersByEmail(fedUserToken, userManage.email)
        UserList userList = response.getEntity(UserList).value

        then:
        response.status == SC_OK

        userList.user.size() == 1
        userList.user.find {it.id == userManage.id}

        when: "list users"
        response = cloud20.listUsers(fedUserToken)
        userList = response.getEntity(UserList).value

        then:
        response.status == SC_OK

        userList.user.size() == 3
        userList.user.find {it.id == userManage.id} != null
        userList.user.find {it.id == userManage2.id} != null
        userList.user.find {it.id == fedUser.id} != null
    }

    def "error check: federated user with user-manager"() {
        given:
        def domainIdOther = utils.createDomain()
        def identityAdminOther, userAdminOther, userManageOther, defaultUserOther
        (identityAdminOther, userAdminOther, userManageOther, defaultUserOther) = utils.createUsers(domainIdOther)
        def users = [defaultUserOther, userManageOther, userAdminOther, identityAdminOther]

        when: "get user by id - user-manager in different domain"
        def response = cloud20.getUserById(fedUserToken, userManageOther.id)

        then:
        response.status == SC_NOT_FOUND

        when: "get user by name - user-manager in different domain"
        response = cloud20.getUserByName(fedUserToken, userManageOther.username)

        then:
        response.status == SC_FORBIDDEN

        when: "get user by email"
        response = cloud20.getUsersByEmail(fedUserToken, userManageOther.email)
        UserList userList = response.getEntity(UserList).value

        then:
        response.status == SC_OK

        userList.user.size() == 0

        cleanup:
        utils.deleteUsersQuietly(users)
    }

    def "federated user with user-manager can do accessible domain operations on user-manager in same domain"() {
        when: "get accessible domains for user"
        def response = cloud20.getAccessibleDomainsForUser(fedUserToken, userManage.id)
        Domains domains = response.getEntity(Domains)

        then:
        response.status == SC_OK

        domains.domain.size() == 1
        domains.domain.find {it.id == domainId}

        when: "get accessible domain endpoints for user"
        response = cloud20.getAccessibleDomainEndpointsForUser(fedUserToken, userManage.id, domainId)
        EndpointList endpointList = response.getEntity(EndpointList).value

        then:
        response.status == SC_OK
        endpointList != null
    }

    def "error check: federated user with user-manager CANNOT do accessible domain operations on user-manager in different domain"() {
        given:
        def domainIdOther = utils.createDomain()
        def identityAdminOther, userAdminOther, userManageOther, defaultUserOther
        (identityAdminOther, userAdminOther, userManageOther, defaultUserOther) = utils.createUsers(domainIdOther)
        def users = [defaultUserOther, userManageOther, userAdminOther, identityAdminOther]

        when: "get accessible domains for user - self"
        def response = cloud20.getAccessibleDomainsForUser(fedUserToken, fedUser.id)

        then:
        response.status == SC_NOT_FOUND // Only works for provisioned users

        when: "get accessible domains for user"
        response = cloud20.getAccessibleDomainsForUser(fedUserToken, userManageOther.id)

        then:
        response.status == SC_FORBIDDEN

        when: "get accessible domain endpoints for user"
        response = cloud20.getAccessibleDomainEndpointsForUser(fedUserToken, userManageOther.id, domainIdOther)

        then:
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsersQuietly(users)
    }

    def "user-manager CANNOT self delete"() {
        when:
        def response = cloud20.deleteUser(userManageToken, userManage.id)

        then:
        response.status == SC_FORBIDDEN
    }

    def "user-manager can delete another user-manager in the same domain"() {
        when:
        def response = cloud20.deleteUser(userManageToken, userManage2.id)

        then:
        response.status == SC_NO_CONTENT
    }

    def "federated user-manager can delete another user-manager in the same domain"() {
        when:
        def response = cloud20.deleteUser(fedUserToken, userManage2.id)

        then:
        response.status == SC_NO_CONTENT
    }
}
