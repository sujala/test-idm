package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dozer.Mapping;

import java.util.Date;
import java.util.HashSet;

@Data
@EqualsAndHashCode(exclude={"uniqueId"})
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_TENANT,
        postEncodeMethod="doPostEncode")
public class Tenant implements Auditable, UniqueId {

    @LDAPDNField
    private String uniqueId;

    @Mapping("id")
    @LDAPField(attribute = LdapRepository.ATTR_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String tenantId;

    @Mapping("enabled")
    @LDAPField(attribute = LdapRepository.ATTR_ENABLED, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private Boolean enabled;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String description;

    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_TENANT_DISPLAY_NAME, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String displayName;

    @LDAPField(attribute = LdapRepository.ATTR_BASEURL_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private HashSet<String> baseUrlIds;

    @LDAPField(attribute = LdapRepository.ATTR_V_ONE_DEFAULT, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private HashSet<String> v1Defaults;

    @LDAPField(attribute = LdapRepository.ATTR_DOMAIN_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String domainId;

    @LDAPField(attribute = LdapRepository.ATTR_TYPE, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private HashSet<String> types;

    @Override
    public String getAuditContext() {
        String format = "tenantId=%s";
        return String.format(format, getTenantId());
    }

    public HashSet<String> getBaseUrlIds() {
        if (baseUrlIds == null) {
            baseUrlIds = new HashSet<String>();
        }
        return baseUrlIds;
    }

    public HashSet<String> getV1Defaults() {
        if (v1Defaults == null) {
            v1Defaults = new HashSet<String>();
        }
        return v1Defaults;
    }

    public HashSet<String> getTypes() {
        if (types == null) {
            types = new HashSet<String>();
        }
        return types;
    }

    private void doPostEncode(final Entry entry) throws LDAPPersistException {
        String[] baseUrls = entry.getAttributeValues(LdapRepository.ATTR_BASEURL_ID);
        if (baseUrls != null && baseUrls.length == 0) {
            entry.removeAttribute(LdapRepository.ATTR_BASEURL_ID);
        }

        String[] v1Defaults = entry.getAttributeValues(LdapRepository.ATTR_V_ONE_DEFAULT);
        if (v1Defaults != null && v1Defaults.length == 0) {
            entry.removeAttribute(LdapRepository.ATTR_V_ONE_DEFAULT);
        }

        String[] types = entry.getAttributeValues(LdapRepository.ATTR_TYPE);
        if (types != null && types.length == 0) {
            entry.removeAttribute(LdapRepository.ATTR_TYPE);
        }
    }
}
