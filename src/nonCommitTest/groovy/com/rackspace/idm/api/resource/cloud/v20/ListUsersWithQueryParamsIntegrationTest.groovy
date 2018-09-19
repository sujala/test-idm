package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import groovy.json.JsonSlurper
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.openstack.docs.identity.api.v2.Tenant
import org.openstack.docs.identity.api.v2.User
import org.openstack.docs.identity.api.v2.UserList
import spock.lang.Shared
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.SC_BAD_REQUEST
import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_OK

class ListUsersWithQueryParamsIntegrationTest extends RootIntegrationTest {

    @Shared User serviceAdmin, identityAdmin, userAdmin, userManage, defaultUser, unverifiedUser
    @Shared def federatedUser
    @Shared Tenant tenant

    def setup(){
        def domainId = utils.createDomain()

        // Create users
        serviceAdmin = utils.createServiceAdmin()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        utils.domainRcnSwitch(domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)
        unverifiedUser = utils.createUnverifiedUser(domainId)
        federatedUser = utils.createFederatedUser(domainId)

        // Create and setup tenant
        tenant = utils.createTenant(testUtils.getRandomUUID("tenant"), true, testUtils.getRandomUUID("tenant"), userAdmin.domainId)
        utils.addRoleToUserOnTenant(userAdmin, tenant)
    }

    def cleanup(){
        def users = [federatedUser, unverifiedUser, defaultUser, userManage, userAdmin, identityAdmin]
        utils.deleteUsersQuietly(users)
    }

