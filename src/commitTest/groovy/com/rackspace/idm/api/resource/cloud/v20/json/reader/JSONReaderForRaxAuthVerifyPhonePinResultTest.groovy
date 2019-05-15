package com.rackspace.idm.api.resource.cloud.v20.json.reader

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerifyPhonePinResult
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForRaxAuthPhonePin
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForRaxAuthVerifyPhonePinResult
import com.rackspace.idm.exception.BadRequestException
import spock.lang.Specification

class JSONReaderForRaxAuthVerifyPhonePinResultTest extends Specification {
    JSONReaderForRaxAuthVerifyPhonePinResult reader = new JSONReaderForRaxAuthVerifyPhonePinResult()

    def "unmarshall phone pin from json"() {

        def jsonPhonePinResult =
                '{' +
                '  "RAX-AUTH:verifyPhonePinResult": {\n' +
                '    "authenticated": false,\n' +
                '    "failureCode": "CODE",\n' +
                '    "failureMessage": "Message",\n' +
                '  }\n' +
                '}'

        when:
        VerifyPhonePinResult verifyPhonePinResult = reader.readFrom(PhonePin, null, null, null, null, new ByteArrayInputStream(jsonPhonePinResult.bytes))

        then:
        verifyPhonePinResult != null
        !verifyPhonePinResult.authenticated
        verifyPhonePinResult.failureCode == "CODE"
        verifyPhonePinResult.failureMessage == "Message"
    }

    def "fails without prefix json"() {
        def jsonPhonePinResult =
                '{' +
                        '  "verifyPhonePinResult": {\n' +
                        '    "authenticated": false,\n' +
                        '    "failureCode": "CODE",\n' +
                        '    "failureMessage": "Message",\n' +
                        '  }\n' +
                        '}'

        when:
        reader.readFrom(VerifyPhonePinResult, null, null, null, null, new ByteArrayInputStream(jsonPhonePinResult.bytes))

        then:
        BadRequestException ex = thrown()
        ex.message == "Invalid json request body"
    }

}


