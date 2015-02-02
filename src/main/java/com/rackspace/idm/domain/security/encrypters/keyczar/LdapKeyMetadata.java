package com.rackspace.idm.domain.security.encrypters.keyczar;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Auditable;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
@LDAPObject(structuralClass= LdapRepository.OBJECTCLASS_KEY_METADATA)
public class LdapKeyMetadata implements KeyMetadata, UniqueId, Auditable {

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute= LdapRepository.ATTR_COMMON_NAME,
            objectClass=LdapRepository.OBJECTCLASS_KEY_METADATA,
            inRDN=true,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true
    )
    private String name;

    @LDAPField(attribute= LdapRepository.ATTR_KEY_CREATED,
            objectClass=LdapRepository.OBJECTCLASS_KEY_METADATA,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=false
    )
    private Date created;

    @LDAPField(attribute= LdapRepository.ATTR_KEY_DATA,
            objectClass=LdapRepository.OBJECTCLASS_KEY_METADATA,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true
    )
    private String data;

    @Override
    public String getAuditContext() {
        return LdapRepository.ATTR_COMMON_NAME + "=" + name;
    }

}
