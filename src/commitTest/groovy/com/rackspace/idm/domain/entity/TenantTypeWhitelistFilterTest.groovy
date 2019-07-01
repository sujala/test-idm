package com.rackspace.idm.domain.entity

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.entity.SourcedRoleAssignments.SourcedRoleAssignment
import org.apache.commons.collections4.CollectionUtils
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

class TenantTypeWhitelistFilterTest extends RootServiceTest {

    TenantTypeWhitelistFilter service

    @Shared
    def role1 = new ImmutableClientRole(entityFactory.createClientRole("role1").with {
        it.id = it.name
        it.roleType = RoleTypeEnum.PROPAGATE
        it
    })

    @Shared
    def role2 = new ImmutableClientRole(entityFactory.createClientRole("role2").with {
        it.id = it.name
        it.roleType = RoleTypeEnum.PROPAGATE

        it
    })

    @Shared
    def role3 = new ImmutableClientRole(entityFactory.createClientRole("role3").with {
        it.id = it.name
        it.roleType = RoleTypeEnum.STANDARD
        it
    })

    @Shared
    def role4Rcn = new ImmutableClientRole(entityFactory.createClientRole("role4").with {
        it.id = it.name
        it.roleType = RoleTypeEnum.RCN
        it
    })

    @Shared
    def tType1 = "type1"

    @Shared
    def tType2 = "type2"

    @Shared
    def tType3 = "type3"

    @Shared
    def t1 = "t1"

    @Shared
    def t2 = "t2"

    @Shared
    def t3 = "t3"

    def setup() {
        service = new TenantTypeWhitelistFilter()
        mockIdentityConfig(service)
        mockTenantService(service)
    }

    def "apply: retrievesWhitelistedTenantTypes"(){
        User user = entityFactory.createUser()
        SourcedRoleAssignments originalAssignments = new SourcedRoleAssignments(user)

        when:
        service.apply(originalAssignments)

        then:
        1 * reloadableConfig.getTenantTypeRoleWhitelistFilterMap() >> new HashMap<>()
    }

    def "apply: When no whitelist map, tenant types are not inferred"(){
        User user = entityFactory.createUser()
        SourcedRoleAssignments originalAssignments = new SourcedRoleAssignments(user)

        originalAssignments.addUserSourcedAssignment(role1, RoleAssignmentType.DOMAIN, [t1, t2] as Set)

        when:
        def finalAssignments = service.apply(originalAssignments)

        then:
        1 * reloadableConfig.getTenantTypeRoleWhitelistFilterMap() >> new HashMap<>()
        0 * tenantService.inferTenantTypeForTenantId(_)

        and: "Assignments are the same"
        finalAssignments == originalAssignments
    }

    def "apply: When whitelist contains a tenant type, must infer tenant types for all tenants"(){
        User user = entityFactory.createUser()
        SourcedRoleAssignments originalAssignments = new SourcedRoleAssignments(user)

        originalAssignments.addUserSourcedAssignment(role1, RoleAssignmentType.DOMAIN, [t1, t2] as Set)

        when:
        def finalAssignments = service.apply(originalAssignments)

        then:
        1 * reloadableConfig.getTenantTypeRoleWhitelistFilterMap() >> [a:[] as Set]

        // The tenant type of each tenentId is retrieved
        1 * tenantService.inferTenantTypeForTenantId(t1, _) >> null
        1 * tenantService.inferTenantTypeForTenantId(t2, _) >> null

        and: "Assignments are the same when no tenants are hidden"
        finalAssignments == originalAssignments
    }

    def "apply: When tenant type matches whitelist and is assigned role, tenant is kept"(){
        User user = entityFactory.createUser()
        SourcedRoleAssignments originalAssignments = new SourcedRoleAssignments(user)

        def whitelistMap = [(tType1):[role1.name] as Set]

        originalAssignments.addUserSourcedAssignment(role1, RoleAssignmentType.DOMAIN, [t1, t2] as Set)

        when:
        def finalAssignments = service.apply(originalAssignments)

        then:
        reloadableConfig.getTenantTypeRoleWhitelistFilterMap() >> whitelistMap
        tenantService.inferTenantTypeForTenantId(_) >> tType1

        and: "Assignments are the same"
        finalAssignments == originalAssignments
    }

