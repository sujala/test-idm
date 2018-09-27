package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.QuestionDao
import com.rackspace.idm.domain.dao.impl.LdapRepository.LdapSearchBuilder
import com.rackspace.idm.domain.entity.Question
import com.rackspace.idm.domain.entity.Questions
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import spock.lang.Shared
import spock.lang.Specification

@WebAppConfiguration
@ContextConfiguration(locations = "classpath:app-config.xml")
class QuestionRepositoryIntegrationTest extends Specification {

    @Autowired
    private QuestionDao questionDao;

    @Autowired Configuration config

    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom

    def setupSpec(){
        sharedRandom = ("$sharedRandomness").replace('-',"")
    }

    def cleanSpec(){
    }

    def "question CRUD" () {
        given:
        def questionId = "question$sharedRandom"

        when:
        questionDao.addQuestion(createQuestion(questionId,"What is this?", null))
        Question question = questionDao.getQuestion(questionId)
        Questions questions = new Questions()
        questions.getQuestion().addAll(questionDao.getQuestions())
        question.question = "What is this now?";
        questionDao.updateQuestion(question)
        Question question2 = questionDao.getQuestion(questionId)
        questionDao.deleteQuestion(questionId)
        Question question3 = questionDao.getQuestion(questionId)

        then:
        question.question == "What is this now?"
        question.id == questionId
        questions.question.size() > 0
        question2.question == "What is this now?"
        question3 == null
    }

    def createQuestion(String id, String question, String uniqueId) {
        new Question().with {
            it.id = id
            it.question = question
            if(uniqueId != null){
                it.uniqueId = uniqueId
            }
            return it
        }
    }
}
