package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.Tenant
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.RoleLevelEnum
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import org.apache.commons.collections4.CollectionUtils
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import static com.rackspace.idm.ErrorCodes.*

class DefaultTenantAssignmentServiceTest extends RootServiceTest{

    DefaultTenantAssignmentService service

    def setup() {
        service = new DefaultTenantAssignmentService()

        mockApplicationService(service)
        mockTenantService(service)
        mockTenantRoleDao(service)
    }

    def "replaceTenantAssignmentsOnUser: Verify static check validations with no backend checks"() {
        given:
        def domainId = "domainId"
        def user = new User().with {
            it.id = "userId"
            it.domainId = domainId
            it.uniqueId = "aLocation"
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
                it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
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

        RoleAssignments roleAssignments = genRoleAssignments(taNoTenants, taAllandExplicitTenants, taMissingRole, taWrongDomainTenant, taInvalidRoleWeight, taNoTenants)

        when: "Duplicate role exist along with other validation errors"
        service.replaceTenantAssignmentsOnUser(user, roleAssignments.tenantAssignments.tenantAssignment, IdentityUserTypeEnum.USER_ADMIN.levelAsInt)

        then: "Throws 400 due to dup roles"
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, BadRequestException, "ROLE-000", ERROR_CODE_DUP_ROLE_ASSIGNMENT_MSG)
        0 * tenantRoleDao._(*_)
        0 * applicationRoleDao._(*_)

        when: "Submit request that includes missing forTenants and invalid for tenants along with invalid backend role errors"
        roleAssignments = genRoleAssignments(taMissingRole, taWrongDomainTenant, taInvalidRoleWeight, taNoTenants, taAllandExplicitTenants, taNonExistantTenant)
        service.replaceTenantAssignmentsOnUser(user, roleAssignments.tenantAssignments.tenantAssignment, IdentityUserTypeEnum.USER_ADMIN.levelAsInt)

        then: "Throws 400 on first static tenant error encountered"
        Exception ex2 = thrown()
        IdmExceptionAssert.assertException(ex2, BadRequestException, "GEN-001", ERROR_CODE_ROLE_ASSIGNMENT_MISSING_FOR_TENANTS_MSG)
        0 * tenantRoleDao._(*_)
        0 * applicationRoleDao._(*_)

        when: "Submit request that includes missing forTenants and invalid forTenants definition along with invalid backend role errors"
        roleAssignments = genRoleAssignments(taMissingRole, taWrongDomainTenant, taInvalidRoleWeight, taAllandExplicitTenants, taNoTenants)
        service.replaceTenantAssignmentsOnUser(user, roleAssignments.tenantAssignments.tenantAssignment, IdentityUserTypeEnum.USER_ADMIN.levelAsInt)

        then: "Throws 400 on first tenant error encountered"
        Exception ex3 = thrown()
        IdmExceptionAssert.assertException(ex3, BadRequestException, "GEN-005", ERROR_CODE_ROLE_ASSIGNMENT_INVALID_FOR_TENANTS_MSG)
        0 * tenantRoleDao._(*_)
        0 * applicationRoleDao._(*_)

        when: "Submit request that includes empty string forTenants"
        roleAssignments = genRoleAssignments(taEmptyStringTenants)
        service.replaceTenantAssignmentsOnUser(user, roleAssignments.tenantAssignments.tenantAssignment, IdentityUserTypeEnum.USER_ADMIN.levelAsInt)

        then: "Throws 400"
        Exception ex4 = thrown()
        IdmExceptionAssert.assertException(ex4, BadRequestException, "GEN-005", ERROR_CODE_ROLE_ASSIGNMENT_INVALID_FOR_TENANTS_MSG)
        0 * tenantRoleDao._(*_)
        0 * applicationRoleDao._(*_)
    }

