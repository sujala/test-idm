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
public class ImpersonatedScopeAccess extends ScopeAccess implements BaseUserToken {
    public static final String IMPERSONATING_USERNAME_HARDCODED_VALUE = "<deprecated>";

    // This field must me mapped on every subclass (UnboundID LDAP SDK v2.3.6 limitation)
    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute=LdapRepository.ATTR_RACKER_ID, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String rackerId;

    @LDAPField(attribute=LdapRepository.ATTR_USER_RS_ID, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String userRsId;

    /**
     * This field is deprecated and should NOT be relied upon to identify the user being impersonated. It is a required field in the
     * schema, however, so a default/junk value will be persisted. The real value is NOT saved since it's incomplete, and
     * do not want anyone to get the mistaken belief that is fully answers the question of who the impersonated user is.
     *
     * @deprecated use {@link #getRsImpersonatingRsId()}
     */
    @Deprecated
    @LDAPField(attribute=LdapRepository.ATTR_IMPERSONATING_USERNAME, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String impersonatingUsername = IMPERSONATING_USERNAME_HARDCODED_VALUE;


    @LDAPField(attribute=LdapRepository.ATTR_IMPERSONATING_RS_ID, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String rsImpersonatingRsId;

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
            return String.format(format, this.getRackerId(), this.getRsImpersonatingRsId());
        } else {
            final String format = "User(userRsId=%s,impersonating=%s)";
            return String.format(format, this.getUserRsId(), this.getRsImpersonatingRsId());
        }
    }

    /**
     * For impersonated tokens either the rackerId OR the userRsId should be set (mutually exclusive). In existing code the
     * rackerId appears to take precedence in the off change that both are set.
     * @return
     */
    @Override
    public String getIssuedToUserId() {
        if (StringUtils.isNotBlank(getRackerId())) {
            return getRackerId();
        } else {
            return getUserRsId();
        }
    }

    /**
     * Shouldn't set this. The value is hardcoded.
     * @param impersonatingUsername
     *
     * @deprecated
     */
    @Deprecated
    public void setImpersonatingUsername(String impersonatingUsername) {
        //no-op
    }

    public String getImpersonatingUsername() {
        return IMPERSONATING_USERNAME_HARDCODED_VALUE;
    }

    /**
     * Do not yet support impersonating a user under a delegation agreement.
     *
     * @return
     */
    @Override
    public boolean isDelegationToken() {
        return false;
    }

    /**
     * Do not yet support impersonating a user under a delegation agreement.
     *
     * @return
     */
    @Override
    public String getDelegationAgreementId() {
        return null;
    }
}
