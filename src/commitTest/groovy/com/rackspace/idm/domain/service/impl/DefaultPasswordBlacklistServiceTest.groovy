package com.rackspace.idm.domain.service.impl

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.GetItemResult
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

    def "Test default behavior is to handle exceptions and return false instead of propagating exception"() {
        given:
        identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled() >> true

        when: "password blacklist service is called"
        boolean isPasswordInBlacklisted = service.isPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)

        then: "by default it handles all exceptions and logs them"
        noExceptionThrown()

        and: "returns that password is not black listed"
        isPasswordInBlacklisted == false
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
        boolean blackListedPasswordIsBlocked = service.isPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)

        then: "password is considered black listed"
        blackListedPasswordIsBlocked == expectation

        1 * identityConfig.getReloadableConfig().getDynamoDBPasswordBlacklistCountMaxAllowed() >> 10

        where:
        countNumber | expectation
        -2000       | false
        -2          | false
        0           | false
        1           | false
        10          | false
        1000        | true
        null        | false
    }

    @Unroll
    def "Test cases when dynamoDb throws exception = #exception"() {
        given:
        identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled() >> true

        when:
        1 * dynamoDB.getItem(_) >> exception
        def result = service.isPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)

        then:
        result == expectedResult
        1 * identityConfig.getReloadableConfig().getDynamoDBPasswordBlacklistCountMaxAllowed() >> 10

        where:
        exception                                            | expectedResult
        new AmazonServiceException("test service exception") | false
        new AmazonClientException("test client exception")   | false
        new Exception("test exception")                      | false
    }
}
