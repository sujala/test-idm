package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainMultiFactorEnforcementLevelEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDomain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static org.apache.http.HttpStatus.*

@ContextConfiguration(locations = "classpath:app-config.xml")
class FederatedUserManageIntegrationTest extends RootIntegrationTest {

    @Shared
    def sharedServiceAdminToken

    @Shared
    def token

    @Shared
    def domainId

    @Shared
    def sharedUserGroup

    @Shared
    def user

    @Shared
    def users

    @Shared
    def tenant

    def setup() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)
        sharedServiceAdminToken = cloud20.authenticateForToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        domainId = utils.createDomain()
        (user, users) = utils.createUserAdmin(domainId)
        sharedUserGroup = utils.createUserGroup(domainId)
        utils.grantRoleAssignmentsOnUserGroup(sharedUserGroup, v2Factory.createSingleRoleAssignment(Constants.USER_MANAGE_ROLE_ID, ['*']))
        tenant = utils.createTenant()

        AuthenticateResponse fedAuthResponse = utils.authenticateFederatedUser(domainId, [sharedUserGroup.name] as Set)
        token = fedAuthResponse.token.id
    }

    def cleanup() {
        utils.deleteUserQuietly(user)
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(domainId)
    }

    def "federated user with user-manage role" () {
        when: "lists roles"
        def response = cloud20.listRoles(token)

        then:
        response.status == SC_OK

        when: "gets role by id"
        response = cloud20.getRole(token, Constants.DEFAULT_USER_ROLE_ID)

        then:
        response.status == SC_FORBIDDEN
    }

    def "federated user with user-manage domain" () {
        when: "get domain by id"
        def response = cloud20.getDomain(token, domainId)
        def domainEntity = response.getEntity(Domain)

        then:
        response.status == SC_OK

        when: "update domain"
        domainEntity.description = "updated"
        cloud20.updateDomain(token, domainId, domainEntity)

        then:
        response.status == SC_OK
    }

    def "federated user with user-manage user-group" () {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)

        when: "create user group"
        def response = cloud20.createUserGroup(token, v2Factory.createUserGroup(domainId))
        def userGroup = response.getEntity(UserGroup)

        then:
        response.status == SC_CREATED

        when: "list user groups"
        response = cloud20.listUserGroupsForDomain(token, domainId)

        then:
        response.status == SC_OK

        when: "get user-group by id"
        response = cloud20.getUserGroup(token, userGroup)

        then:
        response.status == SC_OK

        when: "update user-group"
        userGroup.description = "updated"
        response = cloud20.updateUserGroup(token, userGroup.id, domainId, userGroup)

        then:
        response.status == SC_OK

        when: "grant role to user-group"
        response = cloud20.grantRoleAssignmentsOnUserGroup(token, userGroup, v2Factory.createSingleRoleAssignment(Constants.USER_MANAGE_ROLE_ID, ['*']))

        then:
        response.status == SC_OK

        when: "list roles on user-group"
        response = cloud20.listRoleAssignmentsOnUserGroup(token, userGroup)

        then:
        response.status == SC_OK

        when: "get roles on user-group"
        cloud20.getRoleAssignmentOnUserGroup(token, userGroup, Constants.USER_MANAGE_ROLE_ID)

        then:
        response.status == SC_OK

        when: "revoke roles on user-group"
        cloud20.revokeRoleAssignmentFromUserGroup(token, userGroup, Constants.USER_MANAGE_ROLE_ID)

        then:
        response.status == SC_OK

        when: "grant role on tenant to user-group"
        response = cloud20.grantRoleOnTenantToGroup(token, userGroup, Constants.USER_MANAGE_ROLE_ID, tenant.id)

        then:
        response.status == SC_NO_CONTENT

        when: "revoke role on tenant from user-group"
        response = cloud20.revokeRoleOnTenantToGroup(token, userGroup, Constants.USER_MANAGE_ROLE_ID, tenant.id)

        then:
        response.status == SC_NO_CONTENT

        when: "add user to user-group"
        response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == SC_NO_CONTENT

        when: "get user from user-group"
        response = cloud20.getUsersInUserGroup(token, domainId, userGroup.id)

        then:
        response.status == SC_OK

        when: "remove user from user-group"
        response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == SC_NO_CONTENT

        when: "delete user-group"
        response = cloud20.deleteUserGroup(token, userGroup)

        then:
        response.status == SC_NO_CONTENT
    }

}
