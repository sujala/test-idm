package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao
import com.rackspace.idm.modules.usergroups.service.UserGroupService
import org.apache.commons.lang.RandomStringUtils
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class DeleteUserGroupRoleRestIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    UserGroupService userGroupService

    @Shared
    def sharedIdentityAdminToken

    @Shared User sharedUserAdmin
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminCloudTenant
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminFilesTenant

    @Shared UserGroup sharedUserGroup

    void doSetupSpec() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_SUPPORT_USER_GROUPS_GLOBALLY_PROP, true)

        def authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == HttpStatus.SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)

        Tenants tenants = cloud20.getDomainTenants(sharedIdentityAdminToken, sharedUserAdmin.domainId).getEntity(Tenants).value
        sharedUserAdminCloudTenant = tenants.tenant.find {
            it.id == sharedUserAdmin.domainId
        }
        sharedUserAdminFilesTenant = tenants.tenant.find() {
            it.id != sharedUserAdmin.domainId
        }
    }

    @Unroll
    def "Delete roles assigned to group; mediaType = #mediaType"() {
        setup:
        UserGroup group = assignStandardRolesToGroup()
        com.rackspace.idm.modules.usergroups.entity.UserGroup groupEntity = userGroupService.getGroupById(group.id)

        assert userGroupService.getRoleAssignmentOnGroup(groupEntity, Constants.ROLE_RBAC1_ID) != null
        assert userGroupService.getRoleAssignmentOnGroup(groupEntity, Constants.ROLE_RBAC2_ID) != null

        when: "Delete global assigned role"
        def revokeResponse = cloud20.revokeRoleAssignmentFromUserGroup(sharedIdentityAdminToken, group, Constants.ROLE_RBAC1_ID, mediaType)

        then: "Successful"
        revokeResponse.status == HttpStatus.SC_NO_CONTENT

        and:
        userGroupService.getRoleAssignmentOnGroup(groupEntity, Constants.ROLE_RBAC1_ID) == null

        when: "Delete tenant assigned role"
        def revokeResponse2 = cloud20.revokeRoleAssignmentFromUserGroup(sharedIdentityAdminToken, group, Constants.ROLE_RBAC2_ID, mediaType)

        then: "Successful"
        revokeResponse2.status == HttpStatus.SC_NO_CONTENT

        and:
        userGroupService.getRoleAssignmentOnGroup(groupEntity, Constants.ROLE_RBAC2_ID) == null

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error: Deleting role not assigned to group returns 404; mediaType = #mediaType"() {
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "addRoleTest_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup = cloud20.createUserGroup(sharedIdentityAdminToken, group).getEntity(UserGroup)

        when: "Delete global assigned role"
        def revokeResponse = cloud20.revokeRoleAssignmentFromUserGroup(sharedIdentityAdminToken, createdGroup, Constants.ROLE_RBAC1_ID, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(revokeResponse, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, "GEN-004"
                , String.format(com.rackspace.idm.modules.usergroups.Constants.ERROR_CODE_ROLE_REVOKE_NOT_FOUND_MSG_PATTERN, Constants.ROLE_RBAC1_ID))

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    UserGroup assignStandardRolesToGroup() {
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "addRoleTest_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup = cloud20.createUserGroup(sharedIdentityAdminToken, group).getEntity(UserGroup)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = Constants.ROLE_RBAC1_ID
                            ta.forTenants.add("*")
                            ta
                    })
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = Constants.ROLE_RBAC2_ID
                            ta.forTenants.add(sharedUserAdminCloudTenant.id)
                            ta.forTenants.add(sharedUserAdminFilesTenant.id)
                            ta
                    })
                    tas
            }
            it
        }

        def grantResponse = cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, assignments)
        assert grantResponse.status == HttpStatus.SC_OK

        return createdGroup
    }
}
