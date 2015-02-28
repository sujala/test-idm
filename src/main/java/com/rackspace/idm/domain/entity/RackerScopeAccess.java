package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.joda.time.DateTime;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper=false)
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_RACKERSCOPEACCESS,requestAllAttributes=true)
public class RackerScopeAccess extends ScopeAccess implements HasRefreshToken, BaseUserToken {

    // This field must me mapped on every subclass (UnboundID LDAP SDK v2.3.6 limitation)
    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute=LdapRepository.ATTR_REFRESH_TOKEN, objectClass=LdapRepository.OBJECTCLASS_RACKERSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String refreshTokenString;

    @LDAPField(attribute=LdapRepository.ATTR_REFRESH_TOKEN_EXP, objectClass=LdapRepository.OBJECTCLASS_RACKERSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private Date refreshTokenExp;

    @LDAPField(attribute=LdapRepository.ATTR_RACKER_ID, objectClass=LdapRepository.OBJECTCLASS_RACKERSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String rackerId;

    @Override
    @LDAPGetter(attribute=LdapRepository.ATTR_ACCESS_TOKEN, inRDN=true, filterUsage=FilterUsage.ALWAYS_ALLOWED)
    public String getAccessTokenString() {
        return super.getAccessTokenString();
    }

    @Override
    public void setRefreshTokenExpired() {
        this.refreshTokenExp = new DateTime().minusDays(1).toDate();
    }

    @Override
    public boolean isRefreshTokenExpired(DateTime time) {
        return StringUtils.isBlank(this.refreshTokenString)
        || this.refreshTokenExp == null
        || new DateTime(this.refreshTokenExp).isBefore(time);
    }

    @Override
    public String getAuditContext() {
        final String format = "Racker(rackerId=%s)";
        return String.format(format, getRackerId());
    }

    @Override
    public String getIssuedToUserId() {
        return getRackerId();
    }
}
