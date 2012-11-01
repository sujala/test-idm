package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.impl.LdapRepository.LdapSearchBuilder
import com.rackspace.idm.domain.entity.Question
import com.rackspace.idm.domain.entity.Questions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/26/12
 * Time: 3:36 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapQuestionRepositoryTest extends Specification {

    @Autowired
    private LdapQuestionRepository ldapQuestionRepository;

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
        ldapQuestionRepository.addObject(createQuestion(questionId,"What is this?", null))
        Question question = ldapQuestionRepository.getObject(createSearchFilter(questionId).build())
        Questions questions = new Questions()
        questions.getQuestion().addAll(ldapQuestionRepository.getObjects(createSearchFilter().build()))
        question.question = "What is this now?";
        ldapQuestionRepository.updateObject(question)
        Question question2 = ldapQuestionRepository.getObject(createSearchFilter(questionId).build())
        ldapQuestionRepository.deleteObject(createSearchFilter(questionId).build())
        Question question3 = ldapQuestionRepository.getObject(createSearchFilter(questionId).build())

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