    def "list users with query params - admin tokens"() {
        given:
        // Get tokens
        def serviceAdminToken = utils.getToken(serviceAdmin.username)
        def identityAdminToken = utils.getToken(identityAdmin.username)

        // Build params
        List<ListUsersSearchParams> serviceAdminDomainParamsList = getListUserSelfDomainParams(serviceAdmin)
        List<ListUsersSearchParams> identityAdminDomainParamsList = getListUserSelfDomainParams(identityAdmin)
        List<ListUsersSearchParams> verifiedUsersInDomainParamsList = getListUsersUserTypeVerifiedParams(userAdmin.domainId)
        List<ListUsersSearchParams> unverifiedUsersInDomainParamsList = getListUsersUserTypeUnverifiedParams(unverifiedUser)
        List<ListUsersSearchParams> allUsersInDomainParamsList = getListUsersUserTypeAllParams(userAdmin.domainId)
        List<ListUsersSearchParams> adminOnlyParamsList = getListUsersOnlyAdminParams(userAdmin, tenant.id)
        List<ListUsersSearchParams> defaultUserParamsList = getListUserSingleUserParams(defaultUser, tenant.id)

        def responses = []

        when: "list users in self domain - service admin token"
        serviceAdminDomainParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(serviceAdminToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_OK
            UserList userList = getUsersFromListUsers(response)
            assert userList.user.size() == 1
            assert userList.user.find {it.username == serviceAdmin.username} != null
        }

        when: "list unverified users in domain - service admin token"
        responses.clear()
        unverifiedUsersInDomainParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(serviceAdminToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_OK
            UserList userList = getUsersFromListUsers(response)
            assert userList.user.size() == 1
            assert userList.user.find {it.email == unverifiedUser.email} != null
        }

        when: "list verified users in domain - service admin token"
        responses.clear()
        verifiedUsersInDomainParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(serviceAdminToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_OK
            UserList userList = getUsersFromListUsers(response)
            assert userList.user.size() == 4
            assert userList.user.find {it.username == userAdmin.username} != null
            assert userList.user.find {it.username == userManage.username} != null
            assert userList.user.find {it.username == defaultUser.username} != null
            assert userList.user.find {it.username == federatedUser.name} != null
        }

        when: "list all users in domain - service admin token"
        responses.clear()
        allUsersInDomainParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(serviceAdminToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_OK
            UserList userList = getUsersFromListUsers(response)
            assert userList.user.size() == 5
            assert userList.user.find {it.username == userAdmin.username} != null
            assert userList.user.find {it.username == userManage.username} != null
            assert userList.user.find {it.username == defaultUser.username} != null
            assert userList.user.find {it.username == federatedUser.name} != null
            assert userList.user.find {it.email == unverifiedUser.email} != null
        }

        when: "list users - only admin - service admin token"
        responses.clear()
        adminOnlyParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(serviceAdminToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_OK
            UserList userList = getUsersFromListUsers(response)
            assert userList.user.size() == 1
            assert userList.user.find {it.username == userAdmin.username} != null
        }

        when: "list users - single user - service admin token"
        responses.clear()
        defaultUserParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(serviceAdminToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_OK
            UserList userList = getUsersFromListUsers(response)
            assert userList.user.size() == 1
            assert userList.user.find {it.username == defaultUser.username} != null
        }

        when: "list users in self domain - identity admin token"
        responses.clear()
        identityAdminDomainParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(identityAdminToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_OK
            UserList userList = getUsersFromListUsers(response)
            assert userList.user.size() == 1
            assert userList.user.find {it.username == identityAdmin.username} != null
        }

        when: "list unverified users in domain - identity admin token"
        responses.clear()
        unverifiedUsersInDomainParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(identityAdminToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_OK
            UserList userList = getUsersFromListUsers(response)
            assert userList.user.size() == 1
            assert userList.user.find {it.email == unverifiedUser.email} != null
        }

        when: "list verified users in domain - identity admin token"
        responses.clear()
        verifiedUsersInDomainParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(identityAdminToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_OK
            UserList userList = getUsersFromListUsers(response)
            assert userList.user.size() == 4
            assert userList.user.find {it.username == userAdmin.username} != null
            assert userList.user.find {it.username == userManage.username} != null
            assert userList.user.find {it.username == defaultUser.username} != null
            assert userList.user.find {it.username == federatedUser.name} != null
        }

        when: "list all users in domain - identity admin token"
        responses.clear()
        allUsersInDomainParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(identityAdminToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_OK
            UserList userList = getUsersFromListUsers(response)
            assert userList.user.size() == 5
            assert userList.user.find {it.username == userAdmin.username} != null
            assert userList.user.find {it.username == userManage.username} != null
            assert userList.user.find {it.username == defaultUser.username} != null
            assert userList.user.find {it.username == federatedUser.name} != null
            assert userList.user.find {it.email == unverifiedUser.email} != null
        }

        when: "list users - only admin - identity admin token"
        responses.clear()
        adminOnlyParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(identityAdminToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_OK
            UserList userList = getUsersFromListUsers(response)
            assert userList.user.size() == 1
            assert userList.user.find {it.username == userAdmin.username} != null
        }

        when: "list users - single user - identity admin token"
        responses.clear()
        defaultUserParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(identityAdminToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_OK
            UserList userList = getUsersFromListUsers(response)
            assert userList.user.size() == 1
            assert userList.user.find {it.username == defaultUser.username} != null
        }
    }

