package com.rackspace.idm.api.resource.cloud.v20.json

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.api.resource.cloud.v20.json.writers.JSONWriterForRaxAuthPhonePin
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import spock.lang.Specification

class JSONWriterForRaxAuthPhonePinTest extends Specification {

    JSONWriterForRaxAuthPhonePin writer = new JSONWriterForRaxAuthPhonePin()

    def "write phone pin to json"() {
        PhonePin phonePin = new PhonePin().with {
            it.pin = "2321"
            it
        }

        when:
        def out = new ByteArrayOutputStream()
        writer.writeTo(phonePin, null, null, null, null, null, out)

        then:
        def json = out.toString()
        JSONObject outer = (JSONObject) new JSONParser().parse(json)
        json != null
        JSONObject pp = outer.get(JSONConstants.RAX_AUTH_PHONE_PIN)

        pp.pin == phonePin.pin
    }

}
