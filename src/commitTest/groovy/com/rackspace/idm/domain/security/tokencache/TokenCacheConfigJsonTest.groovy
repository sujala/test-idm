package com.rackspace.idm.domain.security.tokencache

import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.exception.IdmException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert

import java.time.Duration

class TokenCacheConfigJsonTest extends Specification {
    private static final Logger log = LoggerFactory.getLogger(TokenCacheConfigJsonTest.class)

    def "Can revert back and forth from object to json"() {
        List<CacheableUserJson> users = new ArrayList<>()
        users.add(new CacheableUserJson("CID", Arrays.asList("user1"), Arrays.asList(Arrays.asList("authby1", "authby2"), Arrays.asList("authby1")), Duration.parse("PT10M"), Duration.parse("PT1M")))
        users.add(new CacheableUserJson("CID", Arrays.asList("user2"), Arrays.asList(Arrays.asList("authby3", "authby4")), Duration.parse("PT20M"), Duration.parse("PT2M")))
        TokenCacheConfigJson programmatic = new TokenCacheConfigJson(true, 1000, users)

        when:
        TokenCacheConfigJson tokenCacheConfigJson = TokenCacheConfigJson.fromJson(programmatic.toJson())

        then:
        tokenCacheConfigJson.toJson().equalsIgnoreCase(programmatic.toJson())
    }

    def "Can marshall json from string"() {
       String json = '{"tokenCacheConfig":{"enabled":true,"maxSize":1000,"cacheableUsers":[{"userIds":["user1"],"maximumCacheDuration":"PT30M","authenticatedByLists":[["authby1","authby2"],["authby1"]]},{"userIds":["user2"],"maximumCacheDuration":"PT10M","authenticatedByLists":[["authby3","authby4"]]}]}}'

        when:
        TokenCacheConfigJson tokenCacheConfigJson = TokenCacheConfigJson.fromJson(json)

        then:
        tokenCacheConfigJson.toJson().equalsIgnoreCase(json)
    }

    @Unroll
    def "findCacheDurationForUserWithAuthMethods: Searches the configuration for the duration correctly: '#expectedMinimalValidityDuration' for user: '#userId' with authBy: '#authByList' in type: #type"() {
        List<CacheableUserJson> users = new ArrayList<>()
        users.add(new CacheableUserJson("IAM", ["*"], Arrays.asList(Arrays.asList("authby1")), Duration.parse("PT40M"), Duration.parse("PT30M")))
        users.add(new CacheableUserJson("CID", ["*"], Arrays.asList(Arrays.asList("authby1")), Duration.parse("PT10M"), Duration.parse("PT30M")))
        users.add(new CacheableUserJson("CID", ["user1"], Arrays.asList(Arrays.asList("authby1"), Arrays.asList("authby1", "authby2")), Duration.parse("PT20M"), Duration.parse("PT30M")))
        users.add(new CacheableUserJson("CID", ["*"], Arrays.asList(Arrays.asList("authby2")), Duration.parse("PT30M"), Duration.parse("PT30M")))
        users.add(new CacheableUserJson("CID", ["user3"], Arrays.asList(Arrays.asList("authby1")), Duration.parse("PT50M"), Duration.parse("PT30M")))
        TokenCacheConfigJson tokenCacheConfigJson = new TokenCacheConfigJson(true, 1000, users)

        when:
        CacheableUserJson cacheableUserJson = tokenCacheConfigJson.findConfigForUserWithAuthMethods(type, userId, authByList)

        then:
        cacheableUserJson.getMinimumValidityDuration().equals(expectedMinimalValidityDuration)

        where:
        type | userId | authByList | expectedMinimalValidityDuration
        UserManagementSystem.CID | "user1" | ["authby1"] | Duration.parse("PT20M") // Direct match for user that contains 2 sets of authBy
        UserManagementSystem.CID | "user1" | ["authby2"] | Duration.parse("PT30M") // Verify matches whole set, not just partial.
        UserManagementSystem.CID | "user1" | ["authby1", "authby2"] | Duration.parse("PT20M") // Verify can match on entire set
        UserManagementSystem.CID | "user1" | ["authby2", "authby1"] | Duration.parse("PT20M") // Verify order in set is irrelevant
        UserManagementSystem.CID | "user2" | ["authby1"] | Duration.parse("PT10M") // Matches the default auth by
        UserManagementSystem.CID | "user3" | ["authby1"] | Duration.parse("PT50M") // Direct match
        UserManagementSystem.IAM | "user1" | ["authby1"] | Duration.parse("PT40M") // Matches default racker based
    }

    @Unroll
    def "findCacheDurationForUserWithAuthMethods: Searches the configuration for the duration correctly: not found for user: '#userId' with authBy: '#authByList' in type: #type"() {
        List<CacheableUserJson> users = new ArrayList<>()
        users.add(new CacheableUserJson("IAM", ["*"], Arrays.asList(Arrays.asList("authby1")), Duration.parse("PT40M"), Duration.parse("PT30M")))
        users.add(new CacheableUserJson("CID", ["*"], Arrays.asList(Arrays.asList("authby1")), Duration.parse("PT10M"), Duration.parse("PT30M")))
        users.add(new CacheableUserJson("CID", ["user1"], Arrays.asList(Arrays.asList("authby1"), Arrays.asList("authby1", "authby2")), Duration.parse("PT20M"), Duration.parse("PT30M")))
        users.add(new CacheableUserJson("CID", ["*"], Arrays.asList(Arrays.asList("authby2")), Duration.parse("PT30M"), Duration.parse("PT30M")))
        users.add(new CacheableUserJson("CID", ["user3"], Arrays.asList(Arrays.asList("authby1")), Duration.parse("PT50M"), Duration.parse("PT30M")))
        TokenCacheConfigJson tokenCacheConfigJson = new TokenCacheConfigJson(true, 1000, users)

        when:
        CacheableUserJson cacheableUserJson = tokenCacheConfigJson.findConfigForUserWithAuthMethods(type, userId, authByList)

        then:
        cacheableUserJson == null

        where:
        type | userId | authByList | expectedMinimalValidityDuration
        UserManagementSystem.CID | "user1" | ["authby1", "authby3"] | Duration.parse("PT0S") // should not match anything, so returns a 0 duration
        UserManagementSystem.CID | "user1" | ["authby3"] | Duration.parse("PT0S") // should not match anything, so returns a 0 duration
    }

    @Unroll
    def "fromJson: returns null when json is considered blank - '#json'#"() {
        expect:
        TokenCacheConfigJson.fromJson(json) == null

        where:
        json << [null, " ", ""]
    }

    def "fromJson: Throws IdmException when json is invalid"() {
        when:
        TokenCacheConfigJson.fromJson("invalid")

        then:
        def ex = thrown(Exception)
        IdmExceptionAssert.assertException(ex, IdmException, ErrorCodes.ERROR_CODE_INVALID_VALUE, "Error processing token cache config")
    }
}
