package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.StringUtils;
import org.dozer.Mapping;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.Set;

import static com.rackspace.idm.GlobalConstants.TENANT_ALIAS_PATTERN;

@Data
@EqualsAndHashCode(exclude={"uniqueId"})
@LDAPObject(structuralClass= LdapRepository.OBJECTCLASS_BASEURL, auxiliaryClass = LdapRepository.OBJECTCLASS_METADATA)
public class CloudBaseUrl implements Auditable, UniqueId, Metadata {
    @LDAPDNField
    private String uniqueId;

    private Boolean v1Default;

    @Mapping("id")
    @LDAPField(attribute=LdapRepository.ATTR_ID,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            inRDN=true,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true)
    private String baseUrlId;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    @LDAPField(attribute=LdapRepository.ATTR_PUBLIC_URL,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED,
            requiredForEncode=true)
    private String publicUrl;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    @LDAPField(attribute=LdapRepository.ATTR_BASEURL_TYPE,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String baseUrlType;

    @LDAPField(attribute=LdapRepository.ATTR_DEF,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Boolean def;

    @LDAPField(attribute=LdapRepository.ATTR_ENABLED,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Boolean enabled;

    @LDAPField(attribute=LdapRepository.ATTR_INTERNAL_URL,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String internalUrl;

    @Mapping("type")
    @LDAPField(attribute=LdapRepository.ATTR_OPENSTACK_TYPE,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String openstackType;

    @LDAPField(attribute=LdapRepository.ATTR_ADMIN_URL,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String adminUrl;

    @LDAPField(attribute=LdapRepository.ATTR_GLOBAL,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Boolean global;

    @LDAPField(attribute=LdapRepository.ATTR_REGION,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String region;

    @LDAPField(attribute=LdapRepository.ATTR_SERVICE,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String serviceName;

    @LDAPField(attribute=LdapRepository.ATTR_VERSION_ID,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String versionId;

    @LDAPField(attribute=LdapRepository.ATTR_VERSION_INFO,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String versionInfo;

    @LDAPField(attribute=LdapRepository.ATTR_VERSION_LIST,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage= FilterUsage.CONDITIONALLY_ALLOWED)
    private String versionList;

    @LDAPField(attribute=LdapRepository.ATTR_TENANT_ALIAS,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage= FilterUsage.CONDITIONALLY_ALLOWED)
    private String tenantAlias;

    @LDAPField(attribute=LdapRepository.ATTR_INTERNAL_URL_ID,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage= FilterUsage.CONDITIONALLY_ALLOWED)
    private String internalUrlId;

    @LDAPField(attribute=LdapRepository.ATTR_PUBLIC_URL_ID,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage= FilterUsage.CONDITIONALLY_ALLOWED)
    private String publicUrlId;

    @LDAPField(attribute=LdapRepository.ATTR_ADMIN_URL_ID,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage= FilterUsage.CONDITIONALLY_ALLOWED)
    private String adminUrlId;

    @LDAPField(attribute=LdapRepository.ATTR_CLIENT_ID,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage= FilterUsage.CONDITIONALLY_ALLOWED)
    private String clientId;

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
        return String.format("baseUrl=%s", baseUrlId);
    }

    public Boolean getGlobal() {
        if (global == null) {
            return false;
        }
        return global;
    }

    public String getTenantAlias() {
        if (tenantAlias == null) {
            return TENANT_ALIAS_PATTERN;
        } else {
            return tenantAlias;
        }
    }

    /**
     * Alter the baseUrl as it applies to the specified tenant. This includes setting the v1Default as appropriate and
     * applying any necessary aliasing.
     *
     * @param tenant
     */
    public void processBaseUrlForTenant(Tenant tenant) {
        setV1Default(tenant.getV1Defaults().contains(getBaseUrlId()));
        setPublicUrl(appendTenantToBaseUrl(getPublicUrl(), tenant.getName(), getTenantAlias()));
        setAdminUrl(appendTenantToBaseUrl(getAdminUrl(), tenant.getName(), getTenantAlias()));
        setInternalUrl(appendTenantToBaseUrl(getInternalUrl(), tenant.getName(), getTenantAlias()));
    }

    private String appendTenantToBaseUrl(String url, String tenantId, String tenantAlias) {
        if (url == null) {
            return null;
        }

        String stringToAppend =  tenantAlias != null ? tenantAlias.replace(TENANT_ALIAS_PATTERN, tenantId) : tenantId;

        if (StringUtils.isEmpty(stringToAppend)) {
            return url;
        } else if (url.endsWith("/")) {
            return url + stringToAppend;
        } else {
            return url + "/" + stringToAppend;
        }
    }

}
