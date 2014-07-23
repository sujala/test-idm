package com.rackspace.idm.api.resource.cloud.v20

import org.openstack.docs.identity.api.v2.UserList
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

class ListUsersOnTenantIntegrationTest extends RootIntegrationTest {

    @Shared def tenant

    def setup() {
        tenant = utils.createTenant()
    }

    def cleanup() {
        utils.deleteTenant(tenant)
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

}
