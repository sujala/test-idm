package com.rackspace.idm.exception;

import com.rackspace.idm.domain.entity.TenantRole;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

public class FailedGrantRoleAssignmentsException extends IdmException {

    @Getter
    private List<TenantRole> persistedTenantRoles = Collections.emptyList();

    public FailedGrantRoleAssignmentsException(String message, List<TenantRole> persistedTenantRoles, Throwable cause) {
        super(message, cause);
        this.persistedTenantRoles = persistedTenantRoles;
    }
}
