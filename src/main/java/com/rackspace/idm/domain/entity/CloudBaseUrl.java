package com.rackspace.idm.domain.entity;

import com.rackspace.idm.annotation.DeleteNullValues;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dozer.Mapping;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.HashSet;

import static com.rackspace.idm.GlobalConstants.TENANT_ALIAS_PATTERN;

@Data
@EqualsAndHashCode(exclude={"uniqueId"})
@LDAPObject(structuralClass= LdapRepository.OBJECTCLASS_BASEURL,
        postEncodeMethod="doPostEncode")
public class CloudBaseUrl implements Auditable, UniqueId {
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

    @DeleteNullValues
    @LDAPField(attribute=LdapRepository.ATTR_POLICY_ID,
            objectClass=LdapRepository.OBJECTCLASS_BASEURL,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private HashSet<String> policyList;

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

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
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

    @Override
    public String getAuditContext() {
        return String.format("baseUrl=%s", baseUrlId);
    }

    public HashSet<String> getPolicyList() {
        if (policyList == null) {
            policyList = new HashSet<String>();
        }
        return policyList;
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

    private void doPostEncode(final Entry entry) throws LDAPPersistException {
        String[] policyIds = entry.getAttributeValues(LdapRepository.ATTR_POLICY_ID);
        if (policyIds != null && policyIds.length == 0) {
            entry.removeAttribute(LdapRepository.ATTR_POLICY_ID);
        }
    }
}
