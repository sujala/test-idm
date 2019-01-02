package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainMultiFactorEnforcementLevelEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDomain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import org.apache.commons.lang.RandomStringUtils
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory

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

    @Autowired
    TenantRoleDao tenantRoleDao

    @Autowired
    UserGroupDao userGroupDao

    @Autowired
    ApplicationRoleDao roleDao

    def setup() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEATURE_ENABLE_EXTERNAL_USER_IDP_MANAGEMENT_PROP, true)

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)
        sharedServiceAdminToken = cloud20.authenticateForToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        domainId = utils.createDomain()
        (user, users) = utils.createUserAdmin(domainId)
        sharedUserGroup = utils.createUserGroup(domainId)

        //TODO: Update once user-manage is allowed to be set on a user-group
        //utils.grantRoleAssignmentsOnUserGroup(sharedUserGroup, v2Factory.createSingleRoleAssignment(Constants.USER_MANAGE_ROLE_ID, ['*']))
        grantRoleAssignmentsOnUserGroup(sharedUserGroup)

        tenant = utils.createTenant()

        AuthenticateResponse fedAuthResponse = utils.authenticateFederatedUser(domainId, [sharedUserGroup.name] as Set)
        token = fedAuthResponse.token.id
    }

    private void grantRoleAssignmentsOnUserGroup(UserGroup sharedUserGroup) {
        def userGroup = userGroupDao.getGroupById(sharedUserGroup.id)
        def role = roleDao.getRoleByName(Constants.USER_MANAGE_ROLE_NAME)
        RoleAssignments tenantAssignments = v2Factory.createSingleRoleAssignment(Constants.USER_MANAGE_ROLE_ID, ['*'])
        def tenantAssignment = tenantAssignments.tenantAssignments.tenantAssignment.get(0)

        TenantRole tenantRole = new TenantRole();
        tenantRole.setRoleRsId(tenantAssignment.getOnRole());
        tenantRole.setClientId(role.getClientId());
        tenantRole.setName(role.getName());
        tenantRole.setDescription(role.getDescription());
        tenantRole.setRoleType(role.getRoleType());

        tenantRoleDao.addRoleAssignmentOnGroup(userGroup, tenantRole);
    }

    def cleanup() {
        utils.deleteUserQuietly(user)
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(domainId)
    }

    def "federated user with user-manage - role" () {
        when: "lists roles"
        def response = cloud20.listRoles(token)

        then:
        response.status == SC_OK

        when: "gets role by id"
        response = cloud20.getRole(token, Constants.DEFAULT_USER_ROLE_ID)

        then:
        response.status == SC_OK
    }

    def "federated user with user-manage - domain" () {
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

    def "federated user with user-manage - user-group" () {
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
        response = cloud20.grantRoleAssignmentsOnUserGroup(token, userGroup, v2Factory.createSingleRoleAssignment(Constants.ADMIN_ROLE_ID, ['*']))

        then:
        response.status == SC_OK

        when: "list roles on user-group"
        response = cloud20.listRoleAssignmentsOnUserGroup(token, userGroup)

        then:
        response.status == SC_OK

        when: "get roles on user-group"
        cloud20.getRoleAssignmentOnUserGroup(token, userGroup, Constants.ADMIN_ROLE_ID)

        then:
        response.status == SC_OK

        when: "revoke roles on user-group"
        cloud20.revokeRoleAssignmentFromUserGroup(token, userGroup, Constants.ADMIN_ROLE_ID)

        then:
        response.status == SC_OK

        when: "grant role on tenant to user-group"
        response = cloud20.grantRoleOnTenantToGroup(token, userGroup, Constants.ADMIN_ROLE_ID, tenant.id)

        then:
        response.status == SC_NO_CONTENT

        when: "revoke role on tenant from user-group"
        response = cloud20.revokeRoleOnTenantToGroup(token, userGroup, Constants.ADMIN_ROLE_ID, tenant.id)

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

    def "federated user with user-manage - multi-factor" () {
        when: "update multi-factor settings"
        MultiFactorDomain settings = v2Factory.createMultiFactorDomainSettings()
        settings.domainMultiFactorEnforcementLevel = DomainMultiFactorEnforcementLevelEnum.REQUIRED
        def response = cloud20.updateMultiFactorDomainSettings(token, domainId, settings)

        then:
        response.status == SC_NO_CONTENT
    }

    def "federated user with user-manage - identity-provider" () {
        given:
        def identityProvider = utils.createIdentityProvider(sharedServiceAdminToken, IdentityProviderFederationTypeEnum.DOMAIN, domainId)
        String metadata = new SamlFactory().generateMetadataXMLForIDP(testUtils.getRandomUUID("http://example.com/"), RandomStringUtils.randomAlphabetic(8))

        when: "create identity-provider"
        def identityProviderToCreate = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, [domainId] as List)
        def response = cloud20.createIdentityProvider(token, identityProviderToCreate)

        then:
        response.status == SC_FORBIDDEN

        when: "create identity-provider with metadata"
        response = cloud20.createIdentityProviderWithMetadata(token, metadata)
        def idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "delete identity-provider created from metadata"
        response = cloud20.deleteIdentityProvider(token, idp.id)

        then:
        response.status == SC_NO_CONTENT

        when: "list identity-providers"
        response = cloud20.listIdentityProviders(token)

        then:
        response.status == SC_OK

        when: "get identity-provider"
        response = cloud20.getIdentityProvider(token, identityProvider.id)

        then:
        response.status == SC_OK

        when: "update identity-provider"
        identityProvider.description = "updated"
        response = cloud20.updateIdentityProvider(token, identityProvider.id, identityProvider)

        then:
        response.status == SC_OK

        when: "get identity-provider policy"
        response = cloud20.getIdentityProviderPolicy(token, identityProvider.id)

        then:
        response.status == SC_OK

        when: "update identity-provider policy"
        def policy = '{"policy": "updated"}'
        response = cloud20.updateIdentityProviderPolicy(token, identityProvider.id, policy)

        then:
        response.status == SC_NO_CONTENT

        when: "update identity-provider metadata"
        metadata = new SamlFactory().generateMetadataXMLForIDP(identityProvider.issuer, RandomStringUtils.randomAlphabetic(8))
        response = cloud20.updateIdentityProviderUsingMetadata(token, identityProvider.id, metadata)

        then:
        response.status == SC_OK

        when: "get identity-provider metadata"
        response = cloud20.getIdentityProviderMetadata(token, identityProvider.id)

        then:
        response.status == SC_OK

        when: "delete identity-provider"
        response = cloud20.deleteIdentityProvider(token, identityProvider.id)

        then:
        response.status == SC_NO_CONTENT
    }
}
