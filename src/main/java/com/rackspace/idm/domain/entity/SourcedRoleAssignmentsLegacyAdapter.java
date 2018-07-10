package com.rackspace.idm.domain.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wraps a {@link SourcedRoleAssignments} and returns roles as if "apply rcn roles" logic was *not* used. In other words,
 * it unwinds the logic used by apply_rcn_roles by:
 *
 * 1. Removing any RCN roles. When RCN roles are not applied, a user does not appear to have _any_ RCN roles
 * 2. Any roles assigned at domain level will not return any tenantIds
 * 3. Any roles only assigned at tenant level will contain union of all tenants
 */
public class SourcedRoleAssignmentsLegacyAdapter {

    private SourcedRoleAssignments sourcedRoleAssignments;

    private List<TenantRole> rcnAssignedRoles = new ArrayList<>();
    private List<TenantRole> tenantAssignedRoles = new ArrayList<>();
    private List<TenantRole> domainAssignedRoles = new ArrayList<>();

    public SourcedRoleAssignmentsLegacyAdapter(SourcedRoleAssignments sourcedRoleAssignments) {
        this.sourcedRoleAssignments = sourcedRoleAssignments;

        for (SourcedRoleAssignments.SourcedRoleAssignment sourcedRoleAssignment : sourcedRoleAssignments.getSourcedRoleAssignments()) {
            boolean rcnAssigned = false;
            boolean domainAssigned = false;
            boolean tenantAssigned = false;

            for (RoleAssignmentSource roleAssignmentSource : sourcedRoleAssignment.getSources()) {
                if (roleAssignmentSource.getAssignmentType() == RoleAssignmentType.DOMAIN) {
                    domainAssigned = true;
                } else if (roleAssignmentSource.getAssignmentType() == RoleAssignmentType.RCN) {
                    rcnAssigned = true;
                } else if (roleAssignmentSource.getAssignmentType() == RoleAssignmentType.TENANT) {
                    tenantAssigned = true;
                }
            }

            // A given source assignment must be classified as only one of the assignment. The precedence is:
            // RCN -> Domain -> Tenant
            if (rcnAssigned) {
                rcnAssignedRoles.add(asRcnAssignedRole(sourcedRoleAssignment));
            } else if (domainAssigned) {
                domainAssignedRoles.add(asDomainAssignedRole(sourcedRoleAssignment));
            } else if (tenantAssigned) {
                tenantAssignedRoles.add(asTenantAssignedRole(sourcedRoleAssignment));
            }
        }
    }

    /**
     * Retrieves the "standard" legacy based tenant roles which is just the domain and tenant assigned roles. RCN roles
     * are excluded.
     *
     * @return
     */
     public List<TenantRole> getStandardTenantRoles() {
        List<TenantRole> tenantRoles = new ArrayList<>(domainAssignedRoles.size() + tenantAssignedRoles.size());
        tenantRoles.addAll(tenantAssignedRoles);
        tenantRoles.addAll(domainAssignedRoles);

        return tenantRoles;
    }

    public List<TenantRole> getRcnTenantRoles() {
         return new ArrayList<>(rcnAssignedRoles);
    }

    private TenantRole asTenantAssignedRole(SourcedRoleAssignments.SourcedRoleAssignment assignment) {
        Set<String> tenantIds = new HashSet<>();
        for (RoleAssignmentSource roleAssignmentSource : assignment.getSources()) {
            if (roleAssignmentSource.getAssignmentType() == RoleAssignmentType.TENANT) {
                tenantIds.addAll(roleAssignmentSource.getTenantIds());
            }
        }

        TenantRole tenantRole = assignment.asTenantRole();
        tenantRole.setTenantIds(tenantIds);
        return tenantRole;
    }

    private TenantRole asDomainAssignedRole(SourcedRoleAssignments.SourcedRoleAssignment assignment) {
        TenantRole tenantRole = assignment.asTenantRole();
        // Blank out the tenants
        tenantRole.setTenantIds(new HashSet<>());
        return tenantRole;
    }

    /**
     * In legacy perspective an RCN role doesn't really exist. While it's assigned globally, it only applies to
     * tenants of certain types based on the role definition. Since legacy doesn't apply RCN roles, is safer to just
     * assume they don't exist from legacy perspective unless explicitly being returned.
     *
     * @return
     */
    private TenantRole asRcnAssignedRole(SourcedRoleAssignments.SourcedRoleAssignment assignment) {
        TenantRole tenantRole = assignment.asTenantRole();
        // Blank out the tenants
        tenantRole.setTenantIds(new HashSet<>());
        return tenantRole;
    }
}