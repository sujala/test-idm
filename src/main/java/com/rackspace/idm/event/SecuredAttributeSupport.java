package com.rackspace.idm.event;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import static com.rackspace.idm.event.ApiEventPostingAdvice.DATA_UNAVAILABLE;

public class SecuredAttributeSupport {
    private final Logger logger = LoggerFactory.getLogger(NewRelicApiEventListener.class);

    private HashAlgorithmEnum hashAlgorithmEnum;
    private String hashKey;
    private Set<String> securedAttributeList;

    private static HashAlgorithmEnum DEFAULT_HASH = HashAlgorithmEnum.SHA256;

    public SecuredAttributeSupport(String hashKey, Set<String> securedAttributeList) {
        this(null, hashKey, securedAttributeList);
    }

    public SecuredAttributeSupport(HashAlgorithmEnum hashAlgorithmEnum, String hashKey, Set<String> securedAttributeList) {
        if (hashAlgorithmEnum == null) {
            this.hashAlgorithmEnum = DEFAULT_HASH;
        } else {
            this.hashAlgorithmEnum = hashAlgorithmEnum;
        }
        this.hashKey = hashKey;
        this.securedAttributeList = securedAttributeList;
    }

    public String secureAttributeValueIfRequired(NewRelicCustomAttributesEnum nrAttribute, String value) {
        String finalValue;
        if (shouldAttributeValueBeSecured(nrAttribute, value) || ((nrAttribute == NewRelicCustomAttributesEnum.CALLER_TOKEN
                || nrAttribute == NewRelicCustomAttributesEnum.EFFECTIVE_CALLER_TOKEN)
                && !DATA_UNAVAILABLE.equalsIgnoreCase(value))) {
            finalValue = secureAttributeValue(value);
        } else {
            finalValue = value;
        }
        return finalValue;
    }

    private boolean shouldAttributeValueBeSecured(NewRelicCustomAttributesEnum nrAttribute, String value) {
        return nrAttribute.amIInListWithWildcardSupport(securedAttributeList)
                && (value == null || !value.equals(DATA_UNAVAILABLE));
    }

    public String secureAttributeValue(String value) {
        try {
            if (StringUtils.isBlank(hashKey)) {
                logger.debug("Error encountered securing attribute. Key is blank");
                return "<Protected-E1>";
            } else if (hashAlgorithmEnum == null) {
                // Should never happen since constructors prevent from ever being null, but be paranoid...
                logger.debug("Error encountered securing attribute.");
                return "<Protected-E3>";
            }
            return String.format("SV(%s)", Base64.encodeBase64URLSafeString(hashValue(value)));
        } catch (Exception e) {
            // Not sure how this would be triggered, but protect against it.
            logger.debug("Error encountered securing new relic attributes.", e);
            return "<Protected-E2>";
        }
    }

    private byte[] hashValue(String value) {
        byte[] hash = null;
        if (hashAlgorithmEnum == HashAlgorithmEnum.SHA256) {
            hash = HmacUtils.hmacSha256(hashKey, value);
        } else if (hashAlgorithmEnum == HashAlgorithmEnum.SHA1) {
            hash = HmacUtils.hmacSha1(hashKey, value);
        } else {
           throw new IllegalStateException("Unknown Hash Algorithm.");
        }

        return hash;
    }

    public enum HashAlgorithmEnum{
        SHA1, SHA256
    }
}
