package com.rackspace.idm.modules.usergroups.api.resource.converter;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.domain.entity.RoleAssignmentSource;
import com.rackspace.idm.domain.entity.SourcedRoleAssignments;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.impl.DefaultTenantAssignmentService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Converts between the TenantRole "LDAP Entity" and the RoleAssignment "REST request/response" entity
 */
@Component
public class RoleAssignmentConverter {
    private static final Logger logger = LoggerFactory.getLogger(RoleAssignmentConverter.class);

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
            assignment.getForTenants().add(DefaultTenantAssignmentService.ALL_TENANTS_IN_DOMAIN_WILDCARD);
        }
        return assignment;
    }

    /**
     * Converts from the LDAP representation of a userGroup to the request/response web service based representation.
     *
     * @param sourcedRoleAssignment
     * @return
     */
    public TenantAssignment toRoleAssignmentWeb(SourcedRoleAssignments.SourcedRoleAssignment sourcedRoleAssignment) {
        TenantAssignment assignment = new TenantAssignment();

        ImmutableClientRole cr = sourcedRoleAssignment.getRole();

        assignment.setOnRole(cr.getId());
        assignment.setOnRoleName(cr.getName());

        assignment.getForTenants().addAll(sourcedRoleAssignment.getTenantIds());
        assignment.setSources(new AssignmentSources());

        for (RoleAssignmentSource source : sourcedRoleAssignment.getSources()) {
            AssignmentSource assignmentSource = new AssignmentSource();

            if (source.getAssignmentType() != null) {
                try {
                    assignmentSource.setAssignmentType(AssignmentTypeEnum.valueOf(source.getAssignmentType().name()));
                } catch (IllegalArgumentException e) {
                    logger.warn(String.format("Error converting assignment type '%s' to web format", source.getAssignmentType().toString()));
                }
            }
            if (source.getSourceType() != null) {
                try {
                    assignmentSource.setSourceType(SourceTypeEnum.valueOf(source.getSourceType().name()));
                } catch (IllegalArgumentException e) {
                    logger.warn(String.format("Error converting source type '%s' to web format", source.getSourceType().toString()));
                }
            }

            assignmentSource.setSourceId(source.getSourceId());
            assignmentSource.getForTenants().addAll(source.getTenantIds());

            assignment.getSources().getSource().add(assignmentSource);
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
     * @param sourcedRoleAssignments
     * @return
     */
    public TenantAssignments toTenantAssignmentsWeb(SourcedRoleAssignments sourcedRoleAssignments) {
        TenantAssignments tenantAssignments = new TenantAssignments();

        if (sourcedRoleAssignments != null) {
            for (SourcedRoleAssignments.SourcedRoleAssignment sourcedRoleAssignment : sourcedRoleAssignments.getSourcedRoleAssignments()) {
                tenantAssignments.getTenantAssignment().add(toRoleAssignmentWeb(sourcedRoleAssignment));
            }
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

    /**
     * Converts from the LDAP representation of a userGroup list to the request/response web service based representation.
     *
     * @return
     */
    public RoleAssignments fromSourcedRoleAssignmentsToRoleAssignmentsWeb(SourcedRoleAssignments sourcedRoleAssignments) {
        RoleAssignments roleAssignments = new RoleAssignments();
        roleAssignments.setTenantAssignments(toTenantAssignmentsWeb(sourcedRoleAssignments));
        return roleAssignments;
    }
}
