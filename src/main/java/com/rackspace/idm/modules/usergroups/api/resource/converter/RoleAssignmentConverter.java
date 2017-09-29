package com.rackspace.idm.modules.usergroups.api.resource.converter;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.modules.usergroups.Constants;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Converts between the TenantRole "LDAP Entity" and the RoleAssignment "REST request/response" entity
 */
@Component
public class RoleAssignmentConverter {

    @Autowired
    private ApplicationService applicationService;

    /**
     * Converts from the LDAP representation of a userGroup to the request/response web service based representation.
     *
     * @param tenantRole
     * @return
     */
    public TenantAssignment toRoleAssignmentWeb(com.rackspace.idm.domain.entity.TenantRole tenantRole) {
        TenantAssignment assignment = new TenantAssignment();
        assignment.setOnRole(tenantRole.getRoleRsId());
        assignment.setOnRoleName(tenantRole.getName());

        // If name is blank, set the name for the role using client role cache
        if (StringUtils.isBlank(assignment.getOnRoleName()) && StringUtils.isNotBlank(assignment.getOnRole())) {
            ImmutableClientRole cr = applicationService.getCachedClientRoleById(assignment.getOnRole());
            if (cr != null) {
                assignment.setOnRoleName(cr.getName());
            }
        }

        if (CollectionUtils.isNotEmpty(tenantRole.getTenantIds())) {
            assignment.getForTenants().addAll(tenantRole.getTenantIds());
        } else {
            assignment.getForTenants().add(Constants.ALL_TENANT_IN_DOMAIN_WILDCARD);
        }
        return assignment;
    }

    /**
     * Converts from the LDAP representation of a userGroup list to the request/response web service based representation.
     *
     * @param tenantRoles
     * @return
     */
    public TenantAssignments toTenantAssignmentsWeb(Iterable<com.rackspace.idm.domain.entity.TenantRole> tenantRoles) {
        TenantAssignments tenantAssignments = new TenantAssignments();

        for (com.rackspace.idm.domain.entity.TenantRole tenantRole : tenantRoles) {
            tenantAssignments.getTenantAssignment().add(toRoleAssignmentWeb(tenantRole));
        }

        return tenantAssignments;
    }

     /**
     * Converts from the LDAP representation of a userGroup list to the request/response web service based representation.
     *
     * @param tenantRoles
     * @return
     */
    public RoleAssignments toRoleAssignmentsWeb(Iterable<com.rackspace.idm.domain.entity.TenantRole> tenantRoles) {
        RoleAssignments roleAssignments = new RoleAssignments();
        roleAssignments.setTenantAssignments(toTenantAssignmentsWeb(tenantRoles));
        return roleAssignments;
    }

}
