package com.rackspace.idm.event;

import com.newrelic.api.agent.NewRelic;
import com.rackspace.idm.api.filter.ApiEventPostingFilter;
import com.rackspace.idm.domain.config.IdentityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.rackspace.idm.event.NewRelicCustomAttributesEnum.*;

/**
 * A listener on API events that will augment the new relic transaction with additional data. This listener MUST process
 * the events in the same thread that published the event in order for the custom attributes to get associated with the
 * NewRelic transaction.
 */
@Component
public class NewRelicApiEventListener implements ApplicationListener<ApiEventSpringWrapper> {
    private final Logger logger = LoggerFactory.getLogger(NewRelicApiEventListener.class);

    @Autowired
    protected IdentityConfig identityConfig;

    @Override
    public final void onApplicationEvent(ApiEventSpringWrapper eventWrapper) {
        try {
            if (!identityConfig.getReloadableConfig().isFeatureSendNewRelicCustomDataEnabled()) {
                return;
            }

            ApiEvent ev = eventWrapper.getEvent();

            if (ev instanceof AuthApiEvent) {
                AuthApiEvent authApiEvent = (AuthApiEvent) ev;
                postAuthEvent(authApiEvent);
            } else if (ev instanceof ProtectedApiEvent) {
                ProtectedApiEvent protectedApiEvent = (ProtectedApiEvent) ev;
                postProtectedEvent(protectedApiEvent);
            } else if (ev instanceof UnprotectedApiEvent) {
                UnprotectedApiEvent unprotectedApiEvent = (UnprotectedApiEvent) ev;
                postUnprotectedEvent(unprotectedApiEvent);
            }
        } catch (Exception e) {
            logger.warn("Error adding new relic custom attributes to api event", e);
        }
    }

    private void postAuthEvent(AuthApiEvent ev) {
        Set<String> enabledAttributes = identityConfig.getReloadableConfig().getNewRelicCustomDataAttributesForAuthApiResources();
        postCommonAttributes(ev, enabledAttributes);
        addAttributeIfEnabled(CALLER_USERNAME, ev.getUserName(), enabledAttributes);
    }

    private void postProtectedEvent(ProtectedApiEvent ev) {
        Set<String> enabledAttributes = identityConfig.getReloadableConfig().getNewRelicCustomDataAttributesForProtectedApiResources();
        postCommonAttributes(ev, enabledAttributes);

        addAttributeIfEnabled(CALLER_TOKEN, ev.getMaskedCallerToken(), enabledAttributes);
        addAttributeIfEnabled(CALLER_ID, ev.getCaller().getId(), enabledAttributes);
        addAttributeIfEnabled(CALLER_USERNAME, ev.getCaller().getUsername(), enabledAttributes);
        addAttributeIfEnabled(CALLER_USER_TYPE, ev.getCaller().getUserType() != null ? ev.getCaller().getUserType().getRoleName() : ApiEventPostingFilter.DATA_UNAVAILABLE, enabledAttributes);
        addAttributeIfEnabled(EFFECTIVE_CALLER_TOKEN, ev.getMaskedEffectiveCallerToken(), enabledAttributes);
        addAttributeIfEnabled(EFFECTIVE_CALLER_ID, ev.getEffectiveCaller().getId(), enabledAttributes);
        addAttributeIfEnabled(EFFECTIVE_CALLER_USERNAME, ev.getEffectiveCaller().getUsername(), enabledAttributes);
        addAttributeIfEnabled(EFFECTIVE_CALLER_USER_TYPE, ev.getEffectiveCaller().getUserType() != null ? ev.getEffectiveCaller().getUserType().getRoleName() : ApiEventPostingFilter.DATA_UNAVAILABLE, enabledAttributes);
    }

    private void postUnprotectedEvent(UnprotectedApiEvent ev) {
        Set<String> enabledAttributes = identityConfig.getReloadableConfig().getNewRelicCustomDataAttributesForUnprotectedApiResources();
        postCommonAttributes(ev, enabledAttributes);
    }

    private void postCommonAttributes(ApiEvent ev, Set<String> enabledAttributes) {
        addAttributeIfEnabled(EVENT_ID, ev.getEventId(), enabledAttributes);
        addAttributeIfEnabled(EVENT_TYPE, ev.getEventType(), enabledAttributes);
        addAttributeIfEnabled(REQUEST_URI, ev.getRequestUri(), enabledAttributes);
        addAttributeIfEnabled(NODE_NAME, ev.getNodeName(), enabledAttributes);
        addAttributeIfEnabled(REMOTE_IP, ev.getRemoteIp(), enabledAttributes);
        addAttributeIfEnabled(FORWARDED_IP, ev.getForwardedForIp(), enabledAttributes);
    }

    private void addAttributeIfEnabled(NewRelicCustomAttributesEnum nrAttribute, String value, Set<String> enabledAttributeList) {
        if (nrAttribute.amIInListWithWildcardSupport(enabledAttributeList)) {
            NewRelic.addCustomParameter(nrAttribute.getNewRelicAttributeName(), value);
        }
    }

}
