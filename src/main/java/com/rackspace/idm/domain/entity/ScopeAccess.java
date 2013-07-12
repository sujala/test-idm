package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import lombok.Getter;
import org.joda.time.DateTime;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_SCOPEACCESS,
        postEncodeMethod="doPostEncode")
public class ScopeAccess implements Auditable, HasAccessToken {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute=LdapRepository.ATTR_CLIENT_ID, objectClass=LdapRepository.OBJECTCLASS_SCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String clientId;

    @LDAPField(attribute=LdapRepository.ATTR_CLIENT_RCN, objectClass=LdapRepository.OBJECTCLASS_SCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String clientRCN;

    @LDAPField(attribute = LdapRepository.ATTR_ACCESS_TOKEN, objectClass=LdapRepository.OBJECTCLASS_SCOPEACCESS, inRDN=true, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String accessTokenString;

    @LDAPField(attribute = LdapRepository.ATTR_ACCESS_TOKEN_EXP, objectClass=LdapRepository.OBJECTCLASS_SCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private Date accessTokenExp;

    @LDAPField(attribute = LdapRepository.ATTR_RS_TYPE, objectClass = LdapRepository.OBJECTCLASS_SCOPEACCESS, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private List<String> authenticatedBy;

    @LDAPField(attribute = LdapRepository.ATTR_CREATED_DATE, objectClass = LdapRepository.OBJECTCLASS_SCOPEACCESS, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false, inAdd = false, inModify = false)
    private Date createTimestamp;

    public ScopeAccess() {}

    public List<String> getAuthenticatedBy() {
        if (authenticatedBy == null) {
            authenticatedBy =  new ArrayList<String>();
        }
        return authenticatedBy;
    }

    public ReadOnlyEntry getLDAPEntry() {
        return ldapEntry;
    }

    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        }
        else {
            return ldapEntry.getDN();
        }
    }

    @Override
    public void setAccessTokenExpired() {
        this.accessTokenExp = new DateTime().minusDays(1).toDate();
    }

    @Override
    public boolean isAccessTokenExpired(DateTime time) {
        return StringUtils.isBlank(this.accessTokenString)
                || this.accessTokenExp == null
                || new DateTime(this.accessTokenExp).isBefore(time);
    }

    public boolean isAccessTokenWithinRefreshWindow(int refreshTokenWindow){
        DateTime accessToken = new DateTime(this.getAccessTokenExp());
        Date refreshWindowStart = accessToken.minusHours(refreshTokenWindow).toDate();
        Date now = new DateTime().toDate();
        return now.after(refreshWindowStart);
    }

    @Override
    public String getAuditContext() {
        final String format = "ScopeAccess(clientId=%s)";
        return String.format(format, getClientId());
    }

    public void setLdapEntry(ReadOnlyEntry ldapEntry) {
        this.ldapEntry = ldapEntry;
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
