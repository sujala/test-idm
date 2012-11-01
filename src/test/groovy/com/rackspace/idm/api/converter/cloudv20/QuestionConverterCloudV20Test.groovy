package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.domain.entity.Question
import spock.lang.Shared
import spock.lang.Specification
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import org.dozer.DozerBeanMapper

class QuestionConverterCloudV20Test extends Specification {

    @Shared QuestionConverterCloudV20 converterCloudV20

    def setupSpec() {
        converterCloudV20 = new QuestionConverterCloudV20().with {
            it.objFactories = new JAXBObjectFactories()
            it.mapper = new DozerBeanMapper()
            return it
        }
    }

    def cleanupSpec() {
    }

    def "convert question from ldap to jersey object"() {
        given:
        Question question = question("id", "question")

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Question questionEntity = converterCloudV20.toQuestion(question).value

        then:
        question.question == questionEntity.question
        question.id == questionEntity.id
    }

    def "convert question from jersey object to ldap"() {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Question questionEntity = questionEntity("id", "question")

        when:
        Question question = converterCloudV20.fromQuestion(questionEntity)

        then:
        questionEntity.id == question.id
        questionEntity.question == question.question
    }

    def "convert questions from ldap to jersey object" () {
        given:
        Question question = question("id", "question")
        List<Question> questions = new ArrayList<Question>();
        questions.add(question)

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Questions questionsEntity = converterCloudV20.toQuestions(questions).value

        then:
        questions.size() == questionsEntity.question.size();
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Question questionEntity = questionsEntity.question.get(0)
        question.id == questionEntity.id
        question.question == questionEntity.question
    }

    def question(String id, String question) {
        new Question().with {
            it.id = id
            it.question = question
            return it
        }
    }

    def questionEntity(String id, String question) {
        new com.rackspace.docs.identity.api.ext.rax_auth.v1.Question().with {
            it.id = id
            it.question = question
            return it
        }
    }
}
