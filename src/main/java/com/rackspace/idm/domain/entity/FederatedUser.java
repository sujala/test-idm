package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;
import org.dozer.Mapping;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@LDAPObject(structuralClass= LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON)
public class FederatedUser implements EndUser {

    //TODO: Not sure why this property is needed. Look into and remove if not necessary
    private String uniqueId;

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

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

    /**
     * The issuer for this federated user
     */
    @Mapping("federatedIdp")
    @LDAPField(attribute=LdapRepository.ATTR_URI,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String federatedIdpUri;

    private List<TenantRole> roles;

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
     * Federated users don't use customer id. Other code expected this on base classes though so...
     * TODO: Remove this
     *
     * @return
     */
    @Override
    public String getCustomerId() {
        return null;
    }

    @Override
    public String toString() {
        return getAuditContext();
    }

    @Override
    public String getAuditContext() {
        String format = "username=%s";
        return String.format(format, getUsername());
    }

    @Override
    public String getUniqueId() {
        if (uniqueId != null) {
            return uniqueId;
        } else if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    public List<TenantRole> getRoles() {
        if (roles == null) {
            roles = new ArrayList<TenantRole>();
        }

        return roles;
    }
}
