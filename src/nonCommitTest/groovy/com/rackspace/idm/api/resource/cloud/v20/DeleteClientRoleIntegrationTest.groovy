package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.TenantDao
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.util.JSONReaderForRoles
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.configuration.Configuration
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.IdentityFault
import org.openstack.docs.identity.api.v2.Role
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest

import static com.rackspace.idm.Constants.IDENTITY_SERVICE_ID
import static com.rackspace.idm.ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE
import static com.rackspace.idm.ErrorCodes.ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN

class DeleteClientRoleIntegrationTest extends RootIntegrationTest {

    @Shared def identityAdmin, userAdmin, userManage, defaultUser
    @Shared def domainId
    @Shared def service

    @Autowired
    Configuration config

    @Autowired
    TenantRoleDao tenantRoleDao

    @Autowired
    FederatedUserDao federatedUserRepository

    @Autowired
    ApplicationRoleDao applicationRoleDao

    def "cannot delete a client role if the role is assigned to a user"(){
        given:
        def domainId = utils.createDomain()
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)

        def identityAdminToken = utils.getIdentityAdminToken()

        def service = utils.createService()
        def roleNotBeingAssigned = utils.createRole(service)
        def roleBeingAssigned = utils.createRole(service)

        when: "roles are fetched"
        def roles = getAllRoles(identityAdminToken, service)

        then: "newly created roles are listed"
        roles.size() == 2
        roles.find { it.id.contains(roleNotBeingAssigned.id)  != null }
        roles.find { it.id.contains(roleBeingAssigned.id)  != null }

        when: "un assigned role is attempted for deletion"
        def roleResponse  = cloud20.deleteRole(utils.getServiceAdminToken(),roleNotBeingAssigned.id)
        roles = getAllRoles(identityAdminToken, service)

        then: "unassigned role is deleted"
        roleResponse.status == HttpStatus.SC_NO_CONTENT

        and: "it is not listed"
        roles.size() ==1
        roles.find {it -> it.id==roleNotBeingAssigned.id} == null
        roles.find {it -> it.id==roleBeingAssigned.id} != null

        when: "already un assigned role is attempted for deletion"
        utils.addRoleToUser(defaultUser,roleBeingAssigned.id)
        roleResponse  = cloud20.deleteRole(utils.getServiceAdminToken(),roleBeingAssigned.id)
        roles = getAllRoles(identityAdminToken, service)

        then: "assigned role is not deleted"
        IdmAssert.assertOpenStackV2FaultResponse(roleResponse, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                DefaultCloud20Service.ERROR_DELETE_ASSIGNED_ROLE)

        and: "it would still get listed "
        roles.size() ==1
        roles.find {it -> it.id==roleNotBeingAssigned.id} == null
        roles.find {it -> it.id==roleBeingAssigned.id} != null

