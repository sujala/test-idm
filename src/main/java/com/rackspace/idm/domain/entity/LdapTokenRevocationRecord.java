package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;

@Setter
@Getter
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_TOKEN_REVOCATION_RECORD,
        postEncodeMethod="doPostEncode")
public class LdapTokenRevocationRecord implements Auditable, UniqueId, TokenRevocationRecord {

    public static final String AUTHENTICATED_BY_EMPTY_LIST_SUBSTITUTE = "<empty>";
    public static final String AUTHENTICATED_BY_ALL_SUBSTITUTE = "*";

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

    @Override
    public List<AuthenticatedByMethodGroup> getTargetAuthenticatedByMethodGroups() {
        List<AuthenticatedByMethodGroup> authBy = new ArrayList<AuthenticatedByMethodGroup>();

        for (String flattenedAuthBySet : getInternalTargetAuthenticatedBy()) {
            if (AUTHENTICATED_BY_EMPTY_LIST_SUBSTITUTE.equals(flattenedAuthBySet)) {
                authBy.add(AuthenticatedByMethodGroup.NULL);
            } else if (AUTHENTICATED_BY_ALL_SUBSTITUTE.equals(flattenedAuthBySet)){
                authBy.add(AuthenticatedByMethodGroup.ALL);
            } else {
                String[] authByValues = StringUtils.splitPreserveAllTokens(flattenedAuthBySet, ",");
                authBy.add(AuthenticatedByMethodGroup.getGroup(Arrays.asList(authByValues)));
            }
        }
        return authBy;
    }

    @Override
    public void setTargetAuthenticatedByMethodGroups(List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        List<String> flattenedAuthBy = new ArrayList<String>();
        for (AuthenticatedByMethodGroup authBySet : authenticatedByMethodGroups) {
            String flattenedSet;
            if (authBySet.matches(AuthenticatedByMethodGroup.NULL)) {
                //substitute for the empty set since ldap can't handle null/blanks
                flattenedSet = AUTHENTICATED_BY_EMPTY_LIST_SUBSTITUTE;
            } else if (authBySet.matches(AuthenticatedByMethodGroup.ALL)) {
                //substitute for the empty set since ldap can't handle null/blanks
                flattenedSet = AUTHENTICATED_BY_ALL_SUBSTITUTE;
            } else {
                flattenedSet = StringUtils.join(authBySet.getAuthenticatedByMethodsAsValues(), ",");
            }
            flattenedAuthBy.add(flattenedSet);
        }
        internalTargetAuthenticatedBy = flattenedAuthBy;
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
