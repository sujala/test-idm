package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants
import com.rackspace.idm.api.resource.cloud.v20.DelegationAgreementRoleSearchParams
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.DelegationService
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import com.unboundid.ldap.sdk.DN
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

class DefaultDelegationServiceTest extends RootServiceTest {

    @Shared
    DelegationService service

    def setup() {
        service = new DefaultDelegationService()
        mockDelegationAgreementDao(service)
        mockTenantAssignmentService(service)
        mockTenantRoleDao(service)
        mockDelegationAgreementDao(service)
        mockIdentityUserService(service)
        mockUserGroupService(service)
        mockAtomHopperClient(service)
    }

    def "replaceRoleAssignmentsOnDelegationAgreement: calls correct service"() {
        given:
        DelegationAgreement da = new DelegationAgreement().with {
            it.uniqueId = "rsId=1"
            it
        }
        RoleAssignments assignments = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(new TenantAssignment().with {
                it.onRole = "roleId"
                it.forTenants.addAll("tenantId")
                it
            })
            it.tenantAssignments = ta
            it
        }

        def user = entityFactory.createUser()

        when:
        service.replaceRoleAssignmentsOnDelegationAgreement(da, assignments)

        then:
        1 * tenantAssignmentService.replaceTenantAssignmentsOnDelegationAgreement(da, assignments.tenantAssignments.tenantAssignment)
        1 * delegationAgreementDao.getDelegationAgreementDelegates(da) >> [user]
        1 * atomHopperClient.asyncPost((EndUser) user, AtomHopperConstants.UPDATE)
    }

    def "replaceRoleAssignmentsOnDelegationAgreement: error check and invalid cases"() {
        given:
        DelegationAgreement da = new DelegationAgreement().with {
            it.uniqueId = "rsId=1"
            it
        }
        RoleAssignments assignments = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(new TenantAssignment().with {
                it.onRole = "roleId"
                it.forTenants.addAll("tenantId")
                it
            })
            it.tenantAssignments = ta
            it
        }

        when: "da is null"
        service.replaceRoleAssignmentsOnDelegationAgreement(null, assignments)

        then:
        thrown(IllegalArgumentException)

        when: "da's uniqueId is null"
        DelegationAgreement invalidDa = new DelegationAgreement()
        service.replaceRoleAssignmentsOnDelegationAgreement(invalidDa, assignments)

        then:
        thrown(IllegalArgumentException)

        when: "assignments are null"
        service.replaceRoleAssignmentsOnDelegationAgreement(da, null)

        then:
        thrown(IllegalArgumentException)

        when: "tenant assignments are null"
        RoleAssignments invalidAssignments = new RoleAssignments()
        List<TenantRole> tenantRoles = service.replaceRoleAssignmentsOnDelegationAgreement(da, invalidAssignments)

        then:
        tenantRoles.isEmpty()
    }

    def "getRoleAssignmentOnDelegationAgreement: calls correct dao"() {
        given:
        DelegationAgreement da = new DelegationAgreement()
        TenantRole tenantRole = entityFactory.createTenantRole()

        when:
        service.getRoleAssignmentOnDelegationAgreement(da, tenantRole.roleRsId)

        then:
        1 * tenantRoleDao.getRoleAssignmentOnDelegationAgreement(da, tenantRole.roleRsId)
    }

    def "getRoleAssignmentOnDelegationAgreement: error check"() {
        given:
        DelegationAgreement da = new DelegationAgreement()
        TenantRole tenantRole = entityFactory.createTenantRole()

        when: "agreement is null"
        service.getRoleAssignmentOnDelegationAgreement(null, tenantRole.roleRsId)

        then:
        thrown(IllegalArgumentException)

        when: "roleId is null"
        service.getRoleAssignmentOnDelegationAgreement(da, null)

        then:
        thrown(IllegalArgumentException)
    }

    def "revokeRoleAssignmentOnDelegationAgreement: calls correct daos"() {
        given:
        DelegationAgreement da = new DelegationAgreement()
        TenantRole tenantRole = entityFactory.createTenantRole()

        def user = entityFactory.createUser()

        when:
        service.revokeRoleAssignmentOnDelegationAgreement(da, tenantRole.roleRsId)

        then:
        1 * tenantRoleDao.getRoleAssignmentOnDelegationAgreement(da, tenantRole.roleRsId) >> tenantRole
        1 * tenantRoleDao.deleteTenantRole(tenantRole)
        1 * delegationAgreementDao.getDelegationAgreementDelegates(da) >> [user]
        1 * atomHopperClient.asyncPost((EndUser) user, AtomHopperConstants.UPDATE)
    }

    def "revokeRoleAssignmentOnDelegationAgreement: error check"() {
        given:
        DelegationAgreement da = new DelegationAgreement()
        TenantRole tenantRole = entityFactory.createTenantRole()

        when: "agreement is null"
        service.revokeRoleAssignmentOnDelegationAgreement(null, tenantRole.roleRsId)

        then:
        thrown(IllegalArgumentException)

        when: "roleId is null"
        service.revokeRoleAssignmentOnDelegationAgreement(da, null)

        then:
        thrown(IllegalArgumentException)

        when: "assignment does not exists"
        service.revokeRoleAssignmentOnDelegationAgreement(da, tenantRole.roleRsId)

        then:
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, "The specified role does not exist for agreement")

        1 * tenantRoleDao.getRoleAssignmentOnDelegationAgreement(da, tenantRole.roleRsId) >> null
    }

    def "getRoleAssignmentsOnDelegationAgreement: calls correct daos"() {
        given:
        DelegationAgreement da = new DelegationAgreement()
        def searchParams = new DelegationAgreementRoleSearchParams(new PaginationParams())

        when:
        service.getRoleAssignmentsOnDelegationAgreement(da, searchParams)

        then:
        1 * tenantRoleDao.getRoleAssignmentsOnDelegationAgreement(da, searchParams.getPaginationRequest());
    }

    def "getRoleAssignmentsOnDelegationAgreement: error check"() {
        given:
        DelegationAgreement da = new DelegationAgreement()
        def searchParams = new DelegationAgreementRoleSearchParams(new PaginationParams())

        when: "DA is null"
        service.getRoleAssignmentsOnDelegationAgreement(null, searchParams)

        then:
        thrown(IllegalArgumentException)

        when: "searchParam is null"
        service.getRoleAssignmentsOnDelegationAgreement(da, null)

        then:
        thrown(IllegalArgumentException)
    }

    def "removeConsumerFromExplicitDelegationAgreementAssignments deletes DAs for which the consumer is a principal and removes the consumer as a delegate from DA for which they are an explicit delegate"() {
        given:
        def user = new User().with {
            it.id = RandomStringUtils.randomAlphanumeric(20)
            it.uniqueId = "rsId=${it.id},ou=users,o=rackspace,dc=rackspace,dc=com"
            it
        }
        def fedUser = new FederatedUser().with {
            it.id = RandomStringUtils.randomAlphanumeric(20)
            it.username = RandomStringUtils.randomAlphanumeric(20)
            it.uniqueId = "uid=${it.username},ou=users,ou=test,o=externalProviders,dc=rackspace,dc=com"
            it
        }
        def userGroup = new UserGroup().with {
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.uniqueId = "rsId=${it.id},ou=userGroups,ou=groups,ou=cloud,o=rackspace,dc=rackspace,dc=com"
            it
        }
        def delegateDelegationAgreement = new DelegationAgreement().with {
            it.id = "delegateDA"
            it
        }
        def principalDelegationAgreement = new DelegationAgreement().with {
            it.id = "principalDA"
            it
        }

        when: "remove the user from explicit DA assignments"
        delegateDelegationAgreement.delegates = [new DN(user.uniqueId)]
        principalDelegationAgreement.principal = user
        service.removeConsumerFromExplicitDelegationAgreementAssignments(user)

        then:
        1 * delegationAgreementDao.findDelegationAgreements({ it.delegate == user }) >> [delegateDelegationAgreement]
        1 * delegationAgreementDao.deleteAgreement(principalDelegationAgreement)
        1 * delegationAgreementDao.findDelegationAgreements({ it.principal == user }) >> [principalDelegationAgreement]
        1 * identityUserService.getEndUserById(user.id) >> user
        1 * delegationAgreementDao.updateAgreement(delegateDelegationAgreement)
        1 * delegationAgreementDao.getDelegationAgreementDelegates(principalDelegationAgreement) >> [user]

        when: "remove the federated user from explicit DA assignments"
        delegateDelegationAgreement.delegates = [new DN(fedUser.uniqueId)]
        principalDelegationAgreement.principal = fedUser
        service.removeConsumerFromExplicitDelegationAgreementAssignments(fedUser)

        then:
        1 * delegationAgreementDao.findDelegationAgreements({ it.delegate == fedUser }) >> [delegateDelegationAgreement]
        1 * delegationAgreementDao.deleteAgreement(principalDelegationAgreement)
        1 * delegationAgreementDao.findDelegationAgreements({ it.principal == fedUser }) >> [principalDelegationAgreement]
        1 * identityUserService.getEndUserById(fedUser.id) >> fedUser
        1 * delegationAgreementDao.updateAgreement(delegateDelegationAgreement)
        1 * delegationAgreementDao.getDelegationAgreementDelegates(principalDelegationAgreement) >> [fedUser]

        when: "remove the user group from explicit DA assignments"
        delegateDelegationAgreement.delegates = [new DN(userGroup.uniqueId)]
        principalDelegationAgreement.principal = userGroup
        service.removeConsumerFromExplicitDelegationAgreementAssignments(userGroup)

        then:
        1 * delegationAgreementDao.findDelegationAgreements({ it.delegate == userGroup }) >> [delegateDelegationAgreement]
        1 * delegationAgreementDao.deleteAgreement(principalDelegationAgreement)
        1 * delegationAgreementDao.findDelegationAgreements({ it.principal == userGroup }) >> [principalDelegationAgreement]
        1 * userGroupService.getGroupById(userGroup.id) >> userGroup
        1 * delegationAgreementDao.updateAgreement(delegateDelegationAgreement)
        1 * delegationAgreementDao.getDelegationAgreementDelegates(principalDelegationAgreement) >> [userGroup]
        2 * userGroupService.getUsersInGroup(userGroup) >> [user]
    }

    def "updateDelegationAgreement: calls correct daos"() {
        given:
        DelegationAgreement da = new DelegationAgreement()

        when:
        service.updateDelegationAgreement(da)

        then:
        1 * delegationAgreementDao.updateAgreement(da);
    }

    def "updateDelegationAgreement: error check"() {
        given:
        DelegationAgreement da = new DelegationAgreement()

        when: "DA is null"
        service.updateDelegationAgreement(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "countNumberOfDelegationAgreementsByPrincipal: calls correct daos"() {
        DelegationPrincipal delegationPrincipal = Mock(DelegationPrincipal)
        delegationPrincipal.getDn() >> new DN("rsId=1")

        when:
        service.countNumberOfDelegationAgreementsByPrincipal(delegationPrincipal)

        then:
        1 * delegationAgreementDao.countNumberOfDelegationAgreementsByPrincipal(delegationPrincipal)
    }

    def "countNumberOfDelegationAgreementsByPrincipal: error check"() {
        given:
        DelegationPrincipal delegationPrincipal = Mock(DelegationPrincipal)

        when: "principal is null"
        service.countNumberOfDelegationAgreementsByPrincipal(null)

        then:
        thrown(IllegalArgumentException)

        when: "principal's DN is null"
        service.countNumberOfDelegationAgreementsByPrincipal(delegationPrincipal)

        then:
        thrown(IllegalArgumentException)
    }

    def "deleteAgreement deletes the most deeply nested child DAs first"() {
        given:
        def da = entityFactory.createDelegationAgreement().with { it.name = "Parent DA"; it}
        def daChild1 = entityFactory.createDelegationAgreement().with { it.name = "Child 1 DA"; it}
        def daChild1child = entityFactory.createDelegationAgreement().with { it.name = "Child of Child 1 DA"; it}
        def daChild2 = entityFactory.createDelegationAgreement().with { it.name = "Child 2 DA"; it}
        delegationAgreementDao.getChildDelegationAgreements(da.id) >> [daChild1, daChild2]
        delegationAgreementDao.getChildDelegationAgreements(daChild1.id) >> [daChild1child]
        delegationAgreementDao.getDelegationAgreementDelegates(_) >> []

        when:
        service.deleteDelegationAgreement(da)

        then:
        1 * delegationAgreementDao.deleteAgreement(daChild1child)

        then:
        1 * delegationAgreementDao.deleteAgreement(daChild1)

        then:
        1 * delegationAgreementDao.deleteAgreement(daChild2)

        then:
        1 * delegationAgreementDao.deleteAgreement(da)
    }

    def "deleteAgreement deletes only the DA and the DA's children"() {
        given:
        def da = entityFactory.createDelegationAgreement().with { it.name = "Parent DA"; it}
        def daChild1 = entityFactory.createDelegationAgreement().with { it.name = "Child 1 DA"; it}
        def daChild1child = entityFactory.createDelegationAgreement().with { it.name = "Child of Child 1 DA"; it}
        def daChild2 = entityFactory.createDelegationAgreement().with { it.name = "Child 2 DA"; it}
        delegationAgreementDao.getChildDelegationAgreements(da.id) >> [daChild1, daChild2]
        delegationAgreementDao.getChildDelegationAgreements(daChild1.id) >> [daChild1child]
        delegationAgreementDao.getDelegationAgreementDelegates(_) >> []

        when:
        service.deleteDelegationAgreement(daChild1)

        then:
        1 * delegationAgreementDao.deleteAgreement(daChild1child)

        then:
        1 * delegationAgreementDao.deleteAgreement(daChild1)

        then:
        0 * delegationAgreementDao.deleteAgreement(daChild2)
        0 * delegationAgreementDao.deleteAgreement(da)
    }

    def "deleteAgreement does not delete the parent when the deletion of a child fails"() {
        given:
        def da = entityFactory.createDelegationAgreement().with { it.name = "Parent DA"; it}
        def daChild1 = entityFactory.createDelegationAgreement().with { it.name = "Child 1 DA"; it}
        def daChild1child = entityFactory.createDelegationAgreement().with { it.name = "Child of Child 1 DA"; it}
        def daChild2 = entityFactory.createDelegationAgreement().with { it.name = "Child 2 DA"; it}
        delegationAgreementDao.getChildDelegationAgreements(da.id) >> [daChild1, daChild2]
        delegationAgreementDao.getChildDelegationAgreements(daChild1.id) >> [daChild1child]
        delegationAgreementDao.getDelegationAgreementDelegates(_) >> []

        when:
        service.deleteDelegationAgreement(da)

        then:
        1 * delegationAgreementDao.deleteAgreement(daChild1child)

        then:
        1 * delegationAgreementDao.deleteAgreement(daChild1) >> {
            throw new RuntimeException()
        }
        thrown RuntimeException

        then:
        0 * delegationAgreementDao.deleteAgreement(daChild2)
        0 * delegationAgreementDao.deleteAgreement(da)
    }

}
