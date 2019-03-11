package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.entity.SourcedRoleAssignments;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;

import java.util.List;

/**
 * Represents a list of identity managed roles associated with a calling user against which authorization decisions
 * are made.
 */
public class AuthorizationContext {

    SourcedRoleAssignments sourcedRoleAssignments;

    public AuthorizationContext(SourcedRoleAssignments sourcedRoleAssignments) {
        this.sourcedRoleAssignments = sourcedRoleAssignments;
    }

    public boolean hasRoleWithId(String id) {
        return sourcedRoleAssignments.getSourcedRoleAssignmentForRole(id) != null;
    }

    public boolean hasRoleWithName(String name) {
        for (SourcedRoleAssignments.SourcedRoleAssignment sourcedRoleAssignment : sourcedRoleAssignments.getSourcedRoleAssignments()) {
            if (name.equalsIgnoreCase(sourcedRoleAssignment.getRole().getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the lowest weight user type role included in the list of roles or null if no roles match an identity
     * user type.
     *
     * @return
     */
    public IdentityUserTypeEnum getIdentityUserType() {
        return sourcedRoleAssignments.getUserTypeFromAssignedRoles();
    }
}
