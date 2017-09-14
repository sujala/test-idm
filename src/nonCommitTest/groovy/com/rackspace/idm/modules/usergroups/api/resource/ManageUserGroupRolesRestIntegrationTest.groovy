package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.exception.ForbiddenException
import org.apache.commons.lang.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class ManageUserGroupRolesRestIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Shared
    def sharedIdentityAdminToken

    @Shared User sharedUserAdmin
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminCloudTenant
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminFilesTenant

    void doSetupSpec() {
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
    def "grant role to user group; mediaType = #mediaType"() {
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "addRoleTest_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup = cloud20.createUserGroup(sharedIdentityAdminToken, group, mediaType).getEntity(UserGroup)

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
                            ta
                    })
                    tas
            }
            it
        }

        when:
        def getResponse = cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, assignments, mediaType)

        then:
        getResponse.status == HttpStatus.SC_OK
        RoleAssignments retrievedEntity = getResponse.getEntity(RoleAssignments)

        and:
        retrievedEntity.tenantAssignments != null
        def rbac1Assignment = retrievedEntity.tenantAssignments.tenantAssignment.find {it.onRole == Constants.ROLE_RBAC1_ID}
        rbac1Assignment != null
        rbac1Assignment.forTenants.size() == 1
        rbac1Assignment.forTenants[0] == "*"

        def rbac2Assignment = retrievedEntity.tenantAssignments.tenantAssignment.find {it.onRole == Constants.ROLE_RBAC2_ID}
        rbac2Assignment != null
        rbac2Assignment.forTenants.size() == 1
        rbac2Assignment.forTenants[0] == sharedUserAdminCloudTenant.id

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error: Not allowed to grant user-manage role to user group; mediaType = #mediaType"() {
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "addRoleTest_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup = cloud20.createUserGroup(sharedIdentityAdminToken, group, mediaType).getEntity(UserGroup)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = Constants.USER_MANAGE_ROLE_ID
                            ta.forTenants.add("*")
                            ta
                    })
                    tas
            }
            it
        }

        when:
        def response = cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN
                , com.rackspace.idm.modules.usergroups.Constants.ERROR_CODE_USER_GROUPS_INVALID_ATTRIBUTE
                , String.format(com.rackspace.idm.modules.usergroups.Constants.ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, Constants.USER_MANAGE_ROLE_ID));

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }
}
