package com.rackspace.idm.domain.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.service.TenantService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TenantTypeWhitelistFilter implements SourcedRoleAssignmentsFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantTypeWhitelistFilter.class);

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private TenantService tenantService;

    @Override
    public SourcedRoleAssignments apply(SourcedRoleAssignments sourcedRoleAssignments) {
        List<String> tenantsToHide = findTenantsToHide(sourcedRoleAssignments);

        SourcedRoleAssignments finalSourceRoleAssignments = sourcedRoleAssignments;

        // Only need to rebuild the assignments if one or more tenants need to be hidden
        if (CollectionUtils.isNotEmpty(tenantsToHide)) {
            finalSourceRoleAssignments = new SourcedRoleAssignments(sourcedRoleAssignments.getUser());

            // Iterate through all the sources of every assignment and remove the tenant
            for (SourcedRoleAssignments.SourcedRoleAssignment sourcedRoleAssignment : sourcedRoleAssignments.getSourcedRoleAssignments()) {
                ImmutableClientRole cr = sourcedRoleAssignment.getRole();

                for (RoleAssignmentSource rawSource : sourcedRoleAssignment.getSources()) {
                    Set<String> finalTenantIds = sourcedRoleAssignment.getTenantIds();
                    if (finalTenantIds.removeIf(tenantsToHide::contains)) {
                        LOGGER.debug(String.format("Removed whitelist only tenants from role %s", cr.getName()));
                    }

                    RoleAssignmentSource revisedSource = new RoleAssignmentSource(rawSource.getSourceType(), rawSource.getSourceId(), rawSource.getAssignmentType(), finalTenantIds);
                    finalSourceRoleAssignments.addSourceForRole(cr, revisedSource);
                }
            }
        }
        return finalSourceRoleAssignments;
    }

    private List<String> findTenantsToHide(SourcedRoleAssignments sourcedRoleAssignments) {
        Map<String, Set<String>> visibilityGroups = identityConfig.getReloadableConfig().getTenantTypeRoleWhitelistFilterMap();

        List<String> tenantsToHide = Collections.emptyList();

        // Only need to calculate hidden tenants if at least one tenant has a set of whitelisted roles
        if (MapUtils.isNotEmpty(visibilityGroups)) {
            Map<String, Set<ImmutableClientRole>> tenantToRoleMap = mapTenantRoles(sourcedRoleAssignments);

            tenantsToHide = new ArrayList<>();

            // For each tenant, extract the tenant type and determine if visibility role group exists
            for (Map.Entry<String, Set<ImmutableClientRole>> tenantToRolesEntry : tenantToRoleMap.entrySet()) {

                String tenantId = tenantToRolesEntry.getKey();
                String tenantType = tenantService.inferTenantTypeForTenantId(tenantId);
                if (StringUtils.isNotEmpty(tenantType)) {
                    Set<String> tenantTypeWhitelistRoles = visibilityGroups.get(tenantType);

                    if (CollectionUtils.isNotEmpty(tenantTypeWhitelistRoles)) {
                    /* This tenant is protected by a list of whitelisted roles. The tenant MUST be assigned at least
                       one of these roles or any RCN role on the tenant in order for the tenant to be visible.
                     */
                        Set<ImmutableClientRole> tenantAssignedRoles = tenantToRolesEntry.getValue();
                        Optional<ImmutableClientRole> finder = tenantAssignedRoles.stream().filter(cr -> cr.getRoleType() == RoleTypeEnum.RCN || tenantTypeWhitelistRoles.contains(cr.getName())).findFirst();

                        if (!finder.isPresent()) {
                            tenantsToHide.add(tenantId);
                        }
                    }
                }
            }
        }
        return tenantsToHide;
    }

    private Map<String, Set<ImmutableClientRole>> mapTenantRoles(SourcedRoleAssignments assignments) {
        Map<String, Set<ImmutableClientRole>> tenantToRoleMap = new HashMap<>();
        for (SourcedRoleAssignments.SourcedRoleAssignment assignment : assignments.getSourcedRoleAssignments()) {
            String roleName = assignment.getRole().getName();
            Set<String> tenantIds = assignment.getTenantIds();

            for (String tenantId : tenantIds) {
                Set<ImmutableClientRole> roles = tenantToRoleMap.computeIfAbsent(tenantId, s -> new HashSet<>());
                roles.add(assignment.getRole());
            }
        }
        return tenantToRoleMap;
    }
}
