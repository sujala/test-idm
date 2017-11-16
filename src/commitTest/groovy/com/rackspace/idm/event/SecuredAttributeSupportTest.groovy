package com.rackspace.idm.event

import com.rackspace.idm.api.filter.ApiEventPostingFilter
import org.apache.commons.codec.binary.Base64
import spock.lang.Specification
import spock.lang.Unroll

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException

class SecuredAttributeSupportTest extends Specification {
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1"

    def "Doesn't throw exception when used with null arguments"() {
        SecuredAttributeSupport sas = new SecuredAttributeSupport(null, null)

        when:
        sas.secureAttributeValueIfRequired(NewRelicCustomAttributesEnum.CALLER_TOKEN, "hi")

        then:
        notThrown(Exception)
    }

    @Unroll
    def "Does not secure case sensitive <NotAvailable> values for secured attributes: value: '#value'"() {
        SecuredAttributeSupport sas = new SecuredAttributeSupport("anykey", [NewRelicCustomAttributesEnum.CALLER_TOKEN.newRelicAttributeName] as Set)

        when:
        String finalValue = sas.secureAttributeValueIfRequired(NewRelicCustomAttributesEnum.CALLER_TOKEN, value)

        then:
        finalValue == ApiEventPostingFilter.DATA_UNAVAILABLE

        where:
        value << [ApiEventPostingFilter.DATA_UNAVAILABLE]
    }

    @Unroll
    def "Does not secure if attribute not in list of secured attributes"() {
        def unsecuredValue = "value"
        SecuredAttributeSupport sas = new SecuredAttributeSupport("anykey", [NewRelicCustomAttributesEnum.CALLER_TOKEN.newRelicAttributeName] as Set)

        when:
        String finalValue = sas.secureAttributeValueIfRequired(NewRelicCustomAttributesEnum.CALLER_USERNAME, unsecuredValue)

        then:
        finalValue == unsecuredValue
    }

    @Unroll
    def "Protects against missing keys by returning <Protected-E1>: key: '#value'"() {
        SecuredAttributeSupport sas = new SecuredAttributeSupport(value, [NewRelicCustomAttributesEnum.CALLER_TOKEN.newRelicAttributeName] as Set)

        when:
        String finalValue = sas.secureAttributeValueIfRequired(NewRelicCustomAttributesEnum.CALLER_TOKEN, "realValue")

        then:
        finalValue == "<Protected-E1>"

        where:
        value <<  [null, ""]
    }

    @Unroll
    def "Secures null, empty string, and all other values != <NotAvailable> (case sensitive) for protected attributes: value: '#value'"() {
        def key = "anykey"
        SecuredAttributeSupport sas = new SecuredAttributeSupport(key, [NewRelicCustomAttributesEnum.CALLER_TOKEN.newRelicAttributeName] as Set)

        when:
        String finalValue = sas.secureAttributeValueIfRequired(NewRelicCustomAttributesEnum.CALLER_TOKEN, value)

        then:
        Base64.decodeBase64(finalValue) == calculateSha1HMAC(value, key)

        where:
        value << [null, "", "abc", "NotAvailable", ApiEventPostingFilter.DATA_UNAVAILABLE.toLowerCase(), ApiEventPostingFilter.DATA_UNAVAILABLE.toUpperCase()]
    }

    @Unroll
    def "Secures attributes in secured set including wildcard: set: [#setVal]"() {
        def key = "anykey"
        def value = "aval"
        SecuredAttributeSupport sas = new SecuredAttributeSupport(key, ["*"] as Set)

        when:
        String finalValue = sas.secureAttributeValueIfRequired(NewRelicCustomAttributesEnum.CALLER_TOKEN, value)

        then:
        Base64.decodeBase64(finalValue) == calculateSha1HMAC(value, key)

        where:
        setVal << [["*"],[NewRelicCustomAttributesEnum.CALLER_TOKEN],[NewRelicCustomAttributesEnum.EVENT_ID, NewRelicCustomAttributesEnum.CALLER_TOKEN]]
    }

    /*
    Real code uses a library. Hand code here to validate the results via a different method
     */
    byte[] calculateSha1HMAC(String data, String key)
            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {

        byte[] dataBytes = data != null ? data.getBytes("UTF-8") : null
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM)
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM)
        mac.init(signingKey)
        return mac.doFinal(dataBytes)
    }
}