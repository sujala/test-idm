package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.ApplicationService
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*
import static com.rackspace.idm.ErrorCodes.*

class DelegationAgreementRoleHierarchyIntegrationTest extends RootIntegrationTest {

    // Parent DA role assignments
    @Shared RoleAssignments adminAssignment
    @Shared RoleAssignments observerAssignment
    @Shared RoleAssignments billingAdminAssignment
    @Shared RoleAssignments ticketingAdminAssignment

    // Nested DA role assignments
    @Shared RoleAssignments allRoleAssignments
    @Shared RoleAssignments observerAssignments
    @Shared RoleAssignments billingAssignments
    @Shared RoleAssignments ticketingAssignments

    // Parent DAs
    @Shared daWithAdminRole
    @Shared daWithObserverRole
    @Shared daWithBillingAdminRole
    @Shared daWithTicketingAdminRole

    @Shared userAdmin
    @Shared userAdminToken

    @Autowired
    ApplicationService applicationService

    def setup() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_ROLE_HIERARCHY_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_GRANT_ROLES_TO_NESTED_DA_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_DELEGATION_MAX_NUMBER_OF_DA_PER_PRINCIPAL_PROP, 10)

        // Parent DA role assignments
        adminAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ADMIN_ROLE_ID, ["*"]))
                    tas
            }
            it
        }
        observerAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(OBSERVER_ROLE_ID, ["*"]))
                    tas
            }
            it
        }
        billingAdminAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(BILLING_ADMIN_ROLE_ID, ["*"]))
                    tas
            }
            it
        }
        ticketingAdminAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(TICKETING_ADMIN_ROLE_ID, ["*"]))
                    tas
            }
            it
        }

        // Nested DA role assignments
        allRoleAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ADMIN_ROLE_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(OBSERVER_ROLE_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(TICKETING_ADMIN_ROLE_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(TICKETING_OBSERVER_ROLE_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(BILLING_ADMIN_ROLE_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(BILLING_OBSERVER_ROLE_ID, ["*"]))
                    tas
            }
            it
        }
        observerAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(OBSERVER_ROLE_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(TICKETING_OBSERVER_ROLE_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(BILLING_OBSERVER_ROLE_ID, ["*"]))
                    tas
            }
            it
        }
        billingAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(BILLING_ADMIN_ROLE_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(BILLING_OBSERVER_ROLE_ID, ["*"]))
                    tas
            }
            it
        }
        ticketingAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(TICKETING_ADMIN_ROLE_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(TICKETING_OBSERVER_ROLE_ID, ["*"]))
                    tas
            }
            it
        }

        userAdmin = utils.createCloudAccount()
        userAdminToken = utils.getToken(userAdmin.username)

        // Create a root da w/ self as delegate
        DelegationAgreement parentDa = new DelegationAgreement().with {
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = 2
            it
        }

        // Create DAs
        parentDa.name = testUtils.getRandomUUIDOfLength("adminRoleDa", 32)
        daWithAdminRole = utils.createDelegationAgreement(userAdminToken, parentDa)
        utils.addUserDelegate(userAdminToken, daWithAdminRole.id, userAdmin.id)
        utils.grantRoleAssignmentsOnDelegationAgreement(daWithAdminRole, adminAssignment, userAdminToken)

        parentDa.name = testUtils.getRandomUUIDOfLength("observerRoleDa", 32)
        daWithObserverRole = utils.createDelegationAgreement(userAdminToken, parentDa)
        utils.addUserDelegate(userAdminToken, daWithObserverRole.id, userAdmin.id)
        utils.grantRoleAssignmentsOnDelegationAgreement(daWithObserverRole, observerAssignment, userAdminToken)

        parentDa.name = testUtils.getRandomUUIDOfLength("billingAdminRoleDa", 32)
        daWithBillingAdminRole = utils.createDelegationAgreement(userAdminToken, parentDa)
        utils.addUserDelegate(userAdminToken, daWithBillingAdminRole.id, userAdmin.id)
        utils.grantRoleAssignmentsOnDelegationAgreement(daWithBillingAdminRole, billingAdminAssignment, userAdminToken)

        parentDa.name = testUtils.getRandomUUIDOfLength("ticketingAdminRoleDa", 32)
        daWithTicketingAdminRole = utils.createDelegationAgreement(userAdminToken, parentDa)
        utils.addUserDelegate(userAdminToken, daWithTicketingAdminRole.id, userAdmin.id)
        utils.grantRoleAssignmentsOnDelegationAgreement(daWithTicketingAdminRole, ticketingAdminAssignment, userAdminToken)
    }

    def cleanup() {
        reloadableConfiguration.reset()
    }

    def "test nested DA role hierarchy with parent DA assigned the admin role"() {
        given:
        // Create a nested da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = daWithAdminRole.id
            it.subAgreementNestLevel = 1
            it
        }

        subAgreement = utils.createDelegationAgreement(userAdminToken, subAgreement)

        when: "grant tenant role to nested DA - admin assignment"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, adminAssignment)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, ADMIN_ROLE_ID, ["*"])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, ADMIN_ROLE_ID, ["*"])

        when: "grant tenant role to nested DA - observer assignment"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, observerAssignment)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, ["*"])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, ["*"])

        when: "grant tenant role to nested DA - all role assignment"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, allRoleAssignments)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, ADMIN_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, BILLING_ADMIN_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, BILLING_OBSERVER_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, TICKETING_ADMIN_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, TICKETING_OBSERVER_ROLE_ID, ["*"])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, ADMIN_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, BILLING_ADMIN_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, BILLING_OBSERVER_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, TICKETING_ADMIN_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, TICKETING_OBSERVER_ROLE_ID, ["*"])

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, subAgreement.id)
    }

    def "test nested DA role hierarchy with parent DA assigned the observer role"() {
        given:
        // Create a nested da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = daWithObserverRole.id
            it.subAgreementNestLevel = 1
            it
        }

        subAgreement = utils.createDelegationAgreement(userAdminToken, subAgreement)

        when: "grant tenant role to nested DA - observer assignment"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, observerAssignment)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, ["*"])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, ["*"])

        when: "grant tenant role to nested DA - observer assignments"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, observerAssignments)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, BILLING_OBSERVER_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, TICKETING_OBSERVER_ROLE_ID, ["*"])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, BILLING_OBSERVER_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, TICKETING_OBSERVER_ROLE_ID, ["*"])

        when: "grant tenant role to nested DA - invalid assignment"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, ticketingAdminAssignment)

        then: "assert forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, TICKETING_ADMIN_ROLE_ID))

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, subAgreement.id)
    }

    def "test nested DA role hierarchy with parent DA assigned the ticketing admin role"() {
        given:
        // Create a nested da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = daWithTicketingAdminRole.id
            it.subAgreementNestLevel = 1
            it
        }

        subAgreement = utils.createDelegationAgreement(userAdminToken, subAgreement)

        when: "grant tenant role to nested DA - ticketing admin assignment"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, ticketingAdminAssignment)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, TICKETING_ADMIN_ROLE_ID, ["*"])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, TICKETING_ADMIN_ROLE_ID, ["*"])

        when: "grant tenant role to nested DA - ticketing assignments"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, ticketingAssignments)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, TICKETING_ADMIN_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, TICKETING_OBSERVER_ROLE_ID, ["*"])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, TICKETING_ADMIN_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, TICKETING_OBSERVER_ROLE_ID, ["*"])

        when: "grant tenant role to nested DA - invalid assignment"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, billingAdminAssignment)

        then: "assert forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, BILLING_ADMIN_ROLE_ID))

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, subAgreement.id)
    }

    def "test nested DA role hierarchy with parent DA assigned the billing admin role"() {
        given:
        // Create a nested da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = daWithBillingAdminRole.id
            it.subAgreementNestLevel = 1
            it
        }

        subAgreement = utils.createDelegationAgreement(userAdminToken, subAgreement)

        when: "grant tenant role to nested DA - billing admin assignment"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, billingAdminAssignment)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, BILLING_ADMIN_ROLE_ID, ["*"])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, BILLING_ADMIN_ROLE_ID, ["*"])

        when: "grant tenant role to nested DA - billing assignments"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, billingAssignments)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, BILLING_ADMIN_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, BILLING_OBSERVER_ROLE_ID, ["*"])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, BILLING_ADMIN_ROLE_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, BILLING_OBSERVER_ROLE_ID, ["*"])

        when: "grant tenant role to nested DA - invalid assignment"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, ticketingAdminAssignment)

        then: "assert forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, TICKETING_ADMIN_ROLE_ID))

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, subAgreement.id)
    }

    def "test multiple tenant role assignments on the same role"() {
        given:
        def mossoTenant = userAdmin.domainId
        def nastTenant = utils.getNastTenant(userAdmin.domainId)

        DelegationAgreement parentDa = new DelegationAgreement().with {
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = 2
            it
        }

        parentDa.name = testUtils.getRandomUUIDOfLength("multipleTenantAssignments", 32)
        parentDa = utils.createDelegationAgreement(userAdminToken, parentDa)
        utils.addUserDelegate(userAdminToken, parentDa.id, userAdmin.id)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ADMIN_ROLE_ID, [mossoTenant]))
                    tas.tenantAssignment.add(createTenantAssignment(TICKETING_ADMIN_ROLE_ID, [nastTenant]))
                    tas
            }
            it
        }
        utils.grantRoleAssignmentsOnDelegationAgreement(parentDa, assignments, userAdminToken)

        // Create a nested da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = parentDa.id
            it.subAgreementNestLevel = 1
            it
        }

        subAgreement = utils.createDelegationAgreement(userAdminToken, subAgreement)

        when: "grant tenant role to nested DA"
        RoleAssignments tenantAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(OBSERVER_ROLE_ID, [mossoTenant]))
                    tas.tenantAssignment.add(createTenantAssignment(TICKETING_ADMIN_ROLE_ID, [mossoTenant, nastTenant]))
                    tas.tenantAssignment.add(createTenantAssignment(TICKETING_OBSERVER_ROLE_ID, [mossoTenant, nastTenant]))
                    tas
            }
            it
        }
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, tenantAssignments)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, [mossoTenant])
        verifyContainsAssignment(retrievedEntity, TICKETING_ADMIN_ROLE_ID, [mossoTenant, nastTenant])
        verifyContainsAssignment(retrievedEntity, TICKETING_OBSERVER_ROLE_ID, [mossoTenant, nastTenant])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, [mossoTenant])
        verifyContainsAssignment(retrievedEntity, TICKETING_ADMIN_ROLE_ID, [mossoTenant, nastTenant])
        verifyContainsAssignment(retrievedEntity, TICKETING_OBSERVER_ROLE_ID, [mossoTenant, nastTenant])

        when: "grant tenant role to nested DA - invalid assignment"
        tenantAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(OBSERVER_ROLE_ID, [nastTenant]))
                    tas
            }
            it
        }
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, tenantAssignments)

        then: "assert forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_WRONG_TENANTS_MSG_PATTERN, OBSERVER_ROLE_ID))

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, subAgreement.id)
        cloud20.deleteDelegationAgreement(userAdminToken, parentDa.id)
    }

    def "test domain assignments override tenant assignments in role hierarchy"() {
        given:
        def mossoTenant = userAdmin.domainId
        def nastTenant = utils.getNastTenant(userAdmin.domainId)

        DelegationAgreement parentDa = new DelegationAgreement().with {
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = 2
            it
        }

        parentDa.name = testUtils.getRandomUUIDOfLength("assignments", 32)
        parentDa = utils.createDelegationAgreement(userAdminToken, parentDa)
        utils.addUserDelegate(userAdminToken, parentDa.id, userAdmin.id)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(OBSERVER_ROLE_ID, ["*"]))
                    tas.tenantAssignment.add(createTenantAssignment(BILLING_OBSERVER_ROLE_ID, [mossoTenant]))
                    tas
            }
            it
        }
        utils.grantRoleAssignmentsOnDelegationAgreement(parentDa, assignments, userAdminToken)

        // Create a nested da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = parentDa.id
            it.subAgreementNestLevel = 1
            it
        }

        subAgreement = utils.createDelegationAgreement(userAdminToken, subAgreement)

        when: "grant tenant role to nested DA"
        RoleAssignments tenantAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(OBSERVER_ROLE_ID, [mossoTenant, nastTenant]))
                    tas.tenantAssignment.add(createTenantAssignment(BILLING_OBSERVER_ROLE_ID, ["*"]))
                    tas
            }
            it
        }
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, tenantAssignments)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, [mossoTenant, nastTenant])
        verifyContainsAssignment(retrievedEntity, BILLING_OBSERVER_ROLE_ID, ["*"])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, [mossoTenant, nastTenant])
        verifyContainsAssignment(retrievedEntity, BILLING_OBSERVER_ROLE_ID, ["*"])

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, subAgreement.id)
        cloud20.deleteDelegationAgreement(userAdminToken, parentDa.id)
    }

    def "test tenant assignments in role hierarchy"() {
        given:
        def mossoTenant = userAdmin.domainId
        def nastTenant = utils.getNastTenant(userAdmin.domainId)

        DelegationAgreement parentDa = new DelegationAgreement().with {
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = 2
            it
        }

        parentDa.name = testUtils.getRandomUUIDOfLength("assignments", 32)
        parentDa = utils.createDelegationAgreement(userAdminToken, parentDa)
        utils.addUserDelegate(userAdminToken, parentDa.id, userAdmin.id)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(OBSERVER_ROLE_ID, [mossoTenant]))
                    tas.tenantAssignment.add(createTenantAssignment(BILLING_OBSERVER_ROLE_ID, [mossoTenant]))
                    tas
            }
            it
        }
        utils.grantRoleAssignmentsOnDelegationAgreement(parentDa, assignments, userAdminToken)

        // Create a nested da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = parentDa.id
            it.subAgreementNestLevel = 1
            it
        }

        subAgreement = utils.createDelegationAgreement(userAdminToken, subAgreement)

        when: "grant tenant role to nested DA"
        RoleAssignments tenantAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(OBSERVER_ROLE_ID, [mossoTenant]))
                    tas.tenantAssignment.add(createTenantAssignment(BILLING_OBSERVER_ROLE_ID, [mossoTenant]))
                    tas
            }
            it
        }
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, tenantAssignments)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, [mossoTenant])
        verifyContainsAssignment(retrievedEntity, BILLING_OBSERVER_ROLE_ID, [mossoTenant])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, [mossoTenant])
        verifyContainsAssignment(retrievedEntity, BILLING_OBSERVER_ROLE_ID, [mossoTenant])

        when: "grant tenant role to nested DA - invalid assignment"
        tenantAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(OBSERVER_ROLE_ID, [nastTenant]))
                    tas
            }
            it
        }
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, tenantAssignments)

        then: "assert forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_WRONG_TENANTS_MSG_PATTERN, OBSERVER_ROLE_ID))

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, subAgreement.id)
        cloud20.deleteDelegationAgreement(userAdminToken, parentDa.id)
    }

    @Unroll
    def "test feature flag 'feature.enable.role.hierarchy'; value = #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_ROLE_HIERARCHY_PROP, featureEnabled)
        // Create a nested da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = daWithAdminRole.id
            it.subAgreementNestLevel = 1
            it
        }
        subAgreement = utils.createDelegationAgreement(userAdminToken, subAgreement)

        when: "grant tenant role to nested DA"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, observerAssignment)

        then:
        if (featureEnabled) {
            def retrievedEntity = response.getEntity(RoleAssignments)
            assert response.status == HttpStatus.SC_OK
            verifyContainsAssignment(retrievedEntity, OBSERVER_ROLE_ID, ["*"])
        } else {
            IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                    ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, OBSERVER_ROLE_ID))
        }

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, subAgreement.id)

        where:
        featureEnabled << [true, false]
    }

    def "verify hierarchical roles are not applied to parent DA with USER_GROUP principal"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)

        def createUserGroup = utils.createUserGroup(userAdmin.domainId)
        utils.addUserToUserGroup(userAdmin.id, createUserGroup)
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it.principalId = createUserGroup.id
            it.principalType = PrincipalType.USER_GROUP
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)
        utils.addUserDelegate(userAdminToken, createdDA.id, defaultUser.id)

        RoleAssignments assignments = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(createTenantAssignment(ADMIN_ROLE_ID, ["*"]))
            it.tenantAssignments = ta
            it
        }

        // Grant the role on userGroup
        utils.grantRoleAssignmentsOnUserGroup(createUserGroup, assignments)

        when: "grant role to DA"
        RoleAssignments observerAssignment = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(createTenantAssignment(OBSERVER_ROLE_ID, ["*"]))
            it.tenantAssignments = ta
            it
        }
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, observerAssignment)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                    ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, OBSERVER_ROLE_ID))
    }

    def "verify that caller allowed to modify nested DA can grant hierarchical roles whether or not the user is allowed to manage role"() {
        given:
        // Create sub user
        def subUser = utils.createUser(userAdminToken)
        def subUserToken = utils.getToken(subUser.username)

        DelegationAgreement parentDa = new DelegationAgreement().with {
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = 2
            it
        }

        parentDa.name = testUtils.getRandomUUIDOfLength("assignments", 32)
        parentDa = utils.createDelegationAgreement(userAdminToken, parentDa)

        // Add delegates
        utils.addUserDelegate(userAdminToken, parentDa.id, userAdmin.id)
        utils.addUserDelegate(userAdminToken, parentDa.id, subUser.id)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(OBSERVER_ROLE_ID, ["*"]))
                    tas
            }
            it
        }
        utils.grantRoleAssignmentsOnDelegationAgreement(parentDa, assignments, userAdminToken)

        // Create a nested da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = parentDa.id
            it.principalId = subUser.id
            it.principalType = PrincipalType.USER
            it.subAgreementNestLevel = 1
            it
        }

        subAgreement = utils.createDelegationAgreement(userAdminToken, subAgreement)

        when: "grant tenant role to nested DA"
        RoleAssignments tenantAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(BILLING_OBSERVER_ROLE_ID, ["*"]))
                    tas
            }
            it
        }
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(subUserToken, subAgreement, tenantAssignments)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, BILLING_OBSERVER_ROLE_ID, ["*"])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(subUserToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, BILLING_OBSERVER_ROLE_ID, ["*"])

        when: "grant tenant role to nested DA - invalid assignment"
        tenantAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(TICKETING_ADMIN_ROLE_ID, ["*"]))
                    tas
            }
            it
        }
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(subUserToken, subAgreement, tenantAssignments)

        then: "assert forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, TICKETING_ADMIN_ROLE_ID))

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, subAgreement.id)
        cloud20.deleteDelegationAgreement(userAdminToken, parentDa.id)
        utils.deleteUserQuietly(subUser)
    }

    def "Verify privilege escalation is not allowed"() {
        given:
        def mossoTenant = userAdmin.domainId
        def nastTenant = utils.getNastTenant(userAdmin.domainId)

        DelegationAgreement parentDa = new DelegationAgreement().with {
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = 2
            it
        }

        parentDa.name = testUtils.getRandomUUIDOfLength("assignments", 32)
        parentDa = utils.createDelegationAgreement(userAdminToken, parentDa)
        utils.addUserDelegate(userAdminToken, parentDa.id, userAdmin.id)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ADMIN_ROLE_ID, [mossoTenant]))
                    tas.tenantAssignment.add(createTenantAssignment(BILLING_ADMIN_ROLE_ID, [nastTenant]))
                    tas
            }
            it
        }
        utils.grantRoleAssignmentsOnDelegationAgreement(parentDa, assignments, userAdminToken)

        // Create a nested da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = parentDa.id
            it.subAgreementNestLevel = 1
            it
        }

        subAgreement = utils.createDelegationAgreement(userAdminToken, subAgreement)

        when: "Try to escalate privilege on nast tenant to admin"
        RoleAssignments tenantAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(BILLING_OBSERVER_ROLE_ID, [nastTenant]))
                    tas.tenantAssignment.add(createTenantAssignment(ADMIN_ROLE_ID, [nastTenant]))
                    tas
            }
            it
        }
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, tenantAssignments)

        then: "Fails appropriately"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_WRONG_TENANTS_MSG_PATTERN, ADMIN_ROLE_ID))

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, parentDa.id)
    }

    void verifyContainsAssignment(RoleAssignments roleAssignments, String roleId, List<String> tenantIds) {
        ImmutableClientRole imr = applicationService.getCachedClientRoleById(roleId)

        def roleAssignment = roleAssignments.tenantAssignments.tenantAssignment.find {it.onRole == roleId}
        assert roleAssignment != null
        assert roleAssignment.forTenants.size() == tenantIds.size()
        assert roleAssignment.onRoleName == imr.name
        assert CollectionUtils.isEqualCollection(roleAssignment.forTenants, tenantIds)
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
