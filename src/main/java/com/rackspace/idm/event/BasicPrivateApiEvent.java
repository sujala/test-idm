package com.rackspace.idm.event;

import lombok.Getter;

@Getter
public class BasicPrivateApiEvent extends AbstractApiEvent implements PrivateApiEvent {
    private String callerToken;
    private String effectiveCallerToken;
    private Caller caller;
    private Caller effectiveCaller;

    public BasicPrivateApiEvent(IdentityApiResourceRequest resourceContext) {
        super(resourceContext);
    }

    public static final class BasicPrivateApiEventBuilder {
        protected String nodeName;
        protected String remoteIp;
        protected String forwardedForIp;
        protected String requestId;
        protected IdentityApiResourceRequest resourceContext;
        private String callerToken;
        private String effectiveCallerToken;
        private Caller caller;
        private Caller effectiveCaller;

        private BasicPrivateApiEventBuilder() {
        }

        public static BasicPrivateApiEventBuilder aBasicPrivateApiEvent() {
            return new BasicPrivateApiEventBuilder();
        }

        public BasicPrivateApiEventBuilder nodeName(String nodeName) {
            this.nodeName = nodeName;
            return this;
        }

        public BasicPrivateApiEventBuilder callerToken(String callerToken) {
            this.callerToken = callerToken;
            return this;
        }

        public BasicPrivateApiEventBuilder effectiveCallerToken(String effectiveCallerToken) {
            this.effectiveCallerToken = effectiveCallerToken;
            return this;
        }

        public BasicPrivateApiEventBuilder remoteIp(String remoteIp) {
            this.remoteIp = remoteIp;
            return this;
        }

        public BasicPrivateApiEventBuilder forwardedForIp(String forwardedForIp) {
            this.forwardedForIp = forwardedForIp;
            return this;
        }

        public BasicPrivateApiEventBuilder caller(Caller caller) {
            this.caller = caller;
            return this;
        }

        public BasicPrivateApiEventBuilder effectiveCaller(Caller effectiveCaller) {
            this.effectiveCaller = effectiveCaller;
            return this;
        }

        public BasicPrivateApiEventBuilder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public BasicPrivateApiEventBuilder resourceContext(IdentityApiResourceRequest resourceContext) {
            this.resourceContext = resourceContext;
            return this;
        }

        public BasicPrivateApiEvent build() {
            BasicPrivateApiEvent basicPrivateApiEvent = new BasicPrivateApiEvent(resourceContext);
            basicPrivateApiEvent.nodeName = this.nodeName;
            basicPrivateApiEvent.requestId = this.requestId;
            basicPrivateApiEvent.effectiveCallerToken = this.effectiveCallerToken;
            basicPrivateApiEvent.forwardedForIp = this.forwardedForIp;
            basicPrivateApiEvent.caller = this.caller;
            basicPrivateApiEvent.callerToken = this.callerToken;
            basicPrivateApiEvent.effectiveCaller = this.effectiveCaller;
            basicPrivateApiEvent.remoteIp = this.remoteIp;
            return basicPrivateApiEvent;
        }
    }
}
