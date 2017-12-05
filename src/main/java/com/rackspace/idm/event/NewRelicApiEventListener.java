package com.rackspace.idm.event;

import com.newrelic.api.agent.NewRelic;
import com.rackspace.idm.api.filter.ApiEventPostingFilter;
import com.rackspace.idm.domain.config.IdentityConfig;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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

    @PostConstruct
    public void init() {
        try {
            logSecuredAttributeSetupState();
        } catch (Exception e) {
            logger.error("Error encountered while pre-loading MAC to hash new relic attributes. Protected attributes can not be sent", e);
        }
    }

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
        SecuredAttributeSupport securedAttributeSupport = createSecuredAttributeSupport();

        postCommonAttributes(ev, enabledAttributes, securedAttributeSupport);
        addAttributeIfEnabled(CALLER_USERNAME, ev.getUserName(), enabledAttributes, securedAttributeSupport);
    }

    private void postProtectedEvent(ProtectedApiEvent ev) {
        Set<String> enabledAttributes = identityConfig.getReloadableConfig().getNewRelicCustomDataAttributesForProtectedApiResources();
        SecuredAttributeSupport securedAttributeSupport = createSecuredAttributeSupport();

        postCommonAttributes(ev, enabledAttributes, securedAttributeSupport);
        addAttributeIfEnabled(CALLER_TOKEN, ev.getMaskedCallerToken(), enabledAttributes, securedAttributeSupport);
        addAttributeIfEnabled(CALLER_ID, ev.getCaller().getId(), enabledAttributes, securedAttributeSupport);
        addAttributeIfEnabled(CALLER_USERNAME, ev.getCaller().getUsername(), enabledAttributes, securedAttributeSupport);
        addAttributeIfEnabled(CALLER_USER_TYPE, ev.getCaller().getUserType() != null ? ev.getCaller().getUserType().getRoleName() : ApiEventPostingFilter.DATA_UNAVAILABLE, enabledAttributes, securedAttributeSupport);
        addAttributeIfEnabled(EFFECTIVE_CALLER_TOKEN, ev.getMaskedEffectiveCallerToken(), enabledAttributes, securedAttributeSupport);
        addAttributeIfEnabled(EFFECTIVE_CALLER_ID, ev.getEffectiveCaller().getId(), enabledAttributes, securedAttributeSupport);
        addAttributeIfEnabled(EFFECTIVE_CALLER_USERNAME, ev.getEffectiveCaller().getUsername(), enabledAttributes, securedAttributeSupport);
        addAttributeIfEnabled(EFFECTIVE_CALLER_USER_TYPE, ev.getEffectiveCaller().getUserType() != null ? ev.getEffectiveCaller().getUserType().getRoleName() : ApiEventPostingFilter.DATA_UNAVAILABLE, enabledAttributes, securedAttributeSupport);
    }

    private void postUnprotectedEvent(UnprotectedApiEvent ev) {
        Set<String> enabledAttributes = identityConfig.getReloadableConfig().getNewRelicCustomDataAttributesForUnprotectedApiResources();
        SecuredAttributeSupport securedAttributeSupport = createSecuredAttributeSupport();
        postCommonAttributes(ev, enabledAttributes, securedAttributeSupport);
    }

    private void postCommonAttributes(ApiEvent ev, Set<String> enabledAttributes, SecuredAttributeSupport securedAttributeSupport) {
        addAttributeIfEnabled(EVENT_ID, ev.getEventId(), enabledAttributes, securedAttributeSupport);
        addAttributeIfEnabled(EVENT_TYPE, ev.getEventType(), enabledAttributes, securedAttributeSupport);
        addAttributeIfEnabled(REQUEST_URI, ev.getRequestUri(), enabledAttributes, securedAttributeSupport);
        addAttributeIfEnabled(NODE_NAME, ev.getNodeName(), enabledAttributes, securedAttributeSupport);
        addAttributeIfEnabled(REMOTE_IP, ev.getRemoteIp(), enabledAttributes, securedAttributeSupport);
        addAttributeIfEnabled(FORWARDED_IP, ev.getForwardedForIp(), enabledAttributes, securedAttributeSupport);
    }

    private void addAttributeIfEnabled(NewRelicCustomAttributesEnum nrAttribute, String value, Set<String> enabledAttributeList, SecuredAttributeSupport securedAttributeSupport) {
        if (nrAttribute.amIInListWithWildcardSupport(enabledAttributeList)) {
            String finalValue = value;
            if (identityConfig.getReloadableConfig().isFeatureSecureNewRelicApiResourceAttributesEnabled()) {
                finalValue = securedAttributeSupport.secureAttributeValueIfRequired(nrAttribute, value);
            }
            NewRelic.addCustomParameter(nrAttribute.getNewRelicAttributeName(), finalValue);
        }
    }

    private SecuredAttributeSupport createSecuredAttributeSupport() {
        Set<String> securedAttributes = identityConfig.getReloadableConfig().getNewRelicSecuredApiResourceAttributes();
        String hashKey = identityConfig.getReloadableConfig().getNewRelicSecuredApiResourceAttributesKey();

        SecuredAttributeSupport.HashAlgorithmEnum hashEnum = identityConfig.getReloadableConfig().getNewRelicSecuredApiResourceAttributesUsingSha256() ?
                SecuredAttributeSupport.HashAlgorithmEnum.SHA256 : SecuredAttributeSupport.HashAlgorithmEnum.SHA1;

        return new SecuredAttributeSupport(hashEnum, hashKey, securedAttributes);
    }

    private void logSecuredAttributeSetupState() {
        boolean sendingSecure = identityConfig.getReloadableConfig().isFeatureSecureNewRelicApiResourceAttributesEnabled();
        boolean keyConfigured = StringUtils.isNotBlank(identityConfig.getReloadableConfig().getNewRelicSecuredApiResourceAttributesKey());
        if (sendingSecure && keyConfigured) {
            logger.info("Configured to send secure new relic attributes and HMAC key is configured. Can dynamically secure attributes.");
        } else if (sendingSecure) {
            logger.error("Configured to send secure new relic attributes, but HMAC key was not configured. Protected attributes can not be sent");
        } else if (keyConfigured) {
            logger.info("Not configured to send secure new relic attributes, but an HMAC key is configured. Can dynamically secure attributes.");
        } else {
            logger.info("Not configured to send secure new relic attributes and an HMAC key was not configured. Must configure app to send secure attributes.");
        }
    }
}
