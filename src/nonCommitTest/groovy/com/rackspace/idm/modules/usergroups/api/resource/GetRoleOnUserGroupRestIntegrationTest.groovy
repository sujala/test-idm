package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.commons.lang.RandomStringUtils
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

import static com.rackspace.idm.Constants.ROLE_RBAC1_ID
import static com.rackspace.idm.Constants.ROLE_RBAC1_NAME
import static com.rackspace.idm.Constants.ROLE_RBAC2_ID
import static com.rackspace.idm.Constants.ROLE_RBAC2_NAME


class GetRoleOnUserGroupRestIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Shared
    def sharedIdentityAdminToken

    @Shared
    User sharedUserAdmin

    @Shared
    org.openstack.docs.identity.api.v2.Tenant sharedUserAdminCloudTenant
    @Shared
    org.openstack.docs.identity.api.v2.Tenant sharedUserAdminFilesTenant

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
    def "Get role on userGroup; #mediaType"() {

        given:
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "getRoleOnGroup_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup = cloud20.createUserGroup(sharedIdentityAdminToken, group, mediaType).getEntity(UserGroup)

        RoleAssignments assignments = new RoleAssignments().with {
            TenantAssignments tas = new TenantAssignments()
            tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, ROLE_RBAC1_NAME, [sharedUserAdminCloudTenant.id, sharedUserAdminFilesTenant.id]))
            tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, ROLE_RBAC1_NAME, ["*"]))
            it.tenantAssignments = tas
            it
        }

        cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, assignments, mediaType)

        when:
        def response = cloud20.getRoleAssignmentOnUserGroup(sharedIdentityAdminToken, createdGroup, ROLE_RBAC1_ID, mediaType)
        TenantAssignment tenantAssignment = response.getEntity(TenantAssignment)

        then:
        response.status == HttpStatus.SC_OK
        tenantAssignment.getOnRole().equals(ROLE_RBAC1_ID)
        tenantAssignment.getOnRoleName().equals(ROLE_RBAC1_NAME)
        tenantAssignment.getForTenants().size() == 2

        when:
        response = cloud20.getRoleAssignmentOnUserGroup(sharedIdentityAdminToken, createdGroup, ROLE_RBAC2_ID, mediaType)
        tenantAssignment = response.getEntity(TenantAssignment)

        then:
        response.status == HttpStatus.SC_OK
        tenantAssignment.getOnRole().equals(ROLE_RBAC2_ID)
        tenantAssignment.getOnRoleName().equals(ROLE_RBAC2_NAME)
        tenantAssignment.getForTenants().size() == 1
        tenantAssignment.getForTenants().get(0) == "*"

        cleanup:
        utils.deleteUserGroup(createdGroup)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error on Get role on userGroup; #mediaType"() {

        given:
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "getRoleOnGroup_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup = cloud20.createUserGroup(sharedIdentityAdminToken, group, mediaType).getEntity(UserGroup)

        UserGroup anotherGroup = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "getRoleOnGroup1_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def anotherCreatedGroup = cloud20.createUserGroup(sharedIdentityAdminToken, anotherGroup, mediaType).getEntity(UserGroup)

        RoleAssignments assignments = new RoleAssignments().with {
            TenantAssignments tas = new TenantAssignments()
            tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, ROLE_RBAC1_NAME, [sharedUserAdminCloudTenant.id, sharedUserAdminFilesTenant.id]))
            tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, ROLE_RBAC1_NAME, ["*"]))
            it.tenantAssignments = tas
            it
        }

        cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, assignments, mediaType)

        when:
        def response = cloud20.getRoleAssignmentOnUserGroup(sharedIdentityAdminToken, createdGroup, ROLE_RBAC1_ID, mediaType)

        then:
        response.status == HttpStatus.SC_OK

        when: "Invalid role returns 404"
        response = cloud20.getRoleAssignmentOnUserGroup(sharedIdentityAdminToken, createdGroup, "12345", mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, "Role with ID 12345 not found on the user group with ID " + createdGroup.id + ".")

        when: "Valid role assigned to a different group returns 404"
        response = cloud20.getRoleAssignmentOnUserGroup(sharedIdentityAdminToken, anotherCreatedGroup, ROLE_RBAC1_ID, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, "Role with ID " + ROLE_RBAC1_ID + " not found on the user group with ID " + anotherCreatedGroup.id + ".")

        cleanup:
        utils.deleteUserGroup(createdGroup)
        utils.deleteUserGroup(anotherCreatedGroup)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error check: Get role on userGroup with invalid mediaType; mediaType = #mediaType"() {
        given:
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "getRoleOnGroup_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup = cloud20.createUserGroup(sharedIdentityAdminToken, group, MediaType.APPLICATION_JSON_TYPE).getEntity(UserGroup)

        when: "Invalid media type, mediaType - #mediaType"
        def response =  cloud20.getRoleAssignmentOnUserGroup(sharedIdentityAdminToken, createdGroup, ROLE_RBAC1_ID, mediaType)

        then:
        response.status == HttpStatus.SC_NOT_ACCEPTABLE

        where:
        mediaType << [MediaType.TEXT_PLAIN_TYPE, GlobalConstants.TEXT_YAML_TYPE]
    }

    TenantAssignment createTenantAssignment(String roleId, String roleName, List<String> tenants) {
        def assignment = new TenantAssignment().with {
            ta ->
                ta.onRole = roleId
                ta.onRoleName = roleName
                ta.forTenants.addAll(tenants)
                ta
        }
        return assignment
    }
}
