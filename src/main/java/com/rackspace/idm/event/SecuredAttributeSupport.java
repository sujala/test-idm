package com.rackspace.idm.event;

import com.rackspace.idm.api.filter.ApiEventPostingFilter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import java.util.Set;

class SecuredAttributeSupport {
    private final Logger logger = LoggerFactory.getLogger(NewRelicApiEventListener.class);

    String hashKey;
    Set<String> securedAttributeList;

    public SecuredAttributeSupport(String hashKey, Set<String> securedAttributeList) {
        this.hashKey = hashKey;
        this.securedAttributeList = securedAttributeList;
    }

    public String secureAttributeValueIfRequired(NewRelicCustomAttributesEnum nrAttribute, String value) {
        String finalValue;
        if (shouldAttributeValueBeSecured(nrAttribute, value)) {
            finalValue = secureAttributeValue(value);
        } else {
            finalValue = value;
        }
        return finalValue;
    }

    private boolean shouldAttributeValueBeSecured(NewRelicCustomAttributesEnum nrAttribute, String value) {
        return nrAttribute.amIInListWithWildcardSupport(securedAttributeList)
                && (value == null || !value.equals(ApiEventPostingFilter.DATA_UNAVAILABLE));
    }

    private String secureAttributeValue(String value) {
        try {
            if (StringUtils.isBlank(hashKey)) {
                logger.warn("Error encountered securing attribute. Key is blank");
                return "<Protected-E1>";
            }
            return Base64.encodeBase64URLSafeString(HmacUtils.hmacSha1(hashKey, value));
        } catch (Exception e) {
            // Not sure how this would be triggered, but protect against it.
            logger.warn("Error encountered securing new relic attributes.", e);
            return "<Protected-E2>";
        }
    }
}
