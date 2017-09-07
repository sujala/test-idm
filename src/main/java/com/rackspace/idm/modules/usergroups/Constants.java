package com.rackspace.idm.modules.usergroups;

public class Constants {
    public static final String OBJECTCLASS_USER_GROUP = "rsUserGroup";

    public static final String USER_GROUP_BASE_DN = "ou=userGroups,ou=groups,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    public static final String USER_GROUP = "userGroup";
    public static final String RAX_AUTH_USER_GROUP = "RAX-AUTH:" + USER_GROUP;

    public static final String ERROR_CODE_USER_GROUPS_MAX_THRESHOLD_REACHED = "UG-001";
    public static final String ERROR_CODE_USER_GROUPS_MAX_THRESHOLD_REACHED_MSG = "The maximum number of groups has been reached for this domain.";

}