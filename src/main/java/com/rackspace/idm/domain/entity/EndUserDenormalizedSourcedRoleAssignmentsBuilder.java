package com.rackspace.idm.domain.entity;

import com.google.common.collect.Sets;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.domain.service.rolecalculator.UserRoleLookupService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

/**
 * Builder to support a bridge to create a {@link SourcedRoleAssignments} based on passing in the information contained
 * in TenantRole objects and applies appropriate logic to denormalize a Domain (global) role assignment down to tenants.
 * Generates a {@link SourcedRoleAssignments} each time build {@link #build()}} is called.
 *
 * A new instance of this class must be created each time roles need to be calculated. Once the {@link #build()} method
 * is successfully called the service will cache the results and return the same results each time.
 */
@NotThreadSafe
public class EndUserDenormalizedSourcedRoleAssignmentsBuilder {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * A lookup service for external resources required to appropriately perform role assignments down to tenant level.
     */
    private UserRoleLookupService userRoleLookupService;

    /**
     * These tenants are not automatically assigned a global role unless
     */
    private Set<String> hiddenTenantPrefixes = Collections.emptySet();

    private SourcedRoleAssignments interimSourcedRoleAssignments;
    private SourcedRoleAssignments finalSourcedRoleAssignments;

    private Set<String> domainAssignmentTenants;

    public static EndUserDenormalizedSourcedRoleAssignmentsBuilder endUserBuilder(UserRoleLookupService userRoleLookupService) {
        Validate.notNull(userRoleLookupService);
        Validate.notNull(userRoleLookupService.getUser());
        Validate.notEmpty(userRoleLookupService.getUser().getId());
        Validate.notNull(userRoleLookupService.getUserDomain());
        Validate.isTrue(userRoleLookupService.getUser().getDomainId().equalsIgnoreCase(userRoleLookupService.getUserDomain().getDomainId()));

        EndUserDenormalizedSourcedRoleAssignmentsBuilder builder = new EndUserDenormalizedSourcedRoleAssignmentsBuilder();
        builder.userRoleLookupService = userRoleLookupService;

        return builder;
    }

    /**
     * Private to force use of static constructor
     */
    private EndUserDenormalizedSourcedRoleAssignmentsBuilder() {}


    public void setHiddenTenantPrefixes(Set<String> hiddenTenantPrefixes) {
        if (finalSourcedRoleAssignments != null) {
            throw new IllegalArgumentException("Can not change prefixes once roles have been calculated");
        }
        if (CollectionUtils.isNotEmpty(hiddenTenantPrefixes)) {
            this.hiddenTenantPrefixes = new HashSet<>(hiddenTenantPrefixes);
        } else {
            this.hiddenTenantPrefixes = Collections.emptySet();
        }
    }

