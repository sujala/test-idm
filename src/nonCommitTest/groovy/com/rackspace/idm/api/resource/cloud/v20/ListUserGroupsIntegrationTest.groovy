package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.ScopeAccessService
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.UserList
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.RootIntegrationTest

class ListUserGroupsIntegrationTest extends RootIntegrationTest {
    @Autowired
    ScopeAccessService scopeAccessService

    @Autowired
    AuthorizationService authorizationService

    def "test authorization for list user group"() {
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin()
        AuthenticateResponse auth = utils.authenticate(userAdmin)

        UserScopeAccess token = scopeAccessService.getScopeAccessByAccessToken(auth.token.id)
        String uaToken = token.accessTokenString
        String iaToken = utils.getToken(users[0].username)

        def userRole = authorizationService.getCachedIdentityRoleByName(IdentityRole.GET_USER_GROUPS_GLOBAL.getRoleName())

        when: "user admin tries to load identity admin groups"
        def uaResponse = cloud20.listGroupsForUser(uaToken, users[0].id)

        then: "forbidden"
        uaResponse.status == org.apache.http.HttpStatus.SC_FORBIDDEN

        when: "user admin tries to list groups for self"
        uaResponse = cloud20.listGroupsForUser(uaToken, userAdmin.id)

        then: "success"
        uaResponse != null
        uaResponse.status == org.apache.http.HttpStatus.SC_OK

        when: "give user global groups role"
        utils.addRoleToUser(userAdmin, userRole.id)
        uaResponse = cloud20.listGroupsForUser(uaToken, users[0].id)

        then: "user admin can now access groups"
        uaResponse != null
        uaResponse.status == org.apache.http.HttpStatus.SC_OK

        when: "identity admin tries to load groups of user admin"
        def iaResponse = cloud20.listGroupsForUser(iaToken, userAdmin.id)

        then: "allowed"
        iaResponse.status == org.apache.http.HttpStatus.SC_OK

        cleanup:
        reloadableConfiguration.reset()
        try {
            utils.deleteUsers(users)
        } catch (Exception ex) {/*ignore*/
        }
    }

    def "test list users in group with pagination"() {
        given:
        def group = utils.createGroup()
        def domain = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domain)
        utils.addUserToGroup(group, userAdmin)

        when: "index into the only user in the group"
        def response = cloud20.getUsersFromGroup(utils.getServiceAdminToken(), group.id, "10", "0")

        then:
        response.status == 200
        def userList = response.getEntity(UserList).value
        userList.user.size() == 1
        userList.user[0].id == userAdmin.id

        when: "index out of the list of users"
        response = cloud20.getUsersFromGroup(utils.getServiceAdminToken(), group.id, "10", "1")

        then:
        response.status == 200
        def userList2 = response.getEntity(UserList).value
        userList2.user.size() == 0

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domain)
        utils.deleteGroup(group)
    }
}
