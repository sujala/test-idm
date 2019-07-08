package com.rackspace.idm.domain.security.tokencache

import com.rackspace.idm.domain.entity.BaseUser
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.User
import spock.lang.Specification
import spock.lang.Unroll

class CacheKeyTest extends Specification {
    /**
     * This tests various equivalences for different types of CacheKeys. It uses the same type of
     * user for both with same id, and modifies the authBy collections to verify equivalences.
     */
    @Unroll
    def "isEquals/hashcode: Test equivalence: #userGen.getClass().getName(); #authByCombo"() {
        CacheKey key1 = new CacheKey(userGen, authByCombo[0])
        CacheKey key2 = new CacheKey(userGen, authByCombo[1])

        expect:
        key1.equals(key2)

        and: "Since cache keys are used as keys in collections, the hashcodes of equal keys must be the same"
        key1.hashCode() == key2.hashCode()

        where:
        [userGen, authByCombo] << [[this.&genRacker("racker"), this.&genUser("user"), this.&genFedUser("fed"), this.&genBaseUser("base")], [
                                   [["PASSWORD"], ["PASSWORD"]]
                                   , [["PASSWORD", "API"], ["PASSWORD", "API"]]
                                   , [["PASSWORD", "API"], ["API", "PASSWORD"]]
                                   , [["PASSWORD", "API"], ["API", "PASSWORD"] as Set]
        ]].combinations()
    }

    /**
     * This tests that CacheKeys with different types of users, even with the same id, are considered different
     */
    @Unroll
    def "isEquals: Test not equivalent for different user types with same id: #user1Gen.getClass().getName() and #user2Gen.getClass().getName()"() {
        CacheKey key1 = new CacheKey(user1Gen, ["PASSWORD"])
        CacheKey key2 = new CacheKey(user2Gen, ["PASSWORD"])

        expect:
        !key1.equals(key2)

        where:
        user1Gen | user2Gen
        this.&genRacker("id") | this.&genFedUser("id")
        this.&genRacker("id") | this.&genUser("id")
        this.&genRacker("id") | this.&genBaseUser("id")
        this.&genUser("id") | this.&genFedUser("id")
        this.&genUser("id") | this.&genBaseUser("id")
        this.&genFedUser("id") | this.&genBaseUser("id")
    }

    /**
     * This tests that CacheKeys with different auth bys, even with the same user, are considered different
     */
    @Unroll
    def "isEquals: Test different auth by results in not equivalent: #userGen.getClass().getName(); #authByCombo[0]; #authByCombo[1]"() {
        CacheKey key1 = new CacheKey(userGen, authByCombo[0])
        CacheKey key2 = new CacheKey(userGen, authByCombo[1])

        expect:
        !key1.equals(key2)

        where:
        [userGen, authByCombo] << [[this.&genRacker("racker"), this.&genUser("user"), this.&genFedUser("fed"), this.&genBaseUser("base")], [
                [["PASSWORD"], ["API"]]
                , [["PASSWORD", "API"], ["PASSWORD", "PASSWORD", "API"]]
                , [["PASSWORD", "OTP"], ["API", "PASSWORD"]]
        ]].combinations()
    }

    def "toString: Returns userId and auth by methods"() {
        CacheKey key1 = new CacheKey(genUser("theUserId"), ["PASSWORD"])

        when:
        def out = key1.toString()

        then:
        out.contains("userId='theUserId'")
        out.contains("PASSWORD")
    }

    def genRacker(String id) {
        new Racker().with {
            it.id = id
            it
        }
    }

    def genUser(String id) {
        return new User().with {
            it.id = id
            it
        }
    }

    def genFedUser(String id) {
        return new FederatedUser().with {
            it.id = id
            it
        }
    }

    def genBaseUser(String id) {
        new BaseUser() {
            @Override
            String getDomainId() {
                return null
            }

            @Override
            boolean isDisabled() {
                return false
            }

            @Override
            String getUsername() {
                return null
            }

            @Override
            String getId() {
                return id
            }

            @Override
            String getUniqueId() {
                return null
            }

            @Override
            void setUniqueId(String uniqueId) {

            }

            @Override
            String getAuditContext() {
                return null
            }
        }
    }
}
