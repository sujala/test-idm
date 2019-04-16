package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import lombok.Data;
import org.joda.time.DateTime;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public abstract class ScopeAccess implements Auditable, UniqueId, Token {

    private String uniqueId;

    private String clientId;

    private String clientRCN;

    private String accessTokenString;

    private Date accessTokenExp;

    private List<String> authenticatedBy;

    private Date createTimestamp;

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

    @Override
    public String getAuditContext() {
        final String format = "ScopeAccess(clientId=%s)";
        return String.format(format, getClientId());
    }

    @Override
    public String toString() {
        return getAuditContext() ;
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
