package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.ApplicationService
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.*
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static Constants.ROLE_RBAC1_ID
import static Constants.ROLE_RBAC2_ID

class ManageUserGroupRolesRestIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    ApplicationService applicationService

    @Shared
    def sharedIdentityAdminToken

    @Shared User sharedUserAdmin
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminCloudTenant
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminFilesTenant

    void doSetupSpec() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)

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

    /**
     * Test a typical modification to the set of roles assign to a user group
     *
     * 1. No roles -> 1 tenant assigned role : Verify result includes that role assignment
     * 2. Assign 1 global role : Verify both roles returned appropriately
     * 3. Update tenant assigned role to be a global assigned role, and global to be assigned to 2 tenants: Verify result
     * 4. Update tenant assigned role to only have single tenant; verify result as expected
     * 5. Reset tenant to global : Verify result
     *
     * Test all this through both json and xml to verify can appriately reflect the states
     */
    @Unroll
    def "modify roles on user group; mediaType = #mediaType"() {
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "addRoleTest_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup = cloud20.createUserGroup(sharedIdentityAdminToken, group, mediaType).getEntity(UserGroup)

        RoleAssignments assignments0 = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments()
            it
        }

        RoleAssignments assignments1 = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [sharedUserAdminCloudTenant.id]))
            it.tenantAssignments = ta
            it
        }

        RoleAssignments assignments2 = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, ["*"]))
                    tas
            }
            it
        }

        RoleAssignments assignments3 = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [sharedUserAdminCloudTenant.id, sharedUserAdminFilesTenant.id]))
                    tas
            }
            it
        }

        RoleAssignments assignments4 = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [sharedUserAdminCloudTenant.id]))
                    tas
            }
            it
        }

        RoleAssignments assignments5 = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, ["*"]))
                    tas
            }
            it
        }

        when: "assignment 0"
        def getResponse = cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, assignments0, mediaType)
        RoleAssignments retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0

        when: "assignment 1"
        getResponse = cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, assignments1, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [sharedUserAdminCloudTenant.id])
        def rbac2Assignment = retrievedEntity.tenantAssignments.tenantAssignment.find {it.onRole == ROLE_RBAC2_ID}
        rbac2Assignment == null

        when: "assignment 2"
        getResponse = cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, assignments2, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [sharedUserAdminCloudTenant.id])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, ["*"])

        when: "assignment 3"
        getResponse = cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, assignments3, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, [sharedUserAdminCloudTenant.id, sharedUserAdminFilesTenant.id])

        when: "assignment 4"
        getResponse = cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, assignments4, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, [sharedUserAdminCloudTenant.id])

        when: "assignment 5"
        getResponse = cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, assignments5, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, ["*"])

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

    @Unroll
    def "Error: No forTenants value on role assignment returns error; mediaType = #mediaType"() {
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "addRoleTest_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup = cloud20.createUserGroup(sharedIdentityAdminToken, group, mediaType).getEntity(UserGroup)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, []))
                    tas
            }
            it
        }

        when:
        def response = cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST
                , com.rackspace.idm.modules.usergroups.Constants.ERROR_CODE_USER_GROUPS_MISSING_REQUIRED_ATTRIBUTE
                , com.rackspace.idm.modules.usergroups.Constants.ERROR_CODE_ROLE_ASSIGNMENT_MISSING_FOR_TENANTS_MSG)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Grant role on tenant for user group"() {
        given:
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "grantRoleOnTenant" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup = cloud20.createUserGroup(sharedIdentityAdminToken, group).getEntity(UserGroup)

        when: "Granting new role on tenant to user group"
        def response = cloud20.grantRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup, ROLE_RBAC1_ID, sharedUserAdminCloudTenant.id)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "List role on user group"
        def listResponse = cloud20.listRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, null)

        then:
        listResponse.status == HttpStatus.SC_OK
        RoleAssignments retrievedEntity = listResponse.getEntity(RoleAssignments)

        and: "Retrieves roles"
        retrievedEntity.tenantAssignments != null
        def rbac1Assignment = retrievedEntity.tenantAssignments.tenantAssignment.find {it.onRole == Constants.ROLE_RBAC1_ID}
        rbac1Assignment != null
        rbac1Assignment.forTenants.size() == 1
        rbac1Assignment.forTenants[0] == sharedUserAdminCloudTenant.id

        when: "Granting role on tenant to user group - existing role"
        response = cloud20.grantRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup, ROLE_RBAC1_ID, sharedUserAdminFilesTenant.id)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "List role on user group"
        listResponse = cloud20.listRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, null)
        retrievedEntity = listResponse.getEntity(RoleAssignments)
        rbac1Assignment = retrievedEntity.tenantAssignments.tenantAssignment.find {it.onRole == Constants.ROLE_RBAC1_ID}

        then:
        listResponse.status == HttpStatus.SC_OK

        and: "Retrieves roles"
        retrievedEntity.tenantAssignments != null
        rbac1Assignment != null
        rbac1Assignment.forTenants.size() == 2
        rbac1Assignment.forTenants.contains(sharedUserAdminCloudTenant.id)
        rbac1Assignment.forTenants.contains(sharedUserAdminFilesTenant.id)

        cleanup:
        utils.deleteUserGroup(createdGroup)
    }

    def "Error check: Grant role on tenant for user group"() {
        given:
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "grantRoleOnTenant" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup1 = cloud20.createUserGroup(sharedIdentityAdminToken, group).getEntity(UserGroup)
        cloud20.grantRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup1, ROLE_RBAC1_ID, sharedUserAdminCloudTenant.id)

        UserGroup group2 = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "grantRoleOnTenant2" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup2 = cloud20.createUserGroup(sharedIdentityAdminToken, group2).getEntity(UserGroup)
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, ["*"]))
                    tas
            }
            it
        }
        cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup2, assignments)

        when: "Invalid user group"
        UserGroup invalidGroup = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.id = "invalid"
            it
        }
        def response = cloud20.grantRoleOnTenantToGroup(sharedIdentityAdminToken, invalidGroup, ROLE_RBAC1_ID, sharedUserAdminCloudTenant.id)

        then:
        response.status == HttpStatus.SC_NOT_FOUND

        when: "Invalid roleId"
        def invalidRoleId = "invalid"
        response = cloud20.grantRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup1, invalidRoleId, sharedUserAdminCloudTenant.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, String.format("Role '%s' does not exist.", invalidRoleId))

        when: "Invalid tenantId"
        def invalidTenantId = "invalid"
        response = cloud20.grantRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup1, ROLE_RBAC1_ID, invalidTenantId)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, String.format("Tenant with id/name: '%s' was not found.", invalidTenantId))

        when: "Granting role on tenant for user group - existing tenantId"
        response = cloud20.grantRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup1, ROLE_RBAC1_ID, sharedUserAdminCloudTenant.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_CONFLICT, "Role assignment already exist on user group.")

        when: "Granting role on tenant for user group - role assigned globally"
        response = cloud20.grantRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup2, ROLE_RBAC2_ID, sharedUserAdminCloudTenant.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_CONFLICT, "Role assignment already exist on user group.")

        cleanup:
        utils.deleteUserGroup(createdGroup1)
        utils.deleteUserGroup(createdGroup2)
    }

    void verifyContainsAssignment(RoleAssignments roleAssignments, String roleId, List<String> tenantIds) {
        ImmutableClientRole imr = applicationService.getCachedClientRoleById(roleId)

        def rbac1Assignment = roleAssignments.tenantAssignments.tenantAssignment.find {it.onRole == roleId}
        assert rbac1Assignment != null
        assert rbac1Assignment.forTenants.size() == tenantIds.size()
        assert rbac1Assignment.onRoleName == imr.name
        assert CollectionUtils.isEqualCollection(rbac1Assignment.forTenants, tenantIds)
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

    def "Revoke role on tenant for user group"() {
        given:
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "grantRoleOnTenant" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup = cloud20.createUserGroup(sharedIdentityAdminToken, group).getEntity(UserGroup)

        when: "Granting new role on tenant to user group"
        def response = cloud20.grantRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup, ROLE_RBAC1_ID, sharedUserAdminCloudTenant.id)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "List role on user group"
        def listResponse = cloud20.listRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, null)

        then:
        listResponse.status == HttpStatus.SC_OK
        RoleAssignments retrievedEntity = listResponse.getEntity(RoleAssignments)

        and: "Retrieves roles"
        retrievedEntity.tenantAssignments != null
        def rbac1Assignment = retrievedEntity.tenantAssignments.tenantAssignment.find {it.onRole == Constants.ROLE_RBAC1_ID}
        rbac1Assignment != null
        rbac1Assignment.forTenants.size() == 1
        rbac1Assignment.forTenants[0] == sharedUserAdminCloudTenant.id

        when: "Granting role on tenant to user group - existing role"
        response = cloud20.grantRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup, ROLE_RBAC1_ID, sharedUserAdminFilesTenant.id)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "Revoking role on tenant to user group"
        response = cloud20.revokeRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup, ROLE_RBAC1_ID, sharedUserAdminFilesTenant.id)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "List role on user group"
        listResponse = cloud20.listRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, null)
        retrievedEntity = listResponse.getEntity(RoleAssignments)
        rbac1Assignment = retrievedEntity.tenantAssignments.tenantAssignment.find {it.onRole == Constants.ROLE_RBAC1_ID}

        then:
        listResponse.status == HttpStatus.SC_OK

        and: "Retrieves roles"
        retrievedEntity.tenantAssignments != null
        rbac1Assignment != null
        rbac1Assignment.forTenants.size() == 1
        rbac1Assignment.forTenants.contains(sharedUserAdminCloudTenant.id)

        when: "Revoking role on tenant to user group"
        response = cloud20.revokeRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup, ROLE_RBAC1_ID, sharedUserAdminCloudTenant.id)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "List role on user group"
        listResponse = cloud20.listRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup, null)
        retrievedEntity = listResponse.getEntity(RoleAssignments)
        rbac1Assignment = retrievedEntity.tenantAssignments.tenantAssignment.find {it.onRole == Constants.ROLE_RBAC1_ID}

        then:
        listResponse.status == HttpStatus.SC_OK

        and: "Retrieves roles"
        retrievedEntity.tenantAssignments != null
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0
        rbac1Assignment == null

        cleanup:
        utils.deleteUserGroup(createdGroup)
    }

    def "Error check: Revoke role on tenant for user group"() {
        given:
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "grantRoleOnTenant" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup1 = cloud20.createUserGroup(sharedIdentityAdminToken, group).getEntity(UserGroup)
        cloud20.grantRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup1, ROLE_RBAC1_ID, sharedUserAdminCloudTenant.id)

        UserGroup group2 = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "grantRoleOnTenant2" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def createdGroup2 = cloud20.createUserGroup(sharedIdentityAdminToken, group2).getEntity(UserGroup)
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, ["*"]))
                    tas
            }
            it
        }
        cloud20.grantRoleAssignmentsOnUserGroup(sharedIdentityAdminToken, createdGroup2, assignments)

        when: "Invalid user group"
        UserGroup invalidGroup = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.id = "invalid"
            it
        }
        def response = cloud20.revokeRoleOnTenantToGroup(sharedIdentityAdminToken, invalidGroup, ROLE_RBAC1_ID, sharedUserAdminCloudTenant.id)

        then:
        response.status == HttpStatus.SC_NOT_FOUND

        when: "Invalid roleId"
        def invalidRoleId = "invalid"
        response = cloud20.revokeRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup1, invalidRoleId, sharedUserAdminCloudTenant.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, String.format("Role '%s' does not exist.", invalidRoleId))

        when: "Invalid tenantId"
        def invalidTenantId = "invalid"
        response = cloud20.revokeRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup1, ROLE_RBAC1_ID, invalidTenantId)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, String.format("Tenant with id/name: '%s' was not found.", invalidTenantId))

        when: "Revoking role on tenant for user group - role assigned globally"
        response = cloud20.revokeRoleOnTenantToGroup(sharedIdentityAdminToken, createdGroup2, ROLE_RBAC2_ID, sharedUserAdminCloudTenant.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, "Role assignemnt does not exist.")

        cleanup:
        utils.deleteUserGroup(createdGroup1)
        utils.deleteUserGroup(createdGroup2)
    }
}
