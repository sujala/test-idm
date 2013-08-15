package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import org.joda.time.DateTime;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.Date;

@Data
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS,requestAllAttributes=true)
public class DelegatedClientScopeAccess extends ScopeAccess implements HasRefreshToken  {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute=LdapRepository.ATTR_REFRESH_TOKEN, objectClass=LdapRepository.OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String refreshTokenString;

    @LDAPField(attribute=LdapRepository.ATTR_REFRESH_TOKEN_EXP, objectClass=LdapRepository.OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private Date refreshTokenExp;
    
    @LDAPField(attribute=LdapRepository.ATTR_AUTH_CODE, objectClass=LdapRepository.OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false) 
    private String authCode;
    
    @LDAPField(attribute=LdapRepository.ATTR_AUTH_CODE_EXP, objectClass=LdapRepository.OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false) 
    private Date authCodeExp;
    
    @LDAPField(attribute=LdapRepository.ATTR_UID, objectClass=LdapRepository.OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String username;
    
    @LDAPField(attribute=LdapRepository.ATTR_USER_RS_ID, objectClass=LdapRepository.OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String userRsId;

    @LDAPField(attribute=LdapRepository.ATTR_USER_RCN, objectClass=LdapRepository.OBJECTCLASS_DELEGATEDCLIENTSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String userRCN;
    
    @Override
    @LDAPGetter(attribute=LdapRepository.ATTR_ACCESS_TOKEN, inRDN=true, filterUsage=FilterUsage.ALWAYS_ALLOWED)
    public String getAccessTokenString() {
        return super.getAccessTokenString();
    }

    @Override
    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        }
        else {
            return ldapEntry.getDN();
        }
    }

    @Override
    public void setRefreshTokenExpired() {
        this.refreshTokenExp = new DateTime().minusDays(1).toDate();
    }

    public String getAuthCode() {
        return authCode;
    }

    public Date getAuthCodeExp() {
        return authCodeExp;
    }

    @Override
    public boolean isRefreshTokenExpired(DateTime time) {
        return StringUtils.isBlank(this.refreshTokenString)
        || this.refreshTokenExp == null
        || new DateTime(this.refreshTokenExp).isBefore(time);
    }

    public boolean isAuthorizationCodeExpired(DateTime time) {
        
        if (StringUtils.isBlank(this.authCode) || this.authCodeExp == null) {
            return true;
        }
            
        DateTime exp = new DateTime(this.authCodeExp);
        return exp.isBefore(time);
    }
    
    @Override
    public String getAuditContext() {
        final String format = "User(username=%s,customerId=%s)DelegatedToClient(clientId=%s,customerId=%s)";
        return String.format(format, this.getUsername(), this.getUserRCN(), this.getClientId(), this.getClientRCN());
    }
}
