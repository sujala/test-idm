package com.rackspace.idm.domain.entity

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.rolecalculator.UserRoleLookupService
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class EndUserDenormalizedSourcedRoleAssignmentsBuilderTest extends Specification {

    UserRoleLookupService userRoleLookupService

    def setup() {
        userRoleLookupService = Mock()
    }

    def "endUserBuilder: Must supply user with userId to create builder"() {
        given:
        def domain = new Domain().with {
            it.name = "name"
            it.domainId = "domainId"
            it
        }
        userRoleLookupService.getUserDomain() >> domain

        when: "Supply a null user"
        EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService)

        then: "Invalid"
        (1.._) * userRoleLookupService.getUser() >> null
        thrown(IllegalArgumentException)

        when: "Supply a user without a userId"
        EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService)

        then: "Invalid"
        (1.._) * userRoleLookupService.getUser() >> new User().with {
            it.domainId = domain.domainId
            it
        }
        thrown(IllegalArgumentException)
    }

    /**
     * Ignoring this test while Customer Identity must support users authenticating that are an invalid state and do
     * not have a domain (either not linked to domain or the domain linked to does not exist). This is invalid, but
     * for various legacy reasons we allowed the users to continue authenticating even though they're invalid.
     * @return
     */
    @Ignore
    def "endUserBuilder: Must supply domain to create builder"() {
        given:
        userRoleLookupService.getUser() >> new User().with {
            it.id = "userId"
            it.domainId = "domainId"
            it
        }

        when: "Supply null domain"
        EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService)

        then: "Invalid"
        1 * userRoleLookupService.getUserDomain() >> null

        thrown(IllegalArgumentException)
    }

    def "endUserBuilder: Supplied user and domain must match on domainId to create builder"() {
        given:
        def domain = new Domain().with {
            it.name = "name"
            it.domainId = "domainId"
            it
        }
        userRoleLookupService.getUserDomain() >> domain

        when: "Supply a user with different domainId"
        EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService)

        then: "Invalid"
        (1.._) * userRoleLookupService.getUser() >> new User().with {
            it.domainId = "otherDomainId"
            it.id = "userId"
            it
        }
        thrown(IllegalArgumentException)

        when: "Supply a user and domain with matching domainId"
        EndUserDenormalizedSourcedRoleAssignmentsBuilder builder = EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService)

        then: "Creates builder"
        (1.._) * userRoleLookupService.getUser() >> new User().with {
            it.domainId = domain.domainId
            it.id = "userId"
            it
        }
        notThrown(IllegalArgumentException)
        builder != null
    }

    def "When build, loads all roles and requires user type assignment"() {
        userRoleLookupService.getUserDomain() >> defaultDomain()
        userRoleLookupService.getUser() >> defaultUser()

        when: "Don't supply user type role"
        EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService).build()

        then: "Looks up all roles for user"
        1 * userRoleLookupService.getUserSourcedRoles()
        1 * userRoleLookupService.getGroupSourcedRoles()
        1 * userRoleLookupService.getSystemSourcedRoles()
        1 * userRoleLookupService.getOtherSourcedRoles()

        and: "throws exception"
        thrown(IllegalStateException)
    }

    @Unroll
    def "Recognizes each user type role: #identityUserType"() {
        userRoleLookupService.getUserDomain() >> defaultDomain()
        userRoleLookupService.getUser() >> defaultUser()
        addUserSourceDomainRoles(identityUserType.roleName)
        EndUserDenormalizedSourcedRoleAssignmentsBuilder builder = EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService)

        when: "Build"
        SourcedRoleAssignments assignments = builder.build()

        then: "builds without exception"
        notThrown(IllegalStateException)

        and: "Assignments contains user role"
        assignments.sourcedRoleAssignments.find {it.role.name == identityUserType.roleName} != null

        where:
        identityUserType << IdentityUserTypeEnum.values()
    }

    @Unroll
    def "Builder recognizes highest level user type added"() {
        given:
        userRoleLookupService.getUserDomain() >> defaultDomain()
        userRoleLookupService.getUser() >> defaultUser()
        def roleNames = userTypesToAdd.collect { role ->
            role.roleName
        }
        addUserSourceDomainRoles(roleNames as String[])
        EndUserDenormalizedSourcedRoleAssignmentsBuilder builder = EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService)

        when: "Supply as user source role"
        SourcedRoleAssignments assignments = builder.build()

        then: "user type is correctly determined"
        assignments.getUserTypeFromAssignedRoles() == expectedFinalUserType

        where:
        userTypesToAdd                                                                                          | expectedFinalUserType
        [IdentityUserTypeEnum.DEFAULT_USER, IdentityUserTypeEnum.USER_MANAGER]                                  | IdentityUserTypeEnum.USER_MANAGER
        [IdentityUserTypeEnum.USER_MANAGER, IdentityUserTypeEnum.DEFAULT_USER]                                  | IdentityUserTypeEnum.USER_MANAGER
        [IdentityUserTypeEnum.USER_MANAGER, IdentityUserTypeEnum.USER_ADMIN, IdentityUserTypeEnum.DEFAULT_USER] | IdentityUserTypeEnum.USER_ADMIN
    }

    @Unroll
    def "build: users of type #identityUserType receive domain assigned roles on every tenant in domain when no tenants are hidden"() {
        given:
        def tenantIds = ["p1:t1", "p2:t2", "1234"] as String[]
        def domain = defaultDomain().with {
            it.tenantIds = tenantIds
            it
        }
        userRoleLookupService.getUserDomain() >> domain
        userRoleLookupService.getUser() >> defaultUser()
        userRoleLookupService.getTenantIds() >> tenantIds

        EndUserDenormalizedSourcedRoleAssignmentsBuilder builder = EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService)
        addUserSourceDomainRoles(identityUserType.roleName)

        when:
        SourcedRoleAssignments sourcedRoleAssignments = builder.build()

        then: "result has a single assignment for the single role added"
        sourcedRoleAssignments != null
        sourcedRoleAssignments.sourcedRoleAssignments.size() == 1

        and: "user receives domain role on all tenants"
        sourcedRoleAssignments.sourcedRoleAssignments[0].role.name == identityUserType.roleName
        CollectionUtils.isEqualCollection(sourcedRoleAssignments.sourcedRoleAssignments[0].tenantIds, Arrays.asList(domain.tenantIds))

        and: "source created appropriately"
        def userSource = sourcedRoleAssignments.sourcedRoleAssignments[0].sources[0]
        userSource.sourceType == RoleAssignmentSourceType.USER
        CollectionUtils.isEqualCollection(userSource.tenantIds, Arrays.asList(domain.tenantIds) as Set)
        userSource.assignmentType == RoleAssignmentType.DOMAIN

        where:
        identityUserType << IdentityUserTypeEnum.values()
    }

    @Unroll
    def "build: users of type #identityUserType receive domain assigned roles on every tenant in domain"() {
        given:
        def tenantIds = ["p1:t1", "p2:t2", "1234"] as String[]
        def domain = defaultDomain().with {
            it.tenantIds = tenantIds
            it
        }
        userRoleLookupService.getUserDomain() >> domain
        userRoleLookupService.getUser() >> defaultUser()
        userRoleLookupService.getTenantIds() >> tenantIds

        EndUserDenormalizedSourcedRoleAssignmentsBuilder builder = EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService)
        addUserSourceDomainRoles(identityUserType.roleName)

        when:
        SourcedRoleAssignments sourcedRoleAssignments = builder.build()

        then: "result has a single assignment for the single role added"
        sourcedRoleAssignments != null
        sourcedRoleAssignments.sourcedRoleAssignments.size() == 1

        and: "user receives domain role on all tenants"
        sourcedRoleAssignments.sourcedRoleAssignments[0].role.name == identityUserType.roleName
        CollectionUtils.isEqualCollection(sourcedRoleAssignments.sourcedRoleAssignments[0].tenantIds, Arrays.asList(domain.tenantIds))

        and: "source created appropriately"
        def userSource = sourcedRoleAssignments.sourcedRoleAssignments[0].sources[0]
        userSource.sourceType == RoleAssignmentSourceType.USER
        CollectionUtils.isEqualCollection(userSource.tenantIds, Arrays.asList(domain.tenantIds) as Set)
        userSource.assignmentType == RoleAssignmentType.DOMAIN

        where:
        identityUserType << IdentityUserTypeEnum.values()
    }

    def "build: Adding multiple tenant assigned assignments for same role unions the tenants and creates appropriate sources"() {
        given:
        def tenantIds = ["p1:t1", "p1:t2", "p2:t1"] as String[]
        def user = defaultUser()
        def domain = defaultDomain().with {
            it.tenantIds = tenantIds
            it
        }
        userRoleLookupService.getUserDomain() >> domain
        userRoleLookupService.getUser() >> user
        userRoleLookupService.tenantIds() >> tenantIds

        EndUserDenormalizedSourcedRoleAssignmentsBuilder builder = EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService)

        def userTypeTr = createTenantRole()
        def userTypeIcr = createImmutableCrFromTenantRole(IdentityUserTypeEnum.USER_ADMIN.roleName, userTypeTr)

        def userSourceTr = createTenantRole().with {
            it.tenantIds = ["p1:t1"] as Set
            it
        }
        def icr = createImmutableCrFromTenantRole(userSourceTr)
        def groupSourceTr = createTenantRole().with {
            it.tenantIds = ["p2:t1"] as Set
            it.roleRsId = icr.id
            it
        }
        Map<String, List<TenantRole>> groupAssignments = ["aGroupId":[groupSourceTr]] as Map

        when: "add the same role via multiple assignments"
        SourcedRoleAssignments result = builder.build()

        then:
        1 * userRoleLookupService.getUserSourcedRoles() >> [userTypeTr, userSourceTr]
        1 * userRoleLookupService.getGroupSourcedRoles() >> groupAssignments
        (1.._) * userRoleLookupService.getImmutableClientRole(userTypeIcr.id) >> userTypeIcr
        (1.._) * userRoleLookupService.getImmutableClientRole(icr.id) >> icr

        result.sourcedRoleAssignments.size() == 2 // User type role and custom role assignment

        and: "custom role is associated with union of all tenants assigned by all sources"
        SourcedRoleAssignments.SourcedRoleAssignment assignment = result.sourcedRoleAssignments.find {it.role.name == icr.name}
        assignment != null
        CollectionUtils.isEqualCollection(assignment.tenantIds, ["p1:t1", "p2:t1"] as Set)

        and: "2 sources exist for custom role"
        assignment.sources.size() == 2

        and: "User source contains correct info"
        def userSource = assignment.sources.find {it.sourceId == user.id}
        userSource != null
        userSource.sourceType == RoleAssignmentSourceType.USER
        CollectionUtils.isEqualCollection(userSource.tenantIds, ["p1:t1"] as Set)
        userSource.assignmentType == RoleAssignmentType.TENANT

        and: "group source contains expected info"
        def groupSource = assignment.sources.find {it.sourceId == "aGroupId"}
        groupSource != null
        groupSource.sourceType == RoleAssignmentSourceType.USERGROUP
        CollectionUtils.isEqualCollection(groupSource.tenantIds, ["p2:t1"] as Set)
        userSource.assignmentType == RoleAssignmentType.TENANT
    }

    def "Mixing global source and tenant source results in role on all domain tenants"() {
        given:
        def tenantIds = ["p1:t1", "p1:t2", "p2:t1"] as String[]
        def user = defaultUser()
        def domain = defaultDomain().with {
            it.tenantIds = tenantIds
            it
        }
        userRoleLookupService.getUserDomain() >> domain
        userRoleLookupService.getUser() >> user
        userRoleLookupService.getTenantIds() >> tenantIds

        EndUserDenormalizedSourcedRoleAssignmentsBuilder builder = EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService)

        def userTypeTr = createTenantRole()
        def userTypeIcr = createImmutableCrFromTenantRole(IdentityUserTypeEnum.USER_ADMIN.roleName, userTypeTr)

        // tenant based TR assigned to user
        def userSourceTr = createTenantRole().with {
            it.tenantIds = ["p1:t1"] as Set
            it
        }
        def icr = createImmutableCrFromTenantRole(userSourceTr)

        // globally assigned tr assigned to group
        def groupSourceTr = createTenantRole().with {
            it.roleRsId = icr.id
            it
        }
        Map<String, List<TenantRole>> groupAssignments = ["aGroupId":[groupSourceTr]] as Map

        when: "add the same role via multiple assignments"
        SourcedRoleAssignments result = builder.build()

        then:
        1 * userRoleLookupService.getUserSourcedRoles() >> [userTypeTr, userSourceTr]
        1 * userRoleLookupService.getGroupSourcedRoles() >> groupAssignments
        (1.._) * userRoleLookupService.getImmutableClientRole(userTypeIcr.id) >> userTypeIcr
        (1.._) * userRoleLookupService.getImmutableClientRole(icr.id) >> icr

        and:
        result.user == user
        result.sourcedRoleAssignments.size() == 2 // User type role and custom role assignment

        and: "custom role is associated with all domain tenants due to global assignment"
        SourcedRoleAssignments.SourcedRoleAssignment assignment = result.sourcedRoleAssignments.find {it.role.name == icr.name}
        assignment != null
        CollectionUtils.isEqualCollection(assignment.tenantIds, Arrays.asList(domain.tenantIds))

        and: "2 sources exist for custom role"
        assignment.sources.size() == 2

        and: "User source contains correct info"
        def userSource = assignment.sources.find {it.sourceId == user.id}
        userSource != null
        userSource.sourceType == RoleAssignmentSourceType.USER
        CollectionUtils.isEqualCollection(userSource.tenantIds, ["p1:t1"] as Set)
        userSource.assignmentType == RoleAssignmentType.TENANT

        and: "group source associated with all tenants"
        def groupSource = assignment.sources.find {it.sourceId == "aGroupId"}
        groupSource != null
        groupSource.sourceType == RoleAssignmentSourceType.USERGROUP
        CollectionUtils.isEqualCollection(groupSource.tenantIds, Arrays.asList(domain.tenantIds))
        userSource.assignmentType == RoleAssignmentType.TENANT
    }

    def "System source sets appropriate metadata"() {
        given:
        def user = defaultUser()
        def domain = defaultDomain().with {
            it.tenantIds = ["p1:t1", "p1:t2", "p2:t1"] as String[]
            it
        }
        userRoleLookupService.getUserDomain() >> domain
        userRoleLookupService.getUser() >> user

        EndUserDenormalizedSourcedRoleAssignmentsBuilder builder = EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService)

        def userTypeTr = createTenantRole()
        def userTypeIcr = createImmutableCrFromTenantRole(IdentityUserTypeEnum.USER_ADMIN.roleName, userTypeTr)

        def systemSourceTr = createTenantRole().with {
            it.tenantIds = ["p1:t1"] as Set
            it
        }
        def icr = createImmutableCrFromTenantRole(systemSourceTr)
        Map<String, List<TenantRole>> systemAssignments = ["IDENTITY":[systemSourceTr]] as Map

        when: "add a system role"
        SourcedRoleAssignments result = builder.build()

        then:
        1 * userRoleLookupService.getUserSourcedRoles() >> [userTypeTr]
        1 * userRoleLookupService.getSystemSourcedRoles() >> systemAssignments
        (1.._) * userRoleLookupService.getImmutableClientRole(userTypeIcr.id) >> userTypeIcr
        (1.._) * userRoleLookupService.getImmutableClientRole(icr.id) >> icr

        and: "custom role is associated with only specified tenantId"
        result.sourcedRoleAssignments.size() == 2 // User type role and custom role assignment
        SourcedRoleAssignments.SourcedRoleAssignment assignment = result.sourcedRoleAssignments.find {it.role.name == icr.name}
        assignment != null
        CollectionUtils.isEqualCollection(assignment.tenantIds, ["p1:t1"] as Set)

        and: "1 sources exists for custom role"
        assignment.sources.size() == 1

        and: "System source contains correct info"
        def systemSource = assignment.sources[0]
        systemSource.sourceType == RoleAssignmentSourceType.SYSTEM
        systemSource.sourceId == "IDENTITY"
        CollectionUtils.isEqualCollection(systemSource.tenantIds, ["p1:t1"] as Set)
        systemSource.assignmentType == RoleAssignmentType.TENANT
    }

    def "Other source sets appropriate metadata"() {
        given:
        def user = defaultUser()
        def domain = defaultDomain().with {
            it.tenantIds = ["p1:t1", "p1:t2", "p2:t1"] as String[]
            it
        }
        userRoleLookupService.getUserDomain() >> domain
        userRoleLookupService.getUser() >> user

        EndUserDenormalizedSourcedRoleAssignmentsBuilder builder = EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService)

        def userTypeTr = createTenantRole()
        def userTypeIcr = createImmutableCrFromTenantRole(IdentityUserTypeEnum.USER_ADMIN.roleName, userTypeTr)

        def sourceTr = createTenantRole().with {
            it.tenantIds = ["p1:t1"] as Set
            it
        }
        def icr = createImmutableCrFromTenantRole(sourceTr)
        RoleAssignmentSource otherSource = new RoleAssignmentSource(RoleAssignmentSourceType.DA, "abc", null, Collections.emptySet())
        Map<TenantRole, RoleAssignmentSource> otherSources = [(sourceTr):otherSource] as Map

        when: "Build"
        SourcedRoleAssignments result = builder.build()

        then:
        1 * userRoleLookupService.getUserSourcedRoles() >> [userTypeTr]
        (1.._) * userRoleLookupService.getImmutableClientRole(userTypeIcr.id) >> userTypeIcr
        1 * userRoleLookupService.getOtherSourcedRoles() >> otherSources
        (1.._) * userRoleLookupService.getImmutableClientRole(icr.id) >> icr

        and: "custom role is associated with only specified tenantId"
        result.sourcedRoleAssignments.size() == 2 // User type role and other role assignment
        SourcedRoleAssignments.SourcedRoleAssignment assignment = result.sourcedRoleAssignments.find {it.role.name == icr.name}
        assignment != null
        CollectionUtils.isEqualCollection(assignment.tenantIds, ["p1:t1"] as Set)

        and: "1 source exists for other role"
        assignment.sources.size() == 1

        and: "Other source contains correct info"
        def finalOtherSource = assignment.sources[0]
        finalOtherSource.sourceType == otherSource.sourceType
        finalOtherSource.sourceId == otherSource.sourceId
        CollectionUtils.isEqualCollection(finalOtherSource.tenantIds, ["p1:t1"] as Set)
        finalOtherSource.assignmentType == RoleAssignmentType.TENANT
    }

    def "When RCN role assigned, retrieves all tenants within RCN and grants role on all matching tenants"() {
        given:
        def user = defaultUser()
        def domain = defaultDomain().with {
            it.tenantIds = ["p1:t1", "p1:t2", "p2:t1"] as String[]
            it
        }
        userRoleLookupService.getUserDomain() >> domain
        userRoleLookupService.getUser() >> user
        EndUserDenormalizedSourcedRoleAssignmentsBuilder builder = EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService)

        def userTypeTr = createTenantRole()
        def userTypeIcr = createImmutableCrFromTenantRole(IdentityUserTypeEnum.USER_ADMIN.roleName, userTypeTr)

        def rcnRole = createTenantRole()
        def icr = createImmutableRcnCrFromTenantRole(rcnRole, ["type1"])

        def matchingRcnTenantInDomain = new Tenant().with {
            it.tenantId = "domainMatching"
            it.types = ["type1"] as Set
            it
        }
        def matchingRcnTenantOutsideDomain = new Tenant().with {
            it.tenantId = "matching"
            it.types = ["type1"] as Set
            it
        }
        def nonMatchingRcnTenant = new Tenant().with {
            it.tenantId = "nonMatching"
            it.types = ["type2"] as Set
            it
        }

        when: "when build with assigned rcn role"
        SourcedRoleAssignments result = builder.build()

        then:
        1 * userRoleLookupService.getUserSourcedRoles() >> [userTypeTr, rcnRole]
        1 * userRoleLookupService.calculateRcnTenants() >> [matchingRcnTenantInDomain, matchingRcnTenantOutsideDomain, nonMatchingRcnTenant]

        // looks up immutable roles
        (1.._) * userRoleLookupService.getImmutableClientRole(userTypeIcr.id) >> userTypeIcr
        (1.._) * userRoleLookupService.getImmutableClientRole(icr.id) >> icr

        and: "rcn role is associated with matching tenants"
        result.sourcedRoleAssignments.size() == 2 // User type role and rcn role assignment
        SourcedRoleAssignments.SourcedRoleAssignment assignment = result.sourcedRoleAssignments.find {it.role.name == icr.name}
        assignment != null
        CollectionUtils.isEqualCollection(assignment.tenantIds, [matchingRcnTenantInDomain.tenantId, matchingRcnTenantOutsideDomain.tenantId] as Set)

        and: "1 sources exists for custom role"
        assignment.sources.size() == 1

        and: "RCN source contains correct info"
        def systemSource = assignment.sources[0]
        systemSource.sourceType == RoleAssignmentSourceType.USER
        systemSource.sourceId == user.id
        CollectionUtils.isEqualCollection(systemSource.tenantIds, [matchingRcnTenantInDomain.tenantId, matchingRcnTenantOutsideDomain.tenantId] as Set)
        systemSource.assignmentType == RoleAssignmentType.RCN
    }

    User defaultUser(String domainId = "domainId") {
        return new User().with {
            it.domainId = domainId
            it.id = "userId_" + RandomStringUtils.randomAlphabetic(10)
            it
        }
    }

    Domain defaultDomain(String domainId = "domainId") {
        return new Domain().with {
            it.domainId = domainId
            it
        }
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
            it.roleType = RoleTypeEnum.STANDARD
            it.id = tr.roleRsId
            it
        }
        ImmutableClientRole imr = new ImmutableClientRole(cr)
        return imr
    }

    ImmutableClientRole createImmutableRcnCrFromTenantRole(String roleName = "rolename_" + RandomStringUtils.randomAlphabetic(10), TenantRole tr, List<String> tenantTypes) {
        ClientRole cr = new ClientRole().with {
            it.name = roleName
            it.roleType = RoleTypeEnum.RCN
            it.tenantTypes = tenantTypes
            it.id = tr.roleRsId
            it
        }
        ImmutableClientRole imr = new ImmutableClientRole(cr)
        return imr
    }

    def addUserSourceDomainRoles(String... roleNames) {
        List<TenantRole> tenantRoles = new ArrayList()
        roleNames.each { name ->
            def tr = createTenantRole().with {
                it.name = name
                it
            }
            tenantRoles.add(tr)
        }
        addUserSourceRoles((TenantRole [])tenantRoles.toArray())
    }

    def addUserSourceRoles(TenantRole... trs) {
        def userRoles = new ArrayList()
        trs.each {
            def imr = createImmutableCrFromTenantRole(it.name, it)
            userRoles.add(it)
            userRoleLookupService.getImmutableClientRole(imr.id) >> imr
        }
        userRoleLookupService.getUserSourcedRoles() >> userRoles
    }
}