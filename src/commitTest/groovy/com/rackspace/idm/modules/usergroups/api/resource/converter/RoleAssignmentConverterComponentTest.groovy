package com.rackspace.idm.modules.usergroups.api.resource.converter

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.impl.DefaultTenantAssignmentService
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class RoleAssignmentConverterComponentTest extends Specification {
    @Shared RoleAssignmentConverter converter

    @Shared
    ApplicationService applicationService

    void setup() {
        converter = new RoleAssignmentConverter()

        applicationService = Mock()
        converter.applicationService = applicationService
    }

    @Unroll
    def "toRoleAssignmentWeb: w/ various tenants: tenants: #tenants"() {

        TenantRole tenantRole = new TenantRole().with {
            it.tenantIds = tenants
            it.name = "roleName"
            it.description = "description"
            it.clientId = "clientId"
            it.roleRsId = "roleId"
            it.types = new Types().with {
                it.type = ["type1"]
                it
            }
            it
        }

        when:
        RoleAssignment roleAssignment = converter.toRoleAssignmentWeb(tenantRole)

        then:
        roleAssignment.onRole == tenantRole.roleRsId
        roleAssignment.onRoleName == tenantRole.name
        CollectionUtils.isEqualCollection(roleAssignment.forTenants, CollectionUtils.isEmpty(tenants) ? [DefaultTenantAssignmentService.ALL_TENANTS_IN_DOMAIN_WILDCARD] as Set : tenants)

        where:
        tenants << [[] as Set, ["a"] as Set, ["a", "b"] as Set]
    }

    @Unroll
    def "toRoleAssignmentWeb: uses provided name on tenant role if provided: providedName: #providedRoleName"() {

        TenantRole tenantRole = new TenantRole().with {
            it.name = providedRoleName
            it.description = "description"
            it.clientId = "clientId"
            it.roleRsId = "roleId"
            it.types = new Types().with {
                it.type = ["type1"]
                it
            }
            it
        }

        when:
        RoleAssignment roleAssignment = converter.toRoleAssignmentWeb(tenantRole)

        then:
        0 * applicationService.getCachedClientRoleById(_)
        roleAssignment.onRoleName == providedRoleName

        where:
        [providedRoleName] << [["a", "anothervalue"]].combinations()
    }

    /**
     * Note - a set of tests will have cachedRole == null. The test name in these cases will include
     * '...cachedRole: #Error:cachedRole.name' as spock attempts to lookup the name of the cachedRole for the method name.
     *
     * Since the point is just to be able to identify which variables would cause a failure, this, while ugly, identifies
     * the test cases where cachedRole == null
     *
     */
    @Unroll
    def "toRoleAssignmentWeb: populates role name from cache when tenant role doesn't contain value: #providedRoleName ; cachedRole: #cachedRole.name"() {

        TenantRole tenantRole = new TenantRole().with {
            it.name = providedRoleName
            it.description = "description"
            it.clientId = "clientId"
            it.roleRsId = cachedRole != null ? cachedRole.id : "roleId"
            it.types = new Types().with {
                it.type = ["type1"]
                it
            }
            it
        }

        when:
        RoleAssignment roleAssignment = converter.toRoleAssignmentWeb(tenantRole)

        then:
        1 * applicationService.getCachedClientRoleById(tenantRole.roleRsId) >> cachedRole
        if (cachedRole != null) {
            roleAssignment.onRoleName == cachedRole.name
        } else {
            roleAssignment.onRoleName == providedRoleName
        }

        where:
        [providedRoleName, cachedRole] << [[null, ""], [createImmutableClientRole("roleId","roleName"), createImmutableClientRole("roleId",""), null]].combinations()
    }

    ImmutableClientRole createImmutableClientRole(String id, String name) {
        return new ImmutableClientRole(new ClientRole().with {
            it.id = id
            it.name = name
            it
        })

    }

    def "toTenantAssignments: converts tenant role list from entity to web"() {
        List<TenantRole> tenantRoles = new ArrayList<>()
        4.times { index ->
            TenantRole tenantRole = new TenantRole().with {
                it.tenantIds = [RandomStringUtils.randomAlphanumeric(5)] as Set
                it.name = "roleName"
                it.roleRsId = "roleId"
                it
            }
            tenantRoles.add(tenantRole)
        }

        when:
        TenantAssignments tenantAssignments = converter.toTenantAssignmentsWeb(tenantRoles)

        then:
        tenantAssignments.tenantAssignment.size() == tenantRoles.size()
        tenantAssignments.tenantAssignment.eachWithIndex {tenantAssignment, index ->
            def tenantRole = tenantRoles.get(index)
            tenantAssignment.onRole == tenantRole.roleRsId
            tenantAssignment.onRoleName == tenantRole.name
            CollectionUtils.isEqualCollection(tenantAssignment.forTenants, tenantRole.tenantIds)
        }
    }

    def "toRoleAssignments: converts tenant role list from entity to web"() {
        List<TenantRole> tenantRoles = new ArrayList<>()
        4.times { index ->
            TenantRole tenantRole = new TenantRole().with {
                it.tenantIds = [RandomStringUtils.randomAlphanumeric(5)] as Set
                it.name = "roleName"
                it.roleRsId = "roleId"
                it
            }
            tenantRoles.add(tenantRole)
        }

        when:
        RoleAssignments roleAssignments = converter.toRoleAssignmentsWeb(tenantRoles)

        then:
        roleAssignments.tenantAssignments != null
        TenantAssignments tenantAssignments = roleAssignments.tenantAssignments
        tenantAssignments.tenantAssignment.size() == tenantRoles.size()
        tenantAssignments.tenantAssignment.eachWithIndex {tenantAssignment, index ->
            def tenantRole = tenantRoles.get(index)
            tenantAssignment.onRole == tenantRole.roleRsId
            tenantAssignment.onRoleName == tenantRole.name
            CollectionUtils.isEqualCollection(tenantAssignment.forTenants, tenantRole.tenantIds)
        }
    }

    def "fromSourcedRoleAssignmentsToRoleAssignmentsWeb: converts SourcedRoleAssignments entity to web representation"() {
        EndUser user = new User().with {
            it.id = "aUserId"
            it
        }
        def tenants = ["tenantId"] as Set

        SourcedRoleAssignments sourcedRoleAssignments = new SourcedRoleAssignments(user)
        ImmutableClientRole role = createRandomImmutableRole()
        sourcedRoleAssignments.addUserSourcedAssignment(role, RoleAssignmentType.DOMAIN, tenants)

        when:
        RoleAssignments roleAssignments = converter.fromSourcedRoleAssignmentsToRoleAssignmentsWeb(sourcedRoleAssignments)

        then:
        roleAssignments.tenantAssignments != null
        TenantAssignments tenantAssignments = roleAssignments.tenantAssignments
        tenantAssignments.tenantAssignment.size() == 1
        TenantAssignment tenantAssignment = tenantAssignments.tenantAssignment[0]
        tenantAssignment.sources.source.size() == 1
        AssignmentSource source = tenantAssignment.sources.source[0]
        source.assignmentType == AssignmentTypeEnum.DOMAIN
        source.sourceType == SourceTypeEnum.USER
        source.sourceId == user.id
        CollectionUtils.isEqualCollection(source.forTenants, tenants)
    }

    def "fromSourcedRoleAssignmentsToRoleAssignmentsWeb: converts SourcedRoleAssignments entity to web representation w/ multiple sources"() {
        EndUser user = new User().with {
            it.id = "aUserId"
            it
        }
        def tenants = ["tenantId"] as Set

        SourcedRoleAssignments sourcedRoleAssignments = new SourcedRoleAssignments(user)
        ImmutableClientRole role = createRandomImmutableRole()
        sourcedRoleAssignments.addUserSourcedAssignment(role, RoleAssignmentType.DOMAIN, tenants)
        sourcedRoleAssignments.addUserGroupSourcedAssignment(role, "groupId", RoleAssignmentType.TENANT, tenants)

        when:
        RoleAssignments roleAssignments = converter.fromSourcedRoleAssignmentsToRoleAssignmentsWeb(sourcedRoleAssignments)

        then:
        roleAssignments.tenantAssignments != null
        TenantAssignments tenantAssignments = roleAssignments.tenantAssignments
        tenantAssignments.tenantAssignment.size() == 1
        TenantAssignment tenantAssignment = tenantAssignments.tenantAssignment[0]
        tenantAssignment.sources.source.size() == 2
        AssignmentSource userSource = tenantAssignment.sources.source.find {it.sourceType == SourceTypeEnum.USER}
        userSource != null
        userSource.assignmentType == AssignmentTypeEnum.DOMAIN
        userSource.sourceType == SourceTypeEnum.USER
        userSource.sourceId == user.id
        CollectionUtils.isEqualCollection(userSource.forTenants, tenants)
        AssignmentSource groupSource = tenantAssignment.sources.source.find {it.sourceType == SourceTypeEnum.USERGROUP}
        groupSource != null
        groupSource.assignmentType == AssignmentTypeEnum.TENANT
        groupSource.sourceType == SourceTypeEnum.USERGROUP
        groupSource.sourceId == "groupId"
        CollectionUtils.isEqualCollection(groupSource.forTenants, tenants)
    }

    def "fromSourcedRoleAssignmentsToRoleAssignmentsWeb: Returns empty assignments when passed null"() {
        when:
        RoleAssignments roleAssignments = converter.fromSourcedRoleAssignmentsToRoleAssignmentsWeb(null)

        then:
        roleAssignments.tenantAssignments != null
        TenantAssignments tenantAssignments = roleAssignments.tenantAssignments
        tenantAssignments.tenantAssignment.size() == 0
    }

    ImmutableClientRole createRandomImmutableRole() {
        new ImmutableClientRole(new ClientRole().with { cr ->
            cr.id = UUID.randomUUID().toString()
            cr.name = RandomStringUtils.randomAlphabetic(15)
            cr.roleType = RoleTypeEnum.STANDARD
            cr
        })
    }
}
