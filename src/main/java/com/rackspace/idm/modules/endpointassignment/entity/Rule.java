package com.rackspace.idm.modules.endpointassignment.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Auditable;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.Tenant;

import java.util.List;

/**
 * A common interface that all Endpoint Assignment rules must implement.
 */
public interface Rule extends UniqueId, Auditable {
    String LDAP_ATTRIBUTE_CN = LdapRepository.ATTR_COMMON_NAME;
    String LDAP_ATTRIBUTE_DESCRIPTION = LdapRepository.ATTR_DESCRIPTION;

    String getId();

    /**
     * Given the user and tenant, would this rule add endpoints to the user's service catalog
     *
     * @param user
     * @param tenant
     * @return
     */
    boolean matches(EndUser user, Tenant tenant);

    /**
     * Given the specified user and tenant, return the endpoint template ids that apply
     *
     * @param user
     * @param tenant
     * @return
     */
    List<String> matchingEndpointTemplateIds(EndUser user, Tenant tenant);
}
