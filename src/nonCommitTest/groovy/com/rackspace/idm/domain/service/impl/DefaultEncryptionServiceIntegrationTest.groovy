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
    def defaultEncryptionVersionId = "0"

    def "Set user encryption salt and version populates both values"() {
        given:
        def user = entityFactory.createUser()

        when:
        encryptionService.setUserEncryptionSaltAndVersion(user);

        then:
        assert user.salt != null
        assert user.encryptionVersion != null
    }

    def "Set fedUser encryption salt and version populates both values"() {
        given:
        def fedUser = entityFactory.createFederatedUser()

        when:
        encryptionService.setUserEncryptionSaltAndVersion(fedUser);

        then:
        assert fedUser.salt != null
        assert fedUser.encryptionVersion != null
    }


    def "Encrypt user gets default salt from config if salt NOT set in user object"() {
        given:
        def password = "password"
        def secretQuestion = "secretQuestion"
        def secretAnwser = "secretAnwser"
        def secretQuestionId = "secretQuestionId"
        def displayName = "displayName"
        def apiKey = "apiKey"
        def phonePin = "2321"

        def user = entityFactory.createUser()
        user.password = password
        user.secretQuestion = secretQuestion
        user.secretAnswer = secretAnwser
        user.secretQuestionId = secretQuestionId
        user.displayName = displayName
        user.apiKey = apiKey
        user.phonePin = phonePin

        when:
        encryptionService.encryptUser(user);

        then:
        assert user.salt == defaultSalt
    }

    def "Encrypt fedUser gets default salt from config if salt NOT set in user object"() {
        given:
        def phonePin = "2321"
        def fedUser = entityFactory.createFederatedUser()
        fedUser.phonePin = phonePin

        when:
        encryptionService.encryptUser(fedUser)
        then:
        assert fedUser.salt == defaultSalt
    }

    def "Can encrypt and decrypt a user"() {
        given:
        def password = "password"
        def secretQuestion = "secretQuestion"
        def secretAnswer = "secretAnswer"
        def secretQuestionId = "secretQuestionId"
        def displayName = "displayName"
        def apiKey = "apiKey"
        def phonePin = "1234"

        def user = entityFactory.createUser()
        user.password = password
        user.secretQuestion = secretQuestion
        user.secretAnswer = secretAnswer
        user.secretQuestionId = secretQuestionId
        user.displayName = displayName
        user.apiKey = apiKey
        user.phonePin = phonePin

        when:
        encryptionService.encryptUser(user);
        user.password = ""
        user.secretQuestion = ""
        user.secretAnswer = ""
        user.secretQuestionId = ""
        user.displayName = ""
        user.apiKey = ""
        user.phonePin = ""

        encryptionService.decryptUser(user);


        then:
        assert user.password == ""
        assert secretQuestion == user.secretQuestion
        assert secretAnswer == user.secretAnswer
        assert secretQuestionId == user.secretQuestionId
        assert displayName == user.displayName
        assert apiKey == user.apiKey
        assert phonePin == user.phonePin
    }


    def "Can encrypt and decrypt a fedUser"() {
        given:
        def phonePin = "1234"
        def fedUser = entityFactory.createFederatedUser()
        fedUser.phonePin = phonePin


        when:
        encryptionService.encryptUser(fedUser)
        fedUser.phonePin = ""

        encryptionService.decryptUser(fedUser)

        then:
        assert phonePin == fedUser.phonePin
    }


    def "Can read encryptionVersionId from properties service"() {
        when:
        def version = encryptionService.getEncryptionVersionId()

        then:
        assert version == defaultEncryptionVersionId
    }
}
