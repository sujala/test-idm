package com.rackspace.idm.event;

import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import lombok.Getter;

@Getter
public class BasicProtectedApiEvent extends AbstractApiEvent implements ProtectedApiEvent {
    private String maskedCallerToken;
    private String maskedEffectiveCallerToken;
    private Caller caller;
    private Caller effectiveCaller;

    @Override
    public String getEventType() {
        return "ProtectedApi";
    }


    public static final class BasicProtectedApiEventBuilder {
        protected String nodeName;
        protected String requestUri;
        protected String remoteIp;
        protected String forwardedForIp;
        protected String eventId;
        private String maskedCallerToken;
        private String maskedEffectiveCallerToken;
        private Caller caller;
        private Caller effectiveCaller;

        private BasicProtectedApiEventBuilder() {
        }

        public static BasicProtectedApiEventBuilder aBasicProtectedApiEvent() {
            return new BasicProtectedApiEventBuilder();
        }

        public BasicProtectedApiEventBuilder nodeName(String nodeName) {
            this.nodeName = nodeName;
            return this;
        }

        public BasicProtectedApiEventBuilder requestUri(String requestUri) {
            this.requestUri = requestUri;
            return this;
        }

        public BasicProtectedApiEventBuilder remoteIp(String remoteIp) {
            this.remoteIp = remoteIp;
            return this;
        }

        public BasicProtectedApiEventBuilder forwardedForIp(String forwardedForIp) {
            this.forwardedForIp = forwardedForIp;
            return this;
        }

        public BasicProtectedApiEventBuilder maskedCallerToken(String maskedCallerToken) {
            this.maskedCallerToken = maskedCallerToken;
            return this;
        }

        public BasicProtectedApiEventBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public BasicProtectedApiEventBuilder maskedEffectiveCallerToken(String maskedEffectiveCallerToken) {
            this.maskedEffectiveCallerToken = maskedEffectiveCallerToken;
            return this;
        }

        public BasicProtectedApiEventBuilder caller(Caller caller) {
            this.caller = caller;
            return this;
        }

        public BasicProtectedApiEventBuilder effectiveCaller(Caller effectiveCaller) {
            this.effectiveCaller = effectiveCaller;
            return this;
        }

        public BasicProtectedApiEvent build() {
            BasicProtectedApiEvent basicProtectedApiEvent = new BasicProtectedApiEvent();
            basicProtectedApiEvent.effectiveCaller = this.effectiveCaller;
            basicProtectedApiEvent.remoteIp = this.remoteIp;
            basicProtectedApiEvent.eventId = this.eventId;
            basicProtectedApiEvent.nodeName = this.nodeName;
            basicProtectedApiEvent.maskedEffectiveCallerToken = this.maskedEffectiveCallerToken;
            basicProtectedApiEvent.caller = this.caller;
            basicProtectedApiEvent.forwardedForIp = this.forwardedForIp;
            basicProtectedApiEvent.maskedCallerToken = this.maskedCallerToken;
            basicProtectedApiEvent.requestUri = this.requestUri;
            return basicProtectedApiEvent;
        }
    }
}
