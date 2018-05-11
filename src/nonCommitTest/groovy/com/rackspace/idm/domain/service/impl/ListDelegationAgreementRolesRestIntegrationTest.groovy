package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.v20.DelegationAgreementRoleSearchParams
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import com.rackspace.idm.domain.config.IdentityConfig
import org.apache.commons.lang.StringUtils
import org.openstack.docs.identity.api.v2.*
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.*

class ListDelegationAgreementRolesRestIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Shared
    def sharedServiceAdminToken

    @Shared
    def sharedIdentityAdminToken

    @Shared User sharedUserAdmin
    @Shared User sharedUserAdmin2
    @Shared User sharedUserAdmin3
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminCloudTenant

    @Shared RoleAssignments assignments

    void doSetupSpec() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)

        def authResponse = cloud20.authenticatePassword(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        assert authResponse.status == SC_OK
        sharedServiceAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)
        sharedUserAdmin2 = cloud20.createCloudAccount(sharedIdentityAdminToken)
        sharedUserAdmin3 = cloud20.createCloudAccount(sharedIdentityAdminToken)

        // Update domains to have same RCN
        def commonRcn = getRandomRCN()
        def rcnSwitchResponse = cloud20.domainRcnSwitch(sharedServiceAdminToken, sharedUserAdmin.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)
        rcnSwitchResponse = cloud20.domainRcnSwitch(sharedServiceAdminToken, sharedUserAdmin2.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)
        rcnSwitchResponse = cloud20.domainRcnSwitch(sharedServiceAdminToken, sharedUserAdmin3.domainId, commonRcn)
        assert (rcnSwitchResponse.status == SC_NO_CONTENT)

        Tenants tenants = cloud20.getDomainTenants(sharedIdentityAdminToken, sharedUserAdmin.domainId).getEntity(Tenants).value
        sharedUserAdminCloudTenant = tenants.tenant.find {
            it.id == sharedUserAdmin.domainId
        }

        assignments = new RoleAssignments().with {
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
    }

    @Unroll
    def "test that principal and delegate can list DA roles when principal is a user; mediaType = #mediaType"() {
        given:
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = sharedUserAdmin.domainId
            it
        }
        def principalToken = utils.getToken(sharedUserAdmin.username)
        def callerToken = utils.getToken(caller.username)
        def createdDA = utils.createDelegationAgreement(principalToken, delegationAgreement)
        utils.addUserDelegate(principalToken, createdDA.id, sharedUserAdmin2.id)

        when: "list DA roles - no roles"
        def response = cloud20.listRolesOnDelegationAgreement(callerToken, createdDA, null, mediaType)
        RoleAssignments retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == SC_OK
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0

        when: "list DA roles"
        utils.grantRoleAssignmentsOnDelegationAgreement(createdDA, assignments, principalToken)
        response = cloud20.listRolesOnDelegationAgreement(callerToken, createdDA, null, mediaType)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == SC_OK
        verifyRetrievedRoleAssignments(retrievedEntity)

        cleanup:
        utils.deleteDelegationAgreement(principalToken, createdDA)

        where:
        caller           | mediaType
        sharedUserAdmin  | MediaType.APPLICATION_XML_TYPE
        sharedUserAdmin  | MediaType.APPLICATION_JSON_TYPE
        sharedUserAdmin2 | MediaType.APPLICATION_XML_TYPE
        sharedUserAdmin2 | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "error check: list DA roles when principal is a user; mediaType = #mediaType"() {
        given:
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = sharedUserAdmin.domainId
            it
        }
        def principalToken = utils.getToken(sharedUserAdmin.username)
        def createdDA = utils.createDelegationAgreement(principalToken, delegationAgreement)
        utils.addUserDelegate(principalToken, createdDA.id, sharedUserAdmin2.id)

        utils.grantRoleAssignmentsOnDelegationAgreement(createdDA, assignments, principalToken)

        when: "list DA roles with invalid token"
        def response = cloud20.listRolesOnDelegationAgreement("invalid", createdDA, null, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, UnauthorizedFault, SC_UNAUTHORIZED, "No valid token provided. Please use the 'X-Auth-Token' header with a valid token.")

        when: "list DA roles with invalid DA"
        def invalidDA = new DelegationAgreement().with {
            it.id = "invalid"
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = sharedUserAdmin.domainId
            it
        }
        response = cloud20.listRolesOnDelegationAgreement(principalToken, invalidDA, null, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND, "The specified agreement does not exist for this user")

        when: "list DA roles with unauthorized token"
        def callerToken = utils.getToken(sharedUserAdmin3.username)
        response = cloud20.listRolesOnDelegationAgreement(callerToken, createdDA, null, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND, "The specified agreement does not exist for this user")

        when: "list DA roles with delegation token"
        callerToken = utils.getDelegationAgreementToken(sharedUserAdmin2.username, createdDA.id)
        response = cloud20.listRolesOnDelegationAgreement(callerToken, createdDA, null, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN)

        cleanup:
        utils.deleteDelegationAgreement(principalToken, createdDA)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "test that members of user group can list DA roles when principal is a user group; mediaType = #mediaType"() {
        given:
        def userAdminToken = utils.getToken(sharedUserAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)
        def createUserGroup = utils.createUserGroup(sharedUserAdmin.domainId)
        // Add users to user group
        utils.addUserToUserGroup(sharedUserAdmin.id, createUserGroup)
        utils.addUserToUserGroup(defaultUser.id, createUserGroup)

        // Grant roles on userGroup
        utils.grantRoleAssignmentsOnUserGroup(createUserGroup, assignments)

        // Create DA with principal user group
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = sharedUserAdmin.domainId
            it.principalId = createUserGroup.id
            it.principalType = PrincipalType.USER_GROUP
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)
        utils.addUserDelegate(userAdminToken, createdDA.id, sharedUserAdmin2.id)

        when: "list DA roles - no roles"
        def response = cloud20.listRolesOnDelegationAgreement(userAdminToken, createdDA, null, mediaType)
        RoleAssignments retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == SC_OK
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 0

        when: "list DA roles with userAdmin token"
        utils.grantRoleAssignmentsOnDelegationAgreement(createdDA, assignments, userAdminToken)
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, createdDA, null, mediaType)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == SC_OK
        verifyRetrievedRoleAssignments(retrievedEntity)

        when: "list DA roles with defaultUser token"
        def defaultUserToken = utils.getToken(defaultUser.username)
        response = cloud20.listRolesOnDelegationAgreement(defaultUserToken, createdDA, null, mediaType)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == SC_OK
        verifyRetrievedRoleAssignments(retrievedEntity)

        when: "list DA roles with delegate's token"
        def delegateToken = utils.getToken(sharedUserAdmin2.username)
        response = cloud20.listRolesOnDelegationAgreement(delegateToken, createdDA, null, mediaType)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == SC_OK
        verifyRetrievedRoleAssignments(retrievedEntity)

        cleanup:
        utils.deleteDelegationAgreement(userAdminToken, createdDA)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "error check: list DA roles when principal is a user group; mediaType = #mediaType"() {
        given:
        def userAdminToken = utils.getToken(sharedUserAdmin.username)
        def defaultUser = utils.createUser(userAdminToken)
        def createUserGroup = utils.createUserGroup(sharedUserAdmin.domainId)
        // Add users to user group
        utils.addUserToUserGroup(sharedUserAdmin.id, createUserGroup)
        utils.addUserToUserGroup(defaultUser.id, createUserGroup)

        // Grant roles on userGroup
        utils.grantRoleAssignmentsOnUserGroup(createUserGroup, assignments)

        // Create DA with principal user group
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = sharedUserAdmin.domainId
            it.principalId = createUserGroup.id
            it.principalType = PrincipalType.USER_GROUP
            it
        }
        def createdDA = utils.createDelegationAgreement(userAdminToken, delegationAgreement)
        utils.addUserDelegate(userAdminToken, createdDA.id, sharedUserAdmin.id)

        utils.grantRoleAssignmentsOnDelegationAgreement(createdDA, assignments, userAdminToken)

        when: "list DA roles with invalid token"
        def response = cloud20.listRolesOnDelegationAgreement("invalid", createdDA, null, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, UnauthorizedFault, SC_UNAUTHORIZED, "No valid token provided. Please use the 'X-Auth-Token' header with a valid token.")

        when: "list DA roles with invalid DA"
        def invalidDA = new DelegationAgreement().with {
            it.id = "invalid"
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = sharedUserAdmin.domainId
            it
        }
        response = cloud20.listRolesOnDelegationAgreement(userAdminToken, invalidDA, null, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND, "The specified agreement does not exist for this user")

        when: "list DA roles with unauthorized token"
        def callerToken = utils.getToken(sharedUserAdmin3.username)
        response = cloud20.listRolesOnDelegationAgreement(callerToken, createdDA, null, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, SC_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND, "The specified agreement does not exist for this user")

        when: "list DA roles with delegation token"
        callerToken = utils.getDelegationAgreementToken(sharedUserAdmin.username, createdDA.id)
        response = cloud20.listRolesOnDelegationAgreement(callerToken, createdDA, null, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, SC_FORBIDDEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION, GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN)

        cleanup:
        utils.deleteDelegationAgreement(userAdminToken, createdDA)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Retrieve roles on delegation agreement with pagination; mediaType = #mediaType"() {
        given:
        def delegationAgreement = new DelegationAgreement().with {
            it.name = testUtils.getRandomUUIDOfLength("da", 32)
            it.domainId = sharedUserAdmin.domainId
            it
        }
        def principalToken = utils.getToken(sharedUserAdmin.username)
        def createdDA = utils.createDelegationAgreement(principalToken, delegationAgreement)
        utils.addUserDelegate(principalToken, createdDA.id, sharedUserAdmin2.id)

        utils.grantRoleAssignmentsOnDelegationAgreement(createdDA, assignments, principalToken)

        when: "Get first page"
        def searchParams = new DelegationAgreementRoleSearchParams(new PaginationParams(0, 1))
        def response = cloud20.listRolesOnDelegationAgreement(principalToken, createdDA, searchParams, mediaType)

        then: "returns one result"
        response.status == SC_OK
        RoleAssignments firstPage = response.getEntity(RoleAssignments)
        firstPage.tenantAssignments != null
        firstPage.tenantAssignments.tenantAssignment.size() == 1

        and:
        StringUtils.isNotBlank(response.headers.getFirst("Link"))

        when: "Get second page"
        searchParams = new DelegationAgreementRoleSearchParams(new PaginationParams(1 ,1))
        def response2 = cloud20.listRolesOnDelegationAgreement(principalToken, createdDA, searchParams, mediaType)

        then: "returns one result"
        response2.status == SC_OK
        RoleAssignments secondPage = response2.getEntity(RoleAssignments)
        secondPage.tenantAssignments != null
        secondPage.tenantAssignments.tenantAssignment.size() == 1

        and:
        StringUtils.isNotBlank(response2.headers.getFirst("Link"))

        when: "combine both pages"
        def retrievedRoleAssignments = new ArrayList<TenantAssignment>(2)
        retrievedRoleAssignments.addAll(firstPage.tenantAssignments.tenantAssignment)
        retrievedRoleAssignments.addAll(secondPage.tenantAssignments.tenantAssignment)

        then:
        def rbac1Assignment = retrievedRoleAssignments.find {it.onRole == Constants.ROLE_RBAC1_ID}
        rbac1Assignment != null
        rbac1Assignment.forTenants.size() == 1
        rbac1Assignment.forTenants[0] == "*"

        def rbac2Assignment = retrievedRoleAssignments.find {it.onRole == Constants.ROLE_RBAC2_ID}
        rbac2Assignment != null
        rbac2Assignment.forTenants.size() == 1
        rbac2Assignment.forTenants[0] == sharedUserAdminCloudTenant.id

        cleanup:
        utils.deleteDelegationAgreement(principalToken, createdDA)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    private void verifyRetrievedRoleAssignments(RoleAssignments assignments) {
        assert assignments.tenantAssignments.tenantAssignment.size() == 2

        def tr = assignments.tenantAssignments.tenantAssignment.find { it.onRole == Constants.ROLE_RBAC1_ID }
        assert tr != null
        assert tr.forTenants.size() == 1
        assert tr.forTenants[0] == "*"

        def tr2 = assignments.tenantAssignments.tenantAssignment.find { it.onRole == Constants.ROLE_RBAC2_ID }
        assert tr2 != null
        assert tr2.forTenants.size() == 1
        assert tr2.forTenants[0] == sharedUserAdminCloudTenant.id
    }
}
