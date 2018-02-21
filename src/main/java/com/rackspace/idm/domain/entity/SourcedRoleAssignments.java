package com.rackspace.idm.domain.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Types;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;

import java.util.*;

/**
 * Provides a list of roles assigned to a user with metadata on why/how the user is assigned those roles. Instances of
 * this class shouldn't be created directly, but through one of the associated builders to ensure the appropriate logic
 * for calculating roles is applied.
 *
 * @see EndUserDenormalizedSourcedRoleAssignmentsBuilder
 * @see RackerSourcedRoleAssignmentsBuilder
 */
public class SourcedRoleAssignments {
    @Getter
    private BaseUser user;
    private HashMap<String, SourcedRoleAssignment> sourcedRoleAssignments = new HashMap<>();

    /**
     * default modifier to prevent direct instantiation. Use one of the associated builders.
     * @param user
     */
    SourcedRoleAssignments(BaseUser user) {
        this.user = user;
    }

    public Set<SourcedRoleAssignment> getSourcedRoleAssignments() {
        return ImmutableSet.copyOf(sourcedRoleAssignments.values());
    }

    /**
     * Returns a set of source role assignments filtered by a tenantId.
     */
    public Set<SourcedRoleAssignment> getSourcedRoleAssignmentsOnTenant(String tenantId, Set<String> domainTenants) {
        Set<SourcedRoleAssignment> finalSourcedRoleAssignments = new HashSet<>();
        // List of tenantIds that belong to user's domain
        Set<String> validTenantsOnDomain = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        validTenantsOnDomain.addAll(domainTenants);

        for (SourcedRoleAssignment sa : sourcedRoleAssignments.values()) {
            SourcedRoleAssignment sourcedRoleAssignment = null;
            for (Source s : sa.getSources()) {
                Set<String> sourceTenantIds = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                sourceTenantIds.addAll(s.tenantIds);
                /* Determine if a source for assignment need to be created by passing one of the following checks:
                 * 1. Verify tenantId belongs to the list of domain tenants. (global roles)
                 * 2. Verify that the list of tenants on source contains the tenantId.
                 */
                if ((sourceTenantIds.isEmpty() && validTenantsOnDomain.contains(tenantId)) || sourceTenantIds.contains(tenantId)) {
                    Source source = new Source(s.sourceType, s.sourceId, s.assignmentType, Sets.newHashSet(tenantId));
                    sourcedRoleAssignment = new SourcedRoleAssignment(sa.role, source);
                }
            }

            // Add sourceRoleAssignment if the list of sources is not emtpy.
            if (sourcedRoleAssignment != null && !sourcedRoleAssignment.getSources().isEmpty()) {
                finalSourcedRoleAssignments.add(sourcedRoleAssignment);
            }
        }

        return finalSourcedRoleAssignments;
    }

    public IdentityUserTypeEnum getUserTypeFromAssignedRoles() {
        IdentityUserTypeEnum finalUserType = null;
        for (SourcedRoleAssignment sourcedRoleAssignment : sourcedRoleAssignments.values()) {
            ImmutableClientRole role = sourcedRoleAssignment.getRole();
            IdentityUserTypeEnum roleUserType = IdentityUserTypeEnum.fromRoleName(role.getName());
            if (roleUserType != null) {
                if (finalUserType == null) {
                    finalUserType = roleUserType;
                } else if (roleUserType.getLevel().isLowerWeightThan(finalUserType.getLevel())) {
                    finalUserType = roleUserType;
                }
            }
        }
        return finalUserType;
    }


    /**
     * Represents each {@link SourcedRoleAssignment} as a TenantRole through the {@link SourcedRoleAssignment#asTenantRole()}.
     *
     * See the associated documentation in {@link SourcedRoleAssignment#asTenantRole()} to understand the tenantRole
     * representations. However, as a {@link SourcedRoleAssignments} is user aware, when using this method the generated
     * tenant roles <b>will</b> be populated with the associated userId of user. However, as mentioned in the conversion
     * documentation no determinations should be made determining how the user received the assignment.
     *
     * tenantIds
     * @return
     */
    public List<TenantRole> asTenantRoles() {
        List<TenantRole> tenantRoles = new ArrayList<>(sourcedRoleAssignments.size());
        for (SourcedRoleAssignment sourcedRoleAssignment : sourcedRoleAssignments.values()) {
            TenantRole tr = sourcedRoleAssignment.asTenantRole();
            tr.setUserId(user.getId());
            tenantRoles.add(tr);
        }
        return tenantRoles;
    }

    /**
     * Similar to {@link #asTenantRoles()}, but excludes roles the user is assigned, but is not associated with any tenant.
     *
     * This is appropriate for callers that assume all tenant roles w/ an empty set of tenantIds means the user has the role
     * on all tenants within the user's domain.
     *
     * @return
     */
    public List<TenantRole> asTenantRolesExcludeNoTenants() {
        List<TenantRole> allTenantRoles = asTenantRoles();

        List<TenantRole> finalTenantRoles = new ArrayList<>();
        for (TenantRole tenantRole : allTenantRoles) {
            if (CollectionUtils.isNotEmpty(tenantRole.getTenantIds())) {
                finalTenantRoles.add(tenantRole);
            }
        }

        return finalTenantRoles;
    }

