package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.api.resource.cloud.v20.ListUsersSearchParams
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.dao.IdentityUserDao
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ServiceCatalogInfo
import com.rackspace.idm.domain.service.TenantEndpointMeta
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.modules.endpointassignment.entity.TenantTypeRule
import com.rackspace.idm.modules.usergroups.api.resource.UserSearchCriteria
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.Logger
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.EntityFactory
import testHelpers.RootServiceTest

import static com.rackspace.idm.domain.service.impl.DefaultIdentityUserService.DELETE_FEDERATED_USER_FORMAT

class DefaultIdentityUserServiceTest extends RootServiceTest {
    @Shared DefaultIdentityUserService service

    IdentityUserDao identityUserRepository;

    Logger deleteUserLogger = Mock(Logger)

    EntityFactory entityFactory = new EntityFactory()

    def setupSpec() {
        //service being tested
        service = new DefaultIdentityUserService()
    }

    def setup() {
        identityUserRepository = Mock(IdentityUserDao)
        service.identityUserRepository = identityUserRepository
        deleteUserLogger = Mock(Logger)
        service.deleteUserLogger = deleteUserLogger
        mockIdentityConfig(service)
        mockRuleService(service)
        mockUserService(service)
        mockTenantService(service)
        mockDomainService(service)
        mockAuthorizationService(service)
        mockEndpointService(service)
        mockDelegationService(service)
        mockRequestContextHolder(service)
    }

    def "Add Group to user includes feed event"() {
        given:
        def groupid = "1234509"
        def user = entityFactory.createUser()

        when:
        service.addGroupToEndUser(groupid, user.id)

        then:
        1 * identityUserRepository.getEndUserById(user.id) >> user
        1 * identityUserRepository.updateIdentityUser(user)
        user.getRsGroupId().contains(groupid)
    }

    def "Add an existing Group to user doesn't update user or send feed event"() {
        def groupid = "1234509"
        def user = entityFactory.createUser().with {it.getRsGroupId().add(groupid); return it;}

        when:
        service.addGroupToEndUser(groupid, user.id)

        then:
        1 * identityUserRepository.getEndUserById(user.id) >> user
        0 * identityUserRepository.updateIdentityUser(user)
        user.getRsGroupId().contains(groupid)
    }

    def "Remove Group from user removes from user and sends feed event"() {
        given:
        def groupid = "1234509"
        def user = entityFactory.createUser().with {it.getRsGroupId().add(groupid); return it;}

        when:
        service.removeGroupFromEndUser(groupid, user.id)

        then:
        1 * identityUserRepository.getEndUserById(user.id) >> user
        1 * identityUserRepository.updateIdentityUser(user)
        !user.getRsGroupId().contains(groupid)
    }

    def "Remove non-existing group from user doesn't update user or send feed event"(){
        given:
        def groupid = "1234509"
        def user = entityFactory.createUser()

        when:
        service.removeGroupFromEndUser(groupid, user.id)

        then:
        1 * identityUserRepository.getEndUserById(user.id) >> user
        0 * identityUserRepository.updateIdentityUser(user)
        !user.getRsGroupId().contains(groupid)
    }

    def "Delete user adds delete user log entry"() {
        given:
        def user = entityFactory.createFederatedUser()

        when:
        service.deleteUser(user)

        then:
        1 * deleteUserLogger.warn(DELETE_FEDERATED_USER_FORMAT, _)
    }

    /**
     * Ensure retrieving the service catalog calls the appropriate service to retrieve the effective roles for the user
     *
     * @return
     */
    def "getServiceCatalogInfo: Calls getTenantRolesForUserPerformant to retrieve effective user roles"() {
        given:
        authorizationService.getIdentityTypeRoleAsEnum(_) >> IdentityUserTypeEnum.DEFAULT_USER
        def sourcedRoleAssignments = Mock(SourcedRoleAssignments)
        SourcedRoleAssignmentsLegacyAdapter sourcedRoleAssignmentsLegacyAdapter = Mock()

        def user = entityFactory.createUser()

        when:
        service.getServiceCatalogInfo(user)

        then:
        1 * tenantService.getSourcedRoleAssignmentsForUser(user) >> sourcedRoleAssignments
        1 * sourcedRoleAssignments.getSourcedRoleAssignmentsLegacyAdapter() >> sourcedRoleAssignmentsLegacyAdapter
        1 * sourcedRoleAssignmentsLegacyAdapter.getStandardTenantRoles() >> []
    }