    def "apply: When all tenants should be removed due to failure to match whitelist"(){
        User user = entityFactory.createUser()
        SourcedRoleAssignments originalAssignments = new SourcedRoleAssignments(user)

        // All tenants require role2 to be assigned
        tenantService.inferTenantTypeForTenantId(_, _) >> tType1
        reloadableConfig.getTenantTypeRoleWhitelistFilterMap() >> [(tType1):[role2.name] as Set]

        originalAssignments.addUserSourcedAssignment(role1, RoleAssignmentType.DOMAIN, [t1, t2] as Set)

        when:
        def finalAssignments = service.apply(originalAssignments)

        then:
        finalAssignments.getSourcedRoleAssignments().size() == originalAssignments.getSourcedRoleAssignments().size()
        for (SourcedRoleAssignment expectedAssignment : originalAssignments.sourcedRoleAssignments ) {
            assert finalAssignments.getSourcedRoleAssignments().find {isSourceAssignmentEqualExceptMissingTenants(it, expectedAssignment, [t1, t2] as Set)} != null
        }    }

    /**
     * Tests various aspects of filtering.
     * t1's tenant type requires the tenant to be assigned the role 'role2' in some fashion. It's not, so it must be purged.
     * t2's tenant type is null, so it will not be removed from any tenant
     * t3's tenant type requires the tenent to be assigned the role 'role3', which it is so it's not removed
     */
    def "apply: Only those tenants that should be removed are removed"(){
        User user = entityFactory.createUser()
        SourcedRoleAssignments originalAssignments = new SourcedRoleAssignments(user)

        def whitelistMap = [(tType1):[role2.name] as Set, (tType2):[role3.name] as Set]

        originalAssignments.addUserSourcedAssignment(role1, RoleAssignmentType.DOMAIN, [t1, t2] as Set)
        originalAssignments.addUserSourcedAssignment(role2, RoleAssignmentType.DOMAIN, [t2,t3] as Set)
        originalAssignments.addUserSourcedAssignment(role3, RoleAssignmentType.TENANT, [t1,t2,t3] as Set)

        tenantService.inferTenantTypeForTenantId(t1, _) >> tType1
        tenantService.inferTenantTypeForTenantId(t2, _) >> null
        tenantService.inferTenantTypeForTenantId(t3, _) >> tType2

        when:
        def finalAssignments = service.apply(originalAssignments)

        then:
        1 * reloadableConfig.getTenantTypeRoleWhitelistFilterMap() >> whitelistMap

        and: "Tenants are removed"
        finalAssignments.getSourcedRoleAssignments().size() == originalAssignments.getSourcedRoleAssignments().size()
        for (SourcedRoleAssignment expectedAssignment : originalAssignments.sourcedRoleAssignments ) {
            assert finalAssignments.getSourcedRoleAssignments().find {isSourceAssignmentEqualExceptMissingTenants(it, expectedAssignment, [t1] as Set)} != null
        }
    }

    @Unroll
    def "apply: When whitelist is #whitelist, tenants #tenantsRemoved are appropriately removed"(){
        User user = entityFactory.createUser()
        SourcedRoleAssignments originalAssignments = new SourcedRoleAssignments(user)

        originalAssignments.addUserSourcedAssignment(role1, RoleAssignmentType.DOMAIN, [t1,t2] as Set)
        originalAssignments.addUserSourcedAssignment(role1, RoleAssignmentType.TENANT, [t1,t2] as Set)

        originalAssignments.addUserSourcedAssignment(role2, RoleAssignmentType.DOMAIN, [t2,t3] as Set)
        originalAssignments.addUserSourcedAssignment(role2, RoleAssignmentType.TENANT, [t2,t3] as Set)

        originalAssignments.addUserSourcedAssignment(role3, RoleAssignmentType.TENANT, [t1,t2,t3] as Set)

        tenantService.inferTenantTypeForTenantId(t1, _) >> tType1
        tenantService.inferTenantTypeForTenantId(t2, _) >> null
        tenantService.inferTenantTypeForTenantId(t3, _) >> tType2

        reloadableConfig.getTenantTypeRoleWhitelistFilterMap() >> whitelist

        when:
        def finalAssignments = service.apply(originalAssignments)

        then:
        if (CollectionUtils.isEmpty(tenantsRemoved)) {
            assert finalAssignments == originalAssignments
        } else {
            assert finalAssignments.getSourcedRoleAssignments().size() == originalAssignments.getSourcedRoleAssignments().size()
            for (SourcedRoleAssignment expectedAssignment : originalAssignments.sourcedRoleAssignments ) {
                assert finalAssignments.getSourcedRoleAssignments().find {isSourceAssignmentEqualExceptMissingTenants(it, expectedAssignment, tenantsRemoved)} != null
            }
        }

        where:
        whitelist | tenantsRemoved
        [:] | [] as Set
        [(tType1):[role1.name] as Set] | [] as Set
        [(tType1):[role2.name] as Set, (tType2):[role3.name] as Set] | [t1] as Set
        [(tType2):[role1.name] as Set] | [t3] as Set // t3 is not assigned role 1 so is removed
        [(tType3):[role1.name] as Set] | [] as Set
    }

