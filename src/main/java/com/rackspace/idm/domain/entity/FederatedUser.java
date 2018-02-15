package com.rackspace.idm.domain.entity;

import com.google.common.collect.ImmutableSet;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType;
import com.rackspace.idm.annotation.DeleteNullValues;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.dozer.Mapping;
import org.hibernate.validator.constraints.Length;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.*;

@Data
@LDAPObject(structuralClass= LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON)
public class FederatedUser implements EndUser, FederatedBaseUser {
    private static final Logger log = LoggerFactory.getLogger(FederatedUser.class);

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute= LdapRepository.ATTR_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON,
            filterUsage= FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true)
    private String id;

    @NotNull
    @Length(min = 1, max = 32)
    @javax.validation.constraints.Pattern(regexp = RegexPatterns.USERNAME, message = MessageTexts.USERNAME)
    @LDAPField(attribute=LdapRepository.ATTR_UID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON,
            inRDN=true,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String username;

    @LDAPField(attribute=LdapRepository.ATTR_MAIL,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String email;

    @Mapping("defaultRegion")
    @LDAPField(attribute=LdapRepository.ATTR_REGION,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String region;

    @LDAPField(attribute=LdapRepository.ATTR_CREATED_DATE,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED, inModify = false)
    private Date created;

    @LDAPField(attribute=LdapRepository.ATTR_UPDATED_DATE,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED, inModify = false)
    private Date updated;

    @LDAPField(attribute=LdapRepository.ATTR_DOMAIN_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String domainId;

    @LDAPField(attribute = LdapRepository.ATTR_CONTACT_ID,
            objectClass = LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON,
            filterUsage = FilterUsage.CONDITIONALLY_ALLOWED)
    private String contactId;

    @DeleteNullValues
    @LDAPField(attribute=LdapRepository.ATTR_GROUP_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED
    )
    private Set<String> rsGroupId;

    /**
     * The issuer for this federated user
     */
    @Mapping("federatedIdp")
    @LDAPField(attribute=LdapRepository.ATTR_URI,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String federatedIdpUri;

    @LDAPField(attribute=LdapRepository.ATTR_FEDERATED_USER_EXPIRED_TIMESTAMP,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Date expiredTimestamp;

    private List<TenantRole> roles;

    @DeleteNullValues
    @LDAPField(attribute=LdapRepository.ATTR_USER_GROUP_DNS,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED
    )
    private Set<DN> userGroupDNs;

    public FederatedUser() {
    }

    /**
     * Federated users that exist are always enabled.
     * @return
     */
    public boolean isDisabled() {
        return false;
    }

    /**
     * A federated user is considered expired if the expiredTimestamp is either null or contains a date before "now"
     *
     * @return
     */
    public boolean isExpired() {
        if (expiredTimestamp == null) {
            // If user doesn't have expired timestamp, is considered expired.
            return true;
        }
        DateTime expirationDate = new DateTime(expiredTimestamp);
        return expirationDate.isBeforeNow();
    }

    @Override
    public String toString() {
        return getAuditContext();
    }

    @Override
    public String getAuditContext() {
        String format = "FederatedUser(username=%s; federatedIdpUri=%s)";
        return String.format(format, getUsername(), getFederatedIdpUri());
    }

    public Set<String> getRsGroupId() {
        if (rsGroupId == null) {
            rsGroupId = new HashSet<String>();
        }
        return rsGroupId;
    }

    public List<TenantRole> getRoles() {
        if (roles == null) {
            roles = new ArrayList<TenantRole>();
        }

        return roles;
    }

    @Override
    public Set<String> getUserGroupIds() {
        Set<String> ids = new HashSet<>();

        for (DN dn : getUserGroupDNs()) {
            ids.add(dn.getRDNString().split("=")[1]);
        }
        return ImmutableSet.copyOf(ids);
    }

    public Set<DN> getUserGroupDNs() {
        if (userGroupDNs == null) {
            userGroupDNs = new HashSet<>();
        }
        return userGroupDNs;
    }

    @Override
    public PrincipalType getPrincipalType() {
        return null;
    }

    @Override
    public DN getDn() {
        DN dn = null;
        if (StringUtils.isNotBlank(uniqueId)) {
            try {
                dn = new DN(uniqueId);
            } catch (LDAPException e) {
                log.warn("Invalid uniqueId. Can't parse to DN", e);
            }
        }
        return dn;
    }
}
