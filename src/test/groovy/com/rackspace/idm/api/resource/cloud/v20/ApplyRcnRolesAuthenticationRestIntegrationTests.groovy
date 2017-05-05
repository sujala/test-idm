package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

class ApplyRcnRolesAuthenticationRestIntegrationTests extends RootIntegrationTest {

    def cleanup() {
        reloadableConfiguration.reset()
    }

    def "apply_rcn_roles logic only used when feature.performant.service.catalog is enabled"() {
        given:
        def user = utils.createCloudAccount(utils.getIdentityAdminToken())

        when: "auth as standard user-admin with feature disabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_PERFORMANT_SERVICE_CATALOG_PROP, false)
        AuthenticateResponse response = utils.authenticateApplyRcnRoles(user.username)

        then: "identity:admin role is a global role"
        def idAdminRole = response.user.roles.role.find {it.name == IdentityUserTypeEnum.USER_ADMIN.roleName}
        idAdminRole != null
        idAdminRole.getTenantId() == null

        when: "auth as standard user-admin with feature enabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_PERFORMANT_SERVICE_CATALOG_PROP, true)
        AuthenticateResponse response2 = utils.authenticateApplyRcnRoles(user.username)

        then: "identity:user-admin role is not a global role"
        def idAdminRole2 = response2.user.roles.role.find {it.name == IdentityUserTypeEnum.USER_ADMIN.roleName}
        idAdminRole2 != null
        idAdminRole2.getTenantId() != null

        cleanup:
        utils.deleteUserQuietly(user)
    }

    /**
     * Tests whether the rcn role logic is applied to the user's roles based on query param. Does this by using
     * the user's Identity Classification role is denormalized and set on individual tenants as the canary
     * @return
     */
    @Unroll
    def "apply_rcn_roles logic only used when apply_rcn_roles = true (case insensitive): value: #param"() {
        given:
        def user = utils.createCloudAccount(utils.getIdentityAdminToken())

        when: "auth as standard user-admin with feature enabled"
        AuthenticateResponse response2 = utils.authenticateApplyRcnRoles(user.username, param)

        then: "applied"
        def idAdminRole2 = response2.user.roles.role.find {it.name == IdentityUserTypeEnum.USER_ADMIN.roleName}
        idAdminRole2 != null
        if (applied) {
            assert idAdminRole2.getTenantId() != null
        } else {
            assert idAdminRole2.getTenantId() == null
        }

        cleanup:
        utils.deleteUserQuietly(user)

        where:
        param | applied
        "true"  | true
        "TRUE"  | true
        "truE"  | true
        "false"  | false
        "norunme"  | false
        null  | false
    }

    def "apply_rcn_roles causes no global roles to be returned"() {
        given:
        def user = utils.createCloudAccount(utils.getIdentityAdminToken())

        when: "auth as standard user-admin with feature enabled"
        AuthenticateResponse response = utils.authenticateApplyRcnRoles(user.username)

        then: "All resultant roles are tenant based roles"
        response.user.roles.role.each {
            assert it.tenantId != null
        }

        cleanup:
        utils.deleteUserQuietly(user)
    }
}
