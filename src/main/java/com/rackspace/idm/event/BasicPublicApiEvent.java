package com.rackspace.idm.event;

import lombok.Getter;

@Getter
public class BasicPublicApiEvent extends AbstractApiEvent implements PublicApiEvent {

    public BasicPublicApiEvent(IdentityApiResourceRequest resourceContext) {
        super(resourceContext);
    }

    public static final class BasicPublicApiEventBuilder {
        protected String nodeName;
        protected String remoteIp;
        protected String forwardedForIp;
        protected String requestId;
        protected IdentityApiResourceRequest resourceContext;

        private BasicPublicApiEventBuilder() {
        }

        public static BasicPublicApiEventBuilder aBasicPublicApiEvent() {
            return new BasicPublicApiEventBuilder();
        }

        public BasicPublicApiEventBuilder nodeName(String nodeName) {
            this.nodeName = nodeName;
            return this;
        }

        public BasicPublicApiEventBuilder remoteIp(String remoteIp) {
            this.remoteIp = remoteIp;
            return this;
        }

        public BasicPublicApiEventBuilder forwardedForIp(String forwardedForIp) {
            this.forwardedForIp = forwardedForIp;
            return this;
        }

        public BasicPublicApiEventBuilder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public BasicPublicApiEventBuilder resourceContext(IdentityApiResourceRequest resourceContext) {
            this.resourceContext = resourceContext;
            return this;
        }

        public BasicPublicApiEvent build() {
            if (resourceContext == null ||
                    resourceContext.getApiMethod() == null
                    || resourceContext.getContainerRequest() == null) {
                throw new IllegalStateException("Must provide a resource context to build an API event");
            }

            BasicPublicApiEvent basicUnprotectedApiEvent = new BasicPublicApiEvent(resourceContext);
            basicUnprotectedApiEvent.nodeName = this.nodeName;
            basicUnprotectedApiEvent.forwardedForIp = this.forwardedForIp;
            basicUnprotectedApiEvent.remoteIp = this.remoteIp;
            basicUnprotectedApiEvent.requestId = this.requestId;
            return basicUnprotectedApiEvent;
        }
    }
}
