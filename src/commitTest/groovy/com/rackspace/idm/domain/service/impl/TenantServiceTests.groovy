package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.Constants
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Unroll
import testHelpers.RootServiceTest

class TenantServiceTests extends RootServiceTest {

    DefaultTenantService service

    def setup() {
        service = new DefaultTenantService()
        mockIdentityConfig(service)
        mockAuthorizationService(service)
        mockUserGroupAuthorizationService(service)
        mockApplicationService(service)
        mockTenantRoleDao(service)
        mockTenantTypeService(service)
        mockDomainService(service)
        mockUserService(service)
    }

    @Unroll
    def "getTenantRolesForUserPerformant: assigns the auto-assign role based on the exclude tenant prefix feature flag, excludeTenantType = #excludeTenantType"() {
        given:
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def user = new User().with {
            it.username = RandomStringUtils.randomAlphanumeric(8)
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = domainId
            it
        }
        def tenantType = RandomStringUtils.randomAlphanumeric(8)
        def tenant = new Tenant().with {
            it.tenantId = "$tenantType:${RandomStringUtils.randomAlphanumeric(8)}"
            it.name = it.tenantId
            it.domainId = domainId
            it
        }
        def domain = new Domain().with {
            it.domainId = domainId
            it.tenantIds = [tenant.tenantId]
            it
        }
        def tenantAccessRole = new ImmutableClientRole(new ClientRole().with {
            it.id = Constants.IDENTITY_TENANT_ACCESS_ROLE_ID
            it.name = Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME
            it
        })
        def excludedTenantTypes = []
        if (excludeTenantType) {
            excludedTenantTypes << tenantType
        }

        when:
        def userTenantRoles = service.getTenantRolesForUserPerformant(user)

        then: "the correct backend services are called"
        1 * tenantRoleDao.getTenantRolesForUser(user) >> []
        1 * domainService.getDomain(domainId) >> domain
        1 * authorizationService.getCachedIdentityRoleByName(Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME) >> tenantAccessRole
        1 * identityConfig.getReloadableConfig().getTenantPrefixesToExcludeAutoAssignRoleFrom() >> excludedTenantTypes
        if (excludeTenantType) {
            1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.DEFAULT_USER
            0 * applicationService.getCachedClientRoleById(tenantAccessRole.id) >> tenantAccessRole
        } else {
            0 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.DEFAULT_USER
            1 * applicationService.getCachedClientRoleById(tenantAccessRole.id) >> tenantAccessRole
        }

        and: "the user gets the auto-assigned tenant role based on the exclude tenant type config"
        if (excludeTenantType) {
            assert userTenantRoles.find { role -> role.roleRsId == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && role.tenantIds.contains(tenant.tenantId) } == null
        } else {
            assert userTenantRoles.find { role -> role.roleRsId == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && role.tenantIds.contains(tenant.tenantId) } != null
        }

        and: "the user never gets the auto-assign role as a global role"
        userTenantRoles.find { role -> role.roleRsId == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && CollectionUtils.isEmpty(role.tenantIds) } == null

        where:
        excludeTenantType << [true, false]
    }

    def "getTenantRolesForUserPerformant: excludes the auto-assign role based on the exclude tenant prefix for all tenant prefixes in the config"() {
        given:
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def user = new User().with {
            it.username = RandomStringUtils.randomAlphanumeric(8)
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = domainId
            it
        }
        def tenantPrefix1 = RandomStringUtils.randomAlphanumeric(8)
        def tenantPrefix2 = RandomStringUtils.randomAlphanumeric(8)
        def tenant1 = new Tenant().with {
            it.tenantId = "$tenantPrefix1:${RandomStringUtils.randomAlphanumeric(8)}"
            it.name = it.tenantId
            it.domainId = domainId
            it
        }
        def tenant2 = new Tenant().with {
            it.tenantId = "$tenantPrefix2:${RandomStringUtils.randomAlphanumeric(8)}"
            it.name = it.tenantId
            it.domainId = domainId
            it
        }
        def domain = new Domain().with {
            it.domainId = domainId
            it.tenantIds = [tenant1.tenantId, tenant2.tenantId]
            it
        }
        def tenantAccessRole = new ImmutableClientRole(new ClientRole().with {
            it.id = Constants.IDENTITY_TENANT_ACCESS_ROLE_ID
            it.name = Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME
            it
        })
        def excludedTenantTypes = [tenantPrefix1, tenantPrefix2]

        when:
        def userTenantRoles = service.getTenantRolesForUserPerformant(user)

        then: "the correct backend services are called"
        1 * tenantRoleDao.getTenantRolesForUser(user) >> []
        1 * domainService.getDomain(domainId) >> domain
        1 * authorizationService.getCachedIdentityRoleByName(Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME) >> tenantAccessRole
        1 * identityConfig.getReloadableConfig().getTenantPrefixesToExcludeAutoAssignRoleFrom() >> excludedTenantTypes
        1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.DEFAULT_USER

        and: "the user did not get the tenant-access role and the role was not loaded to populate the role details"
        0 * applicationService.getCachedClientRoleById(tenantAccessRole.id) >> tenantAccessRole

        and: "the user gets the auto-assigned tenant role based on the exclude tenant type config"
        assert userTenantRoles.find { role -> role.roleRsId == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && role.tenantIds.contains(tenant1.tenantId) } == null
        assert userTenantRoles.find { role -> role.roleRsId == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && role.tenantIds.contains(tenant2.tenantId) } == null

        and: "the auto-assign role is not assigned as a global role"
        userTenantRoles.find { role -> role.roleRsId == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && CollectionUtils.isEmpty(role.tenantIds) } == null
    }

