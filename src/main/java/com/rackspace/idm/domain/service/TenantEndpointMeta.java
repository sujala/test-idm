package com.rackspace.idm.domain.service;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.modules.endpointassignment.entity.Rule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

/**
 * Class provides a wrapper around all the info required to determine the endpoints that should be associated with
 * a given tenant.
 *
 * Note - While the instance variables are not mutable, the objects to which they point are mutable. Only attempting
 * to provide basic protection against inadvertent change.
 */
@Getter
public class TenantEndpointMeta {
    final EndUser user;
    final Tenant tenant;
    final List<TenantRole> rolesOnTenant;
    final List<Rule> rulesForTenant;

    public TenantEndpointMeta(EndUser user, Tenant tenant, List<TenantRole> rolesOnTenant, List<Rule> rulesForTenant) {
        Assert.notNull(user);
        Assert.notNull(tenant);

        this.user = user;
        this.tenant = tenant;
        this.rolesOnTenant = rolesOnTenant != null ? Collections.unmodifiableList(rolesOnTenant) : Collections.<TenantRole>emptyList();
        this.rulesForTenant = rulesForTenant != null ? Collections.unmodifiableList(rulesForTenant) : Collections.<Rule>emptyList();
    }

    public boolean addGlobalMossoEndpointsOnTenant() {
        if (user.getRegion() != null) {
            for (TenantRole tenantRole : rolesOnTenant) {
                if (GlobalConstants.COMPUTE_DEFAULT_ROLE.equals(tenantRole.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean addGlobalNastEndpointsOnTenant() {
        if (user.getRegion() != null) {
            for (TenantRole tenantRole : rolesOnTenant) {
                if (GlobalConstants.FILES_DEFAULT_ROLE.equals(tenantRole.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getLogString() {
        StringBuilder build = new StringBuilder("TenantEndpointMeta: ");
        build.append(String.format("UserId: '%s'", user.getId()));
        build.append(String.format("; TenantId: '%s'", tenant.getTenantId()));

        build.append("; ClientRoleIds: [");
        for (TenantRole tenantRole : rolesOnTenant) {
            build.append(String.format("'%s',", tenantRole.getRoleRsId()));
        }
        build.append("]");

        build.append("; RulesForTenant: [");
        for (Rule ruleForTenant : rulesForTenant) {
            build.append(String.format("'%s',", ruleForTenant.getId()));
        }
        build.append("]");
        return build.toString();
    }
}
