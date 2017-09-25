package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import org.openstack.docs.identity.api.v2.IdentityFault
import testHelpers.RootIntegrationTest

class DeleteClientRoleIntegrationTest extends RootIntegrationTest {

    def "cannot delete a client role if the role is assigned to a user group"() {
        given:
        def domain = utils.createDomainEntity()
        def role = utils.createRole()
        def group = utils.createUserGroup(domain.id)
        def roleAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                it.tenantAssignment << new TenantAssignment().with {
                    it.onRole = role.id
                    it.forTenants << '*'
                    it
                }
                it
            }
            it
        }
        cloud20.grantRoleAssignmentsOnUserGroup(utils.getIdentityAdminToken(), group, roleAssignments)

        when:
        def response = cloud20.deleteRole(utils.getServiceAdminToken(), role.id)

        then:
        response.status == 403
        response.getEntity(IdentityFault).value.message == DefaultCloud20Service.ERROR_DELETE_ROLE_WITH_GROUPS_ASSIGNED

        cleanup:
        utils.deleteUserGroup(group)
        utils.deleteRole(role)
    }

}
