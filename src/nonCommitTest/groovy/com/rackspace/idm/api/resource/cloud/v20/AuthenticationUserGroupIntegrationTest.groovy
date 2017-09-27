package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.*
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.commons.collections4.CollectionUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.ROLE_RBAC1_ID
import static com.rackspace.idm.Constants.ROLE_RBAC2_ID

class AuthenticationUserGroupIntegrationTest extends RootIntegrationTest {

    @Autowired
    ScopeAccessDao scopeAccessDao

    @Autowired
    IdentityUserService identityUserService

    @Autowired
    EndpointDao endpointDao

    @Autowired
    ApplicationDao applicationDao

    @Autowired
    ApplicationRoleDao applicationRoleDao

    @Autowired
    DomainDao domainDao

    @Autowired
    IdentityConfig identityConfig

    @Shared
    def sharedIdentityAdminToken

    void doSetupSpec() {
        def authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == org.apache.http.HttpStatus.SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id
    }

    /**
     * Test retrieving roles via user group membership
     */
    def "Group roles only"() {
        def sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)

        Tenants tenants = cloud20.getDomainTenants(sharedIdentityAdminToken, sharedUserAdmin.domainId).getEntity(Tenants).value
        def sharedUserAdminCloudTenant = tenants.tenant.find {
            it.id == sharedUserAdmin.domainId
        }
        def sharedUserAdminFilesTenant = tenants.tenant.find() {
            it.id != sharedUserAdmin.domainId
        }

        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "addRoleTest_" + org.apache.commons.lang.RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup = cloud20.createUserGroup(sharedIdentityAdminToken, group).getEntity(UserGroup)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [sharedUserAdminFilesTenant.id]))
                    tas
            }
            it
        }
        def grantResponse = cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, assignments)
        assert grantResponse.status == HttpStatus.SC_OK

        when: "auth without membership to group"
        AuthenticateResponse authResponse = utils.authenticate(sharedUserAdmin)

        then:
        authResponse.user.roles.role.findAll() {it.id == ROLE_RBAC1_ID}.size() == 0
        authResponse.user.roles.role.findAll() {it.id == ROLE_RBAC2_ID}.size() == 0

        when: "auth after becoming member w/o group calculation enabled"
        def addMembershipResponse = cloud20.addUserToUserGroup(sharedIdentityAdminToken, sharedUserAdmin.domainId, createdGroup.id, sharedUserAdmin.id)
        assert addMembershipResponse.status == HttpStatus.SC_NO_CONTENT
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_APPLY_GROUP_MEMBERSHIP_ROLES_PROP, false)
        authResponse = utils.authenticate(sharedUserAdmin)

        then: "Still do not have group roles"
        authResponse.user.roles.role.findAll() {it.id == ROLE_RBAC1_ID}.size() == 0
        authResponse.user.roles.role.findAll() {it.id == ROLE_RBAC2_ID}.size() == 0

        when: "enable group calculation"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_APPLY_GROUP_MEMBERSHIP_ROLES_PROP, true)
        authResponse = utils.authenticate(sharedUserAdmin)

        then: "Have group roles"
        verifyContainsAssignment(authResponse, ROLE_RBAC1_ID, [])
        verifyContainsAssignment(authResponse, ROLE_RBAC2_ID, [sharedUserAdminFilesTenant.id])
    }

    /**
     * Verifies the authenticate response contains the assignment. An empty list of tenantIds means a "global" assignment
     * where the role appears once, with no tenantId value.
     *
     * If a non-empty tenantIds collection is provided it is expected that a distinct role entry will be returned for
     * each tenant provided.
     *
     * @param authenticateResponse
     * @param roleId
     * @param tenantIds
     */
    void verifyContainsAssignment(AuthenticateResponse authenticateResponse, String roleId, List<String> tenantIds) {
        if (CollectionUtils.isEmpty(tenantIds)) {
            List<Role> roles = authenticateResponse.user.roles.role.findAll() {it.id == roleId}
            assert roles != null
            assert roles.size() == 1
            assert roles[0].tenantId == null
        } else {
            List<Role> roles = authenticateResponse.user.roles.role.findAll() {it.id == roleId}
            assert roles != null
            assert roles.size() == tenantIds.size()

            // Search for each tenantId individually
            for (String tenantId : tenantIds) {
                def role = authenticateResponse.user.roles.role.find() {it.id == roleId && tenantId.equalsIgnoreCase(it.tenantId)}
                assert role != null
            }
        }
    }

    TenantAssignment createTenantAssignment(String roleId, List<String> tenants) {
        def assignment = new TenantAssignment().with {
            ta ->
                ta.onRole = roleId
                ta.forTenants.addAll(tenants)
                ta
        }
        return assignment
    }

}
