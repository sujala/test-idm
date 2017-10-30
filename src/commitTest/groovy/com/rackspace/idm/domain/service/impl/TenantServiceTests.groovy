package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.Constants
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.PaginatorContext
import com.rackspace.idm.domain.entity.Tenant
import com.rackspace.idm.domain.entity.TenantType
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
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
    def "getTenantRolesForUserPerformant: assigns the auto-assign role based on the exclude tenant type feature flag, excludeTenantType = #excludeTenantType"() {
        given:
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def user = new User().with {
            it.username = RandomStringUtils.randomAlphanumeric(8)
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = domainId
            it
        }
        def tenantType = RandomStringUtils.randomAlphanumeric(8)
        def tenantTypeEntity = new TenantType().with {
            it.name = tenantType
            it
        }
        PaginatorContext tenantTypePaginatorContext = Mock(PaginatorContext)
        tenantTypePaginatorContext.getValueList() >> [tenantTypeEntity]
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
        1 * identityConfig.getReloadableConfig().isAutomaticallyAssignUserRoleOnDomainTenantsEnabled() >> true
        1 * domainService.getDomain(domainId) >> domain
        1 * identityConfig.getReloadableConfig().isAutomaticallyAssignUserRoleOnDomainTenantsEnabled() >> true
        1 * authorizationService.getCachedIdentityRoleByName(Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME) >> tenantAccessRole
        1 * identityConfig.getReloadableConfig().getAutomaticallyAssignUserRoleOnDomainTenantsRoleName() >> Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME
        1 * identityConfig.getReloadableConfig().getTenantTypesToExcludeAutoAssignRoleFrom() >> excludedTenantTypes
        if (excludeTenantType) {
            1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.DEFAULT_USER
        } else {
            1 * applicationService.getCachedClientRoleById(tenantAccessRole.id) >> tenantAccessRole
        }

        and: "the user gets the auto-assigned tenant role based on the exclude tenant type config"
        if (excludeTenantType) {
            assert userTenantRoles.find { role -> role.roleRsId == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && role.tenantIds.contains(tenant.tenantId) } == null
        } else {
            assert userTenantRoles.find { role -> role.roleRsId == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && role.tenantIds.contains(tenant.tenantId) } != null
        }

        where:
        excludeTenantType << [true, false]
    }

    @Unroll
    def "getEnabledUsersWithEffectiveTenantRole: assigns the auto-assign tenant access role based on the exclude tenant type config, excludeTenantType = #excludeTenantType"() {
        given:
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def user = new User().with {
            it.username = RandomStringUtils.randomAlphanumeric(8)
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.domainId = domainId
            it
        }
        def tenantType = RandomStringUtils.randomAlphanumeric(8)
        def tenantTypeEntity = new TenantType().with {
            it.name = tenantType
            it
        }
        PaginatorContext tenantTypePaginatorContext = Mock(PaginatorContext)
        tenantTypePaginatorContext.getValueList() >> [tenantTypeEntity]
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
        1 * identityConfig.getReloadableConfig().getAutomaticallyAssignUserRoleOnDomainTenantsRoleName() >> Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME
        1 * identityConfig.getReloadableConfig().isAutomaticallyAssignUserRoleOnDomainTenantsEnabled() >> true
        1 * userService.getUsersWithDomainAndEnabledFlag(domainId, true) >> [user]
        1 * identityConfig.getReloadableConfig().getTenantTypesToExcludeAutoAssignRoleFrom() >> excludedTenantTypes
        1 * tenantRoleDao.getAllTenantRolesForTenantAndRole(tenant.tenantId, Constants.IDENTITY_TENANT_ACCESS_ROLE_ID) >> []
        if (excludeTenantType) {
            1 * authorizationService.getIdentityTypeRoleAsEnum(user) >> IdentityUserTypeEnum.DEFAULT_USER
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
}
