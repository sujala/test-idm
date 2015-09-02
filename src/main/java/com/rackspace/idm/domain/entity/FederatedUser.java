package com.rackspace.idm.domain.entity;

import com.rackspace.idm.annotation.DeleteNullValues;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import org.dozer.Mapping;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.util.*;

@Data
@LDAPObject(structuralClass= LdapRepository.OBJECTCLASS_RACKSPACE_FEDERATED_PERSON)
public class FederatedUser implements EndUser, FederatedBaseUser {

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
}
