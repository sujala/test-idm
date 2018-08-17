package com.rackspace.idm.domain.security.globalauth

import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.DefaultAETokenService
import com.rackspace.idm.domain.security.DefaultAETokenServiceBaseIntegrationTest
import com.rackspace.idm.domain.security.UnmarshallTokenException
import com.rackspace.idm.domain.security.tokenproviders.globalauth.GlobalAuthTokenProvider
import com.rackspace.idm.domain.security.tokenproviders.globalauth.MessagePackTokenDataPacker
import org.keyczar.util.Base64Coder
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

/**
 * Tests common error scenarios with generating ae tokens.
 */
class GlobalAuthAETokenServiceIntegrationTest extends DefaultAETokenServiceBaseIntegrationTest {
    @Shared User hardCodedUser

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
        when: "invalid data packing scheme"
        //little fragile and coupled to internal methods to attempt to cause this error
        aeTokenService.unmarshallToken(Base64Coder.encodeWebSafe(globalAuthTokenProvider.secureTokenData("abcd".bytes)))

        then:
        UnmarshallTokenException ex = thrown()
        ex.errorCode == DefaultAETokenService.ERROR_CODE_UNMARSHALL_INVALID_DATAPACKING_SCHEME

        when: "invalid data packed"
        //little fragile and coupled to internal methods to attempt to cause this error
        byte[] packedBytes = "abcd".bytes
        byte[] dataBytes = new byte[1 + packedBytes.length];
        dataBytes[0] = GlobalAuthTokenProvider.DATA_PACKING_SCHEME_MESSAGE_PACK;
        System.arraycopy(packedBytes, 0, dataBytes, 1, packedBytes.length)
        aeTokenService.unmarshallToken(Base64Coder.encodeWebSafe(globalAuthTokenProvider.secureTokenData(dataBytes)));

        then:
        ex = thrown()
        ex.errorCode == MessagePackTokenDataPacker.ERROR_CODE_UNPACK_INVALID_DATA_CONTENTS
    }

}
