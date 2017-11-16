package com.rackspace.idm.event;

import lombok.Getter;

@Getter
public class BasicAuthApiEvent extends AbstractApiEvent implements AuthApiEvent {
    private String userName;

    @Override
    public String getEventType() {
        return "AuthApi";
    }


    public static final class BasicAuthApiEventBuilder {
        protected String nodeName;
        protected String requestUri;
        protected String remoteIp;
        protected String forwardedForIp;
        protected String eventId;
        private String userName;

        private BasicAuthApiEventBuilder() {
        }

        public static BasicAuthApiEventBuilder aBasicAuthApiEvent() {
            return new BasicAuthApiEventBuilder();
        }

        public BasicAuthApiEventBuilder nodeName(String nodeName) {
            this.nodeName = nodeName;
            return this;
        }

        public BasicAuthApiEventBuilder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public BasicAuthApiEventBuilder requestUri(String requestUri) {
            this.requestUri = requestUri;
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

        public BasicAuthApiEventBuilder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public BasicAuthApiEvent build() {
            BasicAuthApiEvent basicAuthApiEvent = new BasicAuthApiEvent();
            basicAuthApiEvent.nodeName = this.nodeName;
            basicAuthApiEvent.eventId = this.eventId;
            basicAuthApiEvent.forwardedForIp = this.forwardedForIp;
            basicAuthApiEvent.userName = this.userName;
            basicAuthApiEvent.requestUri = this.requestUri;
            basicAuthApiEvent.remoteIp = this.remoteIp;
            return basicAuthApiEvent;
        }
    }
}
