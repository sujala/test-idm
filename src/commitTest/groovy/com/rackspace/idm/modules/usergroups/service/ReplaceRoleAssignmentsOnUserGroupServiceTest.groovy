package com.rackspace.idm.modules.usergroups.service

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.Tenant
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.modules.usergroups.Constants
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import org.apache.commons.collections4.CollectionUtils
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

class ReplaceRoleAssignmentsOnUserGroupServiceTest extends RootServiceTest{

    DefaultUserGroupService service
    UserGroupDao dao

    def setup() {
        service = new DefaultUserGroupService()

        mockValidator20(service)
        mockIdentityConfig(service)
        mockApplicationService(service)
        mockTenantService(service)
        mockTenantRoleDao(service)

        dao = Mock()
        service.userGroupDao = dao
    }

    @Unroll
    def "replaceRoleAssignmentsOnGroup: Throws IllegalArgumentException if supplied user group is invalid"() {
        def roleAssignments = new RoleAssignments()

        when: "group arg is null"
        service.replaceRoleAssignmentsOnGroup(null, roleAssignments)

        then:
        thrown(IllegalArgumentException)

        when: "group arg has no unique id"
        service.replaceRoleAssignmentsOnGroup(new UserGroup(), roleAssignments)

        then:
        thrown(IllegalArgumentException)
    }

    @Unroll
    def "replaceRoleAssignmentsOnGroup: Throws IllegalArgumentException if roleAssignments arg is null"() {
        when:
        service.replaceRoleAssignmentsOnGroup(new UserGroup().with {it.uniqueId = "uniqueId";it}, null)

        then:
        thrown(IllegalArgumentException)
    }

    def "replaceRoleAssignmentsOnGroup: Verify static check validations with no backend checks"() {
        def groupId = "groupId"
        def domainId = "domainId"

        def group = new UserGroup().with {
            it.id = groupId
            it.domainId = domainId
            it.uniqueId = "alocation"
            it
        }

        def tenantOtherDomain = "tenantOther"
        def roleIdWrongWeight = "roleIdWrongWeight"
        def roleIdMissing = "roleIdMissing"

        // Generate 5 valid roles
        List validRoles = new ArrayList<>()
        5.times { index ->
            ClientRole role = new ClientRole().with {
                it.id = "validId_" + index
                it.name = "validName_" + index
                it.rsWeight = Constants.USER_GROUP_ALLOWED_ROLE_WEIGHT
                it.clientId = "clientId"
                it
            }
            validRoles.add(role)
        }

        def taNoTenants = new TenantAssignment().with {ta ->  ta.onRole = validRoles[0].id; ta}
        def taAllandExplicitTenants = new TenantAssignment().with {ta ->  ta.onRole = validRoles[1].id; ta.forTenants = ["*", "a"]; ta}
        def taWrongDomainTenant = new TenantAssignment().with {ta ->  ta.onRole = validRoles[2].id; ta.forTenants = [tenantOtherDomain]; ta}
        def taNonExistantTenant = new TenantAssignment().with {ta ->  ta.onRole = validRoles[3].id; ta.forTenants = ["noexisttenant"]; ta}
        def taInvalidRoleWeight = new TenantAssignment().with {ta ->  ta.onRole = roleIdWrongWeight; ta.forTenants = ["*"]; ta}
        def taMissingRole = new TenantAssignment().with {ta ->  ta.onRole = roleIdMissing; ta.forTenants = ["*"]; ta}
        def taEmptyStringTenants = new TenantAssignment().with {ta ->  ta.onRole = validRoles[4].id; ta.forTenants = [""]; ta}

        when: "Duplicate role exist along with other validation errors"
        service.replaceRoleAssignmentsOnGroup(group, genRoleAssignments(taNoTenants, taAllandExplicitTenants, taMissingRole, taWrongDomainTenant, taInvalidRoleWeight, taNoTenants))

        then: "Throws 400 due to dup roles"
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, BadRequestException, "UGA-000", Constants.ERROR_CODE_USER_GROUPS_DUP_ROLE_ASSIGNMENT_MSG)
        0 * tenantRoleDao._(*_)
        0 * applicationRoleDao._(*_)

