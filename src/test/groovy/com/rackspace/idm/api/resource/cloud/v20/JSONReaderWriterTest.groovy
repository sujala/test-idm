package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Question
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Region
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions
import org.apache.commons.io.IOUtils
import spock.lang.Shared
import spock.lang.Specification
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Questions

class JSONReaderWriterTest extends Specification {

    @Shared JSONWriter writer = new JSONWriter();
    @Shared JSONWriterForQuestion writerForQuestion = new JSONWriterForQuestion()
    @Shared JSONWriterForQuestions writerForQuestions = new JSONWriterForQuestions()
    @Shared JSONReaderForQuestion readerForQuestion = new JSONReaderForQuestion()

    def "can write/write region as json"() {
        given:
        def regionEntity = region("name", true, false)

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writer.writeTo(regionEntity, Region.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        def readRegion = JSONReaderForRegion.getRegionFromJSONString(json)

        then:
        regionEntity.name == readRegion.name
        regionEntity.enabled == readRegion.enabled
        regionEntity.isDefault == readRegion.isDefault
    }

    def "can write/write regions as json"() {
        given:
        def regionEntity = region("name", true, false)
        def regionsEntity = new Regions()
        regionsEntity.region.add(regionEntity)

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writer.writeTo(regionsEntity, Regions.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        def readRegions = JSONReaderForRegions.getRegionsFromJSONString(json)
        def readRegion = readRegions.region.get(0)

        then:
        regionsEntity.region.size() == readRegions.region.size()
        regionEntity.name == readRegion.name
        regionEntity.enabled == readRegion.enabled
        regionEntity.isDefault == readRegion.isDefault
    }

    def "can read/write question as json"() {
        given:
        def question = quesiton("id", "question")

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForQuestion.writeTo(question, Question, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()
        InputStream inputStream = IOUtils.toInputStream(json);

        Question readQuestion = readerForQuestion.readFrom(Question, null, null, null, null, inputStream);

        then:
        json != null
        question.id == readQuestion.id
        question.question == readQuestion.question
    }

    def "can write questions as json"() {
        given:
        def questions = new Questions()
        def question = quesiton("id", "question")
        questions.question.add(question)

        when:
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writerForQuestions.writeTo(questions, Questions, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()

        then:
        json != null
    }

    def quesiton(String id, String question) {
        new Question().with {
            it.id = id
            it.question = question
            return it
        }
    }

    def region(String name, Boolean isEnabled, Boolean isDefault) {
        new Region().with {
            it.name = name
            it.enabled = isEnabled
            it.isDefault = isDefault
            return it
        }
    }
}