    /**
     * Ensure retrieving the service catalog calls the appropriate service to retrieve the effective roles for the user
     *
     * @return
     */
    def "getServiceCatalogInfoApplyRcnRoles: Calls getTenantRolesForUserApplyRcnRoles to retrieve effective user roles"() {
        given:
        def user = entityFactory.createUser()
        def sourcedRoleAssignments = Mock(SourcedRoleAssignments)

        authorizationService.getIdentityTypeRoleAsEnum(_) >> IdentityUserTypeEnum.DEFAULT_USER
        domainService.getDomain(_) >> entityFactory.createDomain(user.domainId)

        when:
        ServiceCatalogInfo scInfo = service.getServiceCatalogInfoApplyRcnRoles(user)

        then:
        1 * tenantService.getSourcedRoleAssignmentsForUser(user) >> sourcedRoleAssignments
        1 * sourcedRoleAssignments.asTenantRolesExcludeNoTenants() >> []
        1 * sourcedRoleAssignments.userTypeFromAssignedRoles >> IdentityUserTypeEnum.USER_ADMIN
        scInfo != null
    }

    def "getServiceCatalogInfo: getting service catalog for user uses inferred tenant types when calculating tenantMetaData"() {
        given:
        def tenantType = RandomStringUtils.randomAlphabetic(8)
        def user = entityFactory.createUser()
        def tenant = entityFactory.createTenant().with {
            it.tenantId = it.name
            it
        }
        def userRoles = [entityFactory.createTenantRole().with {
            it.name = IdentityRole.IDENTITY_TENANT_ACCESS.roleName
            it.tenantIds = [tenant.name]
            it
        }]
        def mappingRules = [new TenantTypeRule().with {
            it.tenantType = tenantType
            it
        }]

        def sourcedRoleAssignments = Mock(SourcedRoleAssignments)
        SourcedRoleAssignmentsLegacyAdapter sourcedRoleAssignmentsLegacyAdapter = Mock()

        when:
        service.getServiceCatalogInfo(user)

        then: "the correct services were called"
        // Get roles for user
        1 * tenantService.getSourcedRoleAssignmentsForUser(user) >> sourcedRoleAssignments
        1 * sourcedRoleAssignments.getSourcedRoleAssignmentsLegacyAdapter() >> sourcedRoleAssignmentsLegacyAdapter
        1 * sourcedRoleAssignmentsLegacyAdapter.getStandardTenantRoles() >> userRoles

        // Retrieve user type
        1 * sourcedRoleAssignments.getUserTypeFromAssignedRoles() >> IdentityUserTypeEnum.DEFAULT_USER

        // Various services required to calc metadata
        2 * identityConfig.getReloadableConfig().includeEndpointsBasedOnRules() >> true
        1 * tenantService.getTenant(tenant.name) >> tenant
        1 * ruleService.findAllEndpointAssignmentRules() >> mappingRules
        1 * tenantService.inferTenantTypeForTenantId(tenant.tenantId) >> tenantType

        // Verify tenant metadata is calculated appropriately.
        1 * endpointService.calculateOpenStackEndpointForTenantMeta({it.tenant == tenant}) >> { args ->

            // Extract the tenant metadata passed to endpoint service verify it was generated correctly
            TenantEndpointMeta endpointMeta = args[0]
            assert endpointMeta.user == user
            assert endpointMeta.rolesOnTenant.name.contains(IdentityRole.IDENTITY_TENANT_ACCESS.roleName)
            assert endpointMeta.rulesForTenant.contains(mappingRules[0])

            // Return null as if found no endpoints. Not testing endpoint calculation
            return null
        }
    }

