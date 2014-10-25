package com.rackspace.idm.domain.security

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.packers.MessagePackTokenDataPacker
import org.apache.commons.lang.RandomStringUtils
import org.joda.time.DateTime
import spock.lang.Shared

class DefaultAETokenServiceProvisionedUserImpersonationIntegrationTest extends DefaultAETokenServiceBaseIntegrationTest {
    @Shared User impersonatorUser;
    @Shared User impersonatedUser;

    def setupSpec() {
       impersonatorUser = entityFactory.createUser().with {
            it.id = UUID.randomUUID().toString().replaceAll("-", "")
            it.username = "impersonator"
            return it
        }
        impersonatedUser = entityFactory.createUser().with {
            it.id = UUID.randomUUID().toString().replaceAll("-", "")
            it.username = "impersonated"
            return it
        }

        identityUserService.getProvisionedUserById(impersonatorUser.id) >> impersonatorUser
        identityUserService.getProvisionedUserById(impersonatedUser.id) >> impersonatedUser
        identityUserService.getEndUserById(impersonatedUser.id) >> impersonatedUser
    }

    def "marshall/unmarshall fully populated provisioned user impersonation token for provisioned user"() {
        ImpersonatedScopeAccess originalSA = createImpersonatedToken(impersonatorUser, impersonatedUser).with {
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSCODE)
            return it
        }

        when:
        String webSafeToken = aeTokenService.marshallTokenForUser(impersonatorUser, originalSA)

        then:
        validateWebSafeToken(webSafeToken)

        when: "unmarshall"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then:
        unmarshalledScopeAccess instanceof ImpersonatedScopeAccess
        validateImpersonationScopeAccessesEqual(originalSA, (ImpersonatedScopeAccess) unmarshalledScopeAccess)
    }

    def "marshallTokenForUser() - maximize length"() {
        ImpersonatedScopeAccess originalSA =  new ImpersonatedScopeAccess().with {
            it.accessTokenString = null //irrelevant
            it.accessTokenExp = new Date()
            it.impersonatingRsId = impersonatedUser.id
            it.userRsId = impersonatorUser.id
            it.clientId = config.getString(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME)
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSWORD)
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSCODE)
            it.scope = null
            return it
        }

        when:
        String webSafeToken = aeTokenService.marshallTokenForUser(impersonatorUser, originalSA)

        then:
        validateWebSafeToken(webSafeToken)

        when: "unmarshall"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then:
        unmarshalledScopeAccess instanceof ImpersonatedScopeAccess
        validateImpersonationScopeAccessesEqual(originalSA, (ImpersonatedScopeAccess) unmarshalledScopeAccess)
    }

    def "marshallTokenForUser() - with generated impersonated user token"() {
        ImpersonatedScopeAccess originalSA =  new ImpersonatedScopeAccess().with {
            it.accessTokenString = null //irrelevant
            it.accessTokenExp = new Date()
            it.impersonatingRsId = impersonatedUser.id
            it.userRsId = impersonatorUser.id
            it.clientId = config.getString(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME)
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSWORD)
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSCODE)
            it.scope = null
            return it
        }

        UserScopeAccess usa = createProvisionedUserToken(impersonatedUser).with {
            it.accessTokenExp = originalSA.accessTokenExp
            return it
        }
        aeTokenService.marshallTokenForUser(impersonatedUser, usa)

        originalSA.setImpersonatingToken(usa.getAccessTokenString())

        when:
        String webSafeToken = aeTokenService.marshallTokenForUser(impersonatorUser, originalSA)

        then:
        validateWebSafeToken(webSafeToken)

        when: "unmarshall"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then:
        unmarshalledScopeAccess instanceof ImpersonatedScopeAccess
        validateImpersonationScopeAccessesEqual(originalSA, (ImpersonatedScopeAccess) unmarshalledScopeAccess)
    }


    def "marshallTokenForUser(provisioned user) - errors thrown appropriately"() {
        when: "null impersonator userId in token"
        ImpersonatedScopeAccess impersonatedScopeAccess = createImpersonatedToken(impersonatorUser, impersonatedUser).with {
            it.userRsId = null
            return it
        }
        aeTokenService.marshallTokenForUser(impersonatorUser, impersonatedScopeAccess)

        then:
        thrown(IllegalArgumentException)

        when: "null impersonated userId in token"
        impersonatedScopeAccess = createImpersonatedToken(impersonatorUser, impersonatedUser).with {
            it.impersonatingRsId = null
            return it
        }
        aeTokenService.marshallTokenForUser(impersonatorUser, impersonatedScopeAccess)

        then:
        thrown(IllegalArgumentException)

        when: "token userId does not match provided user"
        impersonatedScopeAccess = createImpersonatedToken(impersonatorUser, impersonatedUser).with {
            it.userRsId += "blah"
            return it
        }
        aeTokenService.marshallTokenForUser(impersonatorUser, impersonatedScopeAccess)

        then:
        thrown(IllegalArgumentException)
    }

    def createImpersonatedToken(User impersonator, User impersonated, String tokenString =  UUID.randomUUID().toString(), Date expiration = new DateTime().plusDays(1).toDate(), List<String> authBy = [GlobalConstants.AUTHENTICATED_BY_PASSWORD]) {
        new ImpersonatedScopeAccess().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = expiration
            it.impersonatingRsId = impersonated.id
            it.userRsId = impersonator.id
            it.clientId = config.getString(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME)
            it.getAuthenticatedBy().addAll(authBy)
            return it
        }
    }
}
