package com.rackspace.idm.api.resource.cloud.v20.json.writer


import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerifyPhonePinResult
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.api.resource.cloud.v20.json.writers.JSONWriterForRaxAuthVerifyPhonePinResult
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import spock.lang.Specification

class JSONWriterForRaxAuthVerifyPhonePinResultTest extends Specification {

    JSONWriterForRaxAuthVerifyPhonePinResult writer = new JSONWriterForRaxAuthVerifyPhonePinResult()

    def "writeTo: Failure written correctly"() {
        VerifyPhonePinResult verifyPhonePinResult = new VerifyPhonePinResult().with {
            it.authenticated = false
            it.failureCode = "CODE"
            it.failureMessage = "Message"
            it
        }

        when:
        def out = new ByteArrayOutputStream()
        writer.writeTo(verifyPhonePinResult, null, null, null, null, null, out)

        then:
        def json = out.toString()
        JSONObject outer = (JSONObject) new JSONParser().parse(json)
        json != null
        JSONObject pp = outer.get(JSONConstants.RAX_AUTH_VERIFY_PHONE_PIN_RESULT)

        pp.authenticated == verifyPhonePinResult.authenticated
        pp.failureCode == verifyPhonePinResult.failureCode
        pp.failureMessage == verifyPhonePinResult.failureMessage
    }

    def "writeTo: Success written correctly"() {
        VerifyPhonePinResult verifyPhonePinResult = new VerifyPhonePinResult().with {
            it.authenticated = true
            it
        }

        when:
        def out = new ByteArrayOutputStream()
        writer.writeTo(verifyPhonePinResult, null, null, null, null, null, out)

        then:
        def json = out.toString()
        JSONObject outer = (JSONObject) new JSONParser().parse(json)
        json != null
        JSONObject pp = outer.get(JSONConstants.RAX_AUTH_VERIFY_PHONE_PIN_RESULT)

        pp.authenticated == verifyPhonePinResult.authenticated
        pp.failureCode == null
        pp.failureMessage == null
    }
}