    def "getServiceCatalogInfo: getting service catalog for user uses tenant types set on tenant when calculating tenantMetaData"() {
        given:
        def tenantType = RandomStringUtils.randomAlphabetic(8)
        def user = entityFactory.createUser()
        def tenant = entityFactory.createTenant().with {
            it.types = [tenantType]
            it.tenantId = it.name
            it
        }
        def userRoles = [entityFactory.createTenantRole().with {
            it.name = IdentityRole.IDENTITY_TENANT_ACCESS.roleName
            it.tenantIds = [tenant.name]
            it
        }]
        def mappingRules = [new TenantTypeRule().with {
            it.tenantType = tenantType
            it
        }]
        def sourcedRoleAssignments = Mock(SourcedRoleAssignments)
        SourcedRoleAssignmentsLegacyAdapter sourcedRoleAssignmentsLegacyAdapter = Mock()

        when:
        service.getServiceCatalogInfo(user)

        then: "the correct services were called"
        // Get roles for user
        1 * tenantService.getSourcedRoleAssignmentsForUser(user) >> sourcedRoleAssignments
        1 * sourcedRoleAssignments.getSourcedRoleAssignmentsLegacyAdapter() >> sourcedRoleAssignmentsLegacyAdapter
        1 * sourcedRoleAssignmentsLegacyAdapter.getStandardTenantRoles() >> userRoles

        // Retrieve user type
        1 * sourcedRoleAssignments.getUserTypeFromAssignedRoles() >> IdentityUserTypeEnum.DEFAULT_USER

        // Various services required to calc metadata
        2 * identityConfig.getReloadableConfig().includeEndpointsBasedOnRules() >> true
        1 * tenantService.getTenant(tenant.name) >> tenant
        1 * ruleService.findAllEndpointAssignmentRules() >> mappingRules

        // The tenant type is NOT inferred for a tenant with a tenant type set
        0 * tenantService.inferTenantTypeForTenantId(tenant.tenantId)

        // Verify tenant metadata is calculated appropriately.
        1 * endpointService.calculateOpenStackEndpointForTenantMeta({it.tenant == tenant}) >> { args ->

            // Extract the tenant metadata passed to endpoint service verify it was generated correctly
            TenantEndpointMeta endpointMeta = args[0]
            assert endpointMeta.user == user
            assert endpointMeta.rolesOnTenant.name.contains(IdentityRole.IDENTITY_TENANT_ACCESS.roleName)
            assert endpointMeta.rulesForTenant.contains(mappingRules[0])

            // Return null as if found no endpoints. Not testing endpoint calculation
            return null
        }
    }

    @Unroll
    def "getServiceCatalogInfo: getting service catalog for user infers tenant types set on tenant when calculating tenantMetaData based on endpoint mapping feature flag, featureEnabled == #featureEnabled"() {
        given:
        def tenantType = RandomStringUtils.randomAlphabetic(8)
        def user = entityFactory.createUser()
        def tenant = entityFactory.createTenant().with {
            it.tenantId = it.name
            it
        }
        def userRoles = [entityFactory.createTenantRole().with {
            it.name = IdentityRole.IDENTITY_TENANT_ACCESS.roleName
            it.tenantIds = [tenant.name]
            it
        }]
        def mappingRules = [new TenantTypeRule().with {
            it.tenantType = tenantType
            it
        }]
        def sourcedRoleAssignments = Mock(SourcedRoleAssignments)
        SourcedRoleAssignmentsLegacyAdapter sourcedRoleAssignmentsLegacyAdapter = Mock()

        when:
        service.getServiceCatalogInfo(user)

        then: "the correct services were called"
        // Get roles for user
        1 * tenantService.getSourcedRoleAssignmentsForUser(user) >> sourcedRoleAssignments
        1 * sourcedRoleAssignments.getSourcedRoleAssignmentsLegacyAdapter() >> sourcedRoleAssignmentsLegacyAdapter
        1 * sourcedRoleAssignmentsLegacyAdapter.getStandardTenantRoles() >> userRoles

        // Retrieve user type
        1 * sourcedRoleAssignments.getUserTypeFromAssignedRoles() >> IdentityUserTypeEnum.DEFAULT_USER

        // Various services required to calc metadata
        2 * identityConfig.getReloadableConfig().includeEndpointsBasedOnRules() >> featureEnabled
        1 * tenantService.getTenant(tenant.name) >> tenant

        // The tenant type is inferred based on the feature flag
        mockTenantService(service)
        if (featureEnabled) {
            1 * ruleService.findAllEndpointAssignmentRules() >> mappingRules
            1 * tenantService.inferTenantTypeForTenantId(tenant.tenantId) >> tenantType
        } else {
            0 * ruleService.findAllEndpointAssignmentRules() >> mappingRules
            0 * tenantService.inferTenantTypeForTenantId(tenant.tenantId)
        }

        // Verify tenant metadata is calculated appropriately.
        1 * endpointService.calculateOpenStackEndpointForTenantMeta({it.tenant == tenant}) >> { args ->

            // Extract the tenant metadata passed to endpoint service verify it was generated correctly
            TenantEndpointMeta endpointMeta = args[0]
            assert endpointMeta.user == user
            assert endpointMeta.rolesOnTenant.name.contains(IdentityRole.IDENTITY_TENANT_ACCESS.roleName)
            // The rule is only mapped to the tenant if the tenant type is inferred
            if (featureEnabled) {
                assert endpointMeta.rulesForTenant.size() == 1
                assert endpointMeta.rulesForTenant.contains(mappingRules[0])
            } else {
                assert endpointMeta.rulesForTenant.size() == 0
            }

            // Return null as if found no endpoints. Not testing endpoint calculation
            return null
        }

        where:
        featureEnabled << [true, false]
    }

