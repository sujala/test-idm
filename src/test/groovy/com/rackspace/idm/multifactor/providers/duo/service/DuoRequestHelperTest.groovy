package com.rackspace.idm.multifactor.providers.duo.service

import com.google.common.collect.ImmutableMap
import com.rackspace.idm.multifactor.providers.duo.config.DuoSecurityConfig
import com.rackspace.idm.multifactor.providers.duo.config.SimpleDuoSecurityConfig
import com.sun.jersey.api.client.WebResource
import org.joda.time.DateTime
import org.springframework.web.bind.annotation.RequestMethod
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.ws.rs.core.MultivaluedMap

class DuoRequestHelperTest extends Specification {

    @Shared DuoRequestHelper duoRequestHelper;

    /**
     * A test config. Should not be real. This is set to the values specified in the duo documentation as an example to verify expectations.
     */
    private static final DuoSecurityConfig testConfig = new SimpleDuoSecurityConfig("DIWJ8X6AEYOR5OMC6TQ1", "Zh5eGmUq9zpfQnyUIu5OL9iWoMMv5ZNmk3zLJ4Ep", "api-XXXXXXXX.duosecurity.com", 30000);

    def setupSpec() {
        duoRequestHelper = new DuoRequestHelper(testConfig);
    }

    def "Can get base web resource with empty path and host passed in config"() {
        when:
        WebResource newResource = duoRequestHelper.createWebResource()

        then:
        newResource.getURI().getPath().equals("")
        newResource.getURI().getHost().equals(testConfig.getApiHostName())
    }

    /**
     * Tests creating new web resources based on configured host
     *
     * @return
     */
    @Unroll("Can create web resource with specified path and configured host (additionalPath='#additionalPath', expectedPath='#expectedPath')")
    def "Can create web resource with specified path and configured host"() {
        when:
        WebResource newResource = duoRequestHelper.createWebResource(additionalPath)

        then:
        newResource.getURI().getPath().equals(expectedPath)
        newResource.getURI().getHost().equals(testConfig.getApiHostName())

        where:
        additionalPath        | expectedPath
        "thePath"             | "/thePath"
        "thePath/"            | "/thePath/"
        "/thePath"            | "/thePath"
        ""                    | ""
        "/thePath/"           | "/thePath/"
        "/thePath/theSecond/" | "/thePath/theSecond/"
    }

    def "can create a web resource with a simple one item path and host passed in config. Path always prefixed with '/' when path is non-empty"() {
        String additionalPath = "thePath"

        when:
        WebResource newResource = duoRequestHelper.createWebResource(additionalPath)

        then:
        newResource.getURI().getPath().equals("/" + additionalPath)
        newResource.getURI().getHost().equals(testConfig.getApiHostName())
    }

    /**
     * Jersey client expects a multivalued map to be provided to add query params to a GET request. Duo Security expects same
     * formatted queryparams within a POST request's content. This test verifies the creation of the multivaluedmap from another map.
     * The value in a multivalued map are lists of the values
     */
    def "can create a multivalued map"() {
        String key1 = "key1"
        String value1 = "value1"

        Map<String, String> map = ImmutableMap.<String, String> builder()
                .put(key1, value1)
                .build();

        when:
        MultivaluedMap mvm = duoRequestHelper.createMultiValuedMap(map)

        then:
        mvm.size() == 1
        mvm.get(key1) == [value1]
    }

    /**
     * Many URL encoders will convert spaces to '+' instead
     * of '%20'. Both are valid. However, encoding as '%20' is required by Duo Security as part of the signature canonicalization.
     * This test verifies that the code will maintain
     * compatibility with the Duo requirement by converting '+' to '%20'.
     */
    def "urlEncodeParameters uses '%20' for spaces rather than '+'"() {
        Map<String, String> map = ImmutableMap.<String, String> builder()
                .put("realname", "First Last")
                .build();

        TreeMap<String, String> sortedMap = new TreeMap(map);

        expect:
        duoRequestHelper.urlEncodeParameters(sortedMap) == "realname=First%20Last"
    }

    /**
     * It was verified through calls that Duo Security requires unicode characters to be URL encoded in the signature. Verify it is encoded correctly before signed.
     */
    def "encode unicode characters"() {
        String encodingRulesTest = "α"
        expect:
        duoRequestHelper.encodeForDuoSecuritySignature(encodingRulesTest) == "%CE%B1"
    }

    /**
     * Verify appropriate characters are encoded as expected by DUO
     *
     * Per due @see <a href="https://www.duosecurity.com/docs/duoverify#request_format">Duo Documentation</a>
     * When URL-encoding, all bytes except ASCII letters, digits, underscore ("_"), period ("."), and hyphen ("-") are replaced by a percent sign ("%")
     * followed by two hexadecimal digits containing the value of the byte. For example, a space is replaced with "%20" and a tilde ("~") becomes "%7E".
     * Use only upper-case A through F for hexadecimal digits.
     *
     * NOTE - DUO Security does NOT work if a tilde ('~') is encoded as they explicitly specify. Must NOT encode it in string that is signed or it will fail.
     */
    def "urlEncodeParameters encodes special characters per Duo Security spec"() {
        String encodingRulesTest = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~`!@#\$%^&*()=+|[]{}';:<>?,\\ "

        expect:
        duoRequestHelper.encodeForDuoSecuritySignature(encodingRulesTest) == "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~%60%21%40%23%24%25%5E%26%2A%28%29%3D%2B%7C%5B%5D%7B%7D%27%3B%3A%3C%3E%3F%2C%5C%20"
    }

    /**
     * This is based on the example provided at <a href="http://www.duosecurity.com/docs/duoverify#authentication">Duo Security documentation</a> to confirm
     * the signature is created appropriately.
     *
     * There is one small discrepancy from the example on Duo's website in that the example uses a timezone of '-0000' to represent UTC. JodaTime uses
     * '+0000'. This is not settable within JodaTime and results in the HMACs not lining up. This does not affect new requests, but since the HMAC listed on
     * Duo's site signed '-0000' instead of '+0000' our code will not generate the same HMAC.
     *
     * I hacked the code to inject -0000 in order to confirm the code would generate the expected value if '-0000' was used instead. I then generated
     * the HMAC that results from using +0000 and added that to this test.
     *
     * @return
     */
    def "generate duo documentation example hmac"() {
        Map<String, String> map = ImmutableMap.<String, String> builder()
                .put("username", "root")
                .put("realname", "First Last")
                .build()

        TreeMap<String, String> sortedMap = new TreeMap<String, String>(map)
        DateTime testedDateTime = DuoRequestHelper.RFC822DATEFORMAT.parseDateTime("Tue, 21 Aug 2012 17:29:18 -0000")

        WebResource baseResource = duoRequestHelper.createWebResource("/verify/v1/status")
        String formattedParameters = duoRequestHelper.urlEncodeParameters(sortedMap)
        String signature = duoRequestHelper.sign(testedDateTime, RequestMethod.POST, baseResource, formattedParameters)

        expect:
        duoRequestHelper.createBasicAuthenticationHeader(signature) == "Basic RElXSjhYNkFFWU9SNU9NQzZUUTE6ZDUxNzZmOTUyMjEyYmEzZGNiYzE0NjZmZTllNTE3M2MyNWFjZGZmMA==";
    }

}
