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

    def "Set user, fedUser encryption salt and version populates both values"() {
        given:
        def user = entityFactory.createUser()
        def fedUser = entityFactory.createFederatedUser()

        when:
        encryptionService.setUserEncryptionSaltAndVersion(user);
        encryptionService.setUserEncryptionSaltAndVersion(fedUser);

        then:
        assert user.salt != null
        assert user.encryptionVersion != null

        assert fedUser.salt != null
        assert fedUser.encryptionVersion != null
    }

    def "Encrypt user, fedUser gets default salt from config if salt NOT set in user object"() {
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

        def fedUser = entityFactory.createFederatedUser()
        fedUser.phonePin = phonePin

        when:
        encryptionService.encryptUser(user);
        encryptionService.encryptUser(fedUser)
        then:
        assert user.salt == defaultSalt
        assert fedUser.salt == defaultSalt
    }

    def "Can encrypt and decrypt a user, fedUser"() {
        given:
        def password = "password"
        def secretQuestion = "secretQuestion"
        def secretAnwser = "secretAnwser"
        def secretQuestionId = "secretQuestionId"
        def displayName = "displayName"
        def apiKey = "apiKey"
        def phonePin = "1234"

        def user = entityFactory.createUser()
        user.password = password
        user.secretQuestion = secretQuestion
        user.secretAnswer = secretAnwser
        user.secretQuestionId = secretQuestionId
        user.displayName = displayName
        user.apiKey = apiKey
        user.phonePin = phonePin

        def fedUser = entityFactory.createFederatedUser()
        fedUser.phonePin = phonePin


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

        encryptionService.encryptUser(fedUser)
        fedUser.phonePin = ""

        encryptionService.decryptUser(fedUser)

        then:
        assert user.password == ""
        assert secretQuestion == user.secretQuestion
        assert secretAnwser == user.secretAnswer
        assert secretQuestionId == user.secretQuestionId
        assert displayName == user.displayName
        assert apiKey == user.apiKey
        assert phonePin == user.phonePin

        assert phonePin == fedUser.phonePin
    }


    def "Can read encryptionVersionId from properties service"() {
        when:
        def version = encryptionService.getEncryptionVersionId()

        then:
        assert version == defaultEncryptionVersionId
    }
}
