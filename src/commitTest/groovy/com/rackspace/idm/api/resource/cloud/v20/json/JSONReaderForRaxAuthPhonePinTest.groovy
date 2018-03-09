package com.rackspace.idm.api.resource.cloud.v20.json

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForRaxAuthPhonePin
import com.rackspace.idm.exception.BadRequestException
import spock.lang.Specification

class JSONReaderForRaxAuthPhonePinTest extends Specification {
    JSONReaderForRaxAuthPhonePin reader = new JSONReaderForRaxAuthPhonePin()

    def "unmarshall phone pin from json"() {

        def jsonPhonePin =
                '{' +
                '  "RAX-AUTH:phonePin": {\n' +
                '    "pin": "3242",\n' +
                '  }\n' +
                '}'

        when:
        PhonePin phonePin = reader.readFrom(PhonePin, null, null, null, null, new ByteArrayInputStream(jsonPhonePin.bytes))

        then:
        phonePin != null
        phonePin.pin == "3242"
    }

    def "fails without prefix json"() {
        def jsonPhonePin =
                '{' +
                '  "phonePin": {\n' +
                '    "pin": "3242",\n' +
                '  }\n' +
                '}'

        when:
        reader.readFrom(PhonePin, null, null, null, null, new ByteArrayInputStream(jsonPhonePin.bytes))

        then:
        BadRequestException ex = thrown()
        ex.message == "Invalid json request body"
    }

}


