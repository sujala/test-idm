package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Question
import com.rackspace.idm.api.resource.cloud.JSONReaders.JSONReaderForRaxAuthQuestion
import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 7/31/13
 * Time: 11:15 AM
 * To change this template use File | Settings | File Templates.
 */
class JSONReaderForQuestionTest extends RootServiceTest{
    @Shared
    JSONReaderForRaxAuthQuestion jsonReader

    def setupSpec() {
        jsonReader = new JSONReaderForRaxAuthQuestion()
    }

    def "Read from Json String" () {
        given:
        def question = "super awesome question thing!"
        def jsonString = '{ "RAX-AUTH:question": { "question": "' + question + '" } }'
        InputStream is = new ByteArrayInputStream(jsonString.bytes)

        when:
        Question questionObject = jsonReader.readFrom(null, null, null, null, null, is)

        then:
        questionObject.question == question
    }
}