    def "Get users in user group paged"() {
        given:
        def group = new UserGroup()
        UserSearchCriteria userSearchCriteria = new UserSearchCriteria(new PaginationParams())

        when:
        service.getEndUsersInUserGroupPaged(group, userSearchCriteria)

        then:
        1 * identityUserRepository.getEndUsersInUserGroupPaged(group, userSearchCriteria)
    }

    def "Get users in user group"() {
        given:
        def group = new UserGroup()

        when:
        service.getEndUsersInUserGroup(group)

        then:
        1 * identityUserRepository.getEndUsersInUserGroup(group)
    }

    def "updateFederatedUser: calls correct DAO"() {
        given:
        def user = new FederatedUser()

        when:
        service.updateFederatedUser(user)

        then:
        1 * identityUserRepository.updateIdentityUser(user)
    }


    def "deleteUser deletes the user from explicit DA assignments"() {
        given:
        def user = new User()

        when:
        service.deleteUser(user)

        then:
        1 * tenantService.getTenantRolesForUser(user) >> []
        1 * delegationService.removeConsumerFromExplicitDelegationAgreementAssignments(user)
    }

    def "getEndUsersPaged: calls correct services and dao"() {
        given:
        Domain domain = entityFactory.createDomain("domainId")
        Tenant tenant = entityFactory.createTenant().with {
            it.domainId = "tenant_domain_id"
            it
        }
        ListUsersSearchParams params

        def caller =  entityFactory.createUser()

        when: "providing 'tenant_id' param"
        params = new ListUsersSearchParams()
        params.tenantId = tenant.tenantId
        service.getEndUsersPaged(params)

        then:
        1 * tenantService.checkAndGetTenant(tenant.tenantId) >> tenant
        1 * domainService.checkAndGetDomain(tenant.domainId) >> domain
        1 * identityUserRepository.getEndUsersPaged(_) >> { args ->
            ListUsersSearchParams usersSearchParams = args[0]
            assert usersSearchParams.domainId == tenant.domainId
        }

        when: "providing 'domain_id' param"
        params = new ListUsersSearchParams()
        params.domainId = domain.domainId
        service.getEndUsersPaged(params)

        then:
        0 * tenantService.checkAndGetTenant(tenant.tenantId)
        1 * domainService.checkAndGetDomain(domain.domainId) >> domain
        1 * identityUserRepository.getEndUsersPaged(_) >> { args ->
            ListUsersSearchParams usersSearchParams = args[0]
            assert usersSearchParams.domainId == domain.domainId
        }

        when: "default to callers domain"
        params = new ListUsersSearchParams()
        params.domainId =  null
        service.getEndUsersPaged(params)

        then:
        0 * tenantService.checkAndGetTenant(tenant.tenantId)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * domainService.checkAndGetDomain(domain.domainId) >> domain
        1 * identityUserRepository.getEndUsersPaged(_) >> { args ->
            ListUsersSearchParams usersSearchParams = args[0]
            assert usersSearchParams.domainId == domain.domainId
        }
    }

