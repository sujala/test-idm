package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.modules.usergroups.Constants
import org.apache.commons.collections4.CollectionUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.openstack.docs.identity.api.v2.UnauthorizedFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.*
import static com.rackspace.idm.ErrorCodes.*

class ManageDelegationAgreementRolesRestIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    ApplicationService applicationService

    void doSetupSpec() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)
    }

    /**
     * Test a typical modification to the set of roles assigned to a delegation agreement with USER_GROUP principal.
     *
     * 1. No roles -> 1 tenant assigned role : Verify result includes that role assignment
     * 2. Assign 1 global role : Verify both roles returned appropriately
     * 3. Update tenant assigned role to be a global assigned role, and global to be assigned to 2 tenants: Verify result
     * 4. Update tenant assigned role to only have single tenant; verify result as expected
     * 5. Reset tenant to global : Verify result
     *
     * Test all this through both json and xml to verify can appropriately reflect the states
     */
    @Unroll
    def "modify roles on DA with USER_GROUP principal; mediaType = #mediaType"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)

        def cloudTenantId = userAdmin.domainId
        def filesTenantId = utils.getNastTenant(userAdmin.domainId)

        def createUserGroup = utils.createUserGroup(userAdmin.domainId)
        utils.addUserToUserGroup(userAdmin.id, createUserGroup)
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it.delegateId = defaultUser.id
            it.principalId = createUserGroup.id
            it.principalType = PrincipalType.USER_GROUP
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)

        RoleAssignments assignments0 = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments()
            it
        }

        RoleAssignments assignments1 = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [cloudTenantId]))
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
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [cloudTenantId, filesTenantId]))
                    tas
            }
            it
        }

        // Grant the roles on userGroup
        utils.grantRoleAssignmentsOnUserGroup(createUserGroup, assignments1)
        utils.grantRoleAssignmentsOnUserGroup(createUserGroup, assignments2)

        when: "assignment 0"
        def getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments0, mediaType)
        RoleAssignments retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0

        when: "assignment 1"
        getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments1, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])
        def rbac2Assignment = retrievedEntity.tenantAssignments.tenantAssignment.find {it.onRole == ROLE_RBAC2_ID}
        rbac2Assignment == null

        when: "assignment 2"
        getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments2, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, ["*"])

        when: "assignment 3 - tenants within the DA's domain"
        getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments3, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, [cloudTenantId, filesTenantId])

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    /**
     * Test a typical modification to the set of roles assigned to a delegation agreement with USER principal.
     *
     * 1. No roles -> 1 tenant assigned role : Verify result includes that role assignment
     * 2. Assign 1 global role : Verify both roles returned appropriately
     * 3. Update tenant assigned role to be a global assigned role, and global to be assigned to 2 tenants: Verify result
     * 4. Update tenant assigned role to only have single tenant; verify result as expected
     * 5. Reset tenant to global : Verify result
     *
     * Test all this through both json and xml to verify can appropriately reflect the states
     */
    @Unroll
    def "modify roles on DA with USER principal; mediaType = #mediaType"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)

        def cloudTenantId = userAdmin.domainId
        def filesTenantId = utils.getNastTenant(userAdmin.domainId)
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it.delegateId = defaultUser.id
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)

        RoleAssignments assignments0 = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments()
            it
        }

        RoleAssignments assignments1 = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [cloudTenantId]))
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
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [cloudTenantId, filesTenantId]))
                    tas
            }
            it
        }

        RoleAssignments assignments4 = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [cloudTenantId]))
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
        def getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments0, mediaType)
        RoleAssignments retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0

        when: "assignment 1"
        getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments1, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])
        def rbac2Assignment = retrievedEntity.tenantAssignments.tenantAssignment.find {it.onRole == ROLE_RBAC2_ID}
        rbac2Assignment == null

        when: "assignment 2"
        getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments2, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, ["*"])

        when: "assignment 3"
        getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments3, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, [cloudTenantId, filesTenantId])

        when: "assignment 4"
        getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments4, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, [cloudTenantId])

        when: "assignment 5"
        getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments5, mediaType)
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
    def "Verify no additional roles can be assigned when the principal is a 'identity:service-admin' or 'identity:admin'; mediaType = #mediaType"() {
        given:
        def identityAdmin = utils.createIdentityAdmin()
        def identityAdmin2 = utils.createIdentityAdmin()
        utils.addUserToDomain(identityAdmin2.id, identityAdmin.domainId)
        def identityAdminToken = utils.getToken(identityAdmin.username)
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = identityAdmin.domainId
            it.delegateId = identityAdmin2.id
            it
        }
        def createdDA = utils.createDelegationAgreement(identityAdminToken, delegationAgreement)

        RoleAssignments assignments = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, ["*"]))
            it.tenantAssignments = ta
            it
        }

        when:
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(identityAdminToken, createdDA, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN , GlobalConstants.NOT_AUTHORIZED_MSG);

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error: Do not allowed to grant user-manage role to delegation agreement; mediaType = #mediaType"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)

        def delegationAgreementWithUserPrincipal = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it.delegateId = defaultUser.id
            it
        }
        def createdDAWithUserPrincipal = utils.createDelegationAgreement(userAdminToken, delegationAgreementWithUserPrincipal)

        def createUserGroup = utils.createUserGroup(userAdmin.domainId)
        utils.addUserToUserGroup(userAdmin.id, createUserGroup)
        def delegationAgreementWithUserGroupPrincipal = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it.delegateId = defaultUser.id
            it.principalId = createUserGroup.id
            it.principalType = PrincipalType.USER_GROUP
            it
        }
        def createdDAWithUserGroupPrincipal = utils.createDelegationAgreement(userAdminToken, delegationAgreementWithUserGroupPrincipal)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = USER_MANAGE_ROLE_ID
                            ta.forTenants.add("*")
                            ta
                    })
                    tas
            }
            it
        }

        when: "delegation agreement with user principal"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDAWithUserPrincipal, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN
                , Constants.ERROR_CODE_USER_GROUPS_INVALID_ATTRIBUTE
                , String.format(ErrorCodes.ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, USER_MANAGE_ROLE_ID));

        when: "delegation agreement with user group principal"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDAWithUserGroupPrincipal, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN
                , Constants.ERROR_CODE_USER_GROUPS_INVALID_ATTRIBUTE
                , String.format(ErrorCodes.ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, USER_MANAGE_ROLE_ID));

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error: No forTenants value on role assignment returns error; mediaType = #mediaType"() {
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)

        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it.delegateId = defaultUser.id
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, []))
                    tas
            }
            it
        }

        when:
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST
                , Constants.ERROR_CODE_USER_GROUPS_MISSING_REQUIRED_ATTRIBUTE
                , ErrorCodes.ERROR_CODE_ROLE_ASSIGNMENT_MISSING_FOR_TENANTS_MSG)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error: Common errors for granting role assignments to delegation agreement; Principal = USER, mediaType = #mediaType"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)

        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it.delegateId = defaultUser.id
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)
        def tenant = utils.createTenant()

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, ["*"]))
                    tas
            }
            it
        }

        when: "invalid delegation agreement"
        def invalidDA = new DelegationAgreement().with {
            it.id = "invalid"
            it
        }
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, invalidDA, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND
                , "The specified agreement does not exist for this user")

        when: "invalid token"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement("invalid", createdDA, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED
                , "No valid token provided. Please use the 'X-Auth-Token' header with a valid token.")

        when: "multiple entities for role"
        RoleAssignments invalidAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, []))
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, []))
                    tas
            }
            it
        }

        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, invalidAssignment, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault,
                HttpStatus.SC_BAD_REQUEST, ERROR_CODE_DUP_ROLE_ASSIGNMENT, ERROR_CODE_DUP_ROLE_ASSIGNMENT_MSG)

        when: "tenant not belonging in target DA's domain"
        invalidAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [tenant.id]))
                    tas
            }
            it
        }

        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, invalidAssignment, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_WRONG_DOMAIN_TENANT_MSG_PATTERN, ROLE_RBAC1_ID, createdDA.domainId))

        when: "administrator on role is set to identity:admin"
        def role = utils.createRole(null, testUtils.getRandomUUID("role"), "identity:admin")
        invalidAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(role.id, ["*"]))
                    tas
            }
            it
        }

        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, invalidAssignment, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, role.id))

        when: "administrator on role is set to identity:service-admin"
        role = utils.createRole(null, testUtils.getRandomUUID("role"), "identity:service-admin")
        invalidAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(role.id, ["*"]))
                    tas
            }
            it
        }

        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, invalidAssignment, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, role.id))

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error: Common errors for granting role assignments to delegation agreement; Principal = USER_GROUP, mediaType = #mediaType"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)

        def cloudTenantId = userAdmin.domainId
        def filesTenantId = utils.getNastTenant(cloudTenantId)

        def createUserGroup = utils.createUserGroup(userAdmin.domainId)
        utils.addUserToUserGroup(userAdmin.id, createUserGroup)
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it.delegateId = defaultUser.id
            it.principalId = createUserGroup.id
            it.principalType = PrincipalType.USER_GROUP
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)

        RoleAssignments assignments1 = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [cloudTenantId]))
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

        // Grant the roles on userGroup
        utils.grantRoleAssignmentsOnUserGroup(createUserGroup, assignments1)
        utils.grantRoleAssignmentsOnUserGroup(createUserGroup, assignments2)

        when: "invalid delegation agreement"
        def invalidDA = new DelegationAgreement().with {
            it.id = "invalid"
            it
        }
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, invalidDA, assignments1, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND
                , "The specified agreement does not exist for this user")

        when: "invalid token"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement("invalid", createdDA, assignments1, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED
                , "No valid token provided. Please use the 'X-Auth-Token' header with a valid token.")

        when: "multiple entities for role"
        RoleAssignments invalidAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, []))
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, []))
                    tas
            }
            it
        }

        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, invalidAssignment, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault,
                HttpStatus.SC_BAD_REQUEST, ERROR_CODE_DUP_ROLE_ASSIGNMENT, ERROR_CODE_DUP_ROLE_ASSIGNMENT_MSG)

        when: "tenant not belonging in target DA's domain"
        def tenant = utils.createTenant()
        invalidAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [tenant.id]))
                    tas
            }
            it
        }

        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, invalidAssignment, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_WRONG_DOMAIN_TENANT_MSG_PATTERN, ROLE_RBAC1_ID, createdDA.domainId))

        when: "administrator on role is set to identity:admin"
        def role = utils.createRole(null, testUtils.getRandomUUID("role"), "identity:admin")
        invalidAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(role.id, ["*"]))
                    tas
            }
            it
        }

        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, invalidAssignment, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, role.id))

        when: "administrator on role is set to identity:service-admin"
        role = utils.createRole(null, testUtils.getRandomUUID("role"), "identity:service-admin")
        invalidAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(role.id, ["*"]))
                    tas
            }
            it
        }

        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, invalidAssignment, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, role.id))

        when: "not subset of allowed tenant roles"
        invalidAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [filesTenantId]))
                    tas
            }
            it
        }

        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, invalidAssignment, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault,
                HttpStatus.SC_FORBIDDEN, ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_WRONG_TENANTS_MSG_PATTERN, ROLE_RBAC1_ID))

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
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
}
