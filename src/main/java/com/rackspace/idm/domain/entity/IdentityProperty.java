package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Getter;
import lombok.Setter;
import org.dozer.Mapping;

import java.nio.charset.StandardCharsets;

@Getter
@Setter
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_IDENTITY_PROPERTY)
public class IdentityProperty implements Auditable, UniqueId {

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute = LdapRepository.ATTR_ID,
            objectClass = LdapRepository.OBJECTCLASS_IDENTITY_PROPERTY,
            inRDN = true,
            filterUsage = FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode = true)
    private String id;

    @LDAPField(attribute = LdapRepository.ATTR_COMMON_NAME,
            objectClass = LdapRepository.OBJECTCLASS_IDENTITY_PROPERTY,
            filterUsage = FilterUsage.ALWAYS_ALLOWED)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_PROPERTY_VALUE,
            objectClass = LdapRepository.OBJECTCLASS_IDENTITY_PROPERTY,
            filterUsage = FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] value;

    @LDAPField(attribute = LdapRepository.ATTR_PROPERTY_VALUE_TYPE,
            objectClass = LdapRepository.OBJECTCLASS_IDENTITY_PROPERTY,
            filterUsage = FilterUsage.ALWAYS_ALLOWED)
    private String valueType;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION,
            objectClass = LdapRepository.OBJECTCLASS_IDENTITY_PROPERTY,
            filterUsage = FilterUsage.ALWAYS_ALLOWED)
    private String description;

    @LDAPField(attribute = LdapRepository.ATTR_PROPERTY_VERSION,
            objectClass = LdapRepository.OBJECTCLASS_IDENTITY_PROPERTY,
            filterUsage = FilterUsage.ALWAYS_ALLOWED)
    private String idmVersion;

    @Mapping("searchable")
    @LDAPField(attribute = LdapRepository.ATTR_PROPERTY_SEARCHABLE,
            objectClass = LdapRepository.OBJECTCLASS_IDENTITY_PROPERTY,
            filterUsage = FilterUsage.ALWAYS_ALLOWED)
    private boolean searchable;

    @Mapping("reloadable")
    @LDAPField(attribute = LdapRepository.ATTR_PROPERTY_RELOADABLE,
            objectClass = LdapRepository.OBJECTCLASS_IDENTITY_PROPERTY,
            filterUsage = FilterUsage.ALWAYS_ALLOWED)
    private boolean reloadable;

    @Override
    public String getAuditContext() {
        return LdapRepository.OBJECTCLASS_IDENTITY_PROPERTY + "=" + id;
    }

    public void setValueAsString(String stringValue) {
        if (stringValue != null) {
            this.value = stringValue.getBytes(StandardCharsets.UTF_8);
        } else {
            this.value = null;
        }
    }

    public String getValueAsString() {
        if (this.value != null) {
            return new String(this.value, StandardCharsets.UTF_8);
        } else {
            return null;
        }
    }

}
