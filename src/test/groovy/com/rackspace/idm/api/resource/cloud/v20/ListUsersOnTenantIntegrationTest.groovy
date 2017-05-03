package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import org.openstack.docs.identity.api.v2.UserList
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
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
        def response = cloud20.listUsersWithTenantId(utils.getServiceAdminToken(), tenant.id, "-1", "100");

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
        def withoutRoleResponse = cloud20.listUsersWithTenantId(utils.getServiceAdminToken(), tenant.id, offset, limit)
        def withRoleResponse = cloud20.listUsersWithTenantIdAndRole(utils.getServiceAdminToken(), tenant.id, role.id, offset, limit)

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
        offset  | limit     | numUsersExpected
        "0"     | "0"       | 6
        "0"     | "1"       | 1
        "0"     | "5"       | 5
        "0"     | "6"       | 6
        "1"     | "0"       | 5
        "1"     | "1"       | 1
        "1"     | "5"       | 5
        "1"     | "6"       | 5
        "5"     | "0"       | 1
        "5"     | "1"       | 1
        "6"     | "0"       | 0
        "6"     | "1"       | 0
    }

    /**
     * This tests the implicit assignment of identity:tenant-access role to all tenants within a user's domain
     *
     * @return
     */
    def "List Users for Tenants: Automatically returns all users within a tenant's domain" () {
        given: "A new user and 2 tenants"
        reloadableConfiguration.setProperty(IdentityConfig.AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_PROP, "identity:tenant-access")
        def adminToken = utils.getIdentityAdminToken()
        def username = testUtils.getRandomUUID("name")
        def domainId = testUtils.getRandomUUID("domainId")
        def user = utils.createUser(adminToken, username, domainId)
        def tenantId1 = testUtils.getRandomUUID("tenant")

        def tenant1 = utils.createTenant(v2Factory.createTenant(tenantId1, tenantId1, ["type1"]).with {
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
        reloadableConfiguration.setProperty(IdentityConfig.AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_PROP, "identity:tenant-access")
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RESTRICT_CREATE_USER_IN_DEFAULT_DOMAIN_PROP, false)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_RESTRICT_CREATE_USER_IN_DOMAIN_WITH_USERS_PROP, false)
        def adminToken = utils.getIdentityAdminToken()
        def domainId = identityConfig.getReloadableConfig().getTenantDefaultDomainId()

        def username = testUtils.getRandomUUID("name")
        def user = utils.createUser(adminToken, username, domainId)

        def tenantId1 = testUtils.getRandomUUID("tenant")
        def tenant1 = utils.createTenant(v2Factory.createTenant(tenantId1, tenantId1, ["type1"]).with {
            it.domainId = domainId
            it
        })

        def userAdminToken = utils.getToken(username)

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
        reloadableConfiguration.setProperty(IdentityConfig.AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_PROP, "identity:tenant-access")
        def adminToken = utils.getIdentityAdminToken()
        def username = testUtils.getRandomUUID("name")
        def domainId = testUtils.getRandomUUID("domainId")
        def user = utils.createUser(adminToken, username, domainId)
        def userAdminToken = utils.getToken(username)

        // Create subuser & explicitly assign the role
        def subUser = utils.createUser(userAdminToken, testUtils.getRandomUUID("defaultUser"), domainId)

        // Create tenant on which to test access
        def tenantId1 = testUtils.getRandomUUID("tenant")
        def tenant1 = utils.createTenant(v2Factory.createTenant(tenantId1, tenantId1, ["type1"]).with {
            it.domainId = domainId
            it
        })

        when: "List users for tenant w/ feature enabled"
        def response = cloud20.listUsersWithTenantIdAndRole(adminToken, tenantId1, Constants.IDENTITY_TENANT_ACCESS_ROLE_ID)

        then: "All users in same domain are returned"
        assert response.status == 200
        def usersEntity3 = getUsersFromResponse(response)
        usersEntity3.user.size == 2
        usersEntity3.user.find {it.id == user.id} != null
        usersEntity3.user.find {it.id == subUser.id} != null

        when: "Add new subuser, list users for tenant w/ feature enabled returns new user"
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

    def getUsersFromResponse(response) {
        def users = response.getEntity(UserList)

        if(response.getType() == MediaType.APPLICATION_XML_TYPE) {
            users = users.value
        }
        return users
    }

}
