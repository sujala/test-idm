package com.rackspace.idm.domain.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoleAssignmentSource {
    @Getter
    private RoleAssignmentSourceType sourceType;

    @Getter
    private String sourceId;

    @Getter
    private RoleAssignmentType assignmentType;

    private Set<String> tenantIds = new HashSet<>();

    public RoleAssignmentSource(RoleAssignmentSourceType sourceType, String sourceId, RoleAssignmentType assignmentType, Set<String> tenantIds) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.tenantIds = ImmutableSet.copyOf(tenantIds);
        this.assignmentType = assignmentType;
    }

    public List<String> getTenantIds() {
        return ImmutableList.copyOf(tenantIds);
    }
}
