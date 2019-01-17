package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials
import org.apache.commons.lang3.StringUtils
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*
import static org.apache.http.HttpStatus.*

class UserManagerAccessibleResourcesIntegrationTest extends RootIntegrationTest {

    @Shared
    User userAdmin, userManage, userManage2

    @Shared
    String userManageToken, userAdminToken, identityAdminToken

    @Shared
    Tenant tenant

    def setupSpec() {
        identityAdminToken = cloud20.authenticatePassword("auth", "auth123").getEntity(AuthenticateResponse).value.token.id

        def random = UUID.randomUUID()
        def domainId = "domainId$random"

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
    }

    def cleanup() {
        utils.deleteUserQuietly(userManage2)
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
        assert StringUtils.isNoneBlank(resetCred.apiKey)
    }

    def "error check: user-manage CANNOT get/delete/reset another user-manage API key credentials in a different domain"() {
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

}