        cleanup:
        utils.deleteUsers(defaultUser, userManage, userAdmin, identityAdmin)
        utils.deleteDomain(domainId)
        utils.deleteService(service)
    }

    def "cannot delete a client role if the role is assigned to a Federated user"() {
        given:
        def role = v2Factory.createRole(testUtils.getRandomUUID("")).with{
            it.administratorRole="identity:user-manage"
            it
        }
        def responseRole = cloud20.createRole(utils.getIdentityAdminToken(), role)
        def gRole = responseRole.getEntity(Role).value
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, [gRole.name])
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        utils.addRoleToUser(userAdmin, gRole.id)

        when: "creating a saml user under a user-admin with a non-propagating role"
        def samlResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value

        then: "the non-propagating role is not shown in the response"
        samlResponse.user.roles.role.id.contains(gRole.id)

        when: "loading the federated user roles from the directory"
        def fedUser = federatedUserRepository.getUserById(samlResponse.user.id)
        assert fedUser != null

        then: "the non-propagating role is assigned to user"
        TenantRole roleOnUser = tenantRoleDao.getTenantRoleForUser(fedUser, gRole.id)
        assert roleOnUser != null
        assert CollectionUtils.isEmpty(roleOnUser.tenantIds)

        when: "role assigned to Fed user is attempted for deletion"
        def roleResponse  = cloud20.deleteRole(utils.getServiceAdminToken(),gRole.id)

        then: "role cannot be deleted"
        roleResponse.status == 403
    }

    def "cannot delete a client role if the role is assigned to a user group"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)

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
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                DefaultCloud20Service.ERROR_DELETE_ASSIGNED_ROLE)

        when: "User group is deleted"
        utils.deleteUserGroup(group)
        response = cloud20.deleteRole(utils.getServiceAdminToken(), role.id)

        then: "role deletion is successful"
        response.status == HttpStatus.SC_NO_CONTENT

    }

    def "cannot delete a client role if the role is assigned to delegation agreement for Provisioned / Fed user as delegate"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENT_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)
        def userAdmin1 = utils.createCloudAccountWithRcn()
        def domain1 = utils.getDomain(userAdmin1.domainId)
        def userAdmin2 = utils.createCloudAccountWithRcn(utils.getIdentityAdminToken(), testUtils.getRandomInteger(), domain1.rackspaceCustomerNumber)
        def defaultUserDomain2 = utils.createUser(utils.getToken(userAdmin2.username))

        // Create role assignment
        Role role1 = v2Factory.createRole().with {
            it.serviceId = IDENTITY_SERVICE_ID
            it.name = UUID.randomUUID().toString()
            it.assignment = RoleAssignmentEnum.BOTH
            it.administratorRole = IdentityUserTypeEnum.USER_MANAGER.roleName
            it.roleType = RoleTypeEnum.STANDARD
            it
        }
        Role assignedRole1 = utils.createRole(role1)

        def assignmentsForProvisionedUser = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = assignedRole1.id
                            ta.forTenants.add("*")
                            ta
                    })
                    tas
            }
            it
        }

        Role role2 = v2Factory.createRole().with {
            it.serviceId = IDENTITY_SERVICE_ID
            it.name = UUID.randomUUID().toString()
            it.assignment = RoleAssignmentEnum.BOTH
            it.administratorRole = IdentityUserTypeEnum.USER_MANAGER.roleName
            it.roleType = RoleTypeEnum.STANDARD
            it
        }
        Role assignedRole2 = utils.createRole(role2)

        def assignmentsForFedUser = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = assignedRole2.id
                            ta.forTenants.add("*")
                            ta
                    })
                    tas
            }
            it
        }

        // assign role to principal
        utils.addRoleToUser(userAdmin2, assignedRole1.id)
        utils.addRoleToUser(userAdmin2, assignedRole2.id)

        def delegationAgreementDomain2DefaultUser = utils.createDelegationAgreementWithUserAsDelegate(utils.getToken(userAdmin2.username), userAdmin2.domainId, defaultUserDomain2.id)

        // Create Fed User
        AuthenticateResponse fedUser2AuthResponse = utils.createFederatedUserForAuthResponse(userAdmin2.domainId)
        def fedUser2Id = fedUser2AuthResponse.user.id

        // Create delegation agreement for Federated user
        def delegationAgreementDomain2FedUser = utils.createDelegationAgreementWithUserAsDelegate(utils.getToken(userAdmin2.username), userAdmin2.domainId, fedUser2Id)

        // create principal tokens
        def principalToken2 = utils.getToken(userAdmin2.username)

        // assign roles to Delegation Agreement
        utils.grantRoleAssignmentsOnDelegationAgreement(delegationAgreementDomain2FedUser, assignmentsForFedUser, principalToken2)
        utils.grantRoleAssignmentsOnDelegationAgreement(delegationAgreementDomain2DefaultUser, assignmentsForProvisionedUser, principalToken2)

        def tenantDomain1 = utils.createTenant()
        utils.addTenantToDomain(domain1.id, tenantDomain1.id)

        when: "role assigned to DA is attempted for deletion (provisioned user)"
        def roleResponse  = cloud20.deleteRole(utils.getServiceAdminToken(),assignedRole1.id)

        then: "assigned role is not deleted"
        IdmAssert.assertOpenStackV2FaultResponse(roleResponse, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                DefaultCloud20Service.ERROR_DELETE_ASSIGNED_ROLE)

        when: "role assigned to DA is revoked and attempted for deletion"
        def revokeResponse = cloud20.revokeRoleAssignmentFromDelegationAgreement(principalToken2, delegationAgreementDomain2DefaultUser, assignedRole1.id)
        utils.deleteRoleOnUser(userAdmin2, assignedRole1.id)
        roleResponse  = cloud20.deleteRole(utils.getServiceAdminToken(),assignedRole1.id)

        then: "unassigned role is deleted"
        revokeResponse.status == HttpStatus.SC_NO_CONTENT
        roleResponse.status == HttpStatus.SC_NO_CONTENT

        when: "role assigned to DA for is attempted for deletion (Fed user)"
        roleResponse  = cloud20.deleteRole(utils.getServiceAdminToken(),assignedRole2.id)

        then: "assigned role is not deleted"
        IdmAssert.assertOpenStackV2FaultResponse(roleResponse, ForbiddenFault, HttpStatus.SC_FORBIDDEN,
                DefaultCloud20Service.ERROR_DELETE_ASSIGNED_ROLE)

        when: "role assigned to DA is revoked and attempted for deletion"
        revokeResponse =  cloud20.revokeRoleAssignmentFromDelegationAgreement(principalToken2, delegationAgreementDomain2FedUser, assignedRole2.id)
        utils.deleteRoleOnUser(userAdmin2, assignedRole2.id)
        roleResponse  = cloud20.deleteRole(utils.getServiceAdminToken(),assignedRole2.id)

        then: "unassigned role is deleted"
        revokeResponse.status == HttpStatus.SC_NO_CONTENT
        roleResponse.status == HttpStatus.SC_NO_CONTENT

        cleanup:
        utils.deleteUsers(userAdmin1, defaultUserDomain2, userAdmin2)
    }

    private List<Role> getAllRoles(String identityAdminToken, Service service) {
        utils.listRoles(identityAdminToken, service.id, "0", "100").role
    }
}
