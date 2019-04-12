package com.rackspace.idm.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class RackerScopeAccess extends ScopeAccess implements BaseUserToken {

    // This field must me mapped on every subclass (UnboundID LDAP SDK v2.3.6 limitation)
    private String uniqueId;

    private String rackerId;

    @Override
    public String getAccessTokenString() {
        return super.getAccessTokenString();
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

    public boolean isFederatedRackerToken() {
        return org.apache.commons.lang.StringUtils.isNotBlank(getFederatedIdpUri());
    }

    /**
     * Return the identity provider URI (part of rackerId suffixed to end of '@' or null
     *
     * @return
     */
    public String getFederatedIdpUri() {
        return Racker.getIdpUriFromFederatedId(rackerId);
    }

    /**
     * Racker tokens are not allowed to be delegated.
     *
     * @return
     */
    @Override
    public boolean isDelegationToken() {
        return false;
    }

    /**
     * Racker tokens are not allowed to be delegated.
     * @return
     */
    @Override
    public String getDelegationAgreementId() {
        return null;
    }
}
