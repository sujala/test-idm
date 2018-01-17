package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import groovy.json.JsonSlurper
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.User
import org.openstack.docs.identity.api.v2.UserList
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class ListUsersOnTenantIntegrationTest extends RootIntegrationTest {

    @Shared def tenant

    @Autowired
    IdentityConfig identityConfig

    def setup() {
        tenant = utils.createTenant()
    }

    def cleanup() {
        utils.deleteTenant(tenant)
        reloadableConfiguration.reset()
    }

    def "listUsersOnTenant returns 400 if marker < 0"() {
        when:
        def response = cloud20.listUsersWithTenantId(utils.getServiceAdminToken(), tenant.id, new ListUsersForTenantParams(null, null, new PaginationParams(-1, 100)));

        then:
        response.status == 400
    }

    @Unroll
    def "listUsersOnTenant paginates correctly offset: #offset limit: #limit numUsersExpected: #numUsersExpected"() {
        given:
        def role = utils.createRole()
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUsers = [defaultUser]
        for(i in (0..4)) {
            defaultUsers << utils.createUser(userAdminToken, testUtils.getRandomUUID("defaultUser"), domainId)
        }
        for(curDefailtUser in defaultUsers) {
            utils.addRoleToUserOnTenant(curDefailtUser, tenant, role.id)
        }

        when:
        def withoutRoleResponse = cloud20.listUsersWithTenantId(utils.getServiceAdminToken(), tenant.id, new ListUsersForTenantParams(null, null, new PaginationParams(offset, limit)))
        def withRoleResponse = cloud20.listUsersWithTenantIdAndRole(utils.getServiceAdminToken(), tenant.id, role.id, offset.toString(), limit.toString())

        then:
        withoutRoleResponse.status == 200
        withRoleResponse.status == 200
        def usersWithTenant = withoutRoleResponse.getEntity(UserList).value
        def usersWithTenantAndRole = withRoleResponse.getEntity(UserList).value
        usersWithTenant.user.size() == numUsersExpected
        usersWithTenantAndRole.user.size() == numUsersExpected

        cleanup:
        utils.deleteUsers(defaultUsers)
        utils.deleteUsers(userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)

        where:
        offset | limit | numUsersExpected
        0      | 0     | 6
        0      | 1     | 1
        0      | 5     | 5
        0      | 6     | 6
        1      | 0     | 5
        1      | 1     | 1
        1      | 5     | 5
        1      | 6     | 5
        5      | 0     | 1
        5      | 1     | 1
        6      | 0     | 0
        6      | 1     | 0
    }

    /**
     * This tests the implicit assignment of identity:tenant-access role to all tenants within a user's domain
     *
     * @return
     */
    def "List Users for Tenants: Automatically returns all users within a tenant's domain" () {
        given: "A new user and 2 tenants"
        def adminToken = utils.getIdentityAdminToken()
        def username = testUtils.getRandomUUID("name")
        def domainId = testUtils.getRandomUUID("domainId")
        def user = utils.createUser(adminToken, username, domainId)
        def tenantId1 = testUtils.getRandomUUID("tenant")

        def tenant1 = utils.createTenant(v2Factory.createTenant(tenantId1, tenantId1, ["cloud"]).with {
            it.domainId = domainId
            it
        })

        def userAdminToken = utils.getToken(username)

        when: "List users for tenant"
        def listTenantResponse = cloud20.listUsersWithTenantId(adminToken, tenantId1)

        then: "User in same domain is returned"
        assert listTenantResponse.status == 200
        def usersEntity2 = getUsersFromResponse(listTenantResponse)
        assert usersEntity2.user.size == 1

        when: "Add new subuser, list users for tenant returns new user"
        def subUser = utils.createUser(userAdminToken, testUtils.getRandomUUID("defaultUser"), domainId)
        listTenantResponse = cloud20.listUsersWithTenantId(adminToken, tenantId1)

        then: "User in same domain is returned"
        assert listTenantResponse.status == 200
        def usersEntity3 = getUsersFromResponse(listTenantResponse)
        assert usersEntity3.user.size == 2

        cleanup:
        utils.deleteUserQuietly(subUser)
        utils.deleteUserQuietly(user)
        utils.deleteTestDomainQuietly(domainId)
        utils.deleteTenantQuietly(tenant1)
    }

    /**
     * This tests the implicit assignment of identity:tenant-access role to all tenants within a user's domain excludes
     * when user is assigned the same domain as the default tenant domain. This test assumes the default domain exists.
     *
     * @return
     */
    def "List Users for Tenants: Automatic assignment of tenant access ignores tenants associated with default domain" () {
        given: "A new user and 2 tenants"
        def adminToken = utils.getIdentityAdminToken()
        def defaultDomainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId()
        def domainId = utils.createDomain()

        def username = testUtils.getRandomUUID("name")
        def user = utils.createUser(adminToken, username, domainId)
        // NOTE(jorge.munoz) Add user to domain still allows to add user to default domain
        utils.addUserToDomain(utils.getIdentityAdminToken(), user.id, defaultDomainId)

        def tenantId1 = testUtils.getRandomUUID("tenant")
        def tenant1 = utils.createTenant(v2Factory.createTenant(tenantId1, tenantId1, ["cloud"]).with {
            it.domainId = domainId
            it
        })

        when: "List users for tenant"
        def listTenantResponse = cloud20.listUsersWithTenantId(adminToken, tenantId1)

        then: "User in same domain still not returned"
        assert listTenantResponse.status == 200
        def usersEntity2 = getUsersFromResponse(listTenantResponse)
        assert usersEntity2.user.size == 0

        cleanup:
        utils.deleteUserQuietly(user)
        utils.deleteTenantQuietly(tenant1)
    }

    @Unroll
    def "listUsersOnTenantForRole: Allows searching on auto assignment role"() {
        given: "A new user and 2 tenants"
        def adminToken = utils.getIdentityAdminToken()
        def username = testUtils.getRandomUUID("name")
        def domainId = testUtils.getRandomUUID("domainId")
        def user = utils.createUser(adminToken, username, domainId)
        def userAdminToken = utils.getToken(username)

        // Create subuser & explicitly assign the role
        def subUser = utils.createUser(userAdminToken, testUtils.getRandomUUID("defaultUser"), domainId)

        // Create tenant on which to test access
        def tenantId1 = testUtils.getRandomUUID("tenant")
        def tenant1 = utils.createTenant(v2Factory.createTenant(tenantId1, tenantId1, ["cloud"]).with {
            it.domainId = domainId
            it
        })

        when: "List users for tenant"
        def response = cloud20.listUsersWithTenantIdAndRole(adminToken, tenantId1, Constants.IDENTITY_TENANT_ACCESS_ROLE_ID)

        then: "All users in same domain are returned"
        assert response.status == 200
        def usersEntity3 = getUsersFromResponse(response)
        usersEntity3.user.size == 2
        usersEntity3.user.find {it.id == user.id} != null
        usersEntity3.user.find {it.id == subUser.id} != null

        when: "Add new subuser, list users for tenant returns new user"
        def subUser2 = utils.createUser(userAdminToken, testUtils.getRandomUUID("defaultUser"), domainId)
        response = cloud20.listUsersWithTenantId(adminToken, tenantId1)

        then: "User in same domain is returned"
        assert response.status == 200
        def usersEntity4 = getUsersFromResponse(response)
        assert usersEntity4.user.size == 3

        and: "order is based on id. lowest first"
        assert usersEntity4.user[0].id.compareToIgnoreCase(usersEntity4.user[1].id) < 0
        assert usersEntity4.user[1].id.compareToIgnoreCase(usersEntity4.user[2].id) < 0

        cleanup:
        utils.deleteUserQuietly(subUser2)
        utils.deleteUserQuietly(subUser)
        utils.deleteUserQuietly(user)
        utils.deleteTenantQuietly(tenant1)
        utils.deleteTestDomainQuietly(domainId)
    }

    @Unroll
    def "List users for tenant using contactId query param: accept = #acceptMediaType"() {
        given:
        def adminToken = utils.getIdentityAdminToken()
        def username = testUtils.getRandomUUID("name")
        def domainId = testUtils.getRandomUUID("domainId")
        def user = utils.createUser(adminToken, username, domainId)
        def userAdminToken = utils.getToken(username)
        def contactId = testUtils.getRandomUUID("contactId")

        // Create subUser
        def subUser = utils.createUser(userAdminToken, testUtils.getRandomUUID("defaultUser"), domainId)

        // Create a non-domain tenant
        def tenantId = testUtils.getRandomUUID("tenant")
        def tenant = utils.createTenant(v2Factory.createTenant(tenantId, tenantId, ["cloud"]))

        // Create non-propagating role
        def role = utils.createRole()

        when: "List users for tenant"
        def response = cloud20.listUsersWithTenantId(adminToken, tenantId, new ListUsersForTenantParams(null, contactId, null), acceptMediaType)
        def users = getUsersFromResponse(response)

        then: "No users for tenant"
        response.status == HttpStatus.SC_OK
        users.user.size == 0

        when: "Add userAdmin to tenant"
        utils.addRoleToUserOnTenant(user, tenant, role.id)
        response = cloud20.listUsersWithTenantId(adminToken, tenantId, new ListUsersForTenantParams(null, contactId, null), acceptMediaType)
        users = getUsersFromResponse(response)

        then: "No users for tenant matching contactId"
        response.status == HttpStatus.SC_OK
        users.user.size == 0

        when: "Update userAdmin's contactId and list users for tenant"
        def updateUser = new User().with {
            it.id = user.id
            it.contactId = contactId
            it
        }
        utils.updateUser(updateUser)
        response = cloud20.listUsersWithTenantId(adminToken, tenantId, new ListUsersForTenantParams(null, contactId, null), acceptMediaType)
        users = getUsersFromResponse(response)

        then: "UserAdmin is returned"
        response.status == HttpStatus.SC_OK
        users.user.size == 1
        users.user.get(0).id == user.id

        when: "Update subUser's contactId and list users for tenant"
        updateUser.id = subUser.id
        utils.updateUser(updateUser)
        response = cloud20.listUsersWithTenantId(adminToken, tenantId, new ListUsersForTenantParams(null, contactId, null), acceptMediaType)
        users = getUsersFromResponse(response)

        then: "Only userAdmin is returned"
        response.status == HttpStatus.SC_OK
        users.user.size == 1
        users.user.get(0).id == user.id

        when: "Add subUser to tenant"
        utils.addRoleToUserOnTenant(subUser, tenant, role.id)
        response = cloud20.listUsersWithTenantId(adminToken, tenantId, new ListUsersForTenantParams(null, contactId, null), acceptMediaType)
        users = getUsersFromResponse(response)

        then: "Return userAdmin and subUser"
        response.status == HttpStatus.SC_OK
        users.user.size == 2
        users.user.find{it.id == user.id} != null
        users.user.find{it.id == subUser.id} != null

        when: "Disable subUser and list users in tenant"
        utils.disableUser(subUser)
        response = cloud20.listUsersWithTenantId(adminToken, tenantId, new ListUsersForTenantParams(null, contactId, null), acceptMediaType)
        users = getUsersFromResponse(response)

        then: "Only return enabled users"
        response.status == HttpStatus.SC_OK
        users.user.size == 1
        users.user.find{it.id == user.id} != null

        cleanup:
        utils.deleteUserQuietly(subUser)
        utils.deleteUserQuietly(user)
        utils.deleteTenantQuietly(tenant)
        utils.deleteRoleQuietly(role)
        utils.deleteTestDomainQuietly(domainId)

        where:
        acceptMediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "List users for tenant with contactId ignores page params: marker = #marker, limit = #limit"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        // Update userAdmin's contactId
        def contactId = testUtils.getRandomUUID("contactId")
        def updateUser = new User().with {
            it.id = userAdmin.id
            it.contactId = contactId
            it
        }
        utils.updateUser(updateUser)

        // Create a non-domain tenant and added to user
        def tenantId = testUtils.getRandomUUID("tenant")
        def tenant = utils.createTenant(v2Factory.createTenant(tenantId, tenantId, ["cloud"]))
        utils.addRoleToUserOnTenant(userAdmin, tenant)

        when:
        def response = cloud20.listUsersWithTenantId(utils.getIdentityAdminToken(), tenantId, new ListUsersForTenantParams(null, contactId, new PaginationParams(marker, limit)))
        def userList = getUsersFromResponse(response)

        then:
        userList.user.size() == 1
        userList.user.get(0).id == userAdmin.id

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
        utils.deleteTenantQuietly(tenant)

        where:
        marker | limit
        0      | 0
        2      | 1
        100    | 100
        null   | 0
        null   | 1
        0      | null
        1      | null
    }

    def "Verify list users for tenant with contactId is case insensitive"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        // Update userAdmin's contactId
        def contactId = testUtils.getRandomUUID("contactId")
        def updateUser = new User().with {
            it.id = userAdmin.id
            it.contactId = contactId
            it
        }
        utils.updateUser(updateUser)

        // Create a non-domain tenant and added to user
        def tenantId = testUtils.getRandomUUID("tenant")
        def tenant = utils.createTenant(v2Factory.createTenant(tenantId, tenantId, ["cloud"]))
        utils.addRoleToUserOnTenant(userAdmin, tenant)

        when:
        def response = cloud20.listUsersWithTenantId(utils.getIdentityAdminToken(), tenantId, new ListUsersForTenantParams(null, contactId.toUpperCase(), new PaginationParams(null, null)))
        def userList = getUsersFromResponse(response)

        then:
        userList.user.size() == 1
        userList.user.get(0).id == userAdmin.id

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
        utils.deleteTenantQuietly(tenant)
    }

    /**
     * Test that user groups are searched appropriately. Verify the following scenarios:
     * 1. Returns user that doesn't belong to any group, but gets role on tenant (user-admin receives identity:tenant-access)
     * 2. Returns user (default) that solely gets access to tenant via group membership
     * 3. Returns user (user-manage) that gets tenant access both through group membership and identity:tenant-access
     *
     * @return
     */
    def "Verify list users for tenant returns group users"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        // Create a tenant in domain that is hidden by default
        def tenantId = "faws:" + testUtils.getRandomUUID()
        def tenant = utils.createTenant(v2Factory.createTenant(tenantId, tenantId, ["faws"]).with {it.domainId = userAdmin.domainId; it})

        // Create a user group, add userManage, default user to it, add role on tenant
        def userGroup = utils.createUserGroup(userAdmin.domainId)
        utils.addUserToUserGroup(userManage.id, userGroup)
        utils.addUserToUserGroup(defaultUser.id, userGroup)
        utils.grantRoleAssignmentsOnUserGroup(userGroup, v2Factory.createSingleRoleAssignment(Constants.ROLE_RBAC1_ID, [tenantId]))

        when: "Return all users with any role on the tenant"
        def response = cloud20.listUsersWithTenantId(utils.getIdentityAdminToken(), tenantId, new ListUsersForTenantParams(null, null, new PaginationParams()))
        def userList = getUsersFromResponse(response)

        then: "returns all users granted a role on group"
        userList.user.size() == 3
        userList.user.find {it.id == userAdmin.id} != null
        userList.user.find {it.id == userManage.id} != null
        userList.user.find {it.id == defaultUser.id} != null

        when: "Return all users with specific role"
        def roleResponse = cloud20.listUsersWithTenantId(utils.getIdentityAdminToken(), tenantId, new ListUsersForTenantParams(Constants.ROLE_RBAC1_ID, null, new PaginationParams()))
        def roleUserList = getUsersFromResponse(roleResponse)

        then: "user-manage and default user returned, but not user-admin"
        roleUserList.user.size() == 2
        roleUserList.user.find {it.id == userAdmin.id} == null
        roleUserList.user.find {it.id == userManage.id} != null
        roleUserList.user.find {it.id == defaultUser.id} != null

        when: "Explicitly assign different role to default user and search on that role"
        utils.addRoleToUserOnTenantId(defaultUser, tenantId, Constants.ROLE_RBAC2_ID)
        def roleResponse2 = cloud20.listUsersWithTenantId(utils.getIdentityAdminToken(), tenantId, new ListUsersForTenantParams(Constants.ROLE_RBAC2_ID, null, new PaginationParams()))
        def roleUserList2 = getUsersFromResponse(roleResponse2)

        then: "default user returned"
        roleUserList2.user.size() == 1
        roleUserList2.user.find {it.id == userAdmin.id} == null
        roleUserList2.user.find {it.id == userManage.id} == null
        roleUserList2.user.find {it.id == defaultUser.id} != null

        cleanup:
        utils.deleteUsersQuietly([defaultUser, userManage, userAdmin, identityAdmin])
        utils.deleteUserGroup(userGroup)
        utils.deleteTenantQuietly(tenant)
        utils.deleteTestDomainQuietly(userAdmin.domainId)
    }

    @Unroll
    def "Error check - list users for tenant: mediaType = #mediaType"() {
        given:
        def adminToken = utils.getIdentityAdminToken()
        def tenant = utils.createTenant()

        when:
        def response = cloud20.listUsersWithTenantId(adminToken, tenant.id, new ListUsersForTenantParams("roleId", "contactId", null), mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, DefaultCloud20Service.LIST_USERS_FOR_TENANT_PARAM_ERROR_MESSAGE)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def getUsersFromResponse(response) {
        if(response.getType() == MediaType.APPLICATION_XML_TYPE) {
            return response.getEntity(UserList).value
        }

        UserList userList = new UserList()
        userList.user.addAll(new JsonSlurper().parseText(response.getEntity(String))["users"])

        return userList
    }
}