    def "replaceTenantAssignmentsOnUserGroup: Verify static check validations with no backend checks"() {
        given:
        def domainId = "domainId"
        def userGroup = new UserGroup().with {
            it.id = "groupId"
            it.domainId = domainId
            it.uniqueId = "aLocation"
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
                it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
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

        RoleAssignments roleAssignments = genRoleAssignments(taNoTenants, taAllandExplicitTenants, taMissingRole, taWrongDomainTenant, taInvalidRoleWeight, taNoTenants)

        when: "Duplicate role exist along with other validation errors"
        service.replaceTenantAssignmentsOnUserGroup(userGroup, roleAssignments.tenantAssignments.tenantAssignment)

        then: "Throws 400 due to dup roles"
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, BadRequestException, "ROLE-000", ERROR_CODE_DUP_ROLE_ASSIGNMENT_MSG)
        0 * tenantRoleDao._(*_)
        0 * applicationRoleDao._(*_)

        when: "Submit request that includes missing forTenants and invalid for tenants along with invalid backend role errors"
        roleAssignments = genRoleAssignments(taMissingRole, taWrongDomainTenant, taInvalidRoleWeight, taNoTenants, taAllandExplicitTenants, taNonExistantTenant)
        service.replaceTenantAssignmentsOnUserGroup(userGroup, roleAssignments.tenantAssignments.tenantAssignment)

        then: "Throws 400 on first static tenant error encountered"
        Exception ex2 = thrown()
        IdmExceptionAssert.assertException(ex2, BadRequestException, "GEN-001", ERROR_CODE_ROLE_ASSIGNMENT_MISSING_FOR_TENANTS_MSG)
        0 * tenantRoleDao._(*_)
        0 * applicationRoleDao._(*_)

        when: "Submit request that includes missing forTenants and invalid forTenants definition along with invalid backend role errors"
        roleAssignments = genRoleAssignments(taMissingRole, taWrongDomainTenant, taInvalidRoleWeight, taAllandExplicitTenants, taNoTenants)
        service.replaceTenantAssignmentsOnUserGroup(userGroup, roleAssignments.tenantAssignments.tenantAssignment)

        then: "Throws 400 on first tenant error encountered"
        Exception ex3 = thrown()
        IdmExceptionAssert.assertException(ex3, BadRequestException, "GEN-005", ERROR_CODE_ROLE_ASSIGNMENT_INVALID_FOR_TENANTS_MSG)
        0 * tenantRoleDao._(*_)
        0 * applicationRoleDao._(*_)

        when: "Submit request that includes empty string forTenants"
        roleAssignments = genRoleAssignments(taEmptyStringTenants)
        service.replaceTenantAssignmentsOnUserGroup(userGroup, roleAssignments.tenantAssignments.tenantAssignment)

        then: "Throws 400"
        Exception ex4 = thrown()
        IdmExceptionAssert.assertException(ex4, BadRequestException, "GEN-005", ERROR_CODE_ROLE_ASSIGNMENT_INVALID_FOR_TENANTS_MSG)
        0 * tenantRoleDao._(*_)
        0 * applicationRoleDao._(*_)
    }

