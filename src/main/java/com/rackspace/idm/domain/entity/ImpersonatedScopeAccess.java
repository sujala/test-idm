package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 3/14/12
 * Time: 12:14 PM
 * To change this template use File | Settings | File Templates.
 */
@Getter
@Setter
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS,requestAllAttributes=true)
public class ImpersonatedScopeAccess extends ScopeAccess {

    // This field must me mapped on every subclass (UnboundID LDAP SDK v2.3.6 limitation)
    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute=LdapRepository.ATTR_RACKER_ID, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String rackerId;

    @LDAPField(attribute=LdapRepository.ATTR_UID, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String username;

    @LDAPField(attribute=LdapRepository.ATTR_USER_RS_ID, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String userRsId;

    @Deprecated
    @LDAPField(attribute=LdapRepository.ATTR_IMPERSONATING_USERNAME, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String impersonatingUsername;

    @LDAPField(attribute=LdapRepository.ATTR_IMPERSONATING_RS_ID, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String impersonatingRsId;

    @LDAPField(attribute=LdapRepository.ATTR_IMPERSONATING_TOKEN, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String impersonatingToken;

    @Override
    @LDAPGetter(attribute=LdapRepository.ATTR_ACCESS_TOKEN, inRDN=true, filterUsage=FilterUsage.ALWAYS_ALLOWED)
    public String getAccessTokenString() {
        return super.getAccessTokenString();
    }

    private DateTime userPasswordExpirationDate;

    @Override
    public String getAuditContext() {
        if(StringUtils.isNotBlank(this.getRackerId())) {
            final String format = "User(rackerId=%s,impersonating=%s)";
            return String.format(format, this.getRackerId(), this.getImpersonatingUsername());
        } else {
            final String format = "User(userRsId=%s,impersonating=%s)";
            return String.format(format, this.getUserRsId(), this.getImpersonatingUsername());
        }
    }

}
