package com.rackspace.idm.modules.endpointassignment.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Auditable;

/**
 * A common interface that all Endpoint Assignment rules must implement.
 */
public interface Rule extends UniqueId, Auditable {
    String LDAP_ATTRIBUTE_CN = LdapRepository.ATTR_COMMON_NAME;
    String LDAP_ATTRIBUTE_DESCRIPTION = LdapRepository.ATTR_DESCRIPTION;

    String getId();
}
