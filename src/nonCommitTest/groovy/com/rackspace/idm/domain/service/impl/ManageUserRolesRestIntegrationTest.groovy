package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.core.event.EventType
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.ApplicationService
import org.apache.commons.collections4.CollectionUtils
import org.apache.http.HttpStatus
import org.mockserver.verify.VerificationTimes
import org.openstack.docs.identity.api.v2.*
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.*
import static com.rackspace.idm.ErrorCodes.*

class ManageUserRolesRestIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    ApplicationService applicationService

    @Shared def sharedIdentityAdminToken

    void doSetupSpec() {
        def authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == HttpStatus.SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id
    }

    def setup() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_GRANT_ROLES_TO_USER_SERVICE_PROP, true)
    }

    def cleanup() {
        reloadableConfiguration.reset()
    }

    /**
     * Test a typical modification to the set of roles assigned to a user
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
    def "Modify roles on user; mediaType = #mediaType"() {
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        // Create tenants that belong in target user's domain
        def tenant = utils.createTenant()
        utils.addTenantToDomain(domainId, tenant.id)
        def tenant2 = utils.createTenant()
        utils.addTenantToDomain(domainId, tenant2.id)
        RoleAssignments assignments0 = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments()
            it
        }

        RoleAssignments assignments1 = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [tenant.id]))
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
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [tenant.id, tenant2.id]))
                    tas
            }
            it
        }

        RoleAssignments assignments4 = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [tenant.id]))
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
        resetCloudFeedsMock()
        def getResponse = cloud20.grantRoleAssignmentsOnUser(sharedIdentityAdminToken, userAdmin, assignments0, mediaType)
        RoleAssignments retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        // Expect the user-admin role
        retrievedEntity.tenantAssignments.tenantAssignment.size() == 1

        and: "verify no update user event is sent"
        cloudFeedsMock.verify(
                testUtils.createUserFeedsRequest(userAdmin, EventType.UPDATE),
                VerificationTimes.exactly(0)
        )

        when: "assignment 1"
        resetCloudFeedsMock()
        getResponse = cloud20.grantRoleAssignmentsOnUser(sharedIdentityAdminToken, userAdmin, assignments1, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [tenant.id])
        def rbac2Assignment = retrievedEntity.tenantAssignments.tenantAssignment.find {it.onRole == ROLE_RBAC2_ID}
        rbac2Assignment == null

        and: "verify an update user event is sent"
        cloudFeedsMock.verify(
                testUtils.createUserFeedsRequest(userAdmin, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        when: "assignment 2"
        resetCloudFeedsMock()
        getResponse = cloud20.grantRoleAssignmentsOnUser(sharedIdentityAdminToken, userAdmin, assignments2, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, [tenant.id])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, ["*"])

        and: "verify an update user event is sent"
        cloudFeedsMock.verify(
                testUtils.createUserFeedsRequest(userAdmin, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        when: "assignment 3"
        resetCloudFeedsMock()
        getResponse = cloud20.grantRoleAssignmentsOnUser(sharedIdentityAdminToken, userAdmin, assignments3, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, [tenant.id, tenant2.id])

        and: "verify an update user event is sent"
        cloudFeedsMock.verify(
                testUtils.createUserFeedsRequest(userAdmin, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        when: "assignment 4"
        resetCloudFeedsMock()
        getResponse = cloud20.grantRoleAssignmentsOnUser(sharedIdentityAdminToken, userAdmin, assignments4, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, [tenant.id])

        and: "verify an update user event is sent"
        cloudFeedsMock.verify(
                testUtils.createUserFeedsRequest(userAdmin, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        when: "assignment 5"
        resetCloudFeedsMock()
        getResponse = cloud20.grantRoleAssignmentsOnUser(sharedIdentityAdminToken, userAdmin, assignments5, mediaType)
        retrievedEntity = getResponse.getEntity(RoleAssignments)

        then:
        getResponse.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC2_ID, ["*"])

        and: "verify an update user event is sent"
        cloudFeedsMock.verify(
                testUtils.createUserFeedsRequest(userAdmin, EventType.UPDATE),
                VerificationTimes.exactly(1)
        )

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteTenantQuietly(tenant)
        utils.deleteTenantQuietly(tenant2)
        utils.deleteTestDomainQuietly(domainId)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error: Do not allow more than the max tenant assignments when granting roles to user - maxTenantAssignments = #maxTa"(){
        given:
        reloadableConfiguration.setProperty(IdentityConfig.ROLE_ASSIGNMENTS_MAX_TENANT_ASSIGNMENTS_PER_REQUEST_PROP, maxTa)
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        // Create tenants that belong in target user's domain
        def tenant = utils.createTenant()
        utils.addTenantToDomain(domainId, tenant.id)
        def tenant2 = utils.createTenant()
        utils.addTenantToDomain(domainId, tenant2.id)

        RoleAssignments assignments = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [tenant.id]))
            ta.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, [tenant2.id]))
            it.tenantAssignments = ta
            it
        }

        when:
        def response = cloud20.grantRoleAssignmentsOnUser(sharedIdentityAdminToken, userAdmin, assignments)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST,
                ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE,
                String.format(ErrorCodes.ERROR_CODE_ROLE_ASSIGNMENT_MAX_TENANT_ASSIGNMENT_MSG_PATTERN, maxTa))

        where:
        maxTa << [0, 1]
    }

    @Unroll
    def "Allow an authorized user to grant roles to target user; mediaType = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]

        RoleAssignments assignments = new RoleAssignments().with {
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

        when: "user-admin adding role to default user"
        def response = cloud20.grantRoleAssignmentsOnUser(utils.getToken(userAdmin.username), defaultUser, assignments, mediaType)
        RoleAssignments retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])

        when: "user-manage adding role to default user"
        response = cloud20.grantRoleAssignmentsOnUser(utils.getToken(userManage.username), defaultUser, assignments, mediaType)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])

        when: "admin adding role to user-admin"
        response = cloud20.grantRoleAssignmentsOnUser(utils.getToken(identityAdmin.username), userAdmin, assignments, mediaType)
        retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, ROLE_RBAC1_ID, ["*"])

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(domainId)
        reloadableConfiguration.reset()

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error: Do not allow authorized caller to grant the 'identity:user-manage' role to non-default user; mediaType = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]

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

        when:
        def response = cloud20.grantRoleAssignmentsOnUser(utils.getToken(identityAdmin.username), userAdmin, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ERROR_CODE_USER_MANAGE_ON_NON_DEFAULT_USER)

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(domainId)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error: Do not allow authorized caller to grant the 'identity:user-manage' role on tenants; mediaType = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]

        def tenant = utils.createTenant()
        utils.addTenantToDomain(domainId, tenant.id)
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = USER_MANAGE_ROLE_ID
                            ta.forTenants.add(tenant.id)
                            ta
                    })
                    tas
            }
            it
        }

        when:
        def response = cloud20.grantRoleAssignmentsOnUser(utils.getToken(userAdmin.username), defaultUser, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, ERROR_CODE_INVALID_ATTRIBUTE)

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(domainId)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error: Do not allow caller to grant roles they don't have access to; mediaType = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]

        def role = utils.createRole(null, testUtils.getRandomUUID("role"), "identity:admin")

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = role.id
                            ta.forTenants.add("*")
                            ta
                    })
                    tas
            }
            it
        }

        when:
        def response = cloud20.grantRoleAssignmentsOnUser(utils.getToken(userAdmin.username), defaultUser, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN
                , ERROR_CODE_INVALID_ATTRIBUTE
                , String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, role.id));

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(domainId)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Do not allow granting identity user type roles to user except user-manager; mediaType = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, manageUser, defaultUser
        (identityAdmin, userAdmin, manageUser, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, manageUser, userAdmin, identityAdmin]

        RoleAssignments assignments0 = new RoleAssignments().with {
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

        RoleAssignments assignments1 = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = USER_ADMIN_ROLE_ID
                            ta.forTenants.add("*")
                            ta
                    })
                    tas
            }
            it
        }

        when: "adding user-manage role to default user"
        def response = cloud20.grantRoleAssignmentsOnUser(utils.getToken(userAdmin.username), defaultUser, assignments0, mediaType)
        RoleAssignments retrievedEntity = response.getEntity(RoleAssignments)

        then:
        response.status == HttpStatus.SC_OK
        retrievedEntity.tenantAssignments != null
        verifyContainsAssignment(retrievedEntity, USER_MANAGE_ROLE_ID, ["*"])

        when: "adding user-admin role to default user"
        response = cloud20.grantRoleAssignmentsOnUser(sharedIdentityAdminToken, defaultUser, assignments1, mediaType)

        then: "return 403 Forbidden"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN
                , ERROR_CODE_INVALID_ATTRIBUTE
                , String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, USER_ADMIN_ROLE_ID));

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(domainId)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error: No forTenants value on role assignment returns error; mediaType = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, []))
                    tas
            }
            it
        }

        when:
        def response = cloud20.grantRoleAssignmentsOnUser(sharedIdentityAdminToken, userAdmin, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST
                , ERROR_CODE_REQUIRED_ATTRIBUTE
                , ERROR_CODE_ROLE_ASSIGNMENT_MISSING_FOR_TENANTS_MSG)

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(domainId)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error: Common errors for granting role assignments to user; mediaType = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, manageUser, defaultUser
        (identityAdmin, userAdmin, manageUser, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, manageUser, userAdmin, identityAdmin]

        def domainId2 = utils.createDomain()
        def userAdmin2, users2
        (userAdmin2, users2) = utils.createUserAdmin(domainId2)

        def tenant = utils.createTenant()

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC2_ID, []))
                    tas
            }
            it
        }

        when: "invalid user"
        def invalidUser = v2Factory.createUser().with {
            it.id = "invalid"
            it
        }
        def response = cloud20.grantRoleAssignmentsOnUser(sharedIdentityAdminToken, invalidUser, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND
                , String.format("User %s not found", invalidUser.id))

        when: "invalid token"
        response = cloud20.grantRoleAssignmentsOnUser("invalidToken", userAdmin, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED
                , "No valid token provided. Please use the 'X-Auth-Token' header with a valid token.")

        when: "using user-manager token"
        response = cloud20.grantRoleAssignmentsOnUser(utils.getToken(defaultUser.username), userAdmin, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN , "Not Authorized")

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

        response = cloud20.grantRoleAssignmentsOnUser(utils.getToken(userAdmin.username), defaultUser, invalidAssignment, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault,
                HttpStatus.SC_BAD_REQUEST, ERROR_CODE_DUP_ROLE_ASSIGNMENT, ERROR_CODE_DUP_ROLE_ASSIGNMENT_MSG)

        when: "tenant not belonging in target user's domain"
        invalidAssignment = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, [tenant.id]))
                    tas
            }
            it
        }

        response = cloud20.grantRoleAssignmentsOnUser(utils.getToken(userAdmin.username), defaultUser, invalidAssignment, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                ERROR_CODE_INVALID_ATTRIBUTE, String.format(ERROR_CODE_ROLE_ASSIGNMENT_WRONG_DOMAIN_TENANT_MSG_PATTERN, ROLE_RBAC1_ID, domainId))

        when: "userAdmin attempts to modify default user on a different domain"
        response = cloud20.grantRoleAssignmentsOnUser(utils.getToken(userAdmin2.username), defaultUser, assignments, mediaType)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteUsersQuietly(users2)
        utils.deleteTestDomainQuietly(domainId)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "grantRolesToUser: test feature flag - mediaType = #mediaType"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_GRANT_ROLES_TO_USER_SERVICE_PROP, false)

        def user = new User().with {
            it.id = "userId"
            it
        }
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(ROLE_RBAC1_ID, []))
                    tas
            }
            it
        }

        when:
        def response = cloud20.grantRoleAssignmentsOnUser(utils.getIdentityAdminToken(), user, assignments, mediaType)

        then:
        response.status == HttpStatus.SC_NOT_FOUND

        cleanup:
        reloadableConfiguration.reset()

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "grantRolesToUser: user-managers can assign/remove the user-manage role from users"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]
        def role = utils.createRole()

        def tenant = utils.createTenant()
        utils.addTenantToDomain(domainId, tenant.id)

        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(USER_MANAGE_ROLE_ID, ['*']))
                    tas
            }
            it
        }

        def token = utils.getToken(userManage.username)

        when: "grant domain role to user"
        def response = cloud20.addUserRole(token, defaultUser.id, USER_MANAGE_ROLE_ID)

        then:
        response.status == HttpStatus.SC_OK

        when: "revoke domain role from user"
        response = cloud20.deleteApplicationRoleOnUser(token, USER_MANAGE_ROLE_ID, defaultUser.id)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "grant role on tenant to user"
        response = cloud20.addRoleToUserOnTenant(token, tenant.id, defaultUser.id, role.id)

        then:
        response.status == HttpStatus.SC_OK

        when: "revoke role on tenant from user"
        response = cloud20.deleteRoleFromUserOnTenant(token, tenant.id, defaultUser.id, role.id)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "grant role to user"
        response = cloud20.grantRoleAssignmentsOnUser(token, defaultUser, assignments)

        then:
        response.status == HttpStatus.SC_OK

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(domainId)
        utils.deleteRoleQuietly(role)
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