    @Unroll
    def "getTenantRolesForUserPerformant: assigns the auto-assign role for excluded tenant prefixed to only user admins and user managers, userType = #userType"() {
        given:
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def user = new User().with {
            it.username = RandomStringUtils.randomAlphanumeric(8)
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = domainId
            it
        }
        def tenantPrefix = RandomStringUtils.randomAlphanumeric(8)
        def tenant = new Tenant().with {
            it.tenantId = "$tenantPrefix:${RandomStringUtils.randomAlphanumeric(8)}"
            it.name = it.tenantId
            it.domainId = domainId
            it
        }
        def domain = new Domain().with {
            it.domainId = domainId
            it.tenantIds = [tenant.tenantId]
            it
        }
        def tenantAccessRole = new ImmutableClientRole(new ClientRole().with {
            it.id = Constants.IDENTITY_TENANT_ACCESS_ROLE_ID
            it.name = Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME
            it
        })
        def excludedTenantTypes = [tenantPrefix]

        when:
        def userTenantRoles = service.getTenantRolesForUserPerformant(user)

        then: "the correct backend services are called"
        1 * tenantRoleDao.getTenantRolesForUser(user) >> []
        1 * domainService.getDomain(domainId) >> domain
        1 * authorizationService.getCachedIdentityRoleByName(Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME) >> tenantAccessRole
        1 * identityConfig.getReloadableConfig().getTenantPrefixesToExcludeAutoAssignRoleFrom() >> excludedTenantTypes
        1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> userType

        and: "the auto-assign role is only loaded by id to populate the role details for users that actually get the role"
        if (IdentityUserTypeEnum.USER_ADMIN == userType || IdentityUserTypeEnum.USER_MANAGER == userType) {
            1 * applicationService.getCachedClientRoleById(tenantAccessRole.id) >> tenantAccessRole
        } else {
            0 * applicationService.getCachedClientRoleById(tenantAccessRole.id) >> tenantAccessRole
        }

        and: "the user gets the auto-assigned tenant role based on the exclude tenant type config"
        if (IdentityUserTypeEnum.USER_ADMIN == userType || IdentityUserTypeEnum.USER_MANAGER == userType) {
            assert userTenantRoles.find { role -> role.roleRsId == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && role.tenantIds.contains(tenant.tenantId) } != null
        } else {
            assert userTenantRoles.find { role -> role.roleRsId == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && role.tenantIds.contains(tenant.tenantId) } == null
        }

        and: "the user never gets the auto-assign role as a global role"
        userTenantRoles.find { role -> role.roleRsId == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && CollectionUtils.isEmpty(role.tenantIds) } == null

        where:
        userType << IdentityUserTypeEnum.values()
    }

    @Unroll
    def "getEnabledUsersWithEffectiveTenantRole: assigns the auto-assign tenant access role based on the exclude tenant prefix config, excludeTenantType = #excludeTenantType"() {
        given:
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def user = new User().with {
            it.username = RandomStringUtils.randomAlphanumeric(8)
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = domainId
            it
        }
        def tenantType = RandomStringUtils.randomAlphanumeric(8)
        def tenant = new Tenant().with {
            it.tenantId = "$tenantType:${RandomStringUtils.randomAlphanumeric(8)}"
            it.name = it.tenantId
            it.domainId = domainId
            it
        }
        def tenantAccessRole = new ClientRole().with {
            it.id = Constants.IDENTITY_TENANT_ACCESS_ROLE_ID
            it.name = Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME
            it
        }
        def excludedTenantTypes = []
        if (excludeTenantType) {
            excludedTenantTypes << tenantType
        }

        when:
        PaginatorContext<User> paginatorContext = service.getEnabledUsersWithEffectiveTenantRole(tenant, tenantAccessRole, 0, 100)

        then: "correct backend services are called"
        1 * userService.getUsersWithDomainAndEnabledFlag(domainId, true) >> [user]
        1 * identityConfig.getReloadableConfig().getTenantPrefixesToExcludeAutoAssignRoleFrom() >> excludedTenantTypes
        1 * tenantRoleDao.getAllTenantRolesForTenantAndRole(tenant.tenantId, Constants.IDENTITY_TENANT_ACCESS_ROLE_ID) >> []
        if (excludeTenantType) {
            1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.DEFAULT_USER
        } else {
            0 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.DEFAULT_USER
        }

        and: "the user is excluded based on the tenant type exclusion property"
        if (excludeTenantType) {
            assert !paginatorContext.getValueList().contains(user)
        } else {
            assert paginatorContext.getValueList().contains(user)
        }

        where:
        excludeTenantType << [true, false]
    }