    def "list users - empty list"() {
        given:
        def identityAdminToken = utils.getToken(identityAdmin.username)
        def domain = utils.createDomainEntity()
        def tenantEntity = utils.createTenant()
        def listUserInDomainParams = getListUsersUserTypeAllParams(domain.id)
        def responses = []

        when: "domain without users"
        listUserInDomainParams.each { params ->
            responses << cloud20.listUsersWithSearchParams(identityAdminToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_OK
            UserList userList = getUsersFromListUsers(response)
            assert userList.user.size() == 0
        }

        when: "invalid email address"
        def response  = cloud20.listUsersWithSearchParams(identityAdminToken, new ListUsersSearchParams(null, "invalid", null, userAdmin.domainId, null, null, new PaginationParams()))
        UserList userList = getUsersFromListUsers(response)

        then:
        assert response.status == SC_OK
        assert userList.user.size() == 0

        when: "adminOnly set to 'true' and default user's username"
        response  = cloud20.listUsersWithSearchParams(identityAdminToken, new ListUsersSearchParams(defaultUser.username, null, null, defaultUser.domainId, true, null, new PaginationParams()))
        userList = getUsersFromListUsers(response)

        then:
        assert response.status == SC_OK
        assert userList.user.size() == 0

        when: "adminOnly set to 'true' and userAdmin in different domain"
        response  = cloud20.listUsersWithSearchParams(identityAdminToken, new ListUsersSearchParams(null, null, null, domain.id, true, null, new PaginationParams()))
        userList = getUsersFromListUsers(response)

        then:
        assert response.status == SC_OK
        assert userList.user.size() == 0

        when: "adminOnly set to 'true' and invalid username"
        response  = cloud20.listUsersWithSearchParams(identityAdminToken, new ListUsersSearchParams("invalid", null, null, userAdmin.domainId, true, null, new PaginationParams()))
        userList = getUsersFromListUsers(response)

        then:
        assert response.status == SC_OK
        assert userList.user.size() == 0

        when: "adminOnly set to 'true' and invalid email"
        response  = cloud20.listUsersWithSearchParams(identityAdminToken, new ListUsersSearchParams(userAdmin.username, "invalid", null, userAdmin.domainId, true, null, new PaginationParams()))
        userList = getUsersFromListUsers(response)

        then:
        assert response.status == SC_OK
        assert userList.user.size() == 0

        when: "tenant belongs to different domain"
        response  = cloud20.listUsersWithSearchParams(identityAdminToken, new ListUsersSearchParams(userAdmin.username, null, tenantEntity.id, null, true, null, new PaginationParams()))
        userList = getUsersFromListUsers(response)

        then:
        assert response.status == SC_OK
        assert userList.user.size() == 0
    }

    def "list user - forbidden actions"() {
        given:
        def defaultUserToken = utils.getToken(defaultUser.username)
        def userManageToken = utils.getToken(userManage.username)
        def userAdminToken = utils.getToken(userAdmin.username)

        List<ListUsersSearchParams> defaultUserParamsList = getListUserSingleUserParams(defaultUser, tenant.id)
        List<ListUsersSearchParams> userManageParamsList = getListUserSingleUserParams(userManage, tenant.id)
        List<ListUsersSearchParams> userAdminParamsList = getListUserSingleUserParams(userAdmin, tenant.id)

        def responses = []

        when: "default user using query params only available to admin users"
        defaultUserParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(defaultUserToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_FORBIDDEN
        }

        when: "user manage using query params only available to admin users"
        responses.clear()
        userManageParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(userManageToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_FORBIDDEN
        }

        when: "user admin using query params only available to admin users"
        responses.clear()
        userAdminParamsList.each { params ->
            responses << cloud20.listUsersWithSearchParams(userAdminToken, params)
        }

        then:
        responses.each { response ->
            assert response.status == SC_FORBIDDEN
        }
    }

    def "error check - list users"() {
        given:
        def serviceAdminToken = utils.getToken(serviceAdmin.username)
        def identityAdminToken = utils.getToken(identityAdmin.username)

        when: "using both 'tenant_id' and 'domain_id' query params - service admin"
        def response  = cloud20.listUsersWithSearchParams(serviceAdminToken, new ListUsersSearchParams(null, null, tenant.id, userAdmin.domainId, null, null, new PaginationParams()))

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST, String.format(ErrorCodes.ERROR_CODE_MUTUALLY_EXCLUSIVE_QUERY_PARAMS_FOR_LIST_USERS_MSG_PATTERN, "tenant_id", "domain_id"))

        when: "using both 'name' and 'user_type' query params - service admin"
        response  = cloud20.listUsersWithSearchParams(serviceAdminToken, new ListUsersSearchParams(userAdmin.username, null, null, null,true, com.rackspace.idm.domain.entity.User.UserType.ALL.name(), new PaginationParams()))

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST, String.format(ErrorCodes.ERROR_CODE_MUTUALLY_EXCLUSIVE_QUERY_PARAMS_FOR_LIST_USERS_MSG_PATTERN, "user_type", "name"))

