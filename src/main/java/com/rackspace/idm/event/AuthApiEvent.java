package com.rackspace.idm.event;

import javax.annotation.Nullable;

public interface AuthApiEvent extends ApiEvent {
    /**
     * The username of the user account being authenticated against
     *
     * @return
     */
    @Nullable
    String getUserName();
}