    def "replaceTenantAssignmentsOnUser: Verify backend check validations"() {
        def domainId = "domainId"
        def user = new User().with {
            it.id = "userId"
            it.uniqueId = "alocation"
            it.domainId = domainId
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
                it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
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
            it.rsWeight = 500
            it.clientId = "clientId"
            it
        }
        applicationService.getClientRoleById(roleIdGlobalOnly) >> new ClientRole().with {
            it.id = roleIdGlobalOnly
            it.name = roleIdGlobalOnly
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
            it.clientId = "clientId"
            it.setAssignmentType(RoleAssignmentEnum.GLOBAL.value())
            it
        }
        applicationService.getClientRoleById(roleIdTenantOnly) >> new ClientRole().with {
            it.id = roleIdTenantOnly
            it.name = roleIdTenantOnly
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
            it.clientId = "clientId"
            it.setAssignmentType(RoleAssignmentEnum.TENANT.value())
            it
        }

        applicationService.getClientRoleById(roleIdMissing) >> null

        def taWrongDomainTenant = new TenantAssignment().with {ta ->  ta.onRole = validRoles[0].id; ta.forTenants = [tenantOtherDomain]; ta}
        def taNonExistentTenant = new TenantAssignment().with {ta ->  ta.onRole = validRoles[1].id; ta.forTenants = ["noexisttenant"]; ta}
        def taInvalidRoleWeight = new TenantAssignment().with {ta ->  ta.onRole = roleIdWrongWeight; ta.forTenants = ["*"]; ta}
        def taInvalidRoleAssignmentGlobal = new TenantAssignment().with {ta ->  ta.onRole = roleIdGlobalOnly; ta.forTenants = [tenantSameDomain]; ta}
        def taInvalidRoleAssignmentTenant = new TenantAssignment().with {ta ->  ta.onRole = roleIdTenantOnly; ta.forTenants = ["*"]; ta}
        def taMissingRole = new TenantAssignment().with {ta ->  ta.onRole = roleIdMissing; ta.forTenants = ["*"]; ta}

        when: "Submit request that includes missing missing roles, invalid role, and invalid tenant (backend state)"
        RoleAssignments roleAssignments = genRoleAssignments(taMissingRole, taNonExistentTenant, taWrongDomainTenant, taInvalidRoleWeight)
        service.replaceTenantAssignmentsOnUser(user, roleAssignments.tenantAssignments.tenantAssignment, IdentityUserTypeEnum.USER_ADMIN.levelAsInt)

        then: "Throws exception on first backend failure (NotFoundException)"
        Exception ex4 = thrown()
        IdmExceptionAssert.assertException(ex4, NotFoundException, "GEN-005", String.format(ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_ROLE_MSG_PATTERN, taMissingRole.onRole))

        when: "Submit request that includes missing missing roles, invalid role, and invalid tenant (backend state)"
        roleAssignments = genRoleAssignments(taInvalidRoleWeight, taNonExistentTenant, taWrongDomainTenant, taMissingRole)
        service.replaceTenantAssignmentsOnUser(user, roleAssignments.tenantAssignments.tenantAssignment, IdentityUserTypeEnum.USER_ADMIN.levelAsInt)

        then: "Throws exception on first backend failure (ForbiddenException)"
        Exception ex5 = thrown()
        IdmExceptionAssert.assertException(ex5, ForbiddenException, "GEN-005", String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, taInvalidRoleWeight.onRole))

        when: "Submit request that includes missing tenant in different domain"
        roleAssignments = genRoleAssignments(taNonExistentTenant, taInvalidRoleWeight, taWrongDomainTenant, taMissingRole)
        service.replaceTenantAssignmentsOnUser(user, roleAssignments.tenantAssignments.tenantAssignment, IdentityUserTypeEnum.USER_ADMIN.levelAsInt)

        then: "Throws exception on first backend failure (NotFoundException)"
        Exception ex6 = thrown()
        IdmExceptionAssert.assertException(ex6, NotFoundException, "GEN-005", String.format(ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_TENANT_MSG_PATTERN, taNonExistentTenant.onRole))

        when: "Submit request that includes missing tenant in different domain"
        roleAssignments = genRoleAssignments(taWrongDomainTenant, taNonExistentTenant, taInvalidRoleWeight, taMissingRole, taInvalidRoleAssignmentGlobal, taInvalidRoleAssignmentTenant)
        service.replaceTenantAssignmentsOnUser(user, roleAssignments.tenantAssignments.tenantAssignment, IdentityUserTypeEnum.USER_ADMIN.levelAsInt)

        then: "Throws exception on first backend failure (ForbiddenException)"
        Exception ex7 = thrown()
        IdmExceptionAssert.assertException(ex7, ForbiddenException, "GEN-005", String.format(ERROR_CODE_ROLE_ASSIGNMENT_WRONG_DOMAIN_TENANT_MSG_PATTERN, taWrongDomainTenant.onRole, domainId))

        when: "Submit request that includes missing tenant in different domain"
        roleAssignments = genRoleAssignments(taInvalidRoleAssignmentGlobal, taWrongDomainTenant, taNonExistentTenant, taInvalidRoleWeight, taMissingRole, taInvalidRoleAssignmentTenant)
        service.replaceTenantAssignmentsOnUser(user, roleAssignments.tenantAssignments.tenantAssignment, IdentityUserTypeEnum.USER_ADMIN.levelAsInt)

