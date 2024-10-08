package com.rackspace.idm.domain.entity;

import com.google.common.collect.Sets;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType;
import com.rackspace.idm.annotation.DeleteNullValues;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
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
        postEncodeMethod="doPostEncode", auxiliaryClass = LdapRepository.OBJECTCLASS_METADATA)
public class DelegationAgreement implements Auditable, UniqueId, Metadata {
    private static final Logger log = LoggerFactory.getLogger(DelegationAgreement.class);

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute = LdapRepository.ATTR_ID, objectClass = LdapRepository.OBJECTCLASS_DELEGATION_AGREEMENT, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String id;

    @LDAPField(attribute = LdapRepository.ATTR_COMMON_NAME, objectClass = LdapRepository.OBJECTCLASS_DELEGATION_AGREEMENT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_DELEGATION_AGREEMENT, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String description;

    @LDAPField(attribute=LdapRepository.ATTR_RS_NEST_LEVEL,
            objectClass=LdapRepository.OBJECTCLASS_DELEGATION_AGREEMENT,
            filterUsage=FilterUsage.ALWAYS_ALLOWED)
    private Integer subAgreementNestLevel;

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

    @LDAPField(attribute=LdapRepository.ATTR_RS_DELEGATION_AGREEMENT_ID,
            objectClass=LdapRepository.OBJECTCLASS_DELEGATION_AGREEMENT,
            filterUsage=FilterUsage.ALWAYS_ALLOWED)
    private String parentDelegationAgreementId;

    @LDAPField(attribute=LdapRepository.ATTR_RS_PRINCIPAL_DOMAIN_ID,
            objectClass=LdapRepository.OBJECTCLASS_DELEGATION_AGREEMENT,
            filterUsage=FilterUsage.ALWAYS_ALLOWED)
    private String principalDomainId;

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

    public int getSubAgreementNestLevelNullSafe() {
        return subAgreementNestLevel != null ? subAgreementNestLevel : 0;
    }

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
     * Null out delegates if empty as LDAP can not store empty values. Called by UnboundId just before persisting the
     * entry.
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

    /**
     * Whether the specified user is effectively considered a delegate on the agreement. The user may be directly
     * assigned as the delegate, or, if the user is a member of a user group that is a delegate.
     *
     * @param endUser
     * @return
     */
    public boolean isEffectiveDelegate(EndUser endUser) {
        HashSet<DN> userEffectiveDns = new HashSet<>();

        // If the specified endUser is also a DelegationDelegate, then possible for the DA to include
        // the endUser's DN explicitly. If not a DelegationDelegate, then no point in adding the user's DN
        if (endUser instanceof DelegationDelegate) {
            userEffectiveDns.add(((DelegationDelegate) endUser).getDn());
        }

        // Add user groups
        userEffectiveDns.addAll(endUser.getUserGroupDNs());

        Sets.SetView<DN> view = Sets.intersection(userEffectiveDns, getDelegates());
        return !view.isEmpty();
    }

    /**
     * Whether the specified user is effectively considered a principal on the agreement. The user may be directly
     * assigned as the principal, or, if the principal is a user group, a member of the user group.
     *
     * @param endUser
     * @return
     */
    public boolean isEffectivePrincipal(EndUser endUser) {
        boolean result = false;

        if (getPrincipalDN() != null) {
            HashSet<DN> userEffectiveDns = new HashSet<>();

            // If the specified endUser is also a DelegationPrincipal, then possible for the DA to include
            // the endUser's DN explicitly. If not a DelegationPrincipal, then no point in adding the user's DN
            if (endUser instanceof DelegationPrincipal) {
                userEffectiveDns.add(((DelegationPrincipal) endUser).getDn());
            }

            // Add user groups
            userEffectiveDns.addAll(endUser.getUserGroupDNs());

            result = userEffectiveDns.contains(getPrincipalDN());
        }
        return result;
    }

    /**
     * Whether the specified potential principal is explicitly listed as the principal on the DA. This differs from
     * {@link #isEffectivePrincipal(EndUser)} in that this method does not take into consideration user group membership.
     *
     * @param principal
     * @return
     */
    public boolean isExplicitPrincipal(DelegationPrincipal principal) {
        if (principal == null || getPrincipal() == null || getPrincipal().getId() == null || principal.getId() == null) {
            return false;
        }

        return getPrincipalType() == principal.getPrincipalType() && getPrincipalDN().equals(principal.getDn());
    }

    /**
     * Whether the specified potential delegate is explicitly listed as a delegate on the DA. This differs from
     * {@link #isEffectiveDelegate(EndUser)} in that this method does not take into consideration user group membership.
     *
     * @param delegate
     * @return
     */
    public boolean isExplicitDelegate(DelegationDelegate delegate) {
        return getDelegates().contains(delegate.getDn());
    }
}
