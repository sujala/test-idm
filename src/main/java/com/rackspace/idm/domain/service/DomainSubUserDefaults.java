package com.rackspace.idm.domain.service;

import com.google.common.collect.ImmutableSet;
import com.rackspace.idm.api.security.ImmutableTenantRole;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.util.RoleUtil;
import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DomainSubUserDefaults {
    @Getter
    private String domainId;

    @Getter
    private String region;

    private Set<String> rateLimitingGroupIds;
    private Set<ImmutableTenantRole> subUserTenantRoles = new HashSet<>();

    public DomainSubUserDefaults(Domain domain, Set<String> groupIds, String defaultRegion, List<TenantRole> defaultRoles) {
        this.domainId = domain.getDomainId();
        this.region = defaultRegion;
        this.rateLimitingGroupIds = ImmutableSet.copyOf(groupIds);

        for (TenantRole defaultRole : defaultRoles) {
            TenantRole tr = RoleUtil.cloneTenantRole(defaultRole);

            // Erase the user specific info
            tr.setUserId(null);
            tr.setUniqueId(null);

            subUserTenantRoles.add(new ImmutableTenantRole(tr));
        }
    }

    public Set<String> getRateLimitingGroupIds() {
        return Collections.unmodifiableSet(rateLimitingGroupIds);
    }

    public Set<ImmutableTenantRole> getSubUserTenantRoles() {
        return Collections.unmodifiableSet(subUserTenantRoles);
    }
}
