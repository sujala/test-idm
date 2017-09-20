package com.rackspace.idm.modules.usergroups;

import com.rackspace.idm.ErrorCodes;

public class Constants {
    public static final String OBJECTCLASS_USER_GROUP = "rsUserGroup";

    public static final String USER_GROUP_BASE_DN = "ou=userGroups,ou=groups,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    public static final String ALL_TENANT_IN_DOMAIN_WILDCARD = "*";

    public static final int USER_GROUP_ALLOWED_ROLE_WEIGHT = 1000;

    public static final String USER_GROUP = "userGroup";
    public static final String RAX_AUTH_USER_GROUP = "RAX-AUTH:" + USER_GROUP;
    public static final String USER_GROUPS = "userGroups";
    public static final String RAX_AUTH_USER_GROUPS = "RAX-AUTH:" + USER_GROUPS;

    public static final String TENANT_ASSIGNMENT = "tenantAssignment";
    public static final String RAX_AUTH_TENANT_ASSIGNMENT = "RAX-AUTH:" + TENANT_ASSIGNMENT;
    public static final String TENANT_ASSIGNMENTS = "tenantAssignments";
    public static final String RAX_AUTH_TENANT_ASSIGNMENTS = "RAX-AUTH:" + TENANT_ASSIGNMENTS;

    public static final String ROLE_ASSIGNMENTS = "roleAssignments";
    public static final String RAX_AUTH_ROLE_ASSIGNMENTS = "RAX-AUTH:" + ROLE_ASSIGNMENTS;

    public static final String ERROR_CODE_USER_GROUPS_MAX_THRESHOLD_REACHED = "UG-001";
    public static final String ERROR_CODE_USER_GROUPS_MAX_THRESHOLD_REACHED_MSG = "The maximum number of groups has been reached for this domain.";

    public static final String ERROR_CODE_USER_GROUPS_DUP_ROLE_ASSIGNMENT = "UGA-000";
    public static final String ERROR_CODE_USER_GROUPS_DUP_ROLE_ASSIGNMENT_MSG = "A given role can only be specified once";


    public static final String ERROR_CODE_USER_GROUPS_MISSING_REQUIRED_ATTRIBUTE = ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE;
    public static final String ERROR_CODE_USER_GROUPS_INVALID_ATTRIBUTE = ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE;

    public static final String ERROR_CODE_ROLE_ASSIGNMENT_MISSING_FOR_TENANTS_MSG = "All role assignments must include 'forTenants' field";
    public static final String ERROR_CODE_ROLE_ASSIGNMENT_INVALID_FOR_TENANTS_MSG = "Role assignments can only be for all tenants or specific tenants in group's domain.";
    public static final String ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_ROLE_MSG_PATTERN = "Invalid assignment for role '%s'. Role does not exist.";
    public static final String ERROR_CODE_ROLE_ASSIGNMENT_GLOBAL_ROLE_ASSIGNMENT_ONLY_MSG_PATTERN = "Invalid assignment for role '%s'. This role must be assigned globally.";
    public static final String ERROR_CODE_ROLE_ASSIGNMENT_TENANT_ASSIGNMENT_ONLY_MSG_PATTERN = "Invalid assignment for role '%s'. This role must be assigned to explicit tenants.";
    public static final String ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN = "Invalid assignment for role '%s'. Not authorized to assign this role.";

    public static final String ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_TENANT_MSG_PATTERN = "Invalid assignment for role '%s'. Tenant does not exist.";
    public static final String ERROR_CODE_ROLE_ASSIGNMENT_WRONG_DOMAIN_TENANT_MSG_PATTERN = "Invalid assignment for role '%s'. Tenant must exist in group's domain";
    public static final String ERROR_CODE_ROLE_REVOKE_NOT_FOUND_MSG_PATTERN = "Invalid revocation of role '%s'. Role not assigned to group";

}
