package com.rackspace.idm.domain.service.impl

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.GetItemResult
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordCheckResultTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.entity.ValidatePasswordResult
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

    def "Test default behavior is to handle exceptions and return INDETERMINATE_ERROR instead of propagating exception"() {
        given:
        identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled() >> true

        when: "password blacklist service is called"
        def isPasswordInBlacklisted = service.checkPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)

        then: "by default it handles all exceptions and logs them"
        noExceptionThrown()

        and: "returns that password is not black listed"
        isPasswordInBlacklisted == PasswordCheckResultTypeEnum.INDETERMINATE_ERROR
    }

    @Unroll
    def "Test scenarios where in a blacklisted password has different publicly compromised count #countNumber"() {
        given:
        identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled() >> true

        Map<String, AttributeValue> returnedItem = new HashMap<String, AttributeValue>() {}
        returnedItem.put("pwd_hash", "F367EA9046AA8EBB1D8BEAF6DEF43C701B6381BE")
        returnedItem.put("count", new AttributeValue().with {
            it.n = countNumber
            it
        })

        1 * dynamoDB.getItem(_) >> new GetItemResult().with {
            it.item = returnedItem
            it
        }

        when: "Black listed password with compromised count more then threshold limit is passed"
        def blackListedPasswordIsBlocked = service.checkPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)

        then: "password is considered black listed"
        blackListedPasswordIsBlocked == expectation

        1 * identityConfig.getReloadableConfig().getDynamoDBPasswordBlacklistCountMaxAllowed() >> 10

        where:
        countNumber | expectation
        -2000       | PasswordCheckResultTypeEnum.PASSED
        -2          | PasswordCheckResultTypeEnum.PASSED
        0           | PasswordCheckResultTypeEnum.PASSED
        1           | PasswordCheckResultTypeEnum.PASSED
        10          | PasswordCheckResultTypeEnum.PASSED
        1000        | PasswordCheckResultTypeEnum.FAILED
        null        | PasswordCheckResultTypeEnum.INDETERMINATE_ERROR
    }

    @Unroll
    def "Test cases when dynamoDb throws exception = #exception"() {
        given:
        identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled() >> true

        when:
        1 * dynamoDB.getItem(_) >> exception
        def result = service.checkPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)

        then:
        result == PasswordCheckResultTypeEnum.INDETERMINATE_ERROR
        1 * identityConfig.getReloadableConfig().getDynamoDBPasswordBlacklistCountMaxAllowed() >> 10

        where:
        exception << [new AmazonServiceException("test service exception"),
                      new AmazonClientException("test client exception"),
                      new Exception("test exception")]
    }

    def "Test when feature flag is turned off then result with DISABLE value is returned"() {
        given:
        identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled() >> false

        when:
        def result = service.checkPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)

        then:
        result == PasswordCheckResultTypeEnum.DISABLED
    }
}