    public SourcedRoleAssignments build() {
        if (finalSourcedRoleAssignments != null) {
            return finalSourcedRoleAssignments;
        }

        /*
          Building is a 2 step process. In order to calculate some role assignments (Domain level roles) we need to know
          first know the user type of the user and all the tenants on which the user has an explicit role. This can't be
          determined until we initially process all the roles associated with the user.

         */
        interimSourcedRoleAssignments = new SourcedRoleAssignments(userRoleLookupService.getUser());
        addUserSourcedAssignmentsToInterim();
        addGroupSourcedAssignmentsToInterim();
        addSystemSourcedAssignmentsToInterim();

        // In order to successfully calculate the roles, the user must have a user type
        IdentityUserTypeEnum userType = interimSourcedRoleAssignments.getUserTypeFromAssignedRoles();
        if (userType == null) {
            String errorMessage = String.format("The user '%s' does not contain a user type role. Roles can not be determined for this user", userRoleLookupService.getUser().getId());
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        /**
         * Finalize the assignments
         */
        finalSourcedRoleAssignments = new SourcedRoleAssignments(userRoleLookupService.getUser());
        for (SourcedRoleAssignments.SourcedRoleAssignment sourcedRoleAssignment : interimSourcedRoleAssignments.getSourcedRoleAssignments()) {
            ImmutableClientRole cr = sourcedRoleAssignment.getRole();

            for (SourcedRoleAssignments.Source rawSource : sourcedRoleAssignment.getSources()) {
                Set<String> finalTenantIds = calculateFinalEffectiveTenantIdsForSource(userType, rawSource);
                SourcedRoleAssignments.Source revisedSource = new SourcedRoleAssignments.Source(rawSource.getSourceType(), rawSource.getSourceId(), rawSource.getAssignmentType(), finalTenantIds);
                finalSourcedRoleAssignments.addSourceForRole(cr, revisedSource);
            }
        }
        return finalSourcedRoleAssignments;
    }

    private void addUserSourcedAssignmentsToInterim() {
        List<TenantRole> tenantRoles = userRoleLookupService.getUserSourcedRoles();
        if (CollectionUtils.isNotEmpty(tenantRoles)) {
            for (TenantRole tenantRole : tenantRoles) {
                addUserSourcedAssignmentToInterim(tenantRole);
            }
        }
    }

    private void addUserSourcedAssignmentToInterim(TenantRole tenantRole) {
        Validate.notNull(tenantRole);
        Validate.notNull(tenantRole.getRoleRsId());

        ImmutableClientRole icr = userRoleLookupService.getImmutableClientRole(tenantRole.getRoleRsId());
        // we ignore missing client roles
        if (icr == null) {
            logger.error(String.format("User '%s' is assigned non-existing role '%s'", userRoleLookupService.getUser().getId(), tenantRole.getRoleRsId()));
        } else {
            SourcedRoleAssignments.AssignmentType assignmentType = determineAssignmentTypeForTenantRole(icr, tenantRole);
            Set<String> effectiveTenantIds = calculatePreliminaryEffectiveTenantIdsForRole(assignmentType, tenantRole);
            interimSourcedRoleAssignments.addUserSourcedAssignment(icr, assignmentType, effectiveTenantIds);
        }
    }

    private void addGroupSourcedAssignmentsToInterim() {
        Map<String, List<TenantRole>> groupSourcedTenantRoles = userRoleLookupService.getGroupSourcedRoles();
        if (MapUtils.isNotEmpty(groupSourcedTenantRoles)) {
            for (Map.Entry<String, List<TenantRole>> groupEntry : groupSourcedTenantRoles.entrySet()) {
                String groupId = groupEntry.getKey();
                List<TenantRole> groupRoles = groupEntry.getValue();

                for (TenantRole tenantRole : groupRoles) {
                    addGroupSourcedAssignmentToInterim(groupId, tenantRole);
                }
            }
        }
    }

    private void addGroupSourcedAssignmentToInterim(String groupId, TenantRole tenantRole) {
        Validate.notEmpty(groupId);
        Validate.notNull(tenantRole);
        Validate.notNull(tenantRole.getRoleRsId());

        ImmutableClientRole icr = userRoleLookupService.getImmutableClientRole(tenantRole.getRoleRsId());

        // we ignore missing client roles
        if (icr == null) {
            logger.error(String.format("Group '%s' is assigned non-existing role '%s'", groupId, tenantRole.getRoleRsId()));
        } else {
            SourcedRoleAssignments.AssignmentType assignmentType = determineAssignmentTypeForTenantRole(icr, tenantRole);
            Set<String> effectiveTenantIds = calculatePreliminaryEffectiveTenantIdsForRole(assignmentType, tenantRole);
            interimSourcedRoleAssignments.addUserGroupSourcedAssignment(icr, groupId, assignmentType, effectiveTenantIds);
        }
    }

    private void addSystemSourcedAssignmentsToInterim() {
        Map<String, List<TenantRole>> systemSourcedTenantRoles = userRoleLookupService.getSystemSourcedRoles();
        if (MapUtils.isNotEmpty(systemSourcedTenantRoles)) {
            for (Map.Entry<String, List<TenantRole>> systemEntry : systemSourcedTenantRoles.entrySet()) {
                String systemId = systemEntry.getKey();
                List<TenantRole> groupRoles = systemEntry.getValue();

                for (TenantRole tenantRole : groupRoles) {
                    addSystemSourcedAssignmentToInterim(systemId, tenantRole);
                }
            }
        }
    }

    private void addSystemSourcedAssignmentToInterim(String systemId, TenantRole tenantRole) {
        Validate.notEmpty(systemId);
        Validate.notNull(tenantRole);
        Validate.notNull(tenantRole.getRoleRsId());

        ImmutableClientRole icr = userRoleLookupService.getImmutableClientRole(tenantRole.getRoleRsId());

        // we ignore missing client roles
        if (icr == null) {
            logger.error(String.format("Assigning via system the non-existing role '%s'", tenantRole.getRoleRsId()));
        } else {
            SourcedRoleAssignments.AssignmentType assignmentType = determineAssignmentTypeForTenantRole(icr, tenantRole);
            Set<String> effectiveTenantIds = calculatePreliminaryEffectiveTenantIdsForRole(assignmentType, tenantRole);
            interimSourcedRoleAssignments.addSystemSourcedAssignment(icr, systemId, assignmentType, effectiveTenantIds);
        }
    }

    /**
     * Determine each tenant that each role assignment is associated with for the user. This can only be calculated for
     * TENANT and RCN role assignments. DOMAIN depends on the other roles the user has assigned, and so must only be
     * determined after all other types have been calculated.
     *
     * @param assignmentType
     * @param tenantRole
     * @return
     */
    private Set<String> calculatePreliminaryEffectiveTenantIdsForRole(SourcedRoleAssignments.AssignmentType assignmentType, TenantRole tenantRole) {
        if (assignmentType == SourcedRoleAssignments.AssignmentType.TENANT) {
            return tenantRole.getTenantIds();
        } else if (assignmentType == SourcedRoleAssignments.AssignmentType.RCN) {
            // Determine tenant types for which the RCN would apply
            ImmutableClientRole icr = userRoleLookupService.getImmutableClientRole(tenantRole.getRoleRsId());
            Set<String> roleTenantTypes = icr.getTenantTypes();

            Set<String> effectiveTenantIds = new HashSet<>();
            for (Tenant rcnTenant : userRoleLookupService.calculateRcnTenants()) {
                Set<String> tenantTenantTypes = rcnTenant.getTypes();
                // If role matches tenant, add tenant to matching list
                if (roleTenantTypes.contains("*")
                        || CollectionUtils.isNotEmpty(CollectionUtils.intersection(roleTenantTypes, tenantTenantTypes))) {
                    effectiveTenantIds.add(rcnTenant.getTenantId());
                }
            }
            return effectiveTenantIds;
        }
        return Collections.emptySet();
    }

    private Set<String> calculateFinalEffectiveTenantIdsForSource(IdentityUserTypeEnum userType, SourcedRoleAssignments.Source source) {

        Set<String> effectiveTenantIds = new HashSet<>();
        if (source.getAssignmentType() == SourcedRoleAssignments.AssignmentType.DOMAIN) {
            effectiveTenantIds.addAll(getDomainAssignmentTenants(userType));
        } else {
            // TENANT | RCN - already calculated as part of preliminary calculation
            effectiveTenantIds.addAll(source.getTenantIds());
        }

        return effectiveTenantIds;
    }

    private Set<String> getDomainAssignmentTenants(IdentityUserTypeEnum userType) {
        if (domainAssignmentTenants == null) {
            domainAssignmentTenants = calculateTenantsForDomainAssignedRolesOnUser(userType);
        }
        return domainAssignmentTenants;
    }

    /*
     Any tenant in the domain on which a role is assigned explicitly to tenant receives all domain level roles.
     Find the tenants in the domain that do *not* have a role explicitly assigned to tenant. This provides a set
     of tenantIds in the domain that we need to check whether or not to exclude from receiving DOMAIN level roles
     */
    private Set<String> calculateTenantsForDomainAssignedRolesOnUser(IdentityUserTypeEnum userType) {
        String[] domainTenantIds = userRoleLookupService.getUserDomain().getTenantIds();

        Set<String> tenantIdsToReceiveDomainRoles = new HashSet<>();
        if (ArrayUtils.isNotEmpty(domainTenantIds)) {
            tenantIdsToReceiveDomainRoles = new HashSet<>(Arrays.asList(domainTenantIds));

            // If user is a regular subuser, some tenants remain hidden
            if (IdentityUserTypeEnum.DEFAULT_USER == userType) {

                // Calc tenants in domain on which user is not assigned a role directly on the tenant
                Set<String> tenantsToCheck = Sets.difference(tenantIdsToReceiveDomainRoles, getTenantsAssignedViaTenantAssignment()).copyInto(new HashSet<String>());

                // Check these tenants to see if should be excluded from receiving domain level roles
                if (!tenantsToCheck.isEmpty() && CollectionUtils.isNotEmpty(hiddenTenantPrefixes)) {
                    for (String tenantId : tenantsToCheck) {
                        String tenantPrefix = parseTenantPrefixFromTenantId(tenantId);

                        // While this means the hidden prefixes are case sensitive, other does this to so okay here.
                        if (StringUtils.isNotBlank(tenantPrefix) && hiddenTenantPrefixes.contains(tenantPrefix)) {
                            tenantIdsToReceiveDomainRoles.remove(tenantId);
                        }
                    }
                }
            }
        }
        return tenantIdsToReceiveDomainRoles;
    }

    private Set<String> getTenantsAssignedViaTenantAssignment() {
        Set<String> tenantsAssignedExplicit = new HashSet();
        for (SourcedRoleAssignments.SourcedRoleAssignment sourcedRoleAssignment : interimSourcedRoleAssignments.getSourcedRoleAssignments()) {
            for (SourcedRoleAssignments.Source source : sourcedRoleAssignment.getSources()) {
                if (source.getAssignmentType() == SourcedRoleAssignments.AssignmentType.TENANT) {
                    tenantsAssignedExplicit.addAll(source.getTenantIds());
                }
            }
        }
        return tenantsAssignedExplicit;
    }

    /**
     * Given a tenant ID, parse out the tenant prefix. The tenant prefix is anything in the tenant ID up to but excluding
     * the first ':'. If the tenant ID does not contain a ':', the tenant ID does not have a prefix.
     * <p>
     * // TODO Use a utility for this rather than copying code
     *
     * @param tenantId
     * @return
     */
    private String parseTenantPrefixFromTenantId(String tenantId) {
        String[] idComponents = tenantId.split(":");
        return idComponents.length >= 2 ? idComponents[0] : null;
    }

    private SourcedRoleAssignments.AssignmentType determineAssignmentTypeForTenantRole(ImmutableClientRole cr, TenantRole tenantRole) {
        if (cr.getRoleType() == RoleTypeEnum.RCN) {
            return SourcedRoleAssignments.AssignmentType.RCN;
        }

        return CollectionUtils.isEmpty(tenantRole.getTenantIds())
                ? SourcedRoleAssignments.AssignmentType.DOMAIN : SourcedRoleAssignments.AssignmentType.TENANT;
    }
}

