package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_TENANT)
public class Tenant implements Auditable {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute = LdapRepository.ATTR_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String tenantId;

    @LDAPField(attribute = LdapRepository.ATTR_ENABLED, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private Boolean enabled;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String description;

    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_TENANT_DISPLAY_NAME, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String displayName;

    @LDAPField(attribute = LdapRepository.ATTR_CREATED_DATE, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Date created;

    @LDAPField(attribute = LdapRepository.ATTR_UPDATED_DATE, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Date updated;

    @LDAPField(attribute = LdapRepository.ATTR_BASEURL_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String[] baseUrlIds;

    public ReadOnlyEntry getLDAPEntry() {
        return ldapEntry;
    }

    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }

    public String[] getBaseUrlIds() {
        return baseUrlIds;
    }

    public void setBaseUrlIds(String[] baseUrlIds) {
        if (baseUrlIds != null) {
            this.baseUrlIds = baseUrlIds.clone();
        } else {
            this.baseUrlIds = null;
        }
    }

    public void addBaseUrlId(String baseUrlId) {
        List<String> baseUrls = new ArrayList<String>();
        if (baseUrlIds != null) {
            Collections.addAll(baseUrls, baseUrlIds);
        }
        if (!baseUrls.contains(baseUrlId)) {
            baseUrls.add(baseUrlId);
        }
        baseUrlIds = baseUrls.toArray(new String[baseUrls.size()]);
    }

    public void removeBaseUrlId(String baseUrlId) {
        if (baseUrlIds == null || baseUrlIds.length == 0) {
            return;
        }
        List<String> baseUrls = new ArrayList<String>();
        Collections.addAll(baseUrls, baseUrlIds);
        if (baseUrls.contains(baseUrlId)) {
            baseUrls.remove(baseUrlId);
        }
        baseUrlIds = baseUrls.toArray(new String[baseUrls.size()]);
    }

    public boolean containsBaseUrlId(String baseUrlId) {
        if (baseUrlIds == null || baseUrlIds.length == 0) {
            return false;
        }
        List<String> baseUrls = new ArrayList<String>();
        Collections.addAll(baseUrls, baseUrlIds);
        return baseUrls.contains(baseUrlId);
    }

    public void copyChanges(Tenant modifiedTenant) {

        if (modifiedTenant.getDescription() != null) {
            if (StringUtils.isBlank(modifiedTenant.getDescription())) {
                setDescription(null);
            } else {
                setDescription(modifiedTenant.getDescription());
            }
        }

        if (modifiedTenant.getName() != null) {
            if (StringUtils.isBlank(modifiedTenant.getName())) {
                setDescription(null);
            } else {
                setDescription(modifiedTenant.getName());
            }
        }

        if (modifiedTenant.isEnabled() != null) {
            setEnabled(modifiedTenant.isEnabled());
        }
    }

    @Override
    public String getAuditContext() {
        String format = "tenantId=%s";
        return String.format(format, getTenantId());
    }

    @Override
    public String toString() {
        return getAuditContext();
    }
}
