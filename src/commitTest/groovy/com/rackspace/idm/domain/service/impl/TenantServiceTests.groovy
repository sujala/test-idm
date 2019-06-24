package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.Constants
import com.rackspace.idm.api.resource.cloud.v20.ListUsersForTenantParams
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
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
    def "getTenantRolesForUserPerformant: assigns the auto-assign role to all users, userType = #userType"() {
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

        when:
        def userTenantRoles = service.getTenantRolesForUserPerformant(user)

        then: "the correct backend services are called"
        1 * tenantRoleDao.getTenantRolesForUser(user) >> []
        1 * domainService.getDomain(domainId) >> domain
        1 * applicationService.getCachedClientRoleByName(Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME) >> tenantAccessRole

        and: "the auto-assign role is only loaded by id to populate the role details for users that actually get the role"
        1 * applicationService.getCachedClientRoleById(tenantAccessRole.id) >> tenantAccessRole

        and: "the user gets the auto-assigned tenant role based on the exclude tenant type config"
        userTenantRoles.find { role -> role.roleRsId == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && role.tenantIds.contains(tenant.tenantId) } != null

        and: "the user never gets the auto-assign role as a global role"
        userTenantRoles.find { role -> role.roleRsId == Constants.IDENTITY_TENANT_ACCESS_ROLE_ID && CollectionUtils.isEmpty(role.tenantIds) } == null

        where:
        userType << IdentityUserTypeEnum.values()
    }

    @Unroll
    def "getEnabledUsersForTenantWithRole: Includes the auto-assign tenant access role for all users: userType = #userType"() {
        given:
        def paginationParams = new PaginationParams(0, 100)
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
        def tenantAccessRole = new ImmutableClientRole(new ClientRole().with {
            it.id = Constants.IDENTITY_TENANT_ACCESS_ROLE_ID
            it.name = Constants.IDENTITY_TENANT_ACCESS_ROLE_NAME
            it
        })

        when: "get users for the first tenant prefix in the list"
        PaginatorContext<User> paginatorContext = service.getEnabledUsersForTenantWithRole(tenant, tenantAccessRole.id, paginationParams)

        then: "correct backend services are called"
        1 * userService.getUsersWithDomainAndEnabledFlag(domainId, true) >> [user]
        1 * tenantRoleDao.getAllTenantRolesForTenantAndRole(tenant.tenantId, Constants.IDENTITY_TENANT_ACCESS_ROLE_ID) >> []
        1 * applicationService.getCachedClientRoleById(tenantAccessRole.id) >> tenantAccessRole

        and: "the user is included"
        paginatorContext.getValueList().contains(user)

        where:
        userType << IdentityUserTypeEnum.values()
    }
}
