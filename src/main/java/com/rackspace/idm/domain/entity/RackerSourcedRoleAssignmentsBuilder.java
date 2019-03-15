package com.rackspace.idm.domain.entity;

import com.rackspace.idm.api.security.ImmutableClientRole;
import org.apache.commons.lang.Validate;

import java.util.Collections;

public class RackerSourcedRoleAssignmentsBuilder {
    private Racker user;

    private SourcedRoleAssignments interimSourcedRoleAssignments;
    private boolean built = false;

    public static RackerSourcedRoleAssignmentsBuilder rackerBuilder(Racker user) {
        Validate.notNull(user);
        Validate.notEmpty(user.getId());
        Validate.notEmpty(user.getUsername());

        RackerSourcedRoleAssignmentsBuilder builder = new RackerSourcedRoleAssignmentsBuilder(user);
        return builder;
    }

    private RackerSourcedRoleAssignmentsBuilder(Racker user) {
        this.user = user;
        interimSourcedRoleAssignments = new SourcedRoleAssignments(user);
    }

    public RackerSourcedRoleAssignmentsBuilder addImplicitAssignment(String adGroupName, ImmutableClientRole role) {
        verifyBuiltStatus();
        RoleAssignmentSource source = new RoleAssignmentSource(RoleAssignmentSourceType.IMPLICIT, adGroupName, RoleAssignmentType.DOMAIN, Collections.EMPTY_SET);
        interimSourcedRoleAssignments.addSourceForRole(role, source);
        return this;
    }

    public RackerSourcedRoleAssignmentsBuilder addIdentitySystemSourcedAssignment(ImmutableClientRole role) {
        verifyBuiltStatus();
        interimSourcedRoleAssignments.addSystemSourcedAssignment(role, "IDENTITY", RoleAssignmentType.DOMAIN, Collections.EMPTY_SET);
        return this;
    }

    public SourcedRoleAssignments build() {
        verifyBuiltStatus();
        SourcedRoleAssignments finalSourceRoleAssignments = new SourcedRoleAssignments(user);
        for (SourcedRoleAssignments.SourcedRoleAssignment sourcedRoleAssignment : interimSourcedRoleAssignments.getSourcedRoleAssignments()) {
            ImmutableClientRole cr = sourcedRoleAssignment.getRole();

            for (RoleAssignmentSource rawSource : sourcedRoleAssignment.getSources()) {
                // Just add the source as is
                finalSourceRoleAssignments.addSourceForRole(cr, rawSource);
            }
        }
        built = true;
        interimSourcedRoleAssignments = null;

        return finalSourceRoleAssignments;
    }

    void verifyBuiltStatus() {
        if (built) {
            throw new IllegalStateException("The source role assignments have already been built. You must create a new builder.");
        }
    }

    public boolean hasBeenBuilt() {
        return built;
    }
}

