package com.rackspace.idm.event;

import lombok.Getter;

@Getter
public abstract class AbstractApiEvent implements ApiEvent {
    protected String nodeName;
    protected String remoteIp;
    protected String forwardedForIp;
    protected String requestId;
    protected IdentityApiResourceRequest resourceContext;

    public AbstractApiEvent(IdentityApiResourceRequest resourceContext) {
        if (resourceContext == null ||
                resourceContext.getApiMethod() == null
                || resourceContext.getContainerRequest() == null) {
            throw new IllegalStateException("Must provide a valid resource context to build an API event");
        }

        this.resourceContext = resourceContext;
    }

    @Override
    public ApiResourceType getResourceType() {
        return resourceContext.getResourceType();
    }
}
