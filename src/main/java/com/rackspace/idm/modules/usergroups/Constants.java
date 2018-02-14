package com.rackspace.idm.modules.usergroups;

import com.rackspace.idm.ErrorCodes;

public class Constants {
    public static final String OBJECTCLASS_USER_GROUP = "rsUserGroup";

    public static final String USER_GROUP_BASE_DN = "ou=userGroups,ou=groups,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    public static final String ALL_TENANT_IN_DOMAIN_WILDCARD = "*";

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

    public static final String ERROR_CODE_USER_GROUPS_MISSING_REQUIRED_ATTRIBUTE = ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE;
    public static final String ERROR_CODE_USER_GROUPS_INVALID_ATTRIBUTE = ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE;

    public static final String ERROR_CODE_ROLE_REVOKE_NOT_FOUND_MSG_PATTERN = "Invalid revocation of role '%s'. Role not assigned to group";

    public static final String ERROR_CODE_USER_GROUPS_NOT_ENABLED_FOR_DOMAIN_MSG_PATTERN = "User groups are not supported for domain '%s'";
}