        when: "Submit request that includes missing forTenants and invalid for tenants along with invalid backend role errors"
        service.replaceRoleAssignmentsOnGroup(group, genRoleAssignments(taMissingRole, taWrongDomainTenant, taInvalidRoleWeight, taNoTenants, taAllandExplicitTenants, taNonExistantTenant))

        then: "Throws 400 on first static tenant error encountered"
        Exception ex2 = thrown()
        IdmExceptionAssert.assertException(ex2, BadRequestException, "GEN-001", Constants.ERROR_CODE_ROLE_ASSIGNMENT_MISSING_FOR_TENANTS_MSG)
        0 * tenantRoleDao._(*_)
        0 * applicationRoleDao._(*_)

        when: "Submit request that includes missing forTenants and invalid forTenants definition along with invalid backend role errors"
        service.replaceRoleAssignmentsOnGroup(group, genRoleAssignments(taMissingRole, taWrongDomainTenant, taInvalidRoleWeight, taAllandExplicitTenants, taNoTenants))

        then: "Throws 400 on first tenant error encountered"
        Exception ex3 = thrown()
        IdmExceptionAssert.assertException(ex3, BadRequestException, "GEN-005", Constants.ERROR_CODE_ROLE_ASSIGNMENT_INVALID_FOR_TENANTS_MSG)
        0 * tenantRoleDao._(*_)
        0 * applicationRoleDao._(*_)

        when: "Submit request that includes empty string forTenants"
        service.replaceRoleAssignmentsOnGroup(group, genRoleAssignments(taEmptyStringTenants))

