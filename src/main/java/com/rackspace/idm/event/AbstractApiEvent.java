package com.rackspace.idm.event;

import lombok.Getter;
import lombok.experimental.Builder;

@Getter
public abstract class AbstractApiEvent implements ApiEvent {
    protected String nodeName;
    protected String requestUri;
    protected String remoteIp;
    protected String forwardedForIp;
    protected String eventId;
}
