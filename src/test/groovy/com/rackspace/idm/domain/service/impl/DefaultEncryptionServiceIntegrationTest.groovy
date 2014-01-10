package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.service.EncryptionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import testHelpers.RootServiceTest

@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultEncryptionServiceIntegrationTest extends RootServiceTest {

    @Autowired
    EncryptionService encryptionService

    def defaultSalt = "c7 73 21 8c 7e c8 ee 99"

    def setup() {
        mockConfiguration(encryptionService)
    }

    def "Set user encryption salt and version populates both values"() {
        given:
        def user = entityFactory.createUser()

        when:
        encryptionService.setUserEncryptionSaltAndVersion(user);

        then:
        user.salt != null
        user.encryptionVersion != null
    }

    def "Encrypt user gets default salt from config if salt NOT set in user object"() {
        given:
        def password = "password"
        def secretQuestion = "secretQuestion"
        def secretAnwser = "secretAnwser"
        def secretQuestionId = "secretQuestionId"
        def firstName = "firstName"
        def lastName = "lastName"
        def displayName = "displayName"
        def apiKey = "apiKey"

        def user = entityFactory.createUser()
        user.password = password
        user.secretQuestion = secretQuestion
        user.secretAnswer = secretAnwser
        user.secretQuestionId = secretQuestionId
        user.firstname = firstName
        user.lastname = lastName
        user.displayName = displayName
        user.apiKey = apiKey

        when:
        encryptionService.encryptUser(user);

        then:
        1 * config.getString("crypto.salt") >> defaultSalt
        user.salt == defaultSalt
    }

    def "can encrypt and decrypt a user"() {
        given:
        def password = "password"
        def secretQuestion = "secretQuestion"
        def secretAnwser = "secretAnwser"
        def secretQuestionId = "secretQuestionId"
        def firstName = "firstName"
        def lastName = "lastName"
        def displayName = "displayName"
        def apiKey = "apiKey"

        def user = entityFactory.createUser()
        user.password = password
        user.secretQuestion = secretQuestion
        user.secretAnswer = secretAnwser
        user.secretQuestionId = secretQuestionId
        user.firstname = firstName
        user.lastname = lastName
        user.displayName = displayName
        user.apiKey = apiKey

        when:
        encryptionService.encryptUser(user);
        user.password = ""
        user.secretQuestion = ""
        user.secretAnswer = ""
        user.secretQuestionId = ""
        user.firstname = ""
        user.lastname = ""
        user.displayName = ""
        user.apiKey = ""
        encryptionService.decryptUser(user);

        then:
        1 * config.getString("crypto.salt") >> defaultSalt
        user.password == ""
        secretQuestion == user.secretQuestion
        secretAnwser == user.secretAnswer
        secretQuestionId == user.secretQuestionId
        firstName == user.firstname
        lastName == user.lastname
        displayName == user.displayName
        apiKey == user.apiKey
    }
}
