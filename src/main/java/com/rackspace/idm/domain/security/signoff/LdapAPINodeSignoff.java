package com.rackspace.idm.domain.security.signoff;

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
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_API_NODE_SIGNOFF)
public class LdapAPINodeSignoff implements APINodeSignoff, UniqueId, Auditable {

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute= LdapRepository.ATTR_ID,
            objectClass=LdapRepository.OBJECTCLASS_API_NODE_SIGNOFF,
            inRDN=true,
            filterUsage= FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true)
    private String id;

    @LDAPField(attribute= LdapRepository.ATTR_COMMON_NAME,
            objectClass=LdapRepository.OBJECTCLASS_API_NODE_SIGNOFF,
            inRDN=false,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true
    )
    private String nodeName;

    @LDAPField(attribute= LdapRepository.ATTR_KEY_CREATED,
            objectClass=LdapRepository.OBJECTCLASS_API_NODE_SIGNOFF,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=false
    )
    private Date cachedMetaCreatedDate;

    @LDAPField(attribute= LdapRepository.ATTR_LOADED_DATE,
            objectClass=LdapRepository.OBJECTCLASS_API_NODE_SIGNOFF,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=false
    )
    private Date loadedDate;

    /**
     * The link to the meta data. In LDAP the metadata does not have an rsId. However, the cn is unique so just storing
     * the cn value here.
     */
    @LDAPField(attribute= LdapRepository.ATTR_KEY_METADATA_ID,
            objectClass=LdapRepository.OBJECTCLASS_API_NODE_SIGNOFF,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=false
    )
    private String keyMetadataId;

    @Override
    public String getAuditContext() {
        return LdapRepository.ATTR_COMMON_NAME + "=" + nodeName;
    }

}
