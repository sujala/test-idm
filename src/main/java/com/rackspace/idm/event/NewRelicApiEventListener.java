package com.rackspace.idm.event;

import com.google.common.collect.ImmutableMap;
import com.newrelic.api.agent.NewRelic;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.config.IdmVersion;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MultivaluedMap;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.rackspace.idm.event.NewRelicCustomAttributesEnum.*;
import static com.rackspace.idm.event.ApiEventPostingAdvice.DATA_UNAVAILABLE;

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

    @Autowired
    private IdmVersion idmVersion;

    public static final String v2TokenValidationAbsolutePathPatternRegex = "^.*/cloud/v2.0/tokens/([^/]+)/?$";
    public static final String v2TokenEndpointAbsolutePathPatternRegex = "^.*/cloud/v2.0/tokens/([^/]+)/endpoints/?$";
    public static final String v11TokenValidationAbsolutePathPatternRegex = "^.*/cloud/v1.1/token/([^/]+)/?$";

    /**
     * A set of pre-compiled patterns matching the regex constants.
     */
    Map<String, Pattern> compiledPatterns = ImmutableMap.<String, Pattern>builder()
            .put(v2TokenValidationAbsolutePathPatternRegex, Pattern.compile(v2TokenValidationAbsolutePathPatternRegex))
            .put(v2TokenEndpointAbsolutePathPatternRegex, Pattern.compile(v2TokenEndpointAbsolutePathPatternRegex))
            .put(v11TokenValidationAbsolutePathPatternRegex, Pattern.compile(v11TokenValidationAbsolutePathPatternRegex))
            .build();

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
            } else if (ev instanceof PrivateApiEvent) {
                PrivateApiEvent privateApiEvent = (PrivateApiEvent) ev;
                postPrivateEvent(privateApiEvent);
            } else if (ev instanceof PublicApiEvent) {
                PublicApiEvent publicApiEvent = (PublicApiEvent) ev;
                postPublicEvent(publicApiEvent);
            }
        } catch (Exception e) {
            logger.warn("Error adding new relic custom attributes to api event", e);
        }
    }

    private void postAuthEvent(AuthApiEvent ev) {
        Set<String> includedAttributes = identityConfig.getReloadableConfig().getIncludedNewRelicCustomDataAttributesForAuthResources();
        Set<String> excludedAttributes = identityConfig.getReloadableConfig().getExcludedNewRelicCustomDataAttributesForAuthResources();
        ReportableAttributeSupport reportableAttributeSupport = new ReportableAttributeSupport(includedAttributes, excludedAttributes);
        SecuredAttributeSupport securedAttributeSupport = createSecuredAttributeSupport();

        postCommonAttributes(ev, reportableAttributeSupport, securedAttributeSupport);
        addAttributeIfEnabled(CALLER_USERNAME, ev.getUserName(), reportableAttributeSupport, securedAttributeSupport);
    }

    private void postPrivateEvent(PrivateApiEvent ev) {
        Set<String> includedAttributes = identityConfig.getReloadableConfig().getIncludedNewRelicCustomDataAttributesForPrivateResources();
        Set<String> excludedAttributes = identityConfig.getReloadableConfig().getExcludedNewRelicCustomDataAttributesForPrivateResources();
        ReportableAttributeSupport reportableAttributeSupport = new ReportableAttributeSupport(includedAttributes, excludedAttributes);
        SecuredAttributeSupport securedAttributeSupport = createSecuredAttributeSupport();

        postCommonAttributes(ev, reportableAttributeSupport, securedAttributeSupport);
        addAttributeIfEnabled(CALLER_TOKEN, ev.getCallerToken(), reportableAttributeSupport, securedAttributeSupport);
        addAttributeIfEnabled(CALLER_ID, ev.getCaller().getId(), reportableAttributeSupport, securedAttributeSupport);
        addAttributeIfEnabled(CALLER_USERNAME, ev.getCaller().getUsername(), reportableAttributeSupport, securedAttributeSupport);
        addAttributeIfEnabled(CALLER_USER_TYPE, ev.getCaller().getUserType() != null ? ev.getCaller().getUserType().getRoleName() : DATA_UNAVAILABLE, reportableAttributeSupport, securedAttributeSupport);
        addAttributeIfEnabled(EFFECTIVE_CALLER_TOKEN, ev.getEffectiveCallerToken(), reportableAttributeSupport, securedAttributeSupport);
        addAttributeIfEnabled(EFFECTIVE_CALLER_ID, ev.getEffectiveCaller().getId(), reportableAttributeSupport, securedAttributeSupport);
        addAttributeIfEnabled(EFFECTIVE_CALLER_USERNAME, ev.getEffectiveCaller().getUsername(), reportableAttributeSupport, securedAttributeSupport);
        addAttributeIfEnabled(EFFECTIVE_CALLER_USER_TYPE, ev.getEffectiveCaller().getUserType() != null ? ev.getEffectiveCaller().getUserType().getRoleName() : DATA_UNAVAILABLE, reportableAttributeSupport, securedAttributeSupport);
    }

    private void postPublicEvent(PublicApiEvent ev) {
        Set<String> includedAttributes = identityConfig.getReloadableConfig().getIncludedNewRelicCustomDataAttributesForPublicResources();
        Set<String> excludedAttributes = identityConfig.getReloadableConfig().getExcludedNewRelicCustomDataAttributesForPublicResources();
        ReportableAttributeSupport reportableAttributeSupport = new ReportableAttributeSupport(includedAttributes, excludedAttributes);
        SecuredAttributeSupport securedAttributeSupport = createSecuredAttributeSupport();

        postCommonAttributes(ev, reportableAttributeSupport, securedAttributeSupport);
    }

    private void postCommonAttributes(ApiEvent ev, ReportableAttributeSupport reportableAttributeSupport, SecuredAttributeSupport securedAttributeSupport) {
        addAttributeIfEnabled(REQUEST_ID, ev.getRequestId(), reportableAttributeSupport, securedAttributeSupport);
        addAttributeIfEnabled(RESOURCE_TYPE, ev.getResourceType().getReportValue(), reportableAttributeSupport, securedAttributeSupport);
        addAttributeIfEnabled(RESOURCE_NAME, ev.getResourceContext().getIdentityApiAnnotation().name(), reportableAttributeSupport, securedAttributeSupport);
        addAttributeIfEnabled(NODE_NAME, ev.getNodeName(), reportableAttributeSupport, securedAttributeSupport);
        addAttributeIfEnabled(REMOTE_IP, ev.getRemoteIp(), reportableAttributeSupport, securedAttributeSupport);
        addAttributeIfEnabled(FORWARDED_IP, ev.getForwardedForIp(), reportableAttributeSupport, securedAttributeSupport);

        IdentityApiResourceRequest rc = ev.getResourceContext();

        if (reportableAttributeSupport.shouldReportAttribute(RESOURCE_PATH)) {
            String finalRequestPath = calculateAbsolutePathToReport(rc, securedAttributeSupport);
            addAttributeIfEnabled(RESOURCE_PATH, finalRequestPath, reportableAttributeSupport, securedAttributeSupport);
        }

        // Note - New Relic will truncate this to 255 characters as required
        if (reportableAttributeSupport.shouldReportAttribute(QUERY_PARAMS)) {
            String finalQueryString = getLoggableQueryString(securedAttributeSupport, rc);
            addAttributeIfEnabled(QUERY_PARAMS, finalQueryString, reportableAttributeSupport, securedAttributeSupport);
        }

        // Set keywords
        if (reportableAttributeSupport.shouldReportAttribute(KEYWORDS)) {
            Set<String> keywords = new HashSet<>();
            if (rc.getIdentityApiAnnotation() != null) {
                for (ApiKeyword apiKeyword : rc.getIdentityApiAnnotation().keywords()) {
                    keywords.add(apiKeyword.getReportValue());
                }
            }
            if (rc.isResourceDeprecated()) {
                keywords.add(ApiKeyword.DEPRECATED.getReportValue());
            }
            addAttributeIfEnabled(KEYWORDS, StringUtils.join(keywords, ","), reportableAttributeSupport, securedAttributeSupport);
        }

        if (reportableAttributeSupport.shouldReportAttribute(IDM_VERSION)) {
            String reportedVersion = DATA_UNAVAILABLE;
            if (idmVersion != null && idmVersion.getVersion() != null) {
                reportedVersion = idmVersion.getVersion().getValue();
            }
            addAttributeIfEnabled(IDM_VERSION, reportedVersion, reportableAttributeSupport, securedAttributeSupport);
        }
    }

    private String calculateAbsolutePathToReport(IdentityApiResourceRequest identityApiResourceRequest, SecuredAttributeSupport securedAttributeSupport) {
        String rawAbsolutePath = identityApiResourceRequest.getContainerRequest().getAbsolutePath().toString();

        // Scrub path if necessary
        String finalRequestPath;
        SecureResourcePath securePathParamAn = identityApiResourceRequest.getSecureResourcePathAnnotation();
        if (securePathParamAn != null && StringUtils.isNotBlank(securePathParamAn.regExPattern())) {
            String regex = securePathParamAn.regExPattern();
            Pattern compiledPattern = compiledPatterns.get(regex);
            if (compiledPattern == null) {
                compiledPattern = Pattern.compile(regex);
            }

            finalRequestPath = secureMatchedGroupValues(compiledPattern, rawAbsolutePath, securedAttributeSupport);

            if (rawAbsolutePath.equalsIgnoreCase(finalRequestPath)) {
                // There was a problem. Something should have been secured, but the raw string == secured string.
                finalRequestPath = "<PROTECTED>";
            }
        } else {
            finalRequestPath = rawAbsolutePath;
        }
        return finalRequestPath;
    }

    private String secureMatchedGroupValues(Pattern pattern, String original, SecuredAttributeSupport securedAttributeSupport) {
        String result = original;

        Matcher matcher = pattern.matcher(original);
        if (matcher.find()) {
            StringBuilder buf = new StringBuilder();

            int ptr = 0;
            int groupCount = matcher.groupCount();
            for (int i=1; i<=groupCount; i++) {
                String plainValue = matcher.group(i);
                String securedValue = securedAttributeSupport.secureAttributeValue(plainValue);

                // Add in every up to first match
                buf.append(original, ptr, matcher.start(i));
                buf.append(securedValue);
                ptr = matcher.end(i);
            }
            if (ptr < original.length()) {
                buf.append(original, ptr, original.length());
            }
            result = buf.toString();
        }
        return result;
    }

    /**
     * Only reports those query params the resource method specifies can be reported via the ReportableQueryParam
     * annotation
     *
     * @param securedAttributeSupport
     * @param resourceContext
     * @return
     */
    private String getLoggableQueryString(SecuredAttributeSupport securedAttributeSupport, IdentityApiResourceRequest resourceContext) {
        // Without request, we don't know the params or pretty much anything, so just short circuit as unavailable
        if (resourceContext == null || resourceContext.getContainerRequest() == null) {
            return "<NotAvailable>";
        }

        MultivaluedMap<String, String> queryParams = resourceContext.getContainerRequest().getQueryParameters();
        Set<String> loweredSecuredParams = resourceContext.getSecuredReportableParamNames();
        Set<String> loweredUnsecuredParams = resourceContext.getUnsecuredReportableParamNames();
        Set<String> loweredIncludedParams = resourceContext.getIncludedReportableParamNames();

        String finalQueryString = null;
        if (!MapUtils.isEmpty(queryParams) &&
                (CollectionUtils.isNotEmpty(loweredUnsecuredParams) || CollectionUtils.isNotEmpty(loweredSecuredParams)
                        || CollectionUtils.isNotEmpty(loweredIncludedParams))) {
            // At least one query param was supplied, and at least one query param is reported
            List<NameValuePair> finalParams = new ArrayList<>();
            for (Map.Entry<String, List<String>> providedParam : queryParams.entrySet()) {
                String providedParamName = providedParam.getKey();
                String searchableParamName = providedParamName.toLowerCase();

                if (loweredIncludedParams.contains(searchableParamName)) {
                    // Standardize on loggable param name format
                    finalParams.add(new BasicNameValuePair(searchableParamName, "<HIDDEN>"));
                } else if (loweredUnsecuredParams.contains(searchableParamName)) {
                    // Standardize on loggable param name format
                    finalParams.add(new BasicNameValuePair(searchableParamName, StringUtils.join(providedParam.getValue(), ",")));
                } else if (loweredSecuredParams.contains(searchableParamName)) {
                    // Standardize on loggable param name format
                    List<String> plainValues = providedParam.getValue();
                    List<String> securedValues = new ArrayList<>(plainValues.size());
                    for (String plainValue : plainValues) {
                        securedValues.add(securedAttributeSupport.secureAttributeValue(plainValue));
                    }
                    finalParams.add(new BasicNameValuePair(searchableParamName, StringUtils.join(securedValues, ",")));
                }
            }

            // Convert list back to string
            if (CollectionUtils.isNotEmpty(finalParams)) {
                try {
                    String encodedQueryStr = URLEncodedUtils.format(finalParams, StandardCharsets.UTF_8);
                    finalQueryString = URLDecoder.decode(encodedQueryStr, StandardCharsets.UTF_8.toString());
                } catch (UnsupportedEncodingException e) {
                    logger.info("Error decoding query str", e);
                    finalQueryString = "<E1>";
                }
            }
        }

        return finalQueryString;
    }

    private void addAttributeIfEnabled(NewRelicCustomAttributesEnum nrAttribute, String value, ReportableAttributeSupport reportableAttributeSupport, SecuredAttributeSupport securedAttributeSupport) {
        if (reportableAttributeSupport.shouldReportAttribute(nrAttribute)) {
            String finalValue = securedAttributeSupport.secureAttributeValueIfRequired(nrAttribute, value);
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
        boolean keyConfigured = StringUtils.isNotBlank(identityConfig.getReloadableConfig().getNewRelicSecuredApiResourceAttributesKey());
        if (keyConfigured) {
            logger.info("HMAC key is configured. Can dynamically secure attributes.");
        } else {
            logger.error("HMAC key was not configured. Secured attributes can not be sent");
        }
    }

    private class ReportableAttributeSupport {
        Set<String> includedAttributes;
        Set<String> excludedAttributes;

        public ReportableAttributeSupport(Set<String> includedAttributes, Set<String> excludedAttributes) {
            this.includedAttributes = includedAttributes;
            this.excludedAttributes = excludedAttributes;
        }

        public boolean shouldReportAttribute(NewRelicCustomAttributesEnum attribute) {
            return attribute.amIInListWithWildcardSupport(includedAttributes) && !attribute.amIInListWithWildcardSupport(excludedAttributes);
        }
    }
}
