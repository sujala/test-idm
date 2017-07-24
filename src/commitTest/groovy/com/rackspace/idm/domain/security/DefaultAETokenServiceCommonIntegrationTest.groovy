package com.rackspace.idm.domain.security

import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.tokenproviders.globalauth.MessagePackTokenDataPacker
import org.keyczar.util.Base64Coder
import spock.lang.Shared

/**
 * Tests common error scenarios with generating ae tokens.
 */
class DefaultAETokenServiceCommonIntegrationTest extends DefaultAETokenServiceBaseIntegrationTest {
    @Shared User hardCodedUser;

    def setupSpec() {
       hardCodedUser = entityFactory.createUser().with {
            it.id = UUID.randomUUID().toString().replaceAll("-", "")
            it.username = "random@random.com"
            return it
        }
    }

    def "marshallTokenForUser() - errors thrown appropriately"() {
        when: "null token"
        aeTokenService.marshallTokenForUser(hardCodedUser, null)

        then:
        thrown(IllegalArgumentException)

        when: "null user"
        UserScopeAccess originalUSA = createProvisionedUserToken(hardCodedUser)
        aeTokenService.marshallTokenForUser(null, originalUSA)

        then:
        thrown(IllegalArgumentException)

        when: "null token expiration"
        originalUSA = createProvisionedUserToken(hardCodedUser).with {
            it.accessTokenExp = null
            return it
        }
        aeTokenService.marshallTokenForUser(hardCodedUser, originalUSA)

        then:
        thrown(IllegalArgumentException)
    }

    def "unMarshallToken() - throws errors appropriately"() {
        when: "null token"
        aeTokenService.unmarshallToken(null)

        then:
        thrown(IllegalArgumentException)

        when: "no data"
        aeTokenService.unmarshallToken("")

        then:
        thrown(IllegalArgumentException)

        when: "non-base 64 encoded token"
        aeTokenService.unmarshallToken("@(&*@")

        then:
        UnmarshallTokenException ex = thrown()
        ex.errorCode == DefaultAETokenService.ERROR_CODE_UNMARSHALL_INVALID_BASE64

        when: "not enough data"
        aeTokenService.unmarshallToken(Base64Coder.encodeWebSafe("0".bytes));

        then:
        ex = thrown()
        ex.errorCode == DefaultAETokenService.ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME

        when: "invalid encryption scheme"
        aeTokenService.unmarshallToken(Base64Coder.encodeWebSafe("b".bytes))

        then:
        ex = thrown()
        ex.errorCode == DefaultAETokenService.ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME
    }

}
