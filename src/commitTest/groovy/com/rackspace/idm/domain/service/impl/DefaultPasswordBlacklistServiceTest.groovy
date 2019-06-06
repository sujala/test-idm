package com.rackspace.idm.domain.service.impl

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.GetItemResult
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordCheckResultTypeEnum
import com.rackspace.idm.Constants
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

class DefaultPasswordBlacklistServiceTest extends RootServiceTest {

    @Shared
    DefaultPasswordBlacklistService service

    def setup() {
        service = new DefaultPasswordBlacklistService()
        mockIdentityConfig(service)
        mockDynamoDB(service)
    }

    def "Test when feature flag is turned off then result with DISABLE value is returned"() {
        given:
        identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled() >> false

        when:
        def status = service.checkPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)

        then:
        status == PasswordCheckResultTypeEnum.DISABLED

        when:
        def result = service.isPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)

        then:
        result == false
    }

    def "Verify blacklisted check returns PASS if item is not found in Dynamodb"() {
        given:
        identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled() >> true

        1 * dynamoDB.getItem(_) >> new GetItemResult().with {
            it.item = null
            it
        }

        when:
        def result = service.checkPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)

        then:
        result == PasswordCheckResultTypeEnum.PASSED
    }

    def "Test default behavior is to handle exceptions and return INDETERMINATE_ERROR instead of propagating exception"() {
        given:
        identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled() >> true

        when: "password blacklist service is called"
        def isPasswordInBlacklisted = service.checkPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)

        then: "by default it handles all exceptions and logs them"
        noExceptionThrown()

        and: "returns INDETERMINATE_ERROR status"
        isPasswordInBlacklisted == PasswordCheckResultTypeEnum.INDETERMINATE_ERROR
    }

    @Unroll
    def "Test blacklisted check returns other status based on threshold count:#countNumber, :#expectedStatus"() {
        given:
        identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled() >> true
        identityConfig.getReloadableConfig().getDynamoDBPasswordBlacklistCountMaxAllowed() >> 10

        Map<String, AttributeValue> returnedItem = new HashMap<String, AttributeValue>() {}
        returnedItem.put("pwd_hash", "F367EA9046AA8EBB1D8BEAF6DEF43C701B6381BE")
        returnedItem.put("count", new AttributeValue().with {
            it.n = countNumber
            it
        })

        2 * dynamoDB.getItem(_) >> new GetItemResult().with {
            it.item = returnedItem
            it
        }

        when: "checkPasswordInBlacklist is called"
        def blackListedPasswordIsBlocked = service.checkPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)

        then: "expected status is returned"
        blackListedPasswordIsBlocked == expectedStatus

        when: "isPasswordInBlacklist is called"
        def result = service.isPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)

        then: "expected boolean value is returned"
        result == expectedResult

        where:
        countNumber | expectedStatus                                  | expectedResult
        -7          | PasswordCheckResultTypeEnum.PASSED              | false
        0           | PasswordCheckResultTypeEnum.PASSED              | false
        7           | PasswordCheckResultTypeEnum.PASSED              | false
        1000        | PasswordCheckResultTypeEnum.FAILED              | true
        null        | PasswordCheckResultTypeEnum.INDETERMINATE_ERROR | false
    }


    @Unroll
    def "Test cases when dynamoDb throws different exceptions: #exception"() {
        given:
        identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled() >> true

        1 * dynamoDB.getItem(_) >> { throw inputException }

        when:
        def result = service.checkPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)

        then:
        result == expectedResult

        where:
        inputException                               | expectedResult
        new AmazonServiceException("test exception") | PasswordCheckResultTypeEnum.INDETERMINATE_ERROR
        new AmazonClientException("test exception")  | PasswordCheckResultTypeEnum.INDETERMINATE_RETRY
        new RuntimeException("test exception")       | PasswordCheckResultTypeEnum.INDETERMINATE_ERROR
    }
}
