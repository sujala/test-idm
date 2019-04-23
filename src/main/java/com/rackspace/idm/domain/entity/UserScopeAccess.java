package com.rackspace.idm.domain.entity;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

@Getter
@Setter
public class UserScopeAccess extends ScopeAccess implements BaseUserToken {

    private String uniqueId;

    private String userRsId;

    private String authenticationDomainId;

    @Override
    public String getAccessTokenString() {
        return super.getAccessTokenString();
    }

    private String delegationAgreementId;

    @Override
    public String getAuditContext() {
        final String format = "User(userRsId=%s)";
        return String.format(format, this.getUserRsId());
    }

    @Override
    public String getIssuedToUserId() {
        return getUserRsId();
    }

    @Override
    public boolean isDelegationToken() {
        return StringUtils.isNotBlank(delegationAgreementId);
    }
}
