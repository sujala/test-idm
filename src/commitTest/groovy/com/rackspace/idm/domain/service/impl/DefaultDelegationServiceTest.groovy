package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.idm.domain.entity.DelegationAgreement
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.DelegationService
import spock.lang.Shared
import testHelpers.RootServiceTest

class DefaultDelegationServiceTest extends RootServiceTest {

    @Shared
    DelegationService service

    def setup() {
        service = new DefaultDelegationService()
        mockTenantAssignmentService(service)
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
}
