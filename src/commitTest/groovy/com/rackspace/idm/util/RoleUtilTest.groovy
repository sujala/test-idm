package com.rackspace.idm.util

import com.rackspace.idm.domain.dao.impl.LdapRepository
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.modules.usergroups.Constants
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class RoleUtilTest extends Specification {

    RoleUtil roleUtil = new RoleUtil()

    // Must use @Shared in order to use this in "where" clause
    @Shared standardClientId = "bde"

    @Unroll
    def "mergeTenantRoles: If merge role with null, resultant role is a clone: assignment: #assignment" () {
        when: "Merge with null"
        def tenantRole = roleUtil.mergeTenantRoles(assignment, null)

        then: "Result is a new tenant role with same values"
        !tenantRole.is(assignment) // Should be unique instance of Tenant Role
        tenantRole == assignment // All values are the same though

        when: "Merge with null in opposite order"
        tenantRole = roleUtil.mergeTenantRoles(null, assignment)

        then: "Result is still a new tenant role with same values"
        !tenantRole.is(assignment) // Should be unique instance of Tenant Role
        tenantRole == assignment // All values are the same though

        where:
        assignment << [createUserRoleAssignment("user1", "role1")
                       , createUserRoleAssignment("user1", "role1", ["tenantA"])
                       , createUserGroupRoleAssignment("group1", "role1")
                       , createUserGroupRoleAssignment("group1", "role1", ["tenantA"])
                       , createUserGroupRoleAssignment("group1", "role1", ["tenantA", "tenantB"])
        ]
    }

    @Unroll
    def "mergeTenantRoles: If merge role with itself, resultant role is a clone: assignment: #assignment" () {
        when: "Merge with null"
        def tenantRole = roleUtil.mergeTenantRoles(assignment, null)

        then: "Result is a new tenant role with same values"
        !tenantRole.is(assignment) // Should be unique instance of Tenant Role
        tenantRole == assignment // All values are the same though

        when: "Merge with null in opposite order"
        tenantRole = roleUtil.mergeTenantRoles(assignment, assignment)

        then: "Result is still a new tenant role with same values"
        !tenantRole.is(assignment) // Should be unique instance of Tenant Role
        tenantRole == assignment // All values are the same though

        where:
        assignment << [createUserRoleAssignment("user1", "role1")
                       , createUserRoleAssignment("user1", "role1", ["tenantA"])
                       , createUserGroupRoleAssignment("group1", "role1")
                       , createUserGroupRoleAssignment("group1", "role1", ["tenantA"])
                       , createUserGroupRoleAssignment("group1", "role1", ["tenantA", "tenantB"])
        ]
    }

    @Unroll
    def "mergeTenantRoles: If merge role with different user/uniqueId, resultant role blanks these out: assignment: #assignment" () {
        when: "Merge with null"
        def tenantRole = roleUtil.mergeTenantRoles(assignment, null)

        then: "Result is a new tenant role with same values"
        !tenantRole.is(assignment) // Should be unique instance of Tenant Role
        tenantRole == assignment // All values are the same though

        when: "Merge with null in opposite order"
        tenantRole = roleUtil.mergeTenantRoles(null, assignment)

        then: "Result is still a new tenant role with same values"
        !tenantRole.is(assignment) // Should be unique instance of Tenant Role
        tenantRole == assignment // All values are the same though

        where:
        assignment << [createUserRoleAssignment("user1", "role1")
                       , createUserRoleAssignment("user1", "role1", ["tenantA"])
                       , createUserGroupRoleAssignment("group1", "role1")
                       , createUserGroupRoleAssignment("group1", "role1", ["tenantA"])
                       , createUserGroupRoleAssignment("group1", "role1", ["tenantA", "tenantB"])
        ]
    }

    /**
     * This also validates that the uniqueId and userId are wiped when the 2 roles have different values for these roles
     */
    @Unroll
    def "mergeTenantRoles: If merge two tenant assigned roles, the result is union of tenants: assignment1: #assignment1; assignment2: #assignment2" () {
        given:
        TenantRole expectedTenantRole = new TenantRole().with {
            it.roleRsId = "role1"
            it.clientId = standardClientId
            it.uniqueId = null
            it.userId = null
            it.tenantIds = ["tenantA", "tenantB"] as Set // All combinations result in both tenants
            it
        }

        when:
        def tenantRoleResult = roleUtil.mergeTenantRoles(assignment1, assignment2)

        then:
        verifyFinalRole(expectedTenantRole, tenantRoleResult)

        when: "Merge in opposite order"
        tenantRoleResult = roleUtil.mergeTenantRoles(assignment2, assignment1)

        then:
        verifyFinalRole(expectedTenantRole, tenantRoleResult)

        where:
        [assignment1, assignment2] << [
                        [createUserRoleAssignment("user1", "role1", ["tenantA"])
                        ,createUserRoleAssignment("user1", "role1", ["tenantA","tenantB"])]
                        ,
                        [createUserGroupRoleAssignment("group1", "role1", ["tenantB"])
                        , createUserGroupRoleAssignment("group1", "role1", ["tenantA", "tenantB"])]
        ].combinations()
    }

    @Unroll
    def "mergeTenantRoleSets: When only single role, resultant role is a clone: assignment: #assignment" () {
        when: "Merge set"
        def tenantRoleList = roleUtil.mergeTenantRoleSets([assignment])

        then: "Result is a new tenant role with same values"
        def tenantRole = tenantRoleList.find {it.roleRsId == "role1"}
        !tenantRole.is(assignment) // Should be unique instance of Tenant Role
        tenantRole == assignment // All values are the same though

        where:
        assignment << [createUserRoleAssignment("user1", "role1")
                       , createUserRoleAssignment("user1", "role1", ["tenantA"])
                       , createUserGroupRoleAssignment("group1", "role1")
                       , createUserGroupRoleAssignment("group1", "role1", ["tenantA"])
                       , createUserGroupRoleAssignment("group1", "role1", ["tenantA", "tenantB"])
        ]
    }

    @Unroll
    def "mergeTenantRoleSets: When set contains same assignment, resultant role is a clone: assignment: #assignment" () {
        when: "Merge set"
        def tenantRoleList = roleUtil.mergeTenantRoleSets([assignment, assignment], [assignment])

        then: "Result is a new tenant role with same values"
        def tenantRole = tenantRoleList.find {it.roleRsId == "role1"}
        !tenantRole.is(assignment) // Should be unique instance of Tenant Role
        tenantRole == assignment // All values are the same though

        where:
        assignment << [createUserRoleAssignment("user1", "role1")
                       , createUserRoleAssignment("user1", "role1", ["tenantA"])
                       , createUserGroupRoleAssignment("group1", "role1")
                       , createUserGroupRoleAssignment("group1", "role1", ["tenantA"])
                       , createUserGroupRoleAssignment("group1", "role1", ["tenantA", "tenantB"])
        ]
    }

    /**
     * This also validates that the uniqueId and userId are wiped when the 2 roles have different values for these roles
     */
    @Unroll
    def "mergeTenantRoleSets: If merge sets of two assignments where one is global assigned, always results in globally assigned role: assignment2: #assignment2" () {
        given:
        TenantRole expectedTenantRole = new TenantRole().with {
            it.roleRsId = "role1"
            it.clientId = standardClientId
            it.uniqueId = null
            it.userId = null
            it.tenantIds = [] as Set
            it
        }

        def assignment1 = createUserRoleAssignment("user1", "role1")

        when: "Merge as single set"
        def tenantRoleResultList = roleUtil.mergeTenantRoleSets([assignment1, assignment2])

        then: "User ends up with a global role"
        verifyFinalRole(expectedTenantRole, tenantRoleResultList.find {it.roleRsId == "role1"})

        when: "Merge as distinct sets"
        tenantRoleResultList = roleUtil.mergeTenantRoleSets([assignment1], [assignment2])

        then: "User ends up with a global role"
        verifyFinalRole(expectedTenantRole, tenantRoleResultList.find {it.roleRsId == "role1"})

        when: "Merge sets in opposite order"
        tenantRoleResultList = roleUtil.mergeTenantRoleSets([assignment2], [assignment1])

        then: "User still ends up with a global role"
        verifyFinalRole(expectedTenantRole, tenantRoleResultList.find {it.roleRsId == "role1"})

        where:
        assignment2 << [createUserGroupRoleAssignment("group1", "role1")
                                  , createUserGroupRoleAssignment("group1", "role1", ["tenantA"])
                                  , createUserGroupRoleAssignment("group1", "role1", ["tenantA", "tenantB"])
        ]
    }

    void verifyFinalRole(TenantRole expected, TenantRole actual) {
        assert actual != null
        assert actual.roleRsId == expected.roleRsId
        assert actual.userId == expected.userId
        assert actual.uniqueId == expected.uniqueId
        assert actual.tenantIds == expected.tenantIds
        assert actual.clientId == expected.clientId
    }

    TenantRole createUserRoleAssignment(String userId, String roleId, List<String> tenantIds = []) {
        return new TenantRole().with {
            it.roleRsId = roleId
            it.tenantIds = tenantIds as Set
            it.uniqueId = "rsId=$roleId,ou=ROLES,rsId=$userId," + LdapRepository.USERS_BASE_DN
            it.userId = userId
            it.clientId = standardClientId
            it
        }
    }

    TenantRole createUserGroupRoleAssignment(String userGroupId, String roleId, List<String> tenantIds = []) {
        return new TenantRole().with {
            it.roleRsId = roleId
            it.tenantIds = tenantIds as Set
            it.uniqueId = "rsId=$roleId,ou=ROLES,rsId=$userGroupId," + Constants.USER_GROUP_BASE_DN
            it.clientId = standardClientId
            it
        }
    }


}