        then: "Throws 400"
        Exception ex4 = thrown()
        IdmExceptionAssert.assertException(ex4, BadRequestException, "GEN-005", Constants.ERROR_CODE_ROLE_ASSIGNMENT_INVALID_FOR_TENANTS_MSG)
        0 * tenantRoleDao._(*_)
        0 * applicationRoleDao._(*_)
    }

    def "replaceRoleAssignmentsOnGroup: Verify backend check validations"() {
        def groupId = "groupId"
        def domainId = "domainId"
        def group = new UserGroup().with {
            it.id = groupId
            it.domainId = domainId
            it.uniqueId = "alocation"
            it
        }

        def tenantSameDomain = "tenantSame"
        def tenantOtherDomain = "tenantOther"
        tenantService.getTenant(tenantSameDomain) >> new Tenant().with {it.tenantId = tenantSameDomain; it.domainId = domainId; it}
        tenantService.getTenant(tenantOtherDomain) >> new Tenant().with {it.tenantId = tenantOtherDomain; it.domainId = "otherDomain"; it}

        // Generate 5 valid roles
        List validRoles = new ArrayList<>()
        4.times { index ->
            ClientRole role = new ClientRole().with {
                it.id = "validId_" + index
                it.name = "validName_" + index
                it.rsWeight = Constants.USER_GROUP_ALLOWED_ROLE_WEIGHT
                it.clientId = "clientId"
                it
            }
            validRoles.add(role)
            applicationService.getClientRoleById(role.id) >> role
        }

        def roleIdWrongWeight = "roleIdWrongWeight"
        def roleIdMissing = "roleIdMissing"
        def roleIdGlobalOnly = "roleIdGlobal"
        def roleIdTenantOnly = "roleIdTenant"

        applicationService.getClientRoleById(roleIdWrongWeight) >> new ClientRole().with {
            it.id = roleIdWrongWeight
            it.name = roleIdWrongWeight
            it.rsWeight = 900
            it.clientId = "clientId"
            it
        }
        applicationService.getClientRoleById(roleIdGlobalOnly) >> new ClientRole().with {
            it.id = roleIdGlobalOnly
            it.name = roleIdGlobalOnly
            it.rsWeight = Constants.USER_GROUP_ALLOWED_ROLE_WEIGHT
            it.clientId = "clientId"
            it.setAssignmentType(RoleAssignmentEnum.GLOBAL.value())
            it
        }
        applicationService.getClientRoleById(roleIdTenantOnly) >> new ClientRole().with {
            it.id = roleIdTenantOnly
            it.name = roleIdTenantOnly
            it.rsWeight = Constants.USER_GROUP_ALLOWED_ROLE_WEIGHT
            it.clientId = "clientId"
            it.setAssignmentType(RoleAssignmentEnum.TENANT.value())
            it
        }

        applicationService.getClientRoleById(roleIdMissing) >> null

        def taWrongDomainTenant = new TenantAssignment().with {ta ->  ta.onRole = validRoles[0].id; ta.forTenants = [tenantOtherDomain]; ta}
        def taNonExistantTenant = new TenantAssignment().with {ta ->  ta.onRole = validRoles[1].id; ta.forTenants = ["noexisttenant"]; ta}
        def taInvalidRoleWeight = new TenantAssignment().with {ta ->  ta.onRole = roleIdWrongWeight; ta.forTenants = ["*"]; ta}
        def taInvalidRoleAssigmentGlobal = new TenantAssignment().with {ta ->  ta.onRole = roleIdGlobalOnly; ta.forTenants = [tenantSameDomain]; ta}
        def taInvalidRoleAssigmentTenant = new TenantAssignment().with {ta ->  ta.onRole = roleIdTenantOnly; ta.forTenants = ["*"]; ta}
        def taMissingRole = new TenantAssignment().with {ta ->  ta.onRole = roleIdMissing; ta.forTenants = ["*"]; ta}

        when: "Submit request that includes missing missing roles, invalid role, and invalid tenant (backend state)"
        service.replaceRoleAssignmentsOnGroup(group, genRoleAssignments(taMissingRole, taNonExistantTenant, taWrongDomainTenant, taInvalidRoleWeight))

        then: "Throws exception on first backend failure (NotFoundException)"
        Exception ex4 = thrown()
        IdmExceptionAssert.assertException(ex4, NotFoundException, "GEN-005", String.format(Constants.ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_ROLE_MSG_PATTERN, taMissingRole.onRole))

        when: "Submit request that includes missing missing roles, invalid role, and invalid tenant (backend state)"
        service.replaceRoleAssignmentsOnGroup(group, genRoleAssignments(taInvalidRoleWeight, taNonExistantTenant, taWrongDomainTenant, taMissingRole))

        then: "Throws exception on first backend failure (ForbiddenException)"
        Exception ex5 = thrown()
        IdmExceptionAssert.assertException(ex5, ForbiddenException, "GEN-005", String.format(Constants.ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, taInvalidRoleWeight.onRole))

        when: "Submit request that includes missing tenant in different domain"
        service.replaceRoleAssignmentsOnGroup(group, genRoleAssignments(taNonExistantTenant, taInvalidRoleWeight, taWrongDomainTenant, taMissingRole))

        then: "Throws exception on first backend failure (NotFoundException)"
        Exception ex6 = thrown()
        IdmExceptionAssert.assertException(ex6, NotFoundException, "GEN-005", String.format(Constants.ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_TENANT_MSG_PATTERN, taNonExistantTenant.onRole))

        when: "Submit request that includes missing tenant in different domain"
        service.replaceRoleAssignmentsOnGroup(group, genRoleAssignments(taWrongDomainTenant, taNonExistantTenant, taInvalidRoleWeight, taMissingRole, taInvalidRoleAssigmentGlobal, taInvalidRoleAssigmentTenant))

        then: "Throws exception on first backend failure (ForbiddenException)"
        Exception ex7 = thrown()
        IdmExceptionAssert.assertException(ex7, ForbiddenException, "GEN-005", String.format(Constants.ERROR_CODE_ROLE_ASSIGNMENT_WRONG_DOMAIN_TENANT_MSG_PATTERN, taWrongDomainTenant.onRole))

        when: "Submit request that includes missing tenant in different domain"
        service.replaceRoleAssignmentsOnGroup(group, genRoleAssignments(taInvalidRoleAssigmentGlobal, taWrongDomainTenant, taNonExistantTenant, taInvalidRoleWeight, taMissingRole, taInvalidRoleAssigmentTenant))

        then: "Throws exception on first backend failure (ForbiddenException)"
        Exception ex8 = thrown()
        IdmExceptionAssert.assertException(ex8, ForbiddenException, "GEN-005", String.format(Constants.ERROR_CODE_ROLE_ASSIGNMENT_GLOBAL_ROLE_ASSIGNMENT_ONLY_MSG_PATTERN, taInvalidRoleAssigmentGlobal.onRole))

        when: "Submit request that includes missing tenant in different domain"
        service.replaceRoleAssignmentsOnGroup(group, genRoleAssignments(taInvalidRoleAssigmentTenant, taInvalidRoleAssigmentGlobal, taWrongDomainTenant, taNonExistantTenant, taInvalidRoleWeight, taMissingRole))

        then: "Throws exception on first backend failure (ForbiddenException)"
        Exception ex9 = thrown()
        IdmExceptionAssert.assertException(ex9, ForbiddenException, "GEN-005", String.format(Constants.ERROR_CODE_ROLE_ASSIGNMENT_TENANT_ASSIGNMENT_ONLY_MSG_PATTERN, taInvalidRoleAssigmentTenant.onRole))
    }

    def RoleAssignments genRoleAssignments(TenantAssignment... taAr) {
        RoleAssignments roleAssignments = new RoleAssignments().with {
            ras ->
                ras.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.addAll(taAr)
                    tas
            }
                ras
        }
        return  roleAssignments
    }

    def "replaceRoleAssignmentsOnGroup: Golden 'new' scenario calls dao and returns response"() {
        def groupId = "groupId"
        def domainId = "domainId"
        def roleAId = "roleA"
        def roleBId = "roleB"
        def tenantBId = "tenantB"

        def group = new UserGroup().with {
            it.id = groupId
            it.domainId = domainId
            it.uniqueId = "alocation"
            it
        }

        def roleACr = new ClientRole().with {
            it.id = roleAId
            it.name = "roleAName"
            it.rsWeight = Constants.USER_GROUP_ALLOWED_ROLE_WEIGHT
            it.clientId = "clientId"
            it
        }
        def roleBCr = new ClientRole().with {
            it.id = roleBId
            it.name = "roleBName"
            it.rsWeight = Constants.USER_GROUP_ALLOWED_ROLE_WEIGHT
            it.clientId = "clientId"
            it
        }

        RoleAssignments roleAssignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = roleAId
                            ta.onRoleName = "roleAName"
                            ta.forTenants.add("*")
                            ta
                    })
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = roleBId
                            ta.onRoleName = "roleBName"
                            ta.forTenants.add(tenantBId)
                            ta
                    })
                    tas
            }
            it
        }

        when:
        List<TenantRole> result = service.replaceRoleAssignmentsOnGroup(group, roleAssignments)

        then:
        // Verify roles are retrieved for verification
        1 * applicationService.getClientRoleById(roleAId) >> roleACr
        1 * applicationService.getClientRoleById(roleBId) >> roleBCr

        // Verify tenants are validated
        1 * tenantService.getTenant(tenantBId) >> new Tenant().with {it.tenantId = tenantBId; it.domainId = domainId; it}

        // Verify queries to see if add or update. Have role 1 be an "add", role 2 be an "update"
        1 * tenantRoleDao.getRoleAssignmentOnGroup(group, roleAId)
        1 * tenantRoleDao.getRoleAssignmentOnGroup(group, roleBId) >> new TenantRole().with {
            it.roleRsId = roleBId
            it.tenantIds = ["otherTenant", "anothertenant", "yetanothertenant"] as Set
            it.clientId = "existingclientId"
            it
        }

        // Verify roles are sent to dao correctly
        1 * tenantRoleDao.addRoleAssignmentOnGroup(group, {it.roleRsId == roleAId}) >> {args ->
            TenantRole role = args[1]
            assert role.roleRsId == roleAId
            assert CollectionUtils.isEmpty(role.tenantIds)
            assert role.clientId == roleACr.clientId
        }
        1 * tenantRoleDao.updateRoleAssignmentOnGroup(group, {it.roleRsId == roleBId}) >> {args ->
            TenantRole role = args[1]
            assert role.roleRsId == roleBId
            assert CollectionUtils.isEqualCollection(role.tenantIds, [tenantBId])
            assert role.clientId == "existingclientId"
        }

        and:
        result.size() == 2
        result.find {it.roleRsId == roleAId} != null
        result.find {it.roleRsId == roleBId} != null
    }
}