        when: "provided 'tenant_id' does not exist - service admin"
        response  = cloud20.listUsersWithSearchParams(serviceAdminToken, new ListUsersSearchParams(null, null, "invalid", null, true, null, new PaginationParams()))

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, String.format("Tenant with id/name: '%s' was not found.", "invalid"))

        when: "provided 'domain_id' does not exist - service admin"
        response  = cloud20.listUsersWithSearchParams(serviceAdminToken, new ListUsersSearchParams(null, null, null, "invalid", true, null, new PaginationParams()))

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, String.format("Domain with ID %s not found.", "invalid"))

        when: "using both 'tenant_id' and 'domain_id' query params - identity admin"
        response  = cloud20.listUsersWithSearchParams(identityAdminToken, new ListUsersSearchParams(null, null, tenant.id, userAdmin.domainId, null, null, new PaginationParams()))

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST, String.format(ErrorCodes.ERROR_CODE_MUTUALLY_EXCLUSIVE_QUERY_PARAMS_FOR_LIST_USERS_MSG_PATTERN, "tenant_id", "domain_id"))

        when: "using both 'name' and 'user_type' query params - identity admin"
        response  = cloud20.listUsersWithSearchParams(identityAdminToken, new ListUsersSearchParams(userAdmin.username, null, null, null, true, com.rackspace.idm.domain.entity.User.UserType.ALL.name(), new PaginationParams()))

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST, String.format(ErrorCodes.ERROR_CODE_MUTUALLY_EXCLUSIVE_QUERY_PARAMS_FOR_LIST_USERS_MSG_PATTERN, "user_type", "name"))

        when: "provided 'tenant_id' does not exist - identity admin"
        response  = cloud20.listUsersWithSearchParams(identityAdminToken, new ListUsersSearchParams(null, null, "invalid", null, true, null, new PaginationParams()))

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, String.format("Tenant with id/name: '%s' was not found.", "invalid"))

        when: "provided 'domain_id' does not exist - identity admin"
        response  = cloud20.listUsersWithSearchParams(identityAdminToken, new ListUsersSearchParams(null, null, null, "invalid", true, null, new PaginationParams()))

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, String.format("Domain with ID %s not found.", "invalid"))
    }

    def getListUserSelfDomainParams(User user) {
        return [new ListUsersSearchParams(null, null, null, null, null, null, new PaginationParams()),
                new ListUsersSearchParams(null, null, null, null, false, null, new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, null, null, null, new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, null, false, null, new PaginationParams()),
                new ListUsersSearchParams(user.username, user.email, null, user.domainId, null, null, new PaginationParams()),
                new ListUsersSearchParams(user.username, user.email, null, user.domainId, false, null, new PaginationParams()),
                new ListUsersSearchParams(null, null, null, user.domainId, null, null, new PaginationParams()),
                new ListUsersSearchParams(null, null, null, user.domainId, false, null, new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, null, com.rackspace.idm.domain.entity.User.UserType.ALL.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, false, com.rackspace.idm.domain.entity.User.UserType.ALL.name(), new PaginationParams()),
                new ListUsersSearchParams(null, null, null, null, null, com.rackspace.idm.domain.entity.User.UserType.ALL.name(), new PaginationParams()),
                new ListUsersSearchParams(null, null, null, null, false, com.rackspace.idm.domain.entity.User.UserType.ALL.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, null, com.rackspace.idm.domain.entity.User.UserType.VERIFIED.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, false, com.rackspace.idm.domain.entity.User.UserType.VERIFIED.name(), new PaginationParams()),
                new ListUsersSearchParams(null, null, null, null, null, com.rackspace.idm.domain.entity.User.UserType.VERIFIED.name(), new PaginationParams()),
                new ListUsersSearchParams(null, null, null, null, false, com.rackspace.idm.domain.entity.User.UserType.VERIFIED.name(), new PaginationParams())
        ]
    }

    def getListUsersUserTypeVerifiedParams(String domainId) {
        return [
                new ListUsersSearchParams(null, null, null, domainId, null, null, new PaginationParams()),
                new ListUsersSearchParams(null, null, null, domainId, false, null, new PaginationParams()),
                new ListUsersSearchParams(null, null, null, domainId, null, com.rackspace.idm.domain.entity.User.UserType.VERIFIED.name(), new PaginationParams()),
                new ListUsersSearchParams(null, null, null, domainId, false, com.rackspace.idm.domain.entity.User.UserType.VERIFIED.name(), new PaginationParams()),
        ]
    }

    def getListUsersUserTypeUnverifiedParams(User user) {
        return [
                new ListUsersSearchParams(null, null, null, user.domainId, null, com.rackspace.idm.domain.entity.User.UserType.UNVERIFIED.name(), new PaginationParams()),
                new ListUsersSearchParams(null, null, null, user.domainId, false, com.rackspace.idm.domain.entity.User.UserType.UNVERIFIED.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, null, com.rackspace.idm.domain.entity.User.UserType.UNVERIFIED.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, false, com.rackspace.idm.domain.entity.User.UserType.UNVERIFIED.name(), new PaginationParams()),
        ]
    }

    def getListUsersUserTypeAllParams(String domainId) {
        return [
                new ListUsersSearchParams(null, null, null, domainId, null, com.rackspace.idm.domain.entity.User.UserType.ALL.name(), new PaginationParams()),
                new ListUsersSearchParams(null, null, null, domainId, false, com.rackspace.idm.domain.entity.User.UserType.ALL.name(), new PaginationParams()),
        ]
    }

    def getListUsersOnlyAdminParams(User user, String tenantId) {
        return [
                new ListUsersSearchParams(null, null, null, user.domainId, true, null, new PaginationParams()),
                new ListUsersSearchParams(user.username, null, null, user.domainId, true, null, new PaginationParams()),
                new ListUsersSearchParams(user.username, null, tenantId, null, true, null, new PaginationParams()),
                new ListUsersSearchParams(user.username, user.email, null, user.domainId, true, null, new PaginationParams()),
                new ListUsersSearchParams(user.username, user.email, tenantId, null, true, null, new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, true, com.rackspace.idm.domain.entity.User.UserType.ALL.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, tenantId, null, true, com.rackspace.idm.domain.entity.User.UserType.ALL.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, true, com.rackspace.idm.domain.entity.User.UserType.VERIFIED.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, tenantId, null, true, com.rackspace.idm.domain.entity.User.UserType.VERIFIED.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, true, null, new PaginationParams()),
                new ListUsersSearchParams(null, user.email, tenantId, null, true, null, new PaginationParams()),
        ]
    }

    def getListUserSingleUserParams(User user, String tenantId){
        return [
                new ListUsersSearchParams(user.username, null, null, user.domainId, null, null, new PaginationParams()),
                new ListUsersSearchParams(user.username, null, null, user.domainId, false, null, new PaginationParams()),
                new ListUsersSearchParams(user.username, null, tenantId, null, null, null, new PaginationParams()),
                new ListUsersSearchParams(user.username, null, tenantId, null, false, null, new PaginationParams()),
                new ListUsersSearchParams(user.username, user.email, null, user.domainId, null, null, new PaginationParams()),
                new ListUsersSearchParams(user.username, user.email, null, user.domainId, false, null, new PaginationParams()),
                new ListUsersSearchParams(user.username, user.email, tenantId, null, null, null, new PaginationParams()),
                new ListUsersSearchParams(user.username, user.email, tenantId, null, false, null, new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, null, null, new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, false, null, new PaginationParams()),
                new ListUsersSearchParams(null, user.email, tenantId, null, null, null, new PaginationParams()),
                new ListUsersSearchParams(null, user.email, tenantId, null, false, null, new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, null, com.rackspace.idm.domain.entity.User.UserType.ALL.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, false, com.rackspace.idm.domain.entity.User.UserType.ALL.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, tenantId, null, null, com.rackspace.idm.domain.entity.User.UserType.ALL.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, tenantId, null, false, com.rackspace.idm.domain.entity.User.UserType.ALL.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, null, com.rackspace.idm.domain.entity.User.UserType.VERIFIED.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, null, user.domainId, false, com.rackspace.idm.domain.entity.User.UserType.VERIFIED.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, tenantId, null, null, com.rackspace.idm.domain.entity.User.UserType.VERIFIED.name(), new PaginationParams()),
                new ListUsersSearchParams(null, user.email, tenantId, null, false, com.rackspace.idm.domain.entity.User.UserType.VERIFIED.name(), new PaginationParams()),
        ]
    }

    def getUsersFromListUsers(response) {
        if(response.getType() == MediaType.APPLICATION_XML_TYPE) {
            return response.getEntity(UserList).value
        }

        UserList userList = new UserList()
        userList.user.addAll(new JsonSlurper().parseText(response.getEntity(String))["users"])

        return userList
    }
}