    def "getEnabledUsersWithEffectiveTenantRole: excludes the auto-assign tenant access role for all tenant prefixes listed in the exclusion config"() {
        given:
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def user = new User().with {
            it.username = RandomStringUtils.randomAlphanumeric(8)
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = domainId
            it
        }
        def tenantType1 = RandomStringUtils.randomAlphanumeric(8)
        def tenantType2 = RandomStringUtils.randomAlphanumeric(8)
        def tenant1 = new Tenant().with {
            it.tenantId = "$tenantType1:${RandomStringUtils.randomAlphanumeric(8)}"
            it.name = it.tenantId
            it.domainId = domainId
            it
        }
        def tenant2 = new Tenant().with {
            it.tenantId = "$tenantType2:${RandomStringUtils.randomAlphanumeric(8)}"
            it.name = it.tenantId
            it.domainId = domainId
            it
        }
        def tenantAccessRole = new ClientRole().with {
            it.id = Constants.IDENTITY_TENANT_ACCESS_ROLE_ID
            it.name = Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME
            it
        }
        def excludedTenantPrefixes = [tenantType1, tenantType2]

        when: "get users for the first tenant prefix in the list"
        PaginatorContext<User> paginatorContext = service.getEnabledUsersWithEffectiveTenantRole(tenant1, tenantAccessRole, 0, 100)

        then: "correct backend services are called"
        1 * userService.getUsersWithDomainAndEnabledFlag(domainId, true) >> [user]
        1 * identityConfig.getReloadableConfig().getTenantPrefixesToExcludeAutoAssignRoleFrom() >> excludedTenantPrefixes
        1 * tenantRoleDao.getAllTenantRolesForTenantAndRole(tenant1.tenantId, Constants.IDENTITY_TENANT_ACCESS_ROLE_ID) >> []
        1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.DEFAULT_USER

        and: "the user is excluded based on the tenant type exclusion property"
        assert !paginatorContext.getValueList().contains(user)

        when: "get users for the second tenant prefix in the list"
        paginatorContext = service.getEnabledUsersWithEffectiveTenantRole(tenant2, tenantAccessRole, 0, 100)

        then: "correct backend services are called"
        1 * userService.getUsersWithDomainAndEnabledFlag(domainId, true) >> [user]
        1 * identityConfig.getReloadableConfig().getTenantPrefixesToExcludeAutoAssignRoleFrom() >> excludedTenantPrefixes
        1 * tenantRoleDao.getAllTenantRolesForTenantAndRole(tenant2.tenantId, Constants.IDENTITY_TENANT_ACCESS_ROLE_ID) >> []
        1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.DEFAULT_USER

        and: "the user is excluded based on the tenant type exclusion property"
        assert !paginatorContext.getValueList().contains(user)
    }

    @Unroll
    def "getEnabledUsersWithEffectiveTenantRole: excludes the auto-assign tenant access role for all users except user admins and user managers: userType = #userType"() {
        given:
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def user = new User().with {
            it.username = RandomStringUtils.randomAlphanumeric(8)
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = domainId
            it
        }
        def tenantType = RandomStringUtils.randomAlphanumeric(8)
        def tenant = new Tenant().with {
            it.tenantId = "$tenantType:${RandomStringUtils.randomAlphanumeric(8)}"
            it.name = it.tenantId
            it.domainId = domainId
            it
        }
        def tenantAccessRole = new ClientRole().with {
            it.id = Constants.IDENTITY_TENANT_ACCESS_ROLE_ID
            it.name = Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME
            it
        }
        def excludedTenantPrefixes = [tenantType]

        when: "get users for the first tenant prefix in the list"
        PaginatorContext<User> paginatorContext = service.getEnabledUsersWithEffectiveTenantRole(tenant, tenantAccessRole, 0, 100)

        then: "correct backend services are called"
        1 * userService.getUsersWithDomainAndEnabledFlag(domainId, true) >> [user]
        1 * identityConfig.getReloadableConfig().getTenantPrefixesToExcludeAutoAssignRoleFrom() >> excludedTenantPrefixes
        1 * tenantRoleDao.getAllTenantRolesForTenantAndRole(tenant.tenantId, Constants.IDENTITY_TENANT_ACCESS_ROLE_ID) >> []
        1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> userType

        and: "the user is excluded based on the tenant type exclusion property"
        if (IdentityUserTypeEnum.USER_ADMIN == userType || IdentityUserTypeEnum.USER_MANAGER == userType) {
            assert paginatorContext.getValueList().contains(user)
        } else {
            assert !paginatorContext.getValueList().contains(user)
        }

        where:
        userType << IdentityUserTypeEnum.values()
    }

}
