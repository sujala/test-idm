package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.idm.Constants
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.ScopeAccessService
import org.apache.http.HttpStatus
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

    def "Allow using a DA token to list legacy user groups"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)

        def userAdmin = utils.createCloudAccount()
        def userAdmin2 = utils.createCloudAccount()

        // Update both domains to same RCN
        def rcn = testUtils.getRandomRCN()
        utils.domainRcnSwitch(userAdmin.domainId, rcn)
        utils.domainRcnSwitch(userAdmin2.domainId, rcn)

        def userAdminToken = utils.getToken(userAdmin.username)

        // Create DA with user principal
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)

        // Add userAdmin2 as a delegate
        utils.addUserDelegate(userAdminToken, createdDA.id, userAdmin2.id)

        // Add rate limiting group to userAdmin
        Group group = utils.createGroup()
        utils.addUserToGroup(group, userAdmin)

        def userAdmin2Token = utils.getToken(userAdmin2.username)

        // Get token authenticated under a DA
        AuthenticateResponse authenticateResponse = utils.authenticateTokenAndDelegationAgreement(userAdmin2Token, createdDA.id)
        def daToken = authenticateResponse.token.id

        when: "list groups using DA token"
        def response = cloud20.listGroupsForUser(daToken, userAdmin2.id)
        Groups groups = testUtils.getEntity(response, Groups)

        then: "Expect groups assigned to user-admin for domain under which the DA token was authenticated"
        response.status == HttpStatus.SC_OK
        groups.group.size() == 2
        groups.group.find{it.id == group.id} != null
        groups.group.find{it.id == Constants.DEFAULT_GROUP_ID} != null

        when: "list groups for userAdmin2"
        response = cloud20.listGroupsForUser(userAdmin2Token, userAdmin2.id)
        groups = testUtils.getEntity(response, Groups)

        then: "Expect default group"
        response.status == HttpStatus.SC_OK
        groups.group.size() == 1
        groups.group.find{it.id == Constants.DEFAULT_GROUP_ID} != null

        cleanup:
        utils.deleteUserQuietly(userAdmin)
        utils.deleteUserQuietly(userAdmin2)
        utils.deleteGroup(group)
    }

    def "Error check: list groups using DA token"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)

        def userAdmin = utils.createCloudAccount()
        def userAdmin2 = utils.createCloudAccount()

        // Update both domains to same RCN
        def rcn = testUtils.getRandomRCN()
        utils.domainRcnSwitch(userAdmin.domainId, rcn)
        utils.domainRcnSwitch(userAdmin2.domainId, rcn)

        def userAdminToken = utils.getToken(userAdmin.username)

        // Create DA with user principal
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)

        // Add userAdmin2 as a delegate
        utils.addUserDelegate(userAdminToken, createdDA.id, userAdmin2.id)

        // Add rate limiting group to userAdmin
        Group group = utils.createGroup()
        utils.addUserToGroup(group, userAdmin)

        def userAdmin2Token = utils.getToken(userAdmin2.username)

        // Get token authenticated under a DA
        AuthenticateResponse authenticateResponse = utils.authenticateTokenAndDelegationAgreement(userAdmin2Token, createdDA.id)
        def daToken = authenticateResponse.token.id

        when: "list groups with invalid user"
        def response = cloud20.listGroupsForUser(daToken, userAdmin.id)

        then: "return forbidden"
        response.status == HttpStatus.SC_FORBIDDEN

        cleanup:
        utils.deleteUserQuietly(userAdmin)
        utils.deleteUserQuietly(userAdmin2)
        utils.deleteGroup(group)
    }
}
