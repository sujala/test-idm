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

    public RackerSourcedRoleAssignmentsBuilder addImplicitAssignment(String adGroupName, ImmutableClientRole role) {
        RoleAssignmentSource source = new RoleAssignmentSource(RoleAssignmentSourceType.IMPLICIT, adGroupName, RoleAssignmentType.DOMAIN, Collections.EMPTY_SET);
        interimSourcedRoleAssignments.addSourceForRole(role, source);
        return this;
    }

    public RackerSourcedRoleAssignmentsBuilder addIdentitySystemSourcedAssignment(ImmutableClientRole role) {
        interimSourcedRoleAssignments.addSystemSourcedAssignment(role, "IDENTITY", RoleAssignmentType.DOMAIN, Collections.EMPTY_SET);
        return this;
    }

    public RackerSourcedRoleAssignmentsBuilder addAdSystemSourcedAssignment(String adGroupName) {
        ClientRole cr = new ClientRole();
        cr.setId("iam:" + adGroupName); // Prefix the id's of racker roles with "iam" to distinguish from identity managed roles.
        cr.setName(adGroupName);

        ImmutableClientRole imr = new ImmutableClientRole(cr);
        interimSourcedRoleAssignments.addSystemSourcedAssignment(imr, "AD", RoleAssignmentType.DOMAIN, Collections.EMPTY_SET);
        return this;
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

