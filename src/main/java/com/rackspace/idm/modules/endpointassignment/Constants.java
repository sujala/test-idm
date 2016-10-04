package com.rackspace.idm.modules.endpointassignment;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.domain.dao.impl.LdapRepository;

public class Constants {
    public static final String ENDPOINT_ASSIGNMENT_RULE_BASE_DN = "ou=baseUrlRules,ou=rules,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    public static final String TENANT_TYPE_ENDPOINT_RULE = "tenantTypeEndpointRule";
    public static final String RAX_AUTH_TENANT_TYPE_ENDPOINT_RULE = "RAX-AUTH:" + TENANT_TYPE_ENDPOINT_RULE;

    public static final String RAX_AUTH_TENANT_TYPE_ENDPOINT_RULE_OSK_ENDPOINT_TEMPLATES_PATH
            = RAX_AUTH_TENANT_TYPE_ENDPOINT_RULE + "." + JSONConstants.OS_KSCATALOG_ENDPOINT_TEMPLATES;

    public static final String RAX_AUTH_TENANT_TYPE_ENDPOINT_RULE_ENDPOINT_TEMPLATES_PATH
            = RAX_AUTH_TENANT_TYPE_ENDPOINT_RULE + "." + JSONConstants.ENDPOINT_TEMPLATES;

    public static final String RAX_AUTH_TENANT_TYPE_ENDPOINT_RULE_ENDPOINT_TEMPLATE_PATH
            = RAX_AUTH_TENANT_TYPE_ENDPOINT_RULE_OSK_ENDPOINT_TEMPLATES_PATH + "." + JSONConstants.ENDPOINT_TEMPLATE;

    public static final String ENDPOINT_TEMPLATE_BASE_DN = LdapRepository.BASEURL_BASE_DN;
}
