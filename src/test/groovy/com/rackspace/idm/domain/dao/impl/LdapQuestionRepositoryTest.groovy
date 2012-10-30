package com.rackspace.idm.domain.dao.impl

import spock.lang.Specification
import org.springframework.test.context.ContextConfiguration
import org.springframework.beans.factory.annotation.Autowired

import com.rackspace.idm.domain.entity.Question
import com.rackspace.idm.domain.entity.Questions

import com.rackspace.idm.domain.dao.impl.LdapRepository.LdapSearchBuilder
import com.unboundid.ldap.sdk.ReadOnlyEntry;


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

    def setupSpec(){
    }

    def cleanSpec(){
    }

    def "question CRUD" () {
        given:
        def questionId = "100123321"

        when:
        ldapQuestionRepository.addObject(createQuestion(questionId,"What is this?", null))
        Question question = ldapQuestionRepository.getObject(questionId,createSearchFilter(questionId).build())
        Questions questions = new Questions()
        questions.getQuestion().addAll(ldapQuestionRepository.getObjects())
        question.question = "What is this now?";
        ldapQuestionRepository.updateObject(question)
        Question question2 = ldapQuestionRepository.getObject(questionId, createSearchFilter(questionId).build())
        ldapQuestionRepository.deleteObject(questionId, createSearchFilter(questionId).build())
        Question question3 = ldapQuestionRepository.getObject(questionId, createSearchFilter(questionId).build())

        then:
        question.question == "What is this now?"
        question.id == "100123321"
        questions.question.size() > 0
        question2.question == "What is this now?"
        question3 == null
    }

    def "null value operations on getQuestion" () {
        given: def questionId = "100123321"
        when: ldapQuestionRepository.getObject(null, createSearchFilter(questionId).build())
        then: thrown(IllegalArgumentException)
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

}
