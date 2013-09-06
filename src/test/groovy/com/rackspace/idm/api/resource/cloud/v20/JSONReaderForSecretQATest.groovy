package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForRaxKsQaSecretQA
import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 7/31/13
 * Time: 12:29 PM
 * To change this template use File | Settings | File Templates.
 */
class JSONReaderForSecretQATest extends RootServiceTest{
    @Shared
    JSONReaderForRaxKsQaSecretQA jsonReader

    def setupSpec() {
        jsonReader = new JSONReaderForRaxKsQaSecretQA()
    }

    def "Read from Json String" () {
        given:
        def question = "super awesome question thing!"
        def answer = "super awesome answer!"
        def jsonString =  '{ "RAX-KSQA:secretQA":{ "question": "' + question + '", "answer": "'+ answer + '" } }'
        InputStream is = new ByteArrayInputStream(jsonString.bytes)

        when:
        SecretQA secretQAObject = jsonReader.readFrom(null, null, null, null, null, is)

        then:
        secretQAObject.question == question
        secretQAObject.answer == answer
    }
}
