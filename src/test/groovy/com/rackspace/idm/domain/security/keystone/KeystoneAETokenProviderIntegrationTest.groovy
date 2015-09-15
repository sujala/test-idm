package com.rackspace.idm.domain.security.keystone

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.DefaultAETokenService
import com.rackspace.idm.domain.security.DefaultAETokenServiceBaseIntegrationTest
import com.rackspace.idm.domain.security.UnmarshallTokenException
import com.rackspace.idm.domain.security.tokenproviders.globalauth.GlobalAuthTokenProvider
import com.rackspace.idm.domain.security.tokenproviders.globalauth.MessagePackTokenDataPacker
import com.rackspace.idm.domain.security.tokenproviders.keystone_keyczar.KeystoneAEMessagePackTokenDataPacker
import com.rackspace.idm.domain.security.tokenproviders.keystone_keyczar.KeystoneAETokenProvider
import org.keyczar.util.Base64Coder
import spock.lang.Shared

/**
 * Tests common error scenarios with generating ae tokens.
 */
class KeystoneAETokenProviderIntegrationTest extends DefaultAETokenServiceBaseIntegrationTest {
    @Shared User hardCodedUser;

    def setupSpec() {
       hardCodedUser = entityFactory.createUser().with {
            it.id = UUID.randomUUID().toString().replaceAll("-", "")
            it.username = "random@random.com"
            return it
        }
    }

    def setup() {
        reloadableConfig.setProperty(IdentityConfig.FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_PROP, true)
    }

    def "Creating token throws error"() {
        expect:
        !keystoneAETokenProvider.supportsCreatingTokenFor(hardCodedUser, new UserScopeAccess())

        when:
        keystoneAETokenProvider.packProtectedTokenData(hardCodedUser, new UserScopeAccess())

        then:
        thrown(UnsupportedOperationException)
    }

    def "unMarshallToken() - throws errors appropriately"() {
        when: "invalid data packing scheme"
        //little fragile and coupled to internal methods to attempt to cause this error
        aeTokenService.unmarshallToken(Base64Coder.encodeWebSafe(keystoneAETokenProvider.secureTokenData("abcd".bytes)))

        then:
        UnmarshallTokenException ex = thrown()
        ex.errorCode == DefaultAETokenService.ERROR_CODE_UNMARSHALL_INVALID_DATAPACKING_SCHEME

        when: "invalid data packed"
        //little fragile and coupled to internal methods to attempt to cause this error
        byte[] packedBytes = "abcd".bytes
        byte[] dataBytes = new byte[1 + packedBytes.length];
        dataBytes[0] = KeystoneAETokenProvider.DATA_PACKING_SCHEME_MESSAGE_PACK;
        System.arraycopy(packedBytes, 0, dataBytes, 1, packedBytes.length)
        aeTokenService.unmarshallToken(Base64Coder.encodeWebSafe(keystoneAETokenProvider.secureTokenData(dataBytes)));

        then:
        ex = thrown()
        ex.errorCode == KeystoneAEMessagePackTokenDataPacker.ERROR_CODE_UNPACK_INVALID_DATA_CONTENTS
    }

}