        then: "Throws exception on first backend failure (ForbiddenException)"
        Exception ex8 = thrown()
        IdmExceptionAssert.assertException(ex8, ForbiddenException, "GEN-005", String.format(ERROR_CODE_ROLE_ASSIGNMENT_GLOBAL_ROLE_ASSIGNMENT_ONLY_MSG_PATTERN, taInvalidRoleAssignmentGlobal.onRole))

        when: "Submit request that includes missing tenant in different domain"
        roleAssignments = genRoleAssignments(taInvalidRoleAssignmentTenant, taInvalidRoleAssignmentGlobal, taWrongDomainTenant, taNonExistentTenant, taInvalidRoleWeight, taMissingRole)
        service.replaceTenantAssignmentsOnUser(user, roleAssignments.tenantAssignments.tenantAssignment, IdentityUserTypeEnum.USER_ADMIN.levelAsInt)

        then: "Throws exception on first backend failure (ForbiddenException)"
        Exception ex9 = thrown()
        IdmExceptionAssert.assertException(ex9, ForbiddenException, "GEN-005", String.format(ERROR_CODE_ROLE_ASSIGNMENT_TENANT_ASSIGNMENT_ONLY_MSG_PATTERN, taInvalidRoleAssignmentTenant.onRole))
    }

    def "replaceTenantAssignmentsOnUserGroup: Verify backend check validations"() {
        def domainId = "domainId"
        def userGroup = new UserGroup().with {
            it.id = "groupId"
            it.uniqueId = "alocation"
            it.domainId = domainId
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
                it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
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
            it.rsWeight = 500
            it.clientId = "clientId"
            it
        }
        applicationService.getClientRoleById(roleIdGlobalOnly) >> new ClientRole().with {
            it.id = roleIdGlobalOnly
            it.name = roleIdGlobalOnly
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
            it.clientId = "clientId"
            it.setAssignmentType(RoleAssignmentEnum.GLOBAL.value())
            it
        }
        applicationService.getClientRoleById(roleIdTenantOnly) >> new ClientRole().with {
            it.id = roleIdTenantOnly
            it.name = roleIdTenantOnly
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
            it.clientId = "clientId"
            it.setAssignmentType(RoleAssignmentEnum.TENANT.value())
            it
        }

        applicationService.getClientRoleById(roleIdMissing) >> null

        def taWrongDomainTenant = new TenantAssignment().with {ta ->  ta.onRole = validRoles[0].id; ta.forTenants = [tenantOtherDomain]; ta}
        def taNonExistentTenant = new TenantAssignment().with {ta ->  ta.onRole = validRoles[1].id; ta.forTenants = ["noexisttenant"]; ta}
        def taInvalidRoleWeight = new TenantAssignment().with {ta ->  ta.onRole = roleIdWrongWeight; ta.forTenants = ["*"]; ta}
        def taInvalidRoleAssignmentGlobal = new TenantAssignment().with {ta ->  ta.onRole = roleIdGlobalOnly; ta.forTenants = [tenantSameDomain]; ta}
        def taInvalidRoleAssignmentTenant = new TenantAssignment().with {ta ->  ta.onRole = roleIdTenantOnly; ta.forTenants = ["*"]; ta}
        def taMissingRole = new TenantAssignment().with {ta ->  ta.onRole = roleIdMissing; ta.forTenants = ["*"]; ta}

        when: "Submit request that includes missing missing roles, invalid role, and invalid tenant (backend state)"
        RoleAssignments roleAssignments = genRoleAssignments(taMissingRole, taNonExistentTenant, taWrongDomainTenant, taInvalidRoleWeight)
        service.replaceTenantAssignmentsOnUserGroup(userGroup, roleAssignments.tenantAssignments.tenantAssignment)

        then: "Throws exception on first backend failure (NotFoundException)"
        Exception ex4 = thrown()
        IdmExceptionAssert.assertException(ex4, NotFoundException, "GEN-005", String.format(ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_ROLE_MSG_PATTERN, taMissingRole.onRole))

        when: "Submit request that includes missing missing roles, invalid role, and invalid tenant (backend state)"
        roleAssignments = genRoleAssignments(taInvalidRoleWeight, taNonExistentTenant, taWrongDomainTenant, taMissingRole)
        service.replaceTenantAssignmentsOnUserGroup(userGroup, roleAssignments.tenantAssignments.tenantAssignment)

        then: "Throws exception on first backend failure (ForbiddenException)"
        Exception ex5 = thrown()
        IdmExceptionAssert.assertException(ex5, ForbiddenException, "GEN-005", String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, taInvalidRoleWeight.onRole))

        when: "Submit request that includes missing tenant in different domain"
        roleAssignments = genRoleAssignments(taNonExistentTenant, taInvalidRoleWeight, taWrongDomainTenant, taMissingRole)
        service.replaceTenantAssignmentsOnUserGroup(userGroup, roleAssignments.tenantAssignments.tenantAssignment)

        then: "Throws exception on first backend failure (NotFoundException)"
        Exception ex6 = thrown()
        IdmExceptionAssert.assertException(ex6, NotFoundException, "GEN-005", String.format(ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_TENANT_MSG_PATTERN, taNonExistentTenant.onRole))

        when: "Submit request that includes missing tenant in different domain"
        roleAssignments = genRoleAssignments(taWrongDomainTenant, taNonExistentTenant, taInvalidRoleWeight, taMissingRole, taInvalidRoleAssignmentGlobal, taInvalidRoleAssignmentTenant)
        service.replaceTenantAssignmentsOnUserGroup(userGroup, roleAssignments.tenantAssignments.tenantAssignment)

        then: "Throws exception on first backend failure (ForbiddenException)"
        Exception ex7 = thrown()
        IdmExceptionAssert.assertException(ex7, ForbiddenException, "GEN-005", String.format(ERROR_CODE_ROLE_ASSIGNMENT_WRONG_DOMAIN_TENANT_MSG_PATTERN, taWrongDomainTenant.onRole, domainId))

        when: "Submit request that includes missing tenant in different domain"
        roleAssignments = genRoleAssignments(taInvalidRoleAssignmentGlobal, taWrongDomainTenant, taNonExistentTenant, taInvalidRoleWeight, taMissingRole, taInvalidRoleAssignmentTenant)
        service.replaceTenantAssignmentsOnUserGroup(userGroup, roleAssignments.tenantAssignments.tenantAssignment)

        then: "Throws exception on first backend failure (ForbiddenException)"
        Exception ex8 = thrown()
        IdmExceptionAssert.assertException(ex8, ForbiddenException, "GEN-005", String.format(ERROR_CODE_ROLE_ASSIGNMENT_GLOBAL_ROLE_ASSIGNMENT_ONLY_MSG_PATTERN, taInvalidRoleAssignmentGlobal.onRole))

        when: "Submit request that includes missing tenant in different domain"
        roleAssignments = genRoleAssignments(taInvalidRoleAssignmentTenant, taInvalidRoleAssignmentGlobal, taWrongDomainTenant, taNonExistentTenant, taInvalidRoleWeight, taMissingRole)
        service.replaceTenantAssignmentsOnUserGroup(userGroup, roleAssignments.tenantAssignments.tenantAssignment)

        then: "Throws exception on first backend failure (ForbiddenException)"
        Exception ex9 = thrown()
        IdmExceptionAssert.assertException(ex9, ForbiddenException, "GEN-005", String.format(ERROR_CODE_ROLE_ASSIGNMENT_TENANT_ASSIGNMENT_ONLY_MSG_PATTERN, taInvalidRoleAssignmentTenant.onRole))
    }

    RoleAssignments genRoleAssignments(TenantAssignment... taAr) {
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

    def "replaceTenantAssignmentsOnUser: Golden 'new' scenario calls dao and returns response"() {
        def domainId = "domainId"
        def roleAId = "roleA"
        def roleBId = "roleB"
        def tenantBId = "tenantB"

        def user = new User().with {
            it.id = "userId"
            it.domainId = domainId
            it.uniqueId = "aLocation"
            it
        }

        def roleACr = new ClientRole().with {
            it.id = roleAId
            it.name = "roleAName"
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
            it.clientId = "clientId"
            it
        }
        def roleBCr = new ClientRole().with {
            it.id = roleBId
            it.name = "roleBName"
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
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
        List<TenantRole> result = service.replaceTenantAssignmentsOnUser(user, roleAssignments.tenantAssignments.tenantAssignment, IdentityUserTypeEnum.USER_ADMIN.levelAsInt)

        then:
        // Verify roles are retrieved for verification
        1 * applicationService.getClientRoleById(roleAId) >> roleACr
        1 * applicationService.getClientRoleById(roleBId) >> roleBCr

        // Verify tenants are validated
        1 * tenantService.getTenant(tenantBId) >> new Tenant().with {it.tenantId = tenantBId; it.domainId = domainId; it}

        // Verify queries to see if add or update. Have role 1 be an "add", role 2 be an "update"
        1 * tenantRoleDao.getTenantRoleForUser(user, roleAId)
        1 * tenantRoleDao.getTenantRoleForUser(user, roleBId) >> new TenantRole().with {
            it.roleRsId = roleBId
            it.tenantIds = ["otherTenant", "anotherTenant", "yetAnotherTenant"] as Set
            it.clientId = "existingClientId"
            it
        }

        // Verify roles are sent to dao correctly
        1 * tenantRoleDao.addTenantRoleToUser(user, {it.roleRsId == roleAId}) >> {args ->
            TenantRole role = args[1]
            assert role.roleRsId == roleAId
            assert CollectionUtils.isEmpty(role.tenantIds)
            assert role.clientId == roleACr.clientId
        }
        1 * tenantRoleDao.updateTenantRole({it.roleRsId == roleBId}) >> {args ->
            TenantRole role = args[0]
            assert role.roleRsId == roleBId
            assert CollectionUtils.isEqualCollection(role.tenantIds, [tenantBId])
            assert role.clientId == "existingClientId"
        }

        and:
        result.size() == 2
        result.find {it.roleRsId == roleAId} != null
        result.find {it.roleRsId == roleBId} != null
    }

    def "replaceTenantAssignmentsOnUserGroup: Golden 'new' scenario calls dao and returns response"() {
        def domainId = "domainId"
        def roleAId = "roleA"
        def roleBId = "roleB"
        def tenantBId = "tenantB"

        def userGroup = new UserGroup().with {
            it.id = "groupId"
            it.domainId = domainId
            it.uniqueId = "aLocation"
            it
        }

        def roleACr = new ClientRole().with {
            it.id = roleAId
            it.name = "roleAName"
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
            it.clientId = "clientId"
            it
        }
        def roleBCr = new ClientRole().with {
            it.id = roleBId
            it.name = "roleBName"
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
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
        List<TenantRole> result = service.replaceTenantAssignmentsOnUserGroup(userGroup, roleAssignments.tenantAssignments.tenantAssignment)

        then:
        // Verify roles are retrieved for verification
        1 * applicationService.getClientRoleById(roleAId) >> roleACr
        1 * applicationService.getClientRoleById(roleBId) >> roleBCr

        // Verify tenants are validated
        1 * tenantService.getTenant(tenantBId) >> new Tenant().with {it.tenantId = tenantBId; it.domainId = domainId; it}

        // Verify queries to see if add or update. Have role 1 be an "add", role 2 be an "update"
        1 * tenantRoleDao.getRoleAssignmentOnGroup(userGroup, roleAId)
        1 * tenantRoleDao.getRoleAssignmentOnGroup(userGroup, roleBId) >> new TenantRole().with {
            it.roleRsId = roleBId
            it.tenantIds = ["otherTenant", "anotherTenant", "yetAnotherTenant"] as Set
            it.clientId = "existingClientId"
            it
        }

        // Verify roles are sent to dao correctly
        1 * tenantRoleDao.addRoleAssignmentOnGroup(userGroup, {it.roleRsId == roleAId}) >> {args ->
            TenantRole role = args[1]
            assert role.roleRsId == roleAId
            assert CollectionUtils.isEmpty(role.tenantIds)
            assert role.clientId == roleACr.clientId
        }
        1 * tenantRoleDao.updateTenantRole({it.roleRsId == roleBId}) >> {args ->
            TenantRole role = args[0]
            assert role.roleRsId == roleBId
            assert CollectionUtils.isEqualCollection(role.tenantIds, [tenantBId])
            assert role.clientId == "existingClientId"
        }

        and:
        result.size() == 2
        result.find {it.roleRsId == roleAId} != null
        result.find {it.roleRsId == roleBId} != null
    }

    def "replaceTenantAssignmentsOnUser: Can switch tenant assignment to domain assignment"() {
        def domainId = "domainId"
        def roleAId = "roleA"

        def user = new User().with {
            it.id = "userId"
            it.uniqueId = "aLocation"
            it.domainId = domainId
            it
        }

        def roleACr = new ClientRole().with {
            it.id = roleAId
            it.name = "roleAName"
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
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
                    tas
            }
            it
        }

        when:
        List<TenantRole> result = service.replaceTenantAssignmentsOnUser(user, roleAssignments.tenantAssignments.tenantAssignment, IdentityUserTypeEnum.USER_ADMIN.levelAsInt)

        then:
        // Verify roles are retrieved for verification
        1 * applicationService.getClientRoleById(roleAId) >> roleACr

        // Verify queries to see if add or update. Simulate "update" of role assigned explicit tenants
        1 * tenantRoleDao.getTenantRoleForUser(user, roleAId) >> new TenantRole().with {
            it.roleRsId = roleACr.id
            it.clientId = roleACr.clientId
            it.tenantIds = ["aTenant"] as Set
            it
        }

        // Verify roles are sent to dao correctly
        1 * tenantRoleDao.updateTenantRole({it.roleRsId == roleAId}) >> {args ->
            TenantRole role = args[0]
            assert role.roleRsId == roleAId
            assert CollectionUtils.isEmpty(role.tenantIds)
            assert role.clientId == roleACr.clientId
        }

        and:
        result.size() == 1
        result.find {it.roleRsId == roleAId} != null
    }

    def "replaceTenantAssignmentsOnUserGroup: Can switch tenant assignment to domain assignment"() {
        def domainId = "domainId"
        def roleAId = "roleA"

        def userGroup = new UserGroup().with {
            it.id = "userId"
            it.uniqueId = "aLocation"
            it.domainId = domainId
            it
        }

        def roleACr = new ClientRole().with {
            it.id = roleAId
            it.name = "roleAName"
            it.rsWeight = RoleLevelEnum.LEVEL_1000.levelAsInt
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
                    tas
            }
            it
        }

        when:
        List<TenantRole> result = service.replaceTenantAssignmentsOnUserGroup(userGroup, roleAssignments.tenantAssignments.tenantAssignment)

        then:
        // Verify roles are retrieved for verification
        1 * applicationService.getClientRoleById(roleAId) >> roleACr

        // Verify queries to see if add or update. Simulate "update" of role assigned explicit tenants
        1 * tenantRoleDao.getRoleAssignmentOnGroup(userGroup, roleAId) >> new TenantRole().with {
            it.roleRsId = roleACr.id
            it.clientId = roleACr.clientId
            it.tenantIds = ["aTenant"] as Set
            it
        }

        // Verify roles are sent to dao correctly
        1 * tenantRoleDao.updateTenantRole({it.roleRsId == roleAId}) >> {args ->
            TenantRole role = args[0]
            assert role.roleRsId == roleAId
            assert CollectionUtils.isEmpty(role.tenantIds)
            assert role.clientId == roleACr.clientId
        }

        and:
        result.size() == 1
        result.find {it.roleRsId == roleAId} != null
    }
}
