package com.rackspace.idm.domain.security

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.BaseUser
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.packers.MessagePackTokenDataPacker
import org.apache.commons.lang.RandomStringUtils
import org.joda.time.DateTime
import spock.lang.Shared
import spock.lang.Unroll

class DefaultAETokenServiceImpersonationIntegrationTest extends DefaultAETokenServiceBaseIntegrationTest {
    @Shared User impersonatorProvisionedUser;
    @Shared Racker impersonatorRackerUser;
    @Shared User impersonatedUser;

    def setupSpec() {
        impersonatorProvisionedUser = entityFactory.createUser().with {
            it.id = UUID.randomUUID().toString().replaceAll("-", "")
            it.username = "impersonatorProvisionedUser"
            return it
        }
        impersonatorRackerUser = entityFactory.createRacker().with {
            it.rackerId = UUID.randomUUID().toString().replaceAll("-", "")
            it.username = "impersonatorRackerUser"
            return it
        }
        impersonatedUser = entityFactory.createUser().with {
            it.id = UUID.randomUUID().toString().replaceAll("-", "")
            it.username = "impersonated"
            return it
        }

        identityUserService.getEndUserById(impersonatedUser.id) >> impersonatedUser
    }

    @Unroll
    def "marshall/unmarshall fully populated impersonation token for impersonator of type #methodDesc"() {
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

        where:
        impersonatorUser | methodDesc
        impersonatorProvisionedUser | "Provisioned User"
        impersonatorRackerUser | "Racker"
    }

    @Unroll
    def "maximize length of marshalling impersonation token for impersonator of type #methodDesc"() {
        ImpersonatedScopeAccess originalSA =  new ImpersonatedScopeAccess().with {
            it.accessTokenString = null //irrelevant
            it.accessTokenExp = new Date()
            it.rsImpersonatingRsId = impersonatedUser.id
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

        where:
        impersonatorUser | methodDesc
        impersonatorProvisionedUser | "Provisioned User"
        impersonatorRackerUser | "Racker"
    }

    @Unroll
    def "marshallTokenForUser() - regenerates impersonation token for impersonator of type #methodDesc"() {
        ImpersonatedScopeAccess originalSA =  new ImpersonatedScopeAccess().with {
            it.accessTokenString = null //irrelevant
            it.accessTokenExp = new Date()
            it.rsImpersonatingRsId = impersonatedUser.id
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

        then: "retrieved token contains new impersonating token"
        unmarshalledScopeAccess instanceof ImpersonatedScopeAccess
        originalSA.impersonatingToken != ((ImpersonatedScopeAccess) unmarshalledScopeAccess).impersonatingToken
        validateImpersonationScopeAccessesEqual(originalSA, (ImpersonatedScopeAccess) unmarshalledScopeAccess)

        where:
        impersonatorUser | methodDesc
        impersonatorProvisionedUser | "Provisioned User"
        impersonatorRackerUser | "Racker"
    }


    @Unroll
    def "marshall impersonated scope access with impersonator of type #methodDesc - errors thrown appropriately"() {
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
            it.rsImpersonatingRsId = null
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

        where:
        impersonatorUser | methodDesc
        impersonatorProvisionedUser | "Provisioned User"
        impersonatorRackerUser | "Racker"
    }

    def createImpersonatedToken(BaseUser impersonator, User impersonated, String tokenString =  UUID.randomUUID().toString(), Date expiration = new DateTime().plusDays(1).toDate(), List<String> authBy = [GlobalConstants.AUTHENTICATED_BY_PASSWORD]) {
        new ImpersonatedScopeAccess().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = expiration
            it.rsImpersonatingRsId = impersonated.id
            it.userRsId = impersonator.id
            it.clientId = config.getString(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME)
            it.getAuthenticatedBy().addAll(authBy)
            return it
        }
    }
}
