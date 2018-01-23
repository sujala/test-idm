package com.rackspace.idm.event;

import lombok.Getter;

@Getter
public class BasicAuthApiEvent extends AbstractApiEvent implements AuthApiEvent {
    private String userName;

    public BasicAuthApiEvent(IdentityApiResourceRequest resourceContext) {
        super(resourceContext);
    }

    public static final class BasicAuthApiEventBuilder {
        protected String nodeName;
        protected String remoteIp;
        protected String forwardedForIp;
        protected String requestId;
        protected IdentityApiResourceRequest resourceContext;
        private String userName;

        private BasicAuthApiEventBuilder() {
        }

        public static BasicAuthApiEventBuilder aBasicAuthApiEvent() {
            return new BasicAuthApiEventBuilder();
        }

        public BasicAuthApiEventBuilder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public BasicAuthApiEventBuilder nodeName(String nodeName) {
            this.nodeName = nodeName;
            return this;
        }

        public BasicAuthApiEventBuilder remoteIp(String remoteIp) {
            this.remoteIp = remoteIp;
            return this;
        }

        public BasicAuthApiEventBuilder forwardedForIp(String forwardedForIp) {
            this.forwardedForIp = forwardedForIp;
            return this;
        }

        public BasicAuthApiEventBuilder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public BasicAuthApiEventBuilder resourceContext(IdentityApiResourceRequest resourceContext) {
            this.resourceContext = resourceContext;
            return this;
        }

        public BasicAuthApiEvent build() {
            BasicAuthApiEvent basicAuthApiEvent = new BasicAuthApiEvent(resourceContext);
            basicAuthApiEvent.userName = this.userName;
            basicAuthApiEvent.nodeName = this.nodeName;
            basicAuthApiEvent.forwardedForIp = this.forwardedForIp;
            basicAuthApiEvent.remoteIp = this.remoteIp;
            basicAuthApiEvent.requestId = this.requestId;
            return basicAuthApiEvent;
        }
    }
}