    /**
     * Tests various aspects of filtering.
     * t1's tenant type requires the tenant to be assigned the role 'role2' in some fashion. It's not, so it must be purged.
     * t2's tenant type is null, so it will not be removed from any tenant
     * t3's tenant type requires the tenent to be assigned the role 'role3', which it is so it's not removed
     */
    def "apply: When a tenant is assigned any RCN role, tenant is always visible"(){
        User user = entityFactory.createUser()
        SourcedRoleAssignments originalAssignments = new SourcedRoleAssignments(user)

        tenantService.inferTenantTypeForTenantId(t1) >> tType1
        tenantService.inferTenantTypeForTenantId(t2) >> null
        tenantService.inferTenantTypeForTenantId(t3) >> tType2
        reloadableConfig.getTenantTypeRoleWhitelistFilterMap() >> [(tType1):[role1.name] as Set]

        originalAssignments.addUserSourcedAssignment(role1, RoleAssignmentType.DOMAIN, [t2] as Set)
        originalAssignments.addUserSourcedAssignment(role4Rcn, RoleAssignmentType.DOMAIN, [t1,t2,t3] as Set)

        when:
        def finalAssignments = service.apply(originalAssignments)

        then:
        finalAssignments == originalAssignments
    }

    boolean isSourceAssignmentEqualExceptMissingTenants(SourcedRoleAssignments.SourcedRoleAssignment actual, SourcedRoleAssignments.SourcedRoleAssignment expected, Set<String> missingTenants = [] as Set) {
        Set expectedTenantIds = expected.getTenantIds()
        expectedTenantIds.removeAll(missingTenants)

        boolean isequal = (CollectionUtils.isEqualCollection(actual.getTenantIds(), expectedTenantIds)
        && actual.role == expected.role
        && actual.sources.size() == expected.sources.size())

        if (isequal) {
            for (RoleAssignmentSource expectedSource : expected.sources) {
                isequal = isequal && actual.getSources().find {isSourcesEqualExceptMissingTenants(it, expectedSource, missingTenants)} != null
            }
        }
        return isequal
    }

    void assertSourceAssignmentExceptMissingTenants(SourcedRoleAssignments.SourcedRoleAssignment actual, SourcedRoleAssignments.SourcedRoleAssignment expected, Set<String> missingTenants = [] as Set) {
        Set expectedTenantIds = expected.getTenantIds()
        expectedTenantIds.removeAll(missingTenants)

        assert CollectionUtils.isEqualCollection(actual.getTenantIds(), expectedTenantIds)
        assert actual.role == expected.role
        assert actual.sources.size() == expected.sources.size()

        for (RoleAssignmentSource expectedSource : expected.sources) {
            assert actual.getSources().find {isSourcesEqualExceptMissingTenants(it, expectedSource, missingTenants)} != null
        }
    }

    boolean isSourcesEqualExceptMissingTenants(RoleAssignmentSource actual, RoleAssignmentSource expected, Set missingTenants = [] as Set) {
        Set expectedTenantIds = expected.getTenantIds()
        expectedTenantIds.removeAll(missingTenants)

        return (actual.assignmentType == expected.assignmentType
                && actual.sourceId == expected.sourceId
                && actual.sourceType == expected.sourceType
                && CollectionUtils.isEqualCollection(actual. tenantIds, expectedTenantIds))
    }
}
