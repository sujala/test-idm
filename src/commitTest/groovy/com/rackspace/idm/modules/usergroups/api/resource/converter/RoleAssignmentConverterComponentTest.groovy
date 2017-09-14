package com.rackspace.idm.modules.usergroups.api.resource.converter

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Types
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.modules.usergroups.Constants
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class RoleAssignmentConverterComponentTest extends Specification {
    @Shared RoleAssignmentConverter converter

    void setupSpec() {
        converter = new RoleAssignmentConverter()
    }

    @Unroll
    def "toRoleAssignmentWeb: w/ various tenants: tenants: #tenants"() {

        TenantRole tenantRole = new TenantRole().with {
            it.tenantIds = tenants
            it.name = "roleName"
            it.description = "description"
            it.clientId = "clientId"
            it.roleRsId = "roleId"
            it.types = new Types().with {
                it.type = ["type1"]
                it
            }
            it
        }

        when:
        RoleAssignment roleAssignment = converter.toRoleAssignmentWeb(tenantRole)

        then:
        roleAssignment.onRole == tenantRole.roleRsId
        roleAssignment.onRoleName == tenantRole.name
        CollectionUtils.isEqualCollection(roleAssignment.forTenants, CollectionUtils.isEmpty(tenants) ? [Constants.ALL_TENANT_IN_DOMAIN_WILDCARD] as Set : tenants)

        where:
        tenants << [[] as Set, ["a"] as Set, ["a", "b"] as Set]
    }

    def "toTenantAssignemnts: converts tenant role list from entity to web"() {
        List<TenantRole> tenantRoles = new ArrayList<>()
        4.times { index ->
            TenantRole tenantRole = new TenantRole().with {
                it.tenantIds = [RandomStringUtils.randomAlphanumeric(5)] as Set
                it.name = "roleName"
                it.roleRsId = "roleId"
                it
            }
            tenantRoles.add(tenantRole)
        }

        when:
        TenantAssignments tenantAssignments = converter.toTenantAssignmentsWeb(tenantRoles)

        then:
        tenantAssignments.tenantAssignment.size() == tenantRoles.size()
        tenantAssignments.tenantAssignment.eachWithIndex {tenantAssignment, index ->
            def tenantRole = tenantRoles.get(index)
            tenantAssignment.onRole == tenantRole.roleRsId
            tenantAssignment.onRoleName == tenantRole.name
            CollectionUtils.isEqualCollection(tenantAssignment.forTenants, tenantRole.tenantIds)
        }
    }

    def "toRoleAssignments: converts tenant role list from entity to web"() {
        List<TenantRole> tenantRoles = new ArrayList<>()
        4.times { index ->
            TenantRole tenantRole = new TenantRole().with {
                it.tenantIds = [RandomStringUtils.randomAlphanumeric(5)] as Set
                it.name = "roleName"
                it.roleRsId = "roleId"
                it
            }
            tenantRoles.add(tenantRole)
        }

        when:
        RoleAssignments roleAssignments = converter.toRoleAssignmentsWeb(tenantRoles)

        then:
        roleAssignments.tenantAssignments != null
        TenantAssignments tenantAssignments = roleAssignments.tenantAssignments
        tenantAssignments.tenantAssignment.size() == tenantRoles.size()
        tenantAssignments.tenantAssignment.eachWithIndex {tenantAssignment, index ->
            def tenantRole = tenantRoles.get(index)
            tenantAssignment.onRole == tenantRole.roleRsId
            tenantAssignment.onRoleName == tenantRole.name
            CollectionUtils.isEqualCollection(tenantAssignment.forTenants, tenantRole.tenantIds)
        }
    }
}
