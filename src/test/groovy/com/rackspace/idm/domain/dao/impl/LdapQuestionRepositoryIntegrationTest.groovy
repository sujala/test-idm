package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.impl.LdapRepository.LdapSearchBuilder
import com.rackspace.idm.domain.entity.Question
import com.rackspace.idm.domain.entity.Questions
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapQuestionRepositoryIntegrationTest extends Specification {

    @Autowired
    private LdapQuestionRepository ldapQuestionRepository;

    @Autowired Configuration config

    @Shared def sharedRandomness = UUID.randomUUID()
    @Shared def sharedRandom

    def setupSpec(){
        sharedRandom = ("$sharedRandomness").replace('-',"")
    }

    def cleanSpec(){
    }

    def "getNextId returns UUID"() {
        given:
        def success = false
        ldapQuestionRepository.config = config
        def originalVal = config.getBoolean("rsid.uuid.enabled", false)
        config.setProperty("rsid.uuid.enabled",true)

        when:
        def id = ldapQuestionRepository.getNextId(LdapRepository.NEXT_QUESTION_ID)
        try {
            Long.parseLong(id)
        } catch (Exception) {
            success = true
        } finally {
            config.setProperty("rsid.uuid.enabled",false)
        }

        then:
        success == true

        cleanup:
        config.setProperty("rsid.uuid.enabled",originalVal)
    }

    def "getNextId returns Long"() {
        given:
        def success = false
        ldapQuestionRepository.config = config
        def originalVal = config.getBoolean("rsid.uuid.enabled", false)
        config.setProperty("rsid.uuid.enabled",false)

        when:
        def id = ldapQuestionRepository.getNextId(LdapRepository.NEXT_QUESTION_ID)
        try {
            Long.parseLong(id)
            success = true
        } catch (Exception) {
            //no-op
        }

        then:
        success == true

        cleanup:
        config.setProperty("rsid.uuid.enabled",originalVal)
    }

    def "question CRUD" () {
        given:
        def questionId = "question$sharedRandom"

        when:
        ldapQuestionRepository.addQuestion(createQuestion(questionId,"What is this?", null))
        Question question = ldapQuestionRepository.getQuestion(questionId)
        Questions questions = new Questions()
        questions.getQuestion().addAll(ldapQuestionRepository.getQuestions())
        question.question = "What is this now?";
        ldapQuestionRepository.updateQuestion(question)
        Question question2 = ldapQuestionRepository.getQuestion(questionId)
        ldapQuestionRepository.deleteQuestion(questionId)
        Question question3 = ldapQuestionRepository.getQuestion(questionId)

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

    def createSearchFilter(String id){
         new LdapSearchBuilder().with {
             it.addEqualAttribute("rsId",id)
             it.addEqualAttribute("objectClass","rsQuestion")
             return it
         }
    }

    def createSearchFilter(){
         new LdapSearchBuilder().with {
             it.addEqualAttribute("objectClass","rsQuestion")
             return it
         }
    }
}
