package com.rackspace.idm.audit

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import spock.lang.Specification

class DelegationAgreementAuditBuilderTest extends Specification {

    def "DelegationAgreementAuditBuilder creates correct text"() {
        when:
        DelegationAgreementAuditBuilder builder = new DelegationAgreementAuditBuilder();

        then:
        builder.build() == ""

        when:
        builder = new DelegationAgreementAuditBuilder();
        builder.delegationAgreementId("da_id")

        then:
        builder.build() == "DelegationAgreement(id=da_id)"

        when:
        builder = new DelegationAgreementAuditBuilder();
        builder.delegationAgreementId("da_id")
        builder.delegateId("d_id")
        builder.delegateType("d_type")

        then:
        builder.build() == "DelegationAgreement(id=da_id) DelegateReference(id=d_id,type=d_type)"

        when:
        builder = new DelegationAgreementAuditBuilder();
        builder.delegationAgreementId("da_id")
        RoleAssignments roleAssignments = getRoleAssignments()
        roleAssignments.tenantAssignments.tenantAssignment.add(getTenantAssignment("role_id", ["1", "2"]))
        builder.roleAssignments(roleAssignments)

        then:
        builder.build() == "DelegationAgreement(id=da_id) Role(id=role_id,tenants=[1,2])"

        when:
        builder = new DelegationAgreementAuditBuilder();
        builder.delegationAgreementId("da_id")
        roleAssignments = getRoleAssignments()
        roleAssignments.tenantAssignments.tenantAssignment.add(getTenantAssignment("role_id", ["1", "2"]))
        roleAssignments.tenantAssignments.tenantAssignment.add(getTenantAssignment("role_id2", ["3", "4"]))
        builder.roleAssignments(roleAssignments)

        then:
        builder.build() == "DelegationAgreement(id=da_id) Role(id=role_id,tenants=[1,2]) Role(id=role_id2,tenants=[3,4])"

        when:
        builder = new DelegationAgreementAuditBuilder();
        builder.delegationAgreementId("da_id")
        builder.roleId("role_id")

        then:
        builder.build() == "DelegationAgreement(id=da_id) Role(id=role_id)"
    }

    private RoleAssignments getRoleAssignments() {
        RoleAssignments roleAssignments = new RoleAssignments()
        TenantAssignments tenantAssignments = new TenantAssignments()
        roleAssignments.tenantAssignments = tenantAssignments
        roleAssignments
    }

    private TenantAssignment getTenantAssignment(role, tenants) {
        TenantAssignment tenantAssignment = new TenantAssignment()
        tenantAssignment.onRole = role
        tenantAssignment.forTenants = tenants
        tenantAssignment
    }
}