    /**
     * Must provide explicit tenantIds since wildcard is not allowed.
     *
     * @param role
     */
    Source addUserSourcedAssignment(ImmutableClientRole role, AssignmentType assignmentType, Set<String> tenantIds) {
        Validate.notNull(role);
        Validate.notNull(role.getId());

        Source source = new Source(SourceType.USER, user.getId(), assignmentType, tenantIds);
        addSourceForRole(role, source);
        return source;
    }

    Source addUserGroupSourcedAssignment(ImmutableClientRole role, String groupId, AssignmentType assignmentType, Set<String> tenantIds) {
        Validate.notNull(role);
        Validate.notNull(role.getId());

        Source source = new Source(SourceType.USERGROUP, groupId, assignmentType, tenantIds);
        addSourceForRole(role, source);
        return source;
    }

    Source addSystemSourcedAssignment(ImmutableClientRole role, String systemId, AssignmentType assignmentType, Set<String> tenantIds) {
        Validate.notNull(role);
        Validate.notNull(role.getId());

        Source source = new Source(SourceType.SYSTEM, systemId, assignmentType, tenantIds);
        addSourceForRole(role, source);
        return source;
    }

    void addSourceForRole(ImmutableClientRole role, Source source) {
        SourcedRoleAssignment ra = sourcedRoleAssignments.get(role.getId());

        if (ra == null) {
            ra = new SourcedRoleAssignment(role, source);
            sourcedRoleAssignments.put(role.getId(), ra);
        } else {
            ra.addAdditionalSource(source);
        }
    }

    public static class SourcedRoleAssignment {
        @Getter
        private ImmutableClientRole role;
        private List<Source> sources = new ArrayList<>();

        public SourcedRoleAssignment(ImmutableClientRole role, Source initialSource) {
            this.role = role;
            sources.add(initialSource);
        }

        public List<Source> getSources() {
            return ImmutableList.copyOf(sources);
        }

        private void addAdditionalSource(Source source) {
            sources.add(source);
        }

        public Set<String> getTenantIds() {
            Set<String> tenantIds = new HashSet<>();
            for (Source source : sources) {
                tenantIds.addAll(source.tenantIds);
            }
            return tenantIds;
        }

        /**
         * Represents the assignment as a TenantRole with an explicit listing of tenantIds to which the user is assigned
         * the role. Tenant roles are fully populated with information from the associated client role. Callers must not make any
         * assumptions as to how the user received the assignment based on the tenantRole returned. It
         * represents a union of all assignments on the user including dynamic assignments. As such it also does not
         * represent an actual entry in the directory.
         *
         * However, the generated tenant role will <b>never</b> have the following fields populated:
         * <ul>
         *     <li>uniqueId: The generated tenant role does not represent an entry in the directory.</li>
         *     <li>userId: A SourcedRoleAssignment is not aware of the user to which the assignment applies</li>
         * </ul>
         *
         * <p>
         * <b>WARNING</b>: A tenant role returned with an empty set of tenantIds <b>must not</b> be considered a domain assigned
         * role (and therefore assumed to have access to all tenants in domain). Instead this <b>must be</b>
         * interpreted as the user *not* having the role on *any* tenants even though the user is assigned the role. This
         * is possible in cases where the user's domain has no tenants, or all the tenants in the user's domain are
         * "hidden" from the user due to excluding certain tenant types from automatic assignment (see https://jira.rax.io/browse/CID-1194)
         * </p>
         *
         * tenantIds
         * @return
         */
        public TenantRole asTenantRole() {
            TenantRole tr = new TenantRole();

            // Copy over info from client role
            tr.setRoleRsId(role.getId());
            tr.setName(role.getName());
            tr.setClientId(role.getClientId());
            tr.setDescription(role.getDescription());
            tr.setRoleType(role.getRoleType());
            if (CollectionUtils.isNotEmpty(role.getTenantTypes())) {
                Types types = new Types();
                types.getType().addAll(role.getTenantTypes());
                tr.setTypes(types);
            }

            // Set the tenant ids. May be an empty set
            tr.setTenantIds(getTenantIds());

            return tr;
        }

    }

    public static class Source {
        @Getter
        private SourceType sourceType;

        @Getter
        private String sourceId;

        @Getter
        private AssignmentType assignmentType;

        private Set<String> tenantIds = new HashSet<>();

        public Source(SourceType sourceType, String sourceId, AssignmentType assignmentType, Set<String> tenantIds) {
            this.sourceType = sourceType;
            this.sourceId = sourceId;
            this.tenantIds = ImmutableSet.copyOf(tenantIds);
            this.assignmentType = assignmentType;
        }

        public List<String> getTenantIds() {
            return ImmutableList.copyOf(tenantIds);
        }
    }

    public enum SourceType {
        USER, USERGROUP, SYSTEM
    }

    public enum AssignmentType {
        RCN, DOMAIN, TENANT
    }
}
