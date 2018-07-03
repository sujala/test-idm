package com.rackspace.idm.domain.entity;

import com.rackspace.idm.api.security.ImmutableClientRole;
import org.apache.commons.lang.Validate;

import java.util.Collections;

public class RackerSourcedRoleAssignmentsBuilder {
    private Racker user;

    private SourcedRoleAssignments interimSourcedRoleAssignments;

    public static RackerSourcedRoleAssignmentsBuilder rackerBuilder(Racker user) {
        Validate.notNull(user);
        Validate.notEmpty(user.getId());

        RackerSourcedRoleAssignmentsBuilder builder = new RackerSourcedRoleAssignmentsBuilder(user);
        return builder;
    }

    private RackerSourcedRoleAssignmentsBuilder(Racker user) {
        this.user = user;
        interimSourcedRoleAssignments = new SourcedRoleAssignments(user);
    }

    public RackerSourcedRoleAssignmentsBuilder addIdentitySystemSourcedAssignment(ImmutableClientRole role) {
        interimSourcedRoleAssignments.addSystemSourcedAssignment(role, "IDENTITY", RoleAssignmentType.DOMAIN, Collections.EMPTY_SET);
        return this;
    }

    public RackerSourcedRoleAssignmentsBuilder addEdirSystemSourcedAssignment(ImmutableClientRole role) {
        interimSourcedRoleAssignments.addSystemSourcedAssignment(role, "EDIR", RoleAssignmentType.DOMAIN, Collections.EMPTY_SET);
        return this;
    }

    public boolean isBuildable() {
        return true;
    }


    public SourcedRoleAssignments build() {
        SourcedRoleAssignments finalSourceRoleAssignments = new SourcedRoleAssignments(user);

        for (SourcedRoleAssignments.SourcedRoleAssignment sourcedRoleAssignment : interimSourcedRoleAssignments.getSourcedRoleAssignments()) {
            ImmutableClientRole cr = sourcedRoleAssignment.getRole();

            for (RoleAssignmentSource rawSource : sourcedRoleAssignment.getSources()) {
                // Just add the source as is
                finalSourceRoleAssignments.addSourceForRole(cr, rawSource);
            }
        }
        return finalSourceRoleAssignments;
    }
}

