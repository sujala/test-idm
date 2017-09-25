package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.IdentityUserDao
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.TenantEndpointMeta
import com.rackspace.idm.modules.endpointassignment.entity.TenantTypeRule
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
        mockTenantService(service)
        mockAuthorizationService(service)
        mockEndpointService(service)
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

    def "getServiceCatalogInfo: getting service catalog for user uses inferred tenant types when calculating tenantMetaData"() {
        given:
        def tenantType = RandomStringUtils.randomAlphabetic(8)
        def user = entityFactory.createUser()
        def tenant = entityFactory.createTenant().with {
            it.tenantId = it.name
            it
        }
        def userRoles = [entityFactory.createTenantRole().with {
            it.name = IdentityConfig.AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_DEFAULT
            it.tenantIds = [tenant.name]
            it
        }]
        def mappingRules = [new TenantTypeRule().with {
            it.tenantType = tenantType
            it
        }]

        when:
        service.getServiceCatalogInfo(user)

        then: "the correct services were called"
        // Get roles for user
        1 * tenantService.getTenantRolesForUserPerformant(user) >> userRoles

        // Retrieve user type
        1 * authorizationService.getIdentityTypeRoleAsEnum(userRoles) >> IdentityUserTypeEnum.DEFAULT_USER

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
            assert endpointMeta.rolesOnTenant.name.contains(IdentityConfig.AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_DEFAULT)
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
            it.name = IdentityConfig.AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_DEFAULT
            it.tenantIds = [tenant.name]
            it
        }]
        def mappingRules = [new TenantTypeRule().with {
            it.tenantType = tenantType
            it
        }]

        when:
        service.getServiceCatalogInfo(user)

        then: "the correct services were called"
        // Get roles for user
        1 * tenantService.getTenantRolesForUserPerformant(user) >> userRoles

        // Retrieve user type
        1 * authorizationService.getIdentityTypeRoleAsEnum(userRoles) >> IdentityUserTypeEnum.DEFAULT_USER

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
            assert endpointMeta.rolesOnTenant.name.contains(IdentityConfig.AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_DEFAULT)
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
            it.name = IdentityConfig.AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_DEFAULT
            it.tenantIds = [tenant.name]
            it
        }]
        def mappingRules = [new TenantTypeRule().with {
            it.tenantType = tenantType
            it
        }]

        when:
        service.getServiceCatalogInfo(user)

        then: "the correct services were called"
        // Get roles for user
        1 * tenantService.getTenantRolesForUserPerformant(user) >> userRoles

        // Retrieve user type
        1 * authorizationService.getIdentityTypeRoleAsEnum(userRoles) >> IdentityUserTypeEnum.DEFAULT_USER

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
            assert endpointMeta.rolesOnTenant.name.contains(IdentityConfig.AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME_DEFAULT)
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

}