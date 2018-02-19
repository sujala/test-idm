package com.rackspace.idm.util;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Types;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;

import java.util.*;

public class RoleUtil {

    /**
     * Merges included collections of tenant roles and returns a combined set of tenant roles. Tenants roles for the same role that
     * exist in both collections are combined into a single tenant role via the {@link #mergeTenantRoles(TenantRole, TenantRole)}
     * service.
     *
     * If multiple assignments are merged for a particular role. Resultant tenant roles are "detached" from an LDAP Entry
     * (uniqueId/userId will be nulled out)  This is because after merging it's impossible to determine from where a
     * particular assignment came.
     *
     * @param iterables
     * @return
     */
    public Collection<TenantRole> mergeTenantRoleSets(Iterable<TenantRole>... iterables) {
        Map<String, TenantRole> masterRoleSet = new HashMap<>();

        for (Iterable<TenantRole> iterable : iterables) {
            for (TenantRole itTenantRole : iterable) {
                TenantRole existingAssignment = masterRoleSet.get(itTenantRole.getRoleRsId());
                TenantRole mergedRole = mergeTenantRoles(existingAssignment, itTenantRole);
                masterRoleSet.put(mergedRole.getRoleRsId(), mergedRole); // Replace or add assignment with merged
            }
        }

        return masterRoleSet.values();
    }

    /**
     * Merge 2 tenant role assignments such that the returned role is the combined assignment of master and other. Guarantees
     * the tenant assignments are set appropriately for the for resultant role. Makes no guarantees on whether fields populated
     * from client role (e.g. role name, weight, etc) will be populated on the resultnant role.
     *
     * 1. If master == null, result reflects the state of "other"
     * 2. If other == null, result reflects the state of "master"
     * 3. If either is a global assigned role, result reflects a global role
     * 4. If both are tenant assigned roles, result reflects a tenant assigned role on the the union of all tenants assigned
     *
     * No modifications are made to arguments. Returns a new TenantRole that is cloned from one of the passed in roles
     *
     * If multiple assignments are merged for a particular role. Resultant tenant roles are "detached" from an LDAP Entry
     * (uniqueId/userId will be nulled out)  This is because after merging it's impossible to determine from where a
     * particular assignment came.
     *
     * @param master
     * @param other
     * @return
     */
    public TenantRole mergeTenantRoles(final TenantRole master, final TenantRole other) {
        Validate.isTrue(master != null || other != null);
        if (master != null && other != null) {
            // If merging 2 roles, they must have the same roleRsId
            Validate.isTrue(master.getRoleRsId().equalsIgnoreCase(other.getRoleRsId()));
        }

        TenantRole result;
        if (master == null) {
            result = cloneTenantRole(other);
        } else if (other == null) {
            result = cloneTenantRole(master);
        } else {
            result = cloneTenantRole(master); // Start with master

            // Reconcile case where one assignment is global, other is tenant
            boolean masterIsDomainAssignment = CollectionUtils.isEmpty(master.getTenantIds());
            boolean otherIsDomainAssignment = CollectionUtils.isEmpty(other.getTenantIds());

            if (masterIsDomainAssignment || otherIsDomainAssignment) {
                // Result tenants should be cleared as it is a global assignment
                result.getTenantIds().clear();
            } else {
                // Add all tenants from 'other' assignment into master. tenantIds is a Set so will automatically
                // account for duplicates
                result.getTenantIds().addAll(other.getTenantIds());
            }

            /*
                We're merging 2 Tenant Roles into one. If the values are different between the identifying fields,
                must clear out.
             */
            result.setUniqueId(reconcileValuesToNull(master.getUniqueId(), other.getUniqueId()));
            result.setUserId(reconcileValuesToNull(master.getUserId(), other.getUserId()));
        }

        return result;
    }

    /**
     * If the values are the same, return that value. Otherwise return null.
     * @param value1
     * @param value2
     * @return
     */
    private String reconcileValuesToNull(String value1, String value2) {
        if (value1 == null || value2 == null) {
            return null;
        } else if (value1.equalsIgnoreCase(value2)) {
            return value1;
        }
        return null;
    }

    public static TenantRole cloneTenantRole(TenantRole source) {
        TenantRole target = new TenantRole();
        target.setName(source.getName());
        target.setRoleType(source.getRoleType());
        target.setDescription(source.getDescription());
        target.setRoleRsId(source.getRoleRsId());
        target.setClientId(source.getClientId());
        target.setUniqueId(source.getUniqueId());
        target.setUserId(source.getUserId());
        target.setTenantIds(new HashSet<String>(source.getTenantIds()));

        Types finalTypes = null;
        if (source.getTypes() != null) {
            finalTypes = new Types();
            if (org.apache.commons.collections.CollectionUtils.isNotEmpty(source.getTypes().getType())) {
                finalTypes.getType().addAll(source.getTypes().getType());
            }
        }
        target.setTypes(finalTypes);

        return target;
    }

    public static ClientRole cloneClientRole(ClientRole source) {
        ClientRole target = new ClientRole();
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setId(source.getId());
        target.setClientId(source.getClientId());
        target.setUniqueId(source.getUniqueId());
        target.setRsWeight(source.getRsWeight());
        target.setAssignmentType(source.getAssignmentType());
        target.setRoleType(source.getRoleType());
        target.setTenantTypes(new HashSet<>(source.getTenantTypes()));

        return target;
    }

    /**
     * Preps a tenant role from a client role. Caller is expected to set user, tenants, etc
     *
     * @param source
     * @return
     */
    public static TenantRole newTenantRoleFromClientRole(ImmutableClientRole source) {
        TenantRole target = new TenantRole();
        target.setName(source.getName());
        target.setRoleType(source.getRoleType());
        target.setDescription(source.getDescription());
        target.setRoleRsId(source.getId());
        target.setClientId(source.getClientId());

        return target;
    }
}