    def "getEndUsersPaged with 'admin_only' param set to true: calls correct services and dao"() {
        given:
        Domain domain = entityFactory.createDomain("domainId")
        Tenant tenant = entityFactory.createTenant().with {
            it.domainId = domain.domainId
            it
        }
        ListUsersSearchParams params

        def caller = entityFactory.createUser()
        def user = entityFactory.createUser()

        when: "providing 'tenant_id' param"
        params = new ListUsersSearchParams()
        params.adminOnly = true
        params.tenantId = tenant.tenantId
        PaginatorContext paginatorContext = service.getEndUsersPaged(params)

        then:
        1 * tenantService.checkAndGetTenant(tenant.tenantId) >> tenant
        1 * domainService.checkAndGetDomain(tenant.domainId) >> domain
        1 * userService.getUserAdminByDomain(domain) >> user

        paginatorContext.valueList.size() == 1

        when: "providing 'domain_id' param"
        params = new ListUsersSearchParams()
        params.adminOnly = true
        params.domainId = domain.domainId
        paginatorContext = service.getEndUsersPaged(params)

        then:
        0 * tenantService.checkAndGetTenant(tenant.tenantId)
        1 * domainService.checkAndGetDomain(domain.domainId) >> domain
        1 * userService.getUserAdminByDomain(domain) >> user

        paginatorContext.valueList.size() == 1

        when: "default to callers domain"
        params = new ListUsersSearchParams()
        params.adminOnly = true
        params.domainId =  null
        paginatorContext = service.getEndUsersPaged(params)

        then:
        0 * tenantService.checkAndGetTenant(tenant.tenantId)
        1 * requestContext.getEffectiveCaller() >> caller
        1 * domainService.checkAndGetDomain(domain.domainId) >> domain
        1 * userService.getUserAdminByDomain(domain) >> user

        paginatorContext.valueList.size() == 1
    }

    def "empty list cases: getEndUsersPaged with 'admin_only' param set to true"() {
        given:
        Domain domain = entityFactory.createDomain("domainId")
        Tenant tenant = entityFactory.createTenant().with {
            it.domainId = domain.domainId
            it
        }
        ListUsersSearchParams params

        def user = entityFactory.createUser().with {
            it.unverified = false
            it
        }

        when: "invalid name param"
        params = new ListUsersSearchParams()
        params.adminOnly = true
        params.name = "otherName"
        params.domainId = domain.domainId
        PaginatorContext paginatorContext = service.getEndUsersPaged(params)

        then:
        1 * domainService.checkAndGetDomain(tenant.domainId) >> domain
        1 * userService.getUserAdminByDomain(domain) >> user

        paginatorContext.valueList.size() == 0

        when: "searching for unverified users"
        params = new ListUsersSearchParams()
        params.adminOnly = true
        params.userType = "UNVERIFIED"
        params.domainId = domain.domainId
        paginatorContext = service.getEndUsersPaged(params)

        then:
        1 * domainService.checkAndGetDomain(domain.domainId) >> domain
        1 * userService.getUserAdminByDomain(domain) >> user

        paginatorContext.valueList.size() == 0

        when: "invalid email"
        params = new ListUsersSearchParams()
        params.adminOnly = true
        params.email = "invalid@email.com"
        params.domainId =  domain.domainId
        paginatorContext = service.getEndUsersPaged(params)

        then:
        1 * domainService.checkAndGetDomain(domain.domainId) >> domain
        1 * userService.getUserAdminByDomain(domain) >> user

        paginatorContext.valueList.size() == 0
    }

    def "error check: getEndUsersPaged"() {
        given:
        Domain domain = entityFactory.createDomain("domainId")
        Tenant tenant = entityFactory.createTenant().with {
            it.domainId = "tenant_domain_id"
            it
        }
        ListUsersSearchParams params

        when: "tenant not found"
        params = new ListUsersSearchParams()
        params.tenantId = tenant.tenantId
        service.getEndUsersPaged(params)

        then:
        1 * tenantService.checkAndGetTenant(tenant.tenantId) >> {throw new NotFoundException()}
        thrown(NotFoundException)

        when: "domain not found"
        params = new ListUsersSearchParams()
        params.domainId = domain.domainId
        service.getEndUsersPaged(params)

        then:
        1 * domainService.checkAndGetDomain(domain.domainId) >> {throw new NotFoundException()}
        thrown(NotFoundException)
    }

}