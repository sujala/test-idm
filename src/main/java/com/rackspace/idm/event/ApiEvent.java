package com.rackspace.idm.event;

import javax.annotation.Nullable;

public interface ApiEvent extends IdmEvent {
    @Nullable
    String getRequestUri();

    @Nullable
    String getRemoteIp();

    @Nullable
    String getForwardedForIp();

}
