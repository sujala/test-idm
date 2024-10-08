package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;

@Setter
@Getter
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_TOKEN_REVOCATION_RECORD,
        postEncodeMethod="doPostEncode", auxiliaryClass = LdapRepository.OBJECTCLASS_METADATA)
public class LdapTokenRevocationRecord implements Auditable, UniqueId, TokenRevocationRecord, Metadata {

    // This field must me mapped on every subclass (UnboundID LDAP SDK v2.3.6 limitation)
    @Setter
    @Getter
    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute= LdapRepository.ATTR_ID,
            objectClass=LdapRepository.OBJECTCLASS_TOKEN_REVOCATION_RECORD,
            inRDN=true,
            filterUsage= FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true)
    private String id;

    @Setter
    @Getter
    @LDAPField(attribute = LdapRepository.ATTR_CREATED_DATE, objectClass = LdapRepository.OBJECTCLASS_TOKEN_REVOCATION_RECORD, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false, inAdd = false, inModify = false)
    private Date createTimestamp;

    @Setter
    @Getter
    @LDAPField(attribute = LdapRepository.ATTR_ACCESS_TOKEN, objectClass=LdapRepository.OBJECTCLASS_TOKEN_REVOCATION_RECORD, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String targetToken;

    @Setter
    @Getter
    @LDAPField(attribute = LdapRepository.ATTR_USER_RS_ID, objectClass=LdapRepository.OBJECTCLASS_TOKEN_REVOCATION_RECORD, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String targetIssuedToId;

    @Setter
    @Getter
    @LDAPField(attribute = LdapRepository.ATTR_ACCESS_TOKEN_EXP, objectClass=LdapRepository.OBJECTCLASS_TOKEN_REVOCATION_RECORD, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private Date targetCreatedBefore;

    @Setter
    @Getter
    @LDAPField(attribute = LdapRepository.ATTR_IDENTITY_PROVIDER_ID, objectClass=LdapRepository.OBJECTCLASS_TOKEN_REVOCATION_RECORD)
    private String identityProviderId;

    /*
     * Internal aspect. Don't set this directly from code. Only allow UnboundId to set directly when loading from LDAP
     */
    @LDAPField(attribute = LdapRepository.ATTR_RS_TYPE, objectClass = LdapRepository.OBJECTCLASS_TOKEN_REVOCATION_RECORD, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private List<String> internalTargetAuthenticatedBy;

    private List<String> getInternalTargetAuthenticatedBy() {
        if (internalTargetAuthenticatedBy == null) {
            internalTargetAuthenticatedBy =  new ArrayList<String>();
        }
        return internalTargetAuthenticatedBy;
    }

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
    public List<AuthenticatedByMethodGroup> getTargetAuthenticatedByMethodGroups() {
        return TokenRevocationRecordUtil.getAuthdByGroupsFromAuthByStrings(internalTargetAuthenticatedBy);
    }

    @Override
    public void setTargetAuthenticatedByMethodGroups(List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        internalTargetAuthenticatedBy = TokenRevocationRecordUtil.getAuthByStringsFromAuthByGroups(authenticatedByMethodGroups);
    }

    @Override
    public String getAuditContext() {
        final String format = "TokenRevocationRecord(rsId=%s)";
        return String.format(format, getId());
    }

    @Override
    public String toString() {
        return getAuditContext() ;
    }

    private void doPostEncode(final Entry entry) throws LDAPPersistException {
        String[] rsTypeList = entry.getAttributeValues(LdapRepository.ATTR_RS_TYPE);
        if (rsTypeList != null && rsTypeList.length == 0) {
            entry.removeAttribute(LdapRepository.ATTR_RS_TYPE);
        }
    }
}
