package com.rackspace.idm.domain.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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

        Source source = new Source(SourceType.USER_GROUP, groupId, assignmentType, tenantIds);
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
        USER, USER_GROUP, SYSTEM
    }

    public enum AssignmentType {
        RCN, DOMAIN, TENANT
    }
}
