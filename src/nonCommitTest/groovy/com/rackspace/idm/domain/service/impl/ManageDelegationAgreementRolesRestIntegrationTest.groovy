package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.api.resource.cloud.v20.DefaultDelegationCloudService
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.modules.usergroups.Constants
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.openstack.docs.identity.api.v2.UnauthorizedFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.*
import static com.rackspace.idm.ErrorCodes.*

class ManageDelegationAgreementRolesRestIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    ApplicationService applicationService

    def setup() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)
    }

    def cleanup() {
        reloadableConfiguration.reset()
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
            it.principalId = createUserGroup.id
            it.principalType = PrincipalType.USER_GROUP
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)
        utils.addUserDelegate(userAdminToken, createdDA.id, defaultUser.id)

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

    @Unroll
    def "modify roles on DA with federated user; mediaType = #mediaType"() {
        given:
        def userAdmin = utils.createCloudAccount()

        // Create faws tenants
        def fawsTenant = utils.createTenantWithTypes(testUtils.getRandomUUID("tenant"), ["faws"])
        def fawsTenant2 = utils.createTenantWithTypes(testUtils.getRandomUUID("tenant"), ["faws"])
        utils.addTenantToDomain(userAdmin.domainId, fawsTenant.id)
        utils.addTenantToDomain(userAdmin.domainId, fawsTenant2.id)

        // Create user group
        def createdUserGroup = utils.createUserGroup(userAdmin.domainId)

        // Create new federated user with roles and user group
        def tenantRole = String.format("%s/%s", ROLE_RBAC2_NAME, fawsTenant2.name)
        def samlRequest = new FederatedDomainAuthGenerationRequest().with {
            it.domainId = userAdmin.domainId
            it.validitySeconds = 100
            it.brokerIssuer = DEFAULT_BROKER_IDP_URI
            it.originIssuer = IDP_V2_DOMAIN_URI
            it.email = DEFAULT_FED_EMAIL
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass = SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = UUID.randomUUID().toString()
            it.roleNames = [ROLE_RBAC1_NAME, tenantRole] as Set
            it.groupNames = [createdUserGroup.name] as Set
            it
        }
        AuthenticateResponse authResponse = utils.authenticateV2FederatedUser(samlRequest)
        def fedUserToken = authResponse.token.id

        def principalId
        if (principalType == PrincipalType.USER) {
            principalId = authResponse.user.id
        } else {
            principalId = createdUserGroup.id
        }

        // Create delegation agreement
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it.principalId = principalId
            it.principalType = principalType
            it
        }
        def createdDA = utils.createDelegationAgreement(fedUserToken, delegationAgreement)

        RoleAssignments assignments1 = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [fawsTenant.id]))
            it.tenantAssignments = ta
            it
        }

        RoleAssignments assignments2 = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [fawsTenant2.id]))
                    tas
            }
            it
        }

        RoleAssignments invalidAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, ["*"]))
                    tas
            }
            it
        }

        // Grant the roles on userGroup
        utils.grantRoleAssignmentsOnUserGroup(createdUserGroup, assignments1)
        utils.grantRoleAssignmentsOnUserGroup(createdUserGroup, assignments2)

        when: "assignment 1"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(fedUserToken, createdDA, assignments1, mediaType)
        RoleAssignments retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [fawsTenant.id])

        when: "assignment 2"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(fedUserToken, createdDA, assignments2, mediaType)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, [fawsTenant2.id])

        when: "invalid assignment"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(fedUserToken, createdDA, invalidAssignment, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_WRONG_TENANTS_MSG_PATTERN, ROLE_RBAC2_ID))

        where:
        principalType            | mediaType
        PrincipalType.USER       | MediaType.APPLICATION_XML_TYPE
        PrincipalType.USER       | MediaType.APPLICATION_JSON_TYPE
        PrincipalType.USER_GROUP | MediaType.APPLICATION_XML_TYPE
        PrincipalType.USER_GROUP | MediaType.APPLICATION_JSON_TYPE
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
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)
        utils.addUserDelegate(userAdminToken, createdDA.id, defaultUser.id)

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

    /**
     * Test a typical modification to the set of roles assigned to a delegation agreement with federated USER principal.
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
    def "modify roles on DA with federated USER principal; mediaType = #mediaType"() {
        given:
        def userAdmin = cloud20.createCloudAccount(utils.getIdentityAdminToken())

        def expSecs = DEFAULT_SAML_EXP_SECS
        def username = testUtils.getRandomUUID("samlUser")
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expSecs, userAdmin.domainId, [ROLE_RBAC1_NAME, ROLE_RBAC2_NAME])
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def fedUserToken = authResponse.token.id

        def cloudTenantId = userAdmin.domainId
        def filesTenantId = utils.getNastTenant(cloudTenantId)
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(fedUserToken, delegationAgreement)

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
        def getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(fedUserToken, createdDA, assignments0, mediaType)
        RoleAssignments retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0

        when: "assignment 1"
        getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(fedUserToken, createdDA, assignments1, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])
        def rbac2Assignment = retrievedEntity.tenantAssignments.tenantAssignment.find {it.onRole == ROLE_RBAC2_ID}
        rbac2Assignment == null

        when: "assignment 2"
        getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(fedUserToken, createdDA, assignments2, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, ["*"])

        when: "assignment 3"
        getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(fedUserToken, createdDA, assignments3, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, [cloudTenantId, filesTenantId])

        when: "assignment 4"
        getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(fedUserToken, createdDA, assignments4, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, [cloudTenantId])

        when: "assignment 5"
        getResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(fedUserToken, createdDA, assignments5, mediaType)
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
    def "Error: Do not allow more than the max tenant assignments when granting roles to delegation agreement - maxTenantAssignments = #maxTa"(){
        given:
        reloadableConfiguration.setProperty(IdentityConfig.ROLE_ASSIGNMENTS_MAX_TENANT_ASSIGNMENTS_PER_REQUEST_PROP, maxTa)
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)

        def cloudTenantId = userAdmin.domainId
        def filesTenantId = utils.getNastTenant(userAdmin.domainId)
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)
        utils.addUserDelegate(userAdminToken, createdDA.id, defaultUser.id)

        RoleAssignments assignments = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [cloudTenantId]))
            ta.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [filesTenantId]))
            it.tenantAssignments = ta
            it
        }

        when:
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST,
                ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE,
                String.format(ErrorCodes.ERROR_CODE_ROLE_ASSIGNMENT_MAX_TENANT_ASSIGNMENT_MSG_PATTERN, maxTa))

        where:
        maxTa << [0, 1]
    }

    @Unroll
    def "verify no additional roles can be assigned when the principal is a 'identity:service-admin' or 'identity:admin'; mediaType = #mediaType"() {
        given:
        def identityAdmin = utils.createIdentityAdmin()
        def identityAdmin2 = utils.createIdentityAdmin()
        utils.addUserToDomain(identityAdmin2.id, identityAdmin.domainId)
        def identityAdminToken = utils.getToken(identityAdmin.username)
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = identityAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(identityAdminToken, delegationAgreement)
        utils.addUserDelegate(identityAdminToken, createdDA.id, identityAdmin2.id)

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
    def "error: Do not allowed to grant user-manage role to delegation agreement; mediaType = #mediaType"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)

        def delegationAgreementWithUserPrincipal = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDAWithUserPrincipal = utils.createDelegationAgreement(userAdminToken, delegationAgreementWithUserPrincipal)
        utils.addUserDelegate(userAdminToken, createdDAWithUserPrincipal.id, defaultUser.id)

        def createUserGroup = utils.createUserGroup(userAdmin.domainId)
        utils.addUserToUserGroup(userAdmin.id, createUserGroup)
        def delegationAgreementWithUserGroupPrincipal = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it.principalId = createUserGroup.id
            it.principalType = PrincipalType.USER_GROUP
            it
        }
        def createdDAWithUserGroupPrincipal = utils.createDelegationAgreement(userAdminToken, delegationAgreementWithUserGroupPrincipal)
        utils.addUserDelegate(userAdminToken, createdDAWithUserGroupPrincipal.id, defaultUser.id)

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
    def "error: No forTenants value on role assignment returns error; mediaType = #mediaType"() {
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)

        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)
        utils.addUserDelegate(userAdminToken, createdDA.id, defaultUser.id)

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
    def "error: Common errors for granting role assignments to delegation agreement; Principal = USER, mediaType = #mediaType"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)

        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)
        def tenant = utils.createTenant()
        utils.addUserDelegate(userAdminToken, createdDA.id, defaultUser.id)

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
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND,
                ErrorCodes.ERROR_CODE_NOT_FOUND, "The specified agreement does not exist for this user")

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
    def "error: Common errors for granting role assignments to delegation agreement; Principal = USER_GROUP, mediaType = #mediaType"() {
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
            it.principalId = createUserGroup.id
            it.principalType = PrincipalType.USER_GROUP
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)
        utils.addUserDelegate(userAdminToken, createdDA.id, defaultUser.id)

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
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND,
                ErrorCodes.ERROR_CODE_NOT_FOUND, "The specified agreement does not exist for this user")

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

    def "test a role assignment can be revoked from a delegation agreement"() {
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)

        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)
        utils.addUserDelegate(userAdminToken, createdDA.id, defaultUser.id)

        def cloudTenantId = userAdmin.domainId

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [cloudTenantId]))
                    tas
            }
            it
        }
        cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments)

        when: "list roles for DA"
        def response = cloud20.listRolesOnDelegationAgreement(userAdminToken, createdDA)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK

        retrievedEntity.tenantAssignments.tenantAssignment.size() == 1
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])

        when: "revoke role"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(userAdminToken, createdDA, ROLE_RBAC1_ID)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "list roles for DA"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, createdDA)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0
    }

    def "error: Common errors for revoking a role from a delegation agreement"() {
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)

        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)
        utils.addUserDelegate(userAdminToken, createdDA.id, defaultUser.id)

        def cloudTenantId = userAdmin.domainId

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [cloudTenantId]))
                    tas
            }
            it
        }
        cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments)

        when: "invalid delegation agreement"
        def invalidDA = new DelegationAgreement().with {
            it.id = "invalid"
            it
        }
        def response = cloud20.revokeRoleAssignmentFromDelegationAgreement(userAdminToken, invalidDA, ROLE_RBAC1_ID)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND,
                ErrorCodes.ERROR_CODE_NOT_FOUND , "The specified agreement does not exist for this user")

        when: "invalid token"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement("invalid", createdDA, ROLE_RBAC1_ID)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED
                , "No valid token provided. Please use the 'X-Auth-Token' header with a valid token.")

        when: "invalid roleId"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(userAdminToken, createdDA, "invalid")

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND, "The specified role does not exist for agreement")
    }

    def "changes to a tenant that remove the tenant from the domain removes all tenant assigned roles for the tenant on DAs"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def domainId = userAdmin.domainId
        def cloudTenantId = domainId
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(utils.getToken(userAdmin.username), delegationAgreement)
        def otherTenant = utils.createTenant()
        utils.addTenantToDomain(domainId, otherTenant.id)

        when: "verify that the tenant is assigned to the DA"
        addRoleOnTenantsForDelegationAgreement(utils.getToken(userAdmin.username), [cloudTenantId, otherTenant.id], createdDA)
        def roleAssignments = utils.listRolesOnDelegationAgreement(utils.getToken(userAdmin.username), createdDA)

        then:
        roleAssignments.getTenantAssignments().tenantAssignment.forTenants.flatten().contains(cloudTenantId)
        roleAssignments.getTenantAssignments().tenantAssignment.forTenants.flatten().contains(otherTenant.id)

        when: "delete the tenant off of the domain (moves it to the default domain)"
        utils.deleteTenantFromDomain(domainId, cloudTenantId)
        roleAssignments = utils.listRolesOnDelegationAgreement(utils.getToken(userAdmin.username), createdDA)

        then:
        !roleAssignments.getTenantAssignments().tenantAssignment.forTenants.flatten().contains(cloudTenantId)
        roleAssignments.getTenantAssignments().tenantAssignment.forTenants.flatten().contains(otherTenant.id)

        when: "add the tenant back to the original domain and assign the tenant back to the DA"
        utils.addTenantToDomain(domainId, cloudTenantId)
        addRoleOnTenantsForDelegationAgreement(utils.getToken(userAdmin.username), [cloudTenantId, otherTenant.id], createdDA)
        roleAssignments = utils.listRolesOnDelegationAgreement(utils.getToken(userAdmin.username), createdDA)

        then:
        roleAssignments.getTenantAssignments().tenantAssignment.forTenants.flatten().contains(cloudTenantId)
        roleAssignments.getTenantAssignments().tenantAssignment.forTenants.flatten().contains(otherTenant.id)

        when: "move the tenant from one non-default domain to another non-default domain"
        def otherDomain = utils.createDomainEntity()
        utils.addTenantToDomain(otherDomain.id, cloudTenantId)
        roleAssignments = utils.listRolesOnDelegationAgreement(utils.getToken(userAdmin.username), createdDA)

        then:
        !roleAssignments.getTenantAssignments().tenantAssignment.forTenants.flatten().contains(cloudTenantId)
        roleAssignments.getTenantAssignments().tenantAssignment.forTenants.flatten().contains(otherTenant.id)

        when: "add the tenant back to the original domain and assign the tenant back to the DA"
        utils.addTenantToDomain(domainId, cloudTenantId)
        addRoleOnTenantsForDelegationAgreement(utils.getToken(userAdmin.username), [cloudTenantId, otherTenant.id], createdDA)
        roleAssignments = utils.listRolesOnDelegationAgreement(utils.getToken(userAdmin.username), createdDA)

        then:
        roleAssignments.getTenantAssignments().tenantAssignment.forTenants.flatten().contains(cloudTenantId)
        roleAssignments.getTenantAssignments().tenantAssignment.forTenants.flatten().contains(otherTenant.id)

        when: "delete the tenant"
        utils.deleteTenant(cloudTenantId)
        roleAssignments = utils.listRolesOnDelegationAgreement(utils.getToken(userAdmin.username), createdDA)

        then:
        !roleAssignments.getTenantAssignments().tenantAssignment.forTenants.flatten().contains(cloudTenantId)
        roleAssignments.getTenantAssignments().tenantAssignment.forTenants.flatten().contains(otherTenant.id)
    }

    def "test authorized RCN admin is allowed to manage roles on DA"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def rcnAdmin = utils.createCloudAccount()

        // Update both domains to same RCN
        def rcn = testUtils.getRandomRCN()
        utils.domainRcnSwitch(userAdmin.domainId, rcn)
        utils.domainRcnSwitch(rcnAdmin.domainId, rcn)

        // Add rcn:admin role
        utils.addRoleToUser(rcnAdmin, RCN_ADMIN_ROLE_ID)

        def userAdminToken = utils.getToken(userAdmin.username)
        def rcnAdminToken = utils.getToken(rcnAdmin.username)

        // Create DA with user principal
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)

        // Create DA with user-group principal
        def userGroup = utils.createUserGroup(userAdmin.domainId)
        utils.addUserToUserGroup(userAdmin.id, userGroup)
        delegationAgreement.principalType = PrincipalType.USER_GROUP
        delegationAgreement.principalId = userGroup.id

        def createdDAUG = utils.createDelegationAgreement(userAdminToken, delegationAgreement)

        // Create role assignments
        def cloudTenantId = userAdmin.domainId
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [cloudTenantId]))
                    tas
            }
            it
        }

        // Add role assignment to userGroup
        utils.grantRoleAssignmentsOnUserGroup(userGroup, assignments)

        when: "grant role to DA with user principal"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(rcnAdminToken, createdDA, assignments)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK

        retrievedEntity.tenantAssignments.tenantAssignment.size() == 1
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])

        when: "revoke role"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(rcnAdminToken, createdDA, ROLE_RBAC1_ID)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "list roles for DA"
        response = cloud20.listRolesOnDelegationAgreement(rcnAdminToken, createdDA)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0

        when: "grant role to DA with user group"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(rcnAdminToken, createdDAUG, assignments)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK

        retrievedEntity.tenantAssignments.tenantAssignment.size() == 1
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])

        when: "revoke role"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(rcnAdminToken, createdDAUG, ROLE_RBAC1_ID)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "list roles for DA"
        response = cloud20.listRolesOnDelegationAgreement(rcnAdminToken, createdDAUG)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0
    }

    def "test authorized user admin is allowed to manage roles on DA"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)

        def userManager = utils.createUser(userAdminToken)
        utils.addRoleToUser(userManager, USER_MANAGE_ROLE_ID)
        def userManagerToken = utils.getToken(userManager.username)

        // Create DA with user principal
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(userManagerToken, delegationAgreement)

        // Create DA with user-group principal
        def userGroup = utils.createUserGroup(userManager.domainId)
        utils.addUserToUserGroup(userManager.id, userGroup)
        delegationAgreement.principalType = PrincipalType.USER_GROUP
        delegationAgreement.principalId = userGroup.id

        def createdDAUG = utils.createDelegationAgreement(userManagerToken, delegationAgreement)

        // Create role assignments
        def cloudTenantId = userAdmin.domainId
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [cloudTenantId]))
                    tas
            }
            it
        }

        // Add role assignment to userGroup
        utils.grantRoleAssignmentsOnUserGroup(userGroup, assignments)

        when: "grant role to DA with user principal"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK

        retrievedEntity.tenantAssignments.tenantAssignment.size() == 1
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])

        when: "revoke role"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(userAdminToken, createdDA, ROLE_RBAC1_ID)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "list roles for DA"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, createdDA)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0

        when: "grant role to DA with user group principal"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDAUG, assignments)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK

        retrievedEntity.tenantAssignments.tenantAssignment.size() == 1
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])

        when: "revoke role"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(userAdminToken, createdDAUG, ROLE_RBAC1_ID)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "list roles for DA"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, createdDAUG)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0
    }

    def "test authorized user is allowed to manage roles on DA where principal is a user manager"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)

        def userManager = utils.createUser(userAdminToken)
        utils.addRoleToUser(userManager, USER_MANAGE_ROLE_ID)
        def userManagerToken = utils.getToken(userManager.username)

        def userManager2 = utils.createUser(userAdminToken)
        utils.addRoleToUser(userManager2, USER_MANAGE_ROLE_ID)
        def userManager2Token = utils.getToken(userManager2.username)

        // Create DA with user principal
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(userManager2Token, delegationAgreement)

        // Create role assignments
        def cloudTenantId = userAdmin.domainId
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [cloudTenantId]))
                    tas
            }
            it
        }

        when: "grant role to DA with user admin token"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK

        retrievedEntity.tenantAssignments.tenantAssignment.size() == 1
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])

        when: "revoke role to DA with user admin token"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(userAdminToken, createdDA, ROLE_RBAC1_ID)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "list roles for DA"
        response = cloud20.listRolesOnDelegationAgreement(userManagerToken, createdDA)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0

        when: "grant role to DA with user manager token"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userManagerToken, createdDA, assignments)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK

        retrievedEntity.tenantAssignments.tenantAssignment.size() == 1
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])

        when: "revoke role from DA with user manager token"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(userManagerToken, createdDA, ROLE_RBAC1_ID)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "list roles for DA"
        response = cloud20.listRolesOnDelegationAgreement(userManagerToken, createdDA)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0
    }

    def "test authorized user is allowed to manage roles on DA where principal is a federated user"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)

        def userManager = utils.createUser(userAdminToken)
        utils.addRoleToUser(userManager, USER_MANAGE_ROLE_ID)
        def userManagerToken = utils.getToken(userManager.username)

        def fedAuthResponse = utils.authenticateFederatedUser(userAdmin.domainId, [] as Set, [ROLE_RBAC1_NAME] as Set)
        def federatedUserToken = fedAuthResponse.token.id

        // Create DA with user principal
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(federatedUserToken, delegationAgreement)

        // Create role assignments
        def cloudTenantId = userAdmin.domainId
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [cloudTenantId]))
                    tas
            }
            it
        }

        when: "grant role to DA with user admin token"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK

        retrievedEntity.tenantAssignments.tenantAssignment.size() == 1
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])

        when: "revoke role to DA with user admin token"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(userAdminToken, createdDA, ROLE_RBAC1_ID)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "list roles for DA"
        response = cloud20.listRolesOnDelegationAgreement(userManagerToken, createdDA)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0

        when: "grant role to DA with user manager token"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userManagerToken, createdDA, assignments)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK

        retrievedEntity.tenantAssignments.tenantAssignment.size() == 1
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])

        when: "revoke role from DA with user manager token"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(userManagerToken, createdDA, ROLE_RBAC1_ID)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "list roles for DA"
        response = cloud20.listRolesOnDelegationAgreement(userManagerToken, createdDA)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0
    }

    def "test authorized user is allowed to manage roles on DA where principal is a user group"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)

        def userManager = utils.createUser(userAdminToken)
        utils.addRoleToUser(userManager, USER_MANAGE_ROLE_ID)
        def userManagerToken = utils.getToken(userManager.username)

        def userGroup = utils.createUserGroup(userManager.domainId)
        utils.addUserToUserGroup(userAdmin.id, userGroup)

        // Create DA with user-group principal
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.principalId = userGroup.id
            it.principalType = PrincipalType.USER_GROUP
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)

        // Create role assignments
        def cloudTenantId = userAdmin.domainId
        RoleAssignments assignments_0 = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [cloudTenantId]))
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, ["*"]))
                    tas
            }
            it
        }

        RoleAssignments assignments_1 = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [cloudTenantId]))
                    tas
            }
            it
        }

        // Add role assignment to userGroup
        utils.grantRoleAssignmentsOnUserGroup(userGroup, assignments_0)

        when: "grant role to DA with user admin token"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, createdDA, assignments_0)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK

        retrievedEntity.tenantAssignments.tenantAssignment.size() == 2
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, ["*"])

        when: "revoke role with user admin token"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(userAdminToken, createdDA, ROLE_RBAC1_ID)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "revoke role with user manager token"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(userManagerToken, createdDA, ROLE_RBAC2_ID)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "list roles for DA"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, createdDA)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0

        when: "grant role to DA with user manager token"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userManagerToken, createdDA, assignments_1)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK

        retrievedEntity.tenantAssignments.tenantAssignment.size() == 1
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [cloudTenantId])

        when: "revoke role with user mananger token"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(userManagerToken, createdDA, ROLE_RBAC1_ID)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "list roles for DA"
        response = cloud20.listRolesOnDelegationAgreement(userManagerToken, createdDA)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0
    }

    def "error: verify user manager is not allowed to manage roles on DA where principal is a user admin"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)

        def userManager = utils.createUser(userAdminToken)
        utils.addRoleToUser(userManager, USER_MANAGE_ROLE_ID)
        def userManagerToken = utils.getToken(userManager.username)

        // Create DA with user principal
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)

        // Create role assignments
        def cloudTenantId = userAdmin.domainId
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [cloudTenantId]))
                    tas
            }
            it
        }

        when: "grant role to DA with user admin token"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userManagerToken, createdDA, assignments)

        then:
        response.status == HttpStatus.SC_NOT_FOUND

        when: "revoke role to DA with user admin token"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(userManagerToken, createdDA, ROLE_RBAC1_ID)

        then:
        response.status == HttpStatus.SC_NOT_FOUND

        when: "list roles for DA"
        response = cloud20.listRolesOnDelegationAgreement(userManagerToken, createdDA)

        then:
        response.status == HttpStatus.SC_NOT_FOUND
    }

    def "error: verify default user is not allowed to manage roles on DA where principal is a federated user"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)

        def defaultUser = utils.createUser(userAdminToken)
        def defaultUserToken = utils.getToken(defaultUser.username)

        def fedAuthResponse = utils.authenticateFederatedUser(userAdmin.domainId, [] as Set, [ROLE_RBAC1_NAME] as Set)
        def federatedUserToken = fedAuthResponse.token.id

        // Create DA with user principal
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = userAdmin.domainId
            it
        }
        def createdDA = utils.createDelegationAgreement(federatedUserToken, delegationAgreement)

        // Create role assignments
        def cloudTenantId = userAdmin.domainId
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [cloudTenantId]))
                    tas
            }
            it
        }

        when: "grant role to DA with default user token"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(defaultUserToken, createdDA, assignments)

        then:
        response.status == HttpStatus.SC_NOT_FOUND

        when: "revoke role to DA with default user token"
        response = cloud20.revokeRoleAssignmentFromDelegationAgreement(defaultUserToken, createdDA, ROLE_RBAC1_ID)

        then:
        response.status == HttpStatus.SC_NOT_FOUND

        when: "list roles for DA"
        response = cloud20.listRolesOnDelegationAgreement(defaultUserToken, createdDA)

        then:
        response.status == HttpStatus.SC_NOT_FOUND
    }

    @Unroll
    def "Roles are only assignable to nested agreements when feature enabled"() {
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)

        // Create a root da w/ self as delegate
        DelegationAgreement parentDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = 2
            it
        }
        parentDa = utils.createDelegationAgreement(userAdminToken, parentDa)
        utils.addUserDelegate(userAdminToken, parentDa.id, userAdmin.id)

        // Create a nested or sub da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = parentDa.id
            it.subAgreementNestLevel = 1
            it
        }

        def assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = ROLE_RBAC1_ID
                            ta.forTenants.add("*")
                            ta
                    })
                    tas
            }
            it
        }

        // Grant assignment to parent DA
        utils.grantRoleAssignmentsOnDelegationAgreement(parentDa, assignments, userAdminToken)

        subAgreement = utils.createDelegationAgreement(userAdminToken, subAgreement)

        when: "feature is disabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_GRANT_ROLES_TO_NESTED_DA_PROP, false)
        def roleGrantResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, assignments)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(roleGrantResponse, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION,
                DefaultDelegationCloudService.ERROR_MSG_NESTED_ROLE_ASSIGNMENT_FORBIDDEN)

        when: "feature is enabled"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_GRANT_ROLES_TO_NESTED_DA_PROP, true)
        roleGrantResponse = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, assignments)

        then:
        assert roleGrantResponse.status == HttpStatus.SC_OK

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, parentDa.id)
        cloud20.deleteDelegationAgreement(userAdminToken, subAgreement.id)
    }

    def "roles assigned to nested DA are based upon the parent DA's roles"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_GRANT_ROLES_TO_NESTED_DA_PROP, true)
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)

        // Create a root da w/ self as delegate
        DelegationAgreement parentDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = 2
            it
        }
        parentDa = utils.createDelegationAgreement(userAdminToken, parentDa)
        utils.addUserDelegate(userAdminToken, parentDa.id, userAdmin.id)

        // Create a nested or sub da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = parentDa.id
            it.subAgreementNestLevel = 1
            it
        }

        def assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = ROLE_RBAC1_ID
                            ta.forTenants.add("*")
                            ta
                    })
                    tas
            }
            it
        }

        subAgreement = utils.createDelegationAgreement(userAdminToken, subAgreement)

        when: "grant role to nested DA with no roles on parent DA"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, assignments)

        then: "assert forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, ROLE_RBAC1_ID))

        when: "grant role to nested DA with role on parent DA"
        utils.grantRoleAssignmentsOnDelegationAgreement(parentDa, assignments, userAdminToken)
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, assignments)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, parentDa.id)
        cloud20.deleteDelegationAgreement(userAdminToken, subAgreement.id)
    }

    def "verify principal of parent DA is not allowed to manage roles on nested DA in a different domain"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_GRANT_ROLES_TO_NESTED_DA_PROP, true)
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)
        def userAdmin2 = utils.createCloudAccount()
        def userAdmin2Token = utils.getToken(userAdmin2.username)

        // update domains to same RCN
        def rcn = testUtils.getRandomRCN()
        utils.domainRcnSwitch(userAdmin.domainId, rcn)
        utils.domainRcnSwitch(userAdmin2.domainId, rcn)

        // Create a root da w/ self as delegate
        DelegationAgreement parentDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = 2
            it
        }
        parentDa = utils.createDelegationAgreement(userAdminToken, parentDa)
        utils.addUserDelegate(userAdminToken, parentDa.id, userAdmin2.id)

        // Create a nested or sub da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = parentDa.id
            it.subAgreementNestLevel = 1
            it
        }
        subAgreement = utils.createDelegationAgreement(userAdmin2Token, subAgreement)

        // Add assignment to parent DA
        def assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = ROLE_RBAC1_ID
                            ta.forTenants.add("*")
                            ta
                    })
                    tas
            }
            it
        }
        utils.grantRoleAssignmentsOnDelegationAgreement(parentDa, assignments, userAdminToken)

        when: "grant role to nested DA with parent DA's principal"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, assignments)

        then: "assert forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND,
                ERROR_CODE_NOT_FOUND, "The specified agreement does not exist for this user")

        when: "grant role to nested DA"
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdmin2Token, subAgreement, assignments)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdmin2Token, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, parentDa.id)
        cloud20.deleteDelegationAgreement(userAdminToken, subAgreement.id)
    }

    def "allow granting a tenant role assignment on nested DA when parent DA has a global role assignment"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_GRANT_ROLES_TO_NESTED_DA_PROP, true)
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)

        // Create a root da w/ self as delegate
        DelegationAgreement parentDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = 2
            it
        }
        parentDa = utils.createDelegationAgreement(userAdminToken, parentDa)
        utils.addUserDelegate(userAdminToken, parentDa.id, userAdmin.id)

        // Create a nested or sub da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = parentDa.id
            it.subAgreementNestLevel = 1
            it
        }

        def parentAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = ROLE_RBAC1_ID
                            ta.forTenants.add("*")
                            ta
                    })
                    tas
            }
            it
        }
        utils.grantRoleAssignmentsOnDelegationAgreement(parentDa, parentAssignments, userAdminToken)

        def assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = ROLE_RBAC1_ID
                            ta.forTenants.add(userAdmin.domainId)
                            ta
                    })
                    tas
            }
            it
        }

        subAgreement = utils.createDelegationAgreement(userAdminToken, subAgreement)

        when: "grant tenant role to nested DA"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, assignments)
        def retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [userAdmin.domainId])

        when: "list roles"
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, subAgreement)
        retrievedEntity = response.getEntity(RoleAssignments)

        then: "successful"
        response.status == HttpStatus.SC_OK
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [userAdmin.domainId])

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, parentDa.id)
        cloud20.deleteDelegationAgreement(userAdminToken, subAgreement.id)
    }

    def "Error check on nested DAs"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_GRANT_ROLES_TO_NESTED_DA_PROP, true)
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)

        // Create a root da w/ self as delegate
        DelegationAgreement parentDa = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.subAgreementNestLevel = 2
            it
        }
        parentDa = utils.createDelegationAgreement(userAdminToken, parentDa)
        utils.addUserDelegate(userAdminToken, parentDa.id, userAdmin.id)

        // Create a nested or sub da
        DelegationAgreement subAgreement = new DelegationAgreement().with {
            it.name = RandomStringUtils.randomAlphabetic(32)
            it.description = RandomStringUtils.randomAlphabetic(255)
            it.parentDelegationAgreementId = parentDa.id
            it.subAgreementNestLevel = 1
            it
        }

        def assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = ROLE_RBAC1_ID
                            ta.forTenants.add(userAdmin.domainId)
                            ta
                    })
                    tas
            }
            it
        }

        subAgreement = utils.createDelegationAgreement(userAdminToken, subAgreement)

        when: "parent agreement has no roles"
        def response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, assignments)

        then: "assert forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, ROLE_RBAC1_ID))

        when: "parent agreement does not exist"
        utils.deleteDelegationAgreement(userAdminToken, parentDa)
        response = cloud20.grantRoleAssignmentsOnDelegationAgreement(userAdminToken, subAgreement, assignments)

        then: "assert forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_DATA_INTEGRITY, "Parent agreement for nested agreement was not found.")

        cleanup:
        cloud20.deleteDelegationAgreement(userAdminToken, subAgreement.id)
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

    def addRoleOnTenantsForDelegationAgreement(token, tenantIds, delegationAgreement, roleId = ROLE_RBAC1_ID) {
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(roleId, tenantIds))
                    tas
            }
            it
        }
        cloud20.grantRoleAssignmentsOnDelegationAgreement(token, delegationAgreement, assignments)
    }
}
