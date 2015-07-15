package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.KeyVersion;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
@LDAPObject(structuralClass= LdapRepository.OBJECTCLASS_KEY_DESCRIPTOR)
public class LdapKeyVersion implements KeyVersion, UniqueId {

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute= LdapRepository.ATTR_KEY_VERSION,
            objectClass=LdapRepository.OBJECTCLASS_KEY_DESCRIPTOR,
            inRDN=true,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true
    )
    private Integer version;

    @LDAPField(attribute= LdapRepository.ATTR_CREATED_DATE,
            objectClass=LdapRepository.OBJECTCLASS_KEY_DESCRIPTOR,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=false
    )
    private Date created;

    @LDAPField(attribute= LdapRepository.ATTR_KEY_DATA,
            objectClass=LdapRepository.OBJECTCLASS_KEY_DESCRIPTOR,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true
    )
    private String data;

}
