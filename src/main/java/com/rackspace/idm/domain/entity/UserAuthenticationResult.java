package com.rackspace.idm.domain.entity;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class UserAuthenticationResult extends AuthenticationResult {

    private final BaseUser user;
    private final List<String> authenticatedBy;

    public UserAuthenticationResult(BaseUser user, boolean authenticated) {
        this(user, authenticated, Collections.EMPTY_LIST);
    }

    public UserAuthenticationResult(BaseUser user, boolean authenticated, List<String> authenticatedBy) {
        super(authenticated);
        this.user = user;
        this.authenticatedBy = Collections.unmodifiableList(authenticatedBy);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
