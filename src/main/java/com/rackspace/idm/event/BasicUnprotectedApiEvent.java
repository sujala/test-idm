package com.rackspace.idm.event;

import lombok.Getter;

@Getter
public class BasicUnprotectedApiEvent extends AbstractApiEvent implements UnprotectedApiEvent {

    @Override
    public String getEventType() {
        return "UnprotectedApi";
    }


    public static final class BasicUnprotectedApiEventBuilder {
        protected String nodeName;
        protected String requestUri;
        protected String remoteIp;
        protected String forwardedForIp;
        protected String eventId;

        private BasicUnprotectedApiEventBuilder() {
        }

        public static BasicUnprotectedApiEventBuilder aBasicUnprotectedApiEvent() {
            return new BasicUnprotectedApiEventBuilder();
        }

        public BasicUnprotectedApiEventBuilder nodeName(String nodeName) {
            this.nodeName = nodeName;
            return this;
        }

        public BasicUnprotectedApiEventBuilder requestUri(String requestUri) {
            this.requestUri = requestUri;
            return this;
        }

        public BasicUnprotectedApiEventBuilder remoteIp(String remoteIp) {
            this.remoteIp = remoteIp;
            return this;
        }

        public BasicUnprotectedApiEventBuilder forwardedForIp(String forwardedForIp) {
            this.forwardedForIp = forwardedForIp;
            return this;
        }

        public BasicUnprotectedApiEventBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public BasicUnprotectedApiEvent build() {
            BasicUnprotectedApiEvent basicUnprotectedApiEvent = new BasicUnprotectedApiEvent();
            basicUnprotectedApiEvent.nodeName = this.nodeName;
            basicUnprotectedApiEvent.eventId = this.eventId;
            basicUnprotectedApiEvent.forwardedForIp = this.forwardedForIp;
            basicUnprotectedApiEvent.requestUri = this.requestUri;
            basicUnprotectedApiEvent.remoteIp = this.remoteIp;
            return basicUnprotectedApiEvent;
        }
    }
}
