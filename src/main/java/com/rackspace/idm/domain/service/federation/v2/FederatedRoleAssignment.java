package com.rackspace.idm.domain.service.federation.v2;

import lombok.Getter;
import org.apache.commons.lang.StringUtils;

@Getter
public class FederatedRoleAssignment {
    public static final String TENANT_ASSIGNMENT_SEPARATOR = "/";

    String roleName = null;
    String tenantName = null;
    String rawValue = null;

    public FederatedRoleAssignment(String roleAssignmentValue) {
        this.rawValue = roleAssignmentValue;

         /*
         Only parse if the value contains at least 1 non-whitespace character. Trim all provided roles
         and tenant names
         */
        if (StringUtils.isNotBlank(roleAssignmentValue)) {
            String roleName;
            String tenantName = null;

            // The _first_ occurrence of separator defines role and tenant
            int index = roleAssignmentValue.indexOf(TENANT_ASSIGNMENT_SEPARATOR);
            if (index == -1) {
                // Global role assignment
                roleName = roleAssignmentValue.trim();
            } else {
                roleName = roleAssignmentValue.substring(0, index).trim();
                String rawTenantName = roleAssignmentValue.substring(index+1).trim();

                if (StringUtils.isNotBlank(rawTenantName)) {
                    tenantName = rawTenantName;
                }
            }

            this.roleName = roleName;
            this.tenantName = tenantName;
        }
    }

    public FederatedRoleAssignment(String roleName, String tenantName, String rawValue) {
        this.roleName = roleName;
        this.tenantName = tenantName;
        this.rawValue = rawValue;
    }
}
