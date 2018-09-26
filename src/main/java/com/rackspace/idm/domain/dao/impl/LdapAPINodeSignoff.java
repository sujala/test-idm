package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.APINodeSignoff;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Auditable;
import com.rackspace.idm.domain.entity.Metadata;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_API_NODE_SIGNOFF, auxiliaryClass = LdapRepository.OBJECTCLASS_METADATA)
public class LdapAPINodeSignoff implements APINodeSignoff, UniqueId, Auditable, Metadata {

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

    @LDAPField(attribute=LdapRepository.ATTR_METADATA_ATTRIBUTE,
               objectClass=LdapRepository.OBJECTCLASS_METADATA,
               filterUsage=FilterUsage.CONDITIONALLY_ALLOWED
    )
    private Set<String> metadata;

    public Set<String> getMedatadata() {
        if (metadata == null) {
            metadata = new HashSet<String>();
        }
        return metadata;
    }

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
