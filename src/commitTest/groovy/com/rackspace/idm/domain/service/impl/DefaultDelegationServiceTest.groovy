package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.api.resource.cloud.v20.DelegationAgreementRoleSearchParams
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import com.rackspace.idm.domain.entity.DelegationAgreement
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.DelegationService
import com.rackspace.idm.exception.NotFoundException
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

        when:
        service.replaceRoleAssignmentsOnDelegationAgreement(da, assignments)

        then:
        1 * tenantAssignmentService.replaceTenantAssignmentsOnDelegationAgreement(da, assignments.tenantAssignments.tenantAssignment)
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

        when:
        service.revokeRoleAssignmentOnDelegationAgreement(da, tenantRole.roleRsId)

        then:
        1 * tenantRoleDao.getRoleAssignmentOnDelegationAgreement(da, tenantRole.roleRsId) >> tenantRole
        1 * tenantRoleDao.deleteTenantRole(tenantRole)
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
}
