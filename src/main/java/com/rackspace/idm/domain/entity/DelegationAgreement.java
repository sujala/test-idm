package com.rackspace.idm.domain.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType;
import com.rackspace.idm.annotation.DeleteNullValues;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.modules.usergroups.Constants;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a delegation agreement within persistent storage
 */
@Getter
@Setter
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_DELEGATION_AGREEMENT, superiorClass={ "groupOfNames", "top" },
        postEncodeMethod="doPostEncode")
public class DelegationAgreement implements Auditable, UniqueId {
    private static final Logger log = LoggerFactory.getLogger(DelegationAgreement.class);

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute = LdapRepository.ATTR_ID, objectClass = LdapRepository.OBJECTCLASS_DELEGATION_AGREEMENT, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String id;

    @LDAPField(attribute = LdapRepository.ATTR_COMMON_NAME, objectClass = LdapRepository.OBJECTCLASS_DELEGATION_AGREEMENT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_DELEGATION_AGREEMENT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String description;

    @LDAPField(attribute = LdapRepository.ATTR_RS_PRINCIPAL_DN, objectClass = LdapRepository.OBJECTCLASS_DELEGATION_AGREEMENT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private DN principalDN;
    private DelegationPrincipal principal;

    @LDAPField(attribute=LdapRepository.ATTR_DOMAIN_ID,
            objectClass=LdapRepository.OBJECTCLASS_DELEGATION_AGREEMENT,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String domainId;

    @DeleteNullValues
    @LDAPField(attribute = LdapRepository.ATTR_MEMBER, objectClass = LdapRepository.OBJECTCLASS_DELEGATION_AGREEMENT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Set<DN> delegates;

    @Override
    public String getAuditContext() {
        return String.format("id=%s", id);
    }

    public void setPrincipal(DelegationPrincipal delegationPrincipal) {
        this.principal = delegationPrincipal;
        principalDN = delegationPrincipal.getDn();
    }

    public PrincipalType getPrincipalType() {
        PrincipalType result = null;
        if (principal != null) {
            result = principal.getPrincipalType();
        }
        return result;
    }

    public String getPrincipalId() {
        String id = null;
        if (principal != null) {
            id = principal.getId();
        }
        return id;
    }

    public boolean isFederatedUserPrincipal() {
        if (principalDN != null) {
            try {
                return principalDN.isDescendantOf(LdapRepository.EXTERNAL_PROVIDERS_BASE_DN, false);
            } catch (LDAPException e) {
                throw new IllegalStateException("Error retrieving principal type", e);
            }
        }
        return false;
    }

    public Set<DN> getDelegates() {
        if (delegates == null) {
            delegates = new HashSet<DN>();
        }
        return delegates;
    }

    /**
     * This is a hack for iteration 1 of create DA service. This will be removed in v2.
     * @return
     */
    public String getFirstDelegateId() {
        String id = null;
        if (CollectionUtils.isNotEmpty(delegates)) {
            DN dn = delegates.iterator().next();
            String rsIdDN = dn.getRDNString();
            if (StringUtils.isNotBlank(rsIdDN)) {
                String[] split = rsIdDN.split("=");
                if (split.length == 2) {
                    id = split[1];
                }
            }
        }
        return id;
    }


    /**
     * Must null out delegates if empty.
     *
     * @param entry
     * @throws LDAPPersistException
     */
    private void doPostEncode(final Entry entry) throws LDAPPersistException {
        String[] dns = entry.getAttributeValues(LdapRepository.ATTR_MEMBER);
        if (dns != null && dns.length == 0) {
            entry.removeAttribute(LdapRepository.ATTR_MEMBER);
        }
    }
}
