package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import static com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum.STANDARD

class DefaultTenantServiceEffectiveRolesTest extends RootServiceTest {
    @Shared DefaultTenantService service
    @Shared FederatedUserDao mockFederatedUserDao

    String clientId = "clientId"
    String name = "name"
    String customerId = "customerId"

    def setup() {
        service = new DefaultTenantService()

        mockConfiguration(service)
        mockIdentityConfig(service)
        mockDomainService(service)
        mockTenantDao(service)
        mockTenantRoleDao(service)
        mockApplicationService(service)
        mockAuthorizationService(service)
        mockUserService(service)
        mockEndpointService(service)
        mockScopeAccessService(service)
        mockAtomHopperClient(service)
        mockFederatedUserDao(service)
        mockUserGroupService(service)
        mockUserGroupAuthorizationService(service)
    }

    def "getSourcedRoleAssignmentsForRacker: Error When supply null racker"() {
        when:
        service.getSourcedRoleAssignmentsForRacker(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "getSourcedRoleAssignmentsForRacker: Error When supply racker with missing id"() {
        when:
        service.getSourcedRoleAssignmentsForRacker(new Racker())

        then:
        thrown(IllegalArgumentException)
    }

    def "getSourcedRoleAssignmentsForRacker: normal case returns standard racker role with single IDENTITY source"() {
        given:
        String rackerRoleId = "1"
        def rackerCr = createImmutableClientRole(rackerRoleId, 2000)
        reloadableConfig.getCacheRolesWithoutApplicationRestartFlag() >> true
        applicationService.getCachedClientRoleById(rackerRoleId) >> rackerCr
        staticConfig.getRackerRoleId() >> rackerRoleId
        Racker racker = new Racker().with {
            it.id = rackerRoleId
            it
        }

        when:
        SourcedRoleAssignments assignments = service.getSourcedRoleAssignmentsForRacker(racker)

        then:
        assignments.user == racker
        assignments.sourcedRoleAssignments.size() == 1 // only returns default identity racker role. Doesn't hit eDir
        def assignment = assignments.sourcedRoleAssignments[0]
        assignment != null
        assignment.role == rackerCr
        CollectionUtils.isEmpty(assignment.tenantIds)
        assignment.sources.size() == 1
        CollectionUtils.isEmpty(assignment.sources[0].tenantIds)
        assignment.sources[0].assignmentType == SourcedRoleAssignments.AssignmentType.DOMAIN
        assignment.sources[0].sourceType == SourcedRoleAssignments.SourceType.SYSTEM
        assignment.sources[0].sourceId == "IDENTITY"
    }

    def "getSourcedRoleAssignmentsForUser: Error When supply null user"() {
        when:
        service.getSourcedRoleAssignmentsForUser(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "getSourcedRoleAssignmentsForUser: Error When supply user with missing id"() {
        when:
        service.getSourcedRoleAssignmentsForUser(new User().with {
            it.domainId = "domainId"
            it
        })

        then:
        thrown(IllegalArgumentException)
    }

    def "getSourcedRoleAssignmentsForUser: Error When supply user with missing domain"() {
        when:
        service.getSourcedRoleAssignmentsForUser(new User().with {
            it.id = "userId"
            it
        })

        then:
        thrown(IllegalArgumentException)
    }

    def "getSourcedRoleAssignmentsForUser: Calls domain service to validate user's domain exists"() {
        given:
        def user = new User().with {
            it.id = "id"
            it.domainId = "domainId"
            it
        }
        when:
        service.getSourcedRoleAssignmentsForUser(user)

        then:
        // the test here is validating service calls the checkAndGet service which
        // correctly throws exception rather than testing exception is thrown
        1 * domainService.getDomain(user.domainId) >> {throw new NotFoundException()}

        // Just verifies exception is bubbled out of service
        thrown(NotFoundException)
    }

    def "getSourcedRoleAssignmentsForUser: User without any role returns error"() {
        given:
        def user = new User().with {
            it.id = "id"
            it.domainId = "domainId"
            it
        }
        def domain = new Domain().with {
            it.domainId = user.domainId
            it
        }
        domainService.checkAndGetDomain(user.domainId) >> domain
        domainService.getDomain(user.getDomainId()) >> domain // Called by logic for created tenant access

        when:
        service.getSourcedRoleAssignmentsForUser(user)

        then:
        1 * tenantRoleDao.getTenantRolesForUser(user) >> [] // retrieves no user sourced roles

        IllegalStateException ex = thrown()
        ex.getMessage() == "The user 'id' does not contain a user type role. Roles can not be determined for this user"
    }

    @Unroll
    def "getSourcedRoleAssignmentsForUser: User with just identity type role #roleName on domain with no tenants returns empty list of tenants for role"() {
        given:
        def user = new User().with {
            it.id = "id"
            it.domainId = "domainId"
            it
        }
        def domain = new Domain().with {
            it.domainId = user.domainId
            it
        }
        domainService.checkAndGetDomain(user.domainId) >> domain
        domainService.getDomain(user.getDomainId()) >> domain // Called by logic for created tenant access
        def tenantRole = createTenantRole("roleId")
        def icr = createImmutableCrFromTenantRole(roleName, tenantRole)

        when:
        SourcedRoleAssignments assignments = service.getSourcedRoleAssignmentsForUser(user)

        then:
        1 * tenantRoleDao.getTenantRolesForUser(user) >> [tenantRole] // retrieves no user sourced roles
        1 * applicationService.getCachedClientRoleById(tenantRole.roleRsId) >> icr

        and: "returns user sourced assignment with no tenants"
        assignments != null
        assignments.user == user
        assignments.sourcedRoleAssignments != null
        assignments.sourcedRoleAssignments.size() == 1
        assignments.sourcedRoleAssignments[0].role == icr
        CollectionUtils.isEmpty(assignments.sourcedRoleAssignments[0].tenantIds)
        assignments.sourcedRoleAssignments[0].sources != null
        assignments.sourcedRoleAssignments[0].sources.size() == 1
        assignments.sourcedRoleAssignments[0].sources[0].sourceType == SourcedRoleAssignments.SourceType.USER
        assignments.sourcedRoleAssignments[0].sources[0].sourceId == user.id
        assignments.sourcedRoleAssignments[0].sources[0].assignmentType == SourcedRoleAssignments.AssignmentType.DOMAIN
        CollectionUtils.isEmpty(assignments.sourcedRoleAssignments[0].sources[0].tenantIds)

        where:
        roleName << IdentityUserTypeEnum.userTypeRoleNames
    }

    @Unroll
    def "getSourcedRoleAssignmentsForUser: Retrieves tenant roles assigned to user"() {
        given:
        def user = new User().with {
            it.id = "id"
            it.domainId = "domainId"
            it
        }
        def domain = new Domain().with {
            it.domainId = user.domainId
            it.tenantIds = tenantIds
            it
        }
        domainService.checkAndGetDomain(user.domainId) >> domain
        domainService.getDomain(user.getDomainId()) >> domain // Called by logic for created tenant access
        def tenantRole = createTenantRole("roleId")
        def icr = createImmutableCrFromTenantRole(IdentityUserTypeEnum.DEFAULT_USER.roleName, tenantRole)

        when:
        SourcedRoleAssignments assignments = service.getSourcedRoleAssignmentsForUser(user)

        then:
        // retrieves user sourced roles
        1 * tenantRoleDao.getTenantRolesForUser(user) >> [tenantRole]

        // Looks up the client role from cache associated to tenant role
        1 * applicationService.getCachedClientRoleById(tenantRole.roleRsId) >> icr

        and: "returns user sourced assignment with appropriate tenants"
        assignments != null
        assignments.user == user
        assignments.sourcedRoleAssignments != null
        assignments.sourcedRoleAssignments.size() == 1
        assignments.sourcedRoleAssignments[0].role == icr
        CollectionUtils.isEqualCollection(assignments.sourcedRoleAssignments[0].tenantIds, Arrays.asList(domain.tenantIds))
        assignments.sourcedRoleAssignments[0].sources != null
        assignments.sourcedRoleAssignments[0].sources.size() == 1
        assignments.sourcedRoleAssignments[0].sources[0].sourceType == SourcedRoleAssignments.SourceType.USER
        assignments.sourcedRoleAssignments[0].sources[0].sourceId == user.id
        assignments.sourcedRoleAssignments[0].sources[0].assignmentType == SourcedRoleAssignments.AssignmentType.DOMAIN
        CollectionUtils.isEqualCollection(assignments.sourcedRoleAssignments[0].sources[0].tenantIds, Arrays.asList(domain.tenantIds))

        where:
        tenantIds << [["1", "2", "3"] as String[]]
    }

    @Unroll
    def "getSourcedRoleAssignmentsForUser: Retrieves roles assigned to groups to which user is member"() {
        given:
        def user = new User().with {
            it.id = "id"
            it.domainId = "domainId"
            it
        }
        def domain = new Domain().with {
            it.domainId = user.domainId
            it.tenantIds = ["tenant1"]
            it
        }
        domainService.checkAndGetDomain(user.domainId) >> domain
        domainService.getDomain(user.getDomainId()) >> domain // Called by logic for created tenant access
        def tenantRole = createTenantRole("roleId")
        def icr = createImmutableCrFromTenantRole(IdentityUserTypeEnum.DEFAULT_USER.roleName, tenantRole)

        new UserGroup().with {
            it.id = "ug1"
            it.domainId = ""
        }

        when:
        SourcedRoleAssignments assignments = service.getSourcedRoleAssignmentsForUser(user)

        then:
        // retrieves user sourced roles
        1 * tenantRoleDao.getTenantRolesForUser(user) >> [tenantRole]

        // Looks up the client role from cache associated to tenant role
        1 * applicationService.getCachedClientRoleById(tenantRole.roleRsId) >> icr

        and: "returns user sourced assignment with appropriate tenants"
        assignments != null
        assignments.user == user
        assignments.sourcedRoleAssignments != null
        assignments.sourcedRoleAssignments.size() == 1
        assignments.sourcedRoleAssignments[0].role == icr
        CollectionUtils.isEqualCollection(assignments.sourcedRoleAssignments[0].tenantIds, Arrays.asList(domain.tenantIds))
        assignments.sourcedRoleAssignments[0].sources != null
        assignments.sourcedRoleAssignments[0].sources.size() == 1
        assignments.sourcedRoleAssignments[0].sources[0].sourceType == SourcedRoleAssignments.SourceType.USER
        assignments.sourcedRoleAssignments[0].sources[0].sourceId == user.id
        assignments.sourcedRoleAssignments[0].sources[0].assignmentType == SourcedRoleAssignments.AssignmentType.DOMAIN
        CollectionUtils.isEqualCollection(assignments.sourcedRoleAssignments[0].sources[0].tenantIds, Arrays.asList(domain.tenantIds))

        where:
        userGroups << [["1", "2", "3"] as String[]]
    }

    TenantRole createTenantRole(String roleId = "roleId_" + RandomStringUtils.randomAlphabetic(10)) {
        new TenantRole().with {
            it.roleRsId = roleId
            it
        }
    }

    ImmutableClientRole createImmutableCrFromTenantRole(String roleName = "rolename_" + RandomStringUtils.randomAlphabetic(10), TenantRole tr) {
        ClientRole cr = new ClientRole().with {
            it.name = roleName
            it.id = tr.roleRsId
            it
        }
        ImmutableClientRole imr = new ImmutableClientRole(cr)
        return imr
    }

    def createImmutableClientRole(String name, int weight = 1000) {
        return new ImmutableClientRole(new ClientRole().with {
            it.name = name
            it.id = name
            it.clientId="clientId"
            it.roleType = STANDARD
            it.rsWeight = weight
            it
        })
    }

    def mockFederatedUserDao(service) {
        mockFederatedUserDao = Mock(FederatedUserDao)
        service.federatedUserDao = mockFederatedUserDao
    }
}