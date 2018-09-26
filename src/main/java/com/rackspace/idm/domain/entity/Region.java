package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;
import org.dozer.Mapping;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: wmendiza
 * Date: 10/22/12
 * Time: 2:45 PM
 * To change this template use File | Settings | File Templates.
 */

@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_REGION, auxiliaryClass = LdapRepository.OBJECTCLASS_METADATA)
public class Region implements Auditable, UniqueId, Metadata {
    @LDAPDNField()
    private String uniqueId;

    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_REGION, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_CLOUD, objectClass = LdapRepository.OBJECTCLASS_REGION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String cloud;

    @Mapping("enabled")
    @LDAPField(attribute = LdapRepository.ATTR_ENABLED, objectClass = LdapRepository.OBJECTCLASS_REGION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private Boolean isEnabled;

    @Mapping("isDefault")
    @LDAPField(attribute = LdapRepository.ATTR_USE_FOR_DEFAULT_REGION, objectClass = LdapRepository.OBJECTCLASS_REGION, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private Boolean isDefault;

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

    @Override
    public String getAuditContext() {
        return String.format("regionId=%s", name);
    }

}
