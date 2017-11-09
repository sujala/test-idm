package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import org.joda.time.DateTime;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_SCOPEACCESS,
        postEncodeMethod="doPostEncode")
public class ScopeAccess implements Auditable, UniqueId, Token {

    // This field must me mapped on every subclass (UnboundID LDAP SDK v2.3.6 limitation)
    @LDAPDNField
    private String uniqueId;

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

    @LDAPField(attribute = LdapRepository.ATTR_SCOPE, objectClass=LdapRepository.OBJECTCLASS_SCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String scope;

    public ScopeAccess() {}

    public List<String> getAuthenticatedBy() {
        if (authenticatedBy == null) {
            authenticatedBy =  new ArrayList<String>();
        }
        return authenticatedBy;
    }

    public void setAccessTokenExpired() {
        this.accessTokenExp = new DateTime().minusDays(1).toDate();
    }

    public boolean isAccessTokenExpired(DateTime time) {
        return StringUtils.isBlank(this.accessTokenString)
                || this.accessTokenExp == null
                || new DateTime(this.accessTokenExp).isBefore(time);
    }

    @Override
    public boolean isAccessTokenExpired() {
        return isAccessTokenExpired(new DateTime());
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

    @Override
    public String getMaskedAccessTokenString() {
        String masked = null;
        if (org.apache.commons.lang.StringUtils.isNotBlank(accessTokenString)) {
            masked = org.apache.commons.lang.StringUtils.repeat("*", 5) + org.apache.commons.lang.StringUtils.right(accessTokenString, 4);
        }
        return masked;
    }
}
