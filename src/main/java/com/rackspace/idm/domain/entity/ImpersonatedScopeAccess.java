package com.rackspace.idm.domain.entity;

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
public class ImpersonatedScopeAccess extends ScopeAccess implements BaseUserToken {
    public static final String IMPERSONATING_USERNAME_HARDCODED_VALUE = "<deprecated>";

    private String uniqueId;

    private String rackerId;

    private String userRsId;

    private String rsImpersonatingRsId;

    private String impersonatingToken;

    @Override
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
