package com.rackspace.idm.domain.service.impl

import spock.lang.Specification
import spock.lang.Shared
import com.rackspace.idm.domain.dao.impl.LdapQuestionRepository
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.domain.entity.Question
import com.rackspace.idm.exception.DuplicateException
import com.rackspace.idm.exception.NotFoundException

class DefaultQuestionServiceTest extends Specification {

    @Shared def randomness = UUID.randomUUID()
    @Shared def random

    @Shared DefaultQuestionService questionService
    @Shared LdapQuestionRepository questionDao

    def setupSpec() {
        random = ("$randomness").replace('-',"")
        questionService = new DefaultQuestionService()
    }

    def cleanupSpec() {
    }

    def "create question with null question throws bad request "() {
        when:
        questionService.addQuestion(null)

        then:
        thrown(BadRequestException)
    }

    def "create question id is required"() {
        when:
        questionService.addQuestion(question(null, "question"))

        then:
        thrown(BadRequestException)
    }

    def "create question question is required"(){
        when:
        questionService.addQuestion(question("id", null))

        then:
        thrown(BadRequestException)
    }

    def "update question with null questionId throws bad request"() {
        when:
        questionService.updateQuestion(null, question("id", "question"))

        then:
        thrown(BadRequestException)
    }

    def "update question with null question throws bad request"() {
        when:
        questionService.updateQuestion("id", null)

        then:
        thrown(BadRequestException)
    }

    def "update question id is required"() {
        when:
        questionService.updateQuestion("id", question(null, "question"))

        then:
        thrown(BadRequestException)
    }

    def "update question question is required"(){
        when:
        questionService.updateQuestion("id", question("id", null))

        then:
        thrown(BadRequestException)
    }

    def "delete question with null questionId throws bad request"() {
        when:
        questionService.deleteQuestion(null)

        then:
        thrown(BadRequestException)
    }

    def "get question with null questionId throws bad request"() {
        when:
        questionService.getQuestion(null)

        then:
        thrown(BadRequestException)
    }

    def "add question with already exisiting questionId throws duplicate"() {
        given:
        setupMocks()
        questionDao.getObject(_) >> question("id", "some question")

        when:
        questionService.addQuestion(question("id", "question"))

        then:
        thrown(DuplicateException)
    }

    def "update question with wrong questionId in entity throws bad request"() {
        when:
        questionService.updateQuestion("id", question("id2", "question"))

        then:
        thrown(BadRequestException)
    }

    def "update question when question does not exist throws not found"() {
        given:
        setupMocks()

        when:
        questionService.updateQuestion("id", question("id", "question"))

        then:
        thrown(NotFoundException)
    }

    def "delete question when question does not exist throws not found"() {
        given:
        setupMocks()

        when:
        questionService.deleteQuestion("id")

        then:
        thrown(NotFoundException)
    }

    def "create question uses calls ldap repository"() {
        given:
        setupMocks()
        def question = question("id", "question")

        when:
        questionService.addQuestion(question)

        then:
        1 * questionDao.addObject(question)
    }

    def "update question calls ldap repository"() {
        given:
        setupMocks()
        def question = question("id", "question")
        questionDao.getObject(_) >> question

        when:
        questionService.updateQuestion("id", question)

        then:
        1 * questionDao.updateObject(_)
    }

    def "delete question calls ldap repository"(){
        given:
        setupMocks()
        def question = question("id", "question")
        questionDao.getObject(_) >> question

        when:
        questionService.deleteQuestion("id")

        then:
        1 * questionDao.deleteObject(_)

    }

    def "get question calls ldap repository"() {
        given:
        def getObjectCalled = false
        setupMocks()
        def question = question("id", "question")
        questionDao.getObject(_) >> { getObjectCalled = true; question }

        when:
        questionService.getQuestion("id")

        then:
        getObjectCalled == true
    }

    def "get questions calls ldap repository"() {
        given:
        setupMocks()

        when:
        questionService.getQuestions()

        then:
        1 * questionDao.getObjects(_)
    }

    def setupMocks() {
        questionDao = Mock()
        questionService.questionDao = questionDao
    }

    def question(String id, String question) {
        new Question().with {
            it.id = id
            it.question = question
            return it
        }
    }
}
