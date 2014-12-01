package com.rackspace.idm.domain.security.encrypters.keyczar;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
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

    @LDAPField(attribute= LdapRepository.ATTR_KEY_TIMESTAMP,
            objectClass=LdapRepository.OBJECTCLASS_KEY_DESCRIPTOR,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=false
    )
    private Date timestamp;

    @LDAPField(attribute= LdapRepository.ATTR_KEY_DATA,
            objectClass=LdapRepository.OBJECTCLASS_KEY_DESCRIPTOR,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true
    )
    private String data;

}
