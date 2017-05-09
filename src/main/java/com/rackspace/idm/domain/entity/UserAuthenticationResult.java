package com.rackspace.idm.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper=false)
public class UserAuthenticationResult extends AuthenticationResult {

    private final BaseUser user;
    private final List<String> authenticatedBy;
    private final String scope;

    public UserAuthenticationResult(BaseUser user, boolean authenticated) {
        this(user, authenticated, Collections.EMPTY_LIST);
    }

    public UserAuthenticationResult(BaseUser user, boolean authenticated, List<String> authenticatedBy) {
        this(user, authenticated, authenticatedBy, null);
    }

    public UserAuthenticationResult(BaseUser user, boolean authenticated, List<String> authenticatedBy, String scope) {
        super(authenticated);
        this.user = user;
        this.authenticatedBy = Collections.unmodifiableList(authenticatedBy);
        this.scope = scope;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
