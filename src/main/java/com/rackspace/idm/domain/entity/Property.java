package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/30/12
 * Time: 2:08 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_PROPERTY, auxiliaryClass = LdapRepository.OBJECTCLASS_METADATA)
public class Property implements UniqueId, Metadata {

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_PROPERTY, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_VALUE, objectClass = LdapRepository.OBJECTCLASS_PROPERTY, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private List<String> value;

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

}
