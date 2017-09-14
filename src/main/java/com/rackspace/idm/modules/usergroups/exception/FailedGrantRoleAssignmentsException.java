package com.rackspace.idm.modules.usergroups.exception;

import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.exception.IdmException;
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
