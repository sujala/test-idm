package com.rackspace.idm.domain.service.impl

import spock.lang.Shared
import spock.lang.Specification
import com.rackspace.idm.domain.entity.SecretQA
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.SecretQAs
import com.rackspace.idm.domain.entity.Question
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.exception.BadRequestException

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/13/12
 * Time: 2:51 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultSecretQAServiceTest extends Specification{
    @Shared DefaultSecretQAService secretQAService

    @Shared DefaultUserService defaultUserService
    @Shared DefaultQuestionService defaultQuestionService

    def setupSpec() {
        secretQAService = new DefaultSecretQAService();
    }

    def "Create/Update secretQA" (){
        given:
        setupMocks()
        User user = new User()
        user.secretQuestion = "my old question?"
        user.secretAnswer = "my old Answer"
        user.secretQuestionId = "2"
        Question question = new Question();
        question.setQuestion("My question?")
        defaultUserService.checkAndGetUserById(_) >> user
        defaultQuestionService.getQuestion(_) >> question

        when:
        secretQAService.addSecretQA("1",createSecretQA("1","My question?","My answer"))

        then:
        1 * defaultUserService.updateUser(_,_)

    }

    def "Get secreatQAs" (){
        given:
        setupMocks()
        User user = new User()
        user.secretQuestion = "my old question?"
        user.secretAnswer = "my old Answer"
        user.secretQuestionId = "2"
        defaultUserService.checkAndGetUserById(_) >> user

        when:
        SecretQAs secretQAs = secretQAService.getSecretQAs("1")

        then:
        secretQAs != null
        secretQAs.secretqa.size() == 1
    }

    def "Get secreatQAs null answer" (){
        given:
        setupMocks()
        User user = new User()
        user.secretQuestion = "my old question?"
        user.secretQuestionId = "2"
        defaultUserService.checkAndGetUserById(_) >> user

        when:
        secretQAService.getSecretQAs("1")

        then:
        thrown(NotFoundException)
    }

    def "Get secreatQAs null question" (){
        given:
        setupMocks()
        User user = new User()
        user.secretQuestion = "my old question?"
        user.secretQuestionId = "2"
        defaultUserService.checkAndGetUserById(_) >> user

        when:
        secretQAService.getSecretQAs("1")

        then:
        thrown(NotFoundException)
    }

    def "Get secreatQAs null questionId" (){
        given:
        setupMocks()
        User user = new User()
        user.secretQuestion = "my old question?"
        user.secretAnswer = "my old Answer"
        defaultUserService.checkAndGetUserById(_) >> user

        when:
        SecretQAs secretQAs = secretQAService.getSecretQAs("1")

        then:
        secretQAs != null
        secretQAs.secretqa.size() == 1
        secretQAs.secretqa.get(0).id == "0"
    }

    def "Create/Update bad question id" () {
        given:
        setupMocks()
        User user = new User()
        user.secretQuestion = "my old question?"
        user.secretAnswer = "my old Answer"
        user.secretQuestionId = "2"
        defaultUserService.checkAndGetUserById(_) >> user

        when:
        secretQAService.addSecretQA("1",createSecretQA("1","My question?","My answer"))

        then:
        thrown(NotFoundException)
    }

    def "Create/Update null question id" () {
        given:
        setupMocks()
        User user = new User()
        user.secretQuestion = "my old question?"
        user.secretAnswer = "my old Answer"
        user.secretQuestionId = "2"
        defaultUserService.checkAndGetUserById(_) >> user

        when:
        secretQAService.addSecretQA("1",createSecretQA(null,"My question?","My answer"))

        then:
        thrown(BadRequestException)
    }

    def "Create/Update null answer" () {
        given:
        setupMocks()
        User user = new User()
        user.secretQuestion = "my old question?"
        user.secretAnswer = "my old Answer"
        user.secretQuestionId = "2"
        defaultUserService.checkAndGetUserById(_) >> user

        when:
        secretQAService.addSecretQA("1",createSecretQA("1","My question?",null))

        then:
        thrown(BadRequestException)
    }

    def "Create/Update question mismatch" () {
        given:
        setupMocks()
        User user = new User()
        user.secretQuestion = "my old question?"
        user.secretAnswer = "my old Answer"
        user.secretQuestionId = "2"
        Question question = new Question();
        question.setQuestion("My other question?")
        defaultUserService.checkAndGetUserById(_) >> user
        defaultQuestionService.getQuestion(_) >> question

        when:
        secretQAService.addSecretQA("1",createSecretQA("1","My question?",null))

        then:
        thrown(BadRequestException)
    }

    def createSecretQA(String secretQuestionId, String question, String answer) {
        new SecretQA().with {
            it.id = secretQuestionId
            it.question = question
            it.answer = answer
            return it
        }
    }

    def setupMocks() {
        defaultUserService = Mock()
        defaultQuestionService = Mock()
        secretQAService.defaultUserService = defaultUserService
        secretQAService.defaultQuestionService = defaultQuestionService
    }
}
