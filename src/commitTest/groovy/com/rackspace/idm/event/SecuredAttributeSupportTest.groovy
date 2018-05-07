package com.rackspace.idm.event

import com.rackspace.idm.event.SecuredAttributeSupport.HashAlgorithmEnum
import org.apache.commons.codec.binary.Base64
import spock.lang.Specification
import spock.lang.Unroll

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.SignatureException

import static com.rackspace.idm.event.ApiEventPostingAdvice.DATA_UNAVAILABLE

class SecuredAttributeSupportTest extends Specification {
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1"
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256"

    def "Doesn't throw exception when used with null arguments"() {
        SecuredAttributeSupport sas = new SecuredAttributeSupport(null, null)

        when:
        sas.secureAttributeValueIfRequired(NewRelicCustomAttributesEnum.CALLER_TOKEN, "hi")

        then:
        notThrown(Exception)
    }

    @Unroll
    def "Does not secure case sensitive <NotAvailable> values for secured attributes: value: '#value'"() {
        SecuredAttributeSupport sas = new SecuredAttributeSupport("anykey", [NewRelicCustomAttributesEnum.CALLER_USERNAME.newRelicAttributeName] as Set)

        when:
        String finalValue = sas.secureAttributeValueIfRequired(NewRelicCustomAttributesEnum.CALLER_USERNAME, value)

        then:
        finalValue == DATA_UNAVAILABLE

        where:
        value << [DATA_UNAVAILABLE]
    }

    @Unroll
    def "Does not secure if attribute not in list of secured attributes"() {
        def unsecuredValue = "value"
        SecuredAttributeSupport sas = new SecuredAttributeSupport("anykey", [NewRelicCustomAttributesEnum.EFFECTIVE_CALLER_USERNAME.newRelicAttributeName] as Set)

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
        assertSecuredValue(finalValue, value, HashAlgorithmEnum.SHA1, key)

        where:
        value << [null, "", "abc", "NotAvailable", DATA_UNAVAILABLE.toLowerCase(), DATA_UNAVAILABLE.toUpperCase()]
    }

    @Unroll
    def "Secures attributes in secured set including wildcard: set: [#setVal]"() {
        def key = "anykey"
        def value = "aval"
        SecuredAttributeSupport sas = new SecuredAttributeSupport(key, ["*"] as Set)

        when:
        String finalValue = sas.secureAttributeValueIfRequired(NewRelicCustomAttributesEnum.CALLER_TOKEN, value)

        then:
        assertSecuredValue(finalValue, value, HashAlgorithmEnum.SHA1, key)

        where:
        setVal << [["*"],[NewRelicCustomAttributesEnum.CALLER_TOKEN],[NewRelicCustomAttributesEnum.REQUEST_ID, NewRelicCustomAttributesEnum.CALLER_TOKEN]]
    }

    def "Hashes with SHA1 when specified"() {
        def value = "asecurevalue"
        def key = "anykey"
        SecuredAttributeSupport sas = new SecuredAttributeSupport(SecuredAttributeSupport.HashAlgorithmEnum.SHA1, key, [NewRelicCustomAttributesEnum.CALLER_TOKEN.newRelicAttributeName] as Set)

        when:
        String finalValue =sas.secureAttributeValueIfRequired(NewRelicCustomAttributesEnum.CALLER_TOKEN, value)

        then:
        assertSecuredValue(finalValue, value, HashAlgorithmEnum.SHA1, key)
   }

    void assertSecuredValue(String actualValue, String plaintext, HashAlgorithmEnum hashAlgorithmEnum, String hashKey) {
        byte[] hash = null;
        if (hashAlgorithmEnum == HashAlgorithmEnum.SHA256) {
            hash = new String(calculateSha256HMAC(plaintext, hashKey),  )
        } else if (hashAlgorithmEnum == HashAlgorithmEnum.SHA1) {
            hash = calculateSha1HMAC(plaintext, hashKey)
        }
        String encoded = Base64.encodeBase64(hash)

        actualValue == String.format("SV(%s)", encoded)
    }

    @Unroll
    def "Hashes with SHA256 when specified as '#hashAlg'"() {
        def value = "asecurevalue"
        def key = "anykey"
        SecuredAttributeSupport sas = new SecuredAttributeSupport(hashAlg, key, [NewRelicCustomAttributesEnum.CALLER_TOKEN.newRelicAttributeName] as Set)

        when:
        String finalValue = sas.secureAttributeValueIfRequired(NewRelicCustomAttributesEnum.CALLER_TOKEN, value)

        then:
        assertSecuredValue(finalValue, value, HashAlgorithmEnum.SHA256, key)

        where:
        hashAlg << [null, SecuredAttributeSupport.HashAlgorithmEnum.SHA256]
    }


    def calculateHashForSASAndValue(SecuredAttributeSupport sas, String value) {
        if (sas.hashAlgorithmEnum == SecuredAttributeSupport.HashAlgorithmEnum.SHA256) {
            return calculateSha256HMAC(value, sas.hashKey)
        } else if (sas.hashAlgorithmEnum == SecuredAttributeSupport.HashAlgorithmEnum.SHA1) {
            return calculateSha1HMAC(value, sas.hashKey)
        }
        throw new RuntimeException("Invalid algorithm")
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

    /*
    Real code uses a library. Hand code here to validate the results via a different method
     */
    byte[] calculateSha256HMAC(String data, String key)
            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {

        byte[] dataBytes = data != null ? data.getBytes("UTF-8") : null
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA256_ALGORITHM)
        Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM)
        mac.init(signingKey)
        return mac.doFinal(dataBytes)
    }
}