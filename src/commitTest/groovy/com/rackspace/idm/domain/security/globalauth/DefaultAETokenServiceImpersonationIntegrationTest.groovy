package com.rackspace.idm.domain.security.globalauth

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.BaseUser
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.DefaultAETokenServiceBaseIntegrationTest
import com.rackspace.idm.domain.security.tokenproviders.globalauth.MessagePackTokenDataPacker
import org.apache.commons.lang3.RandomStringUtils
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
    def "marshall/unmarshall fully populated impersonation token for impersonator of type #user.getClass().getName(). cache: #cache; enableDomainTokens: #enableDomainTokens"() {
        repositoryConfig.shouldWriteDomainTokens() >> enableDomainTokens
        repositoryConfig.shouldReadDomainTokens() >> enableDomainTokens
        setTokenCaching(cache)

        ImpersonatedScopeAccess originalSA = createImpersonatedToken(user, impersonatedUser).with {
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSCODE)
            it.authenticationDomainId = RandomStringUtils.randomAlphabetic(32)
            return it
        }

        when:
        String webSafeToken = aeTokenService.marshallTokenForUser(user, originalSA)

        then:
        validateWebSafeToken(webSafeToken)

        when: "unmarshall"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then:
        unmarshalledScopeAccess instanceof ImpersonatedScopeAccess
        validateImpersonationScopeAccessesEqual(originalSA, (ImpersonatedScopeAccess) unmarshalledScopeAccess)

        where:
        [user, cache, enableDomainTokens] << [[impersonatorProvisionedUser, impersonatorRackerUser],[true, false], [true, false]].combinations()
    }

    @Unroll
    def "maximize length of marshalling impersonation token for impersonator of type #user.getClass().getName(). cache: #cache"() {
        setTokenCaching(cache)

        ImpersonatedScopeAccess originalSA =  new ImpersonatedScopeAccess().with {
            it.accessTokenString = null //irrelevant
            it.accessTokenExp = new Date()
            it.rsImpersonatingRsId = impersonatedUser.id
            it.userRsId = user.id
            it.clientId = staticConfig.getString(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME)
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSWORD)
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSCODE)
            it.scope = null
            return it
        }

        when:
        String webSafeToken = aeTokenService.marshallTokenForUser(user, originalSA)

        then:
        validateWebSafeToken(webSafeToken)

        when: "unmarshall"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then:
        unmarshalledScopeAccess instanceof ImpersonatedScopeAccess
        validateImpersonationScopeAccessesEqual(originalSA, (ImpersonatedScopeAccess) unmarshalledScopeAccess)

        where:
        [user, cache] << [[impersonatorProvisionedUser, impersonatorRackerUser], [true, false]].combinations()
    }

    @Unroll
    def "marshallTokenForUser() - regenerates impersonation token for impersonator of type #user.getClass().getName(). cache: #cache"() {
        setTokenCaching(cache)

        ImpersonatedScopeAccess originalSA =  new ImpersonatedScopeAccess().with {
            it.accessTokenString = null //irrelevant
            it.accessTokenExp = new Date()
            it.rsImpersonatingRsId = impersonatedUser.id
            it.userRsId = user.id
            it.clientId = staticConfig.getString(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME)
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
        String webSafeToken = aeTokenService.marshallTokenForUser(user, originalSA)

        then:
        validateWebSafeToken(webSafeToken)

        when: "unmarshall"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then: "retrieved token contains new impersonating token"
        unmarshalledScopeAccess instanceof ImpersonatedScopeAccess
        originalSA.impersonatingToken != ((ImpersonatedScopeAccess) unmarshalledScopeAccess).impersonatingToken
        validateImpersonationScopeAccessesEqual(originalSA, (ImpersonatedScopeAccess) unmarshalledScopeAccess)

        where:
        [user, cache] << [[impersonatorProvisionedUser, impersonatorRackerUser], [true, false]].combinations()
    }


    @Unroll
    def "marshall impersonated scope access with impersonator of type  #user.getClass().getName(). cache: #cache - errors thrown appropriately"() {
        when: "null impersonator userId in token"
        setTokenCaching(cache)

        ImpersonatedScopeAccess impersonatedScopeAccess = createImpersonatedToken(user, impersonatedUser).with {
            it.userRsId = null
            return it
        }
        aeTokenService.marshallTokenForUser(user, impersonatedScopeAccess)

        then:
        thrown(IllegalArgumentException)

        when: "null impersonated userId in token"
        impersonatedScopeAccess = createImpersonatedToken(user, impersonatedUser).with {
            it.rsImpersonatingRsId = null
            return it
        }
        aeTokenService.marshallTokenForUser(user, impersonatedScopeAccess)

        then:
        thrown(IllegalArgumentException)

        when: "token userId does not match provided user"
        impersonatedScopeAccess = createImpersonatedToken(user, impersonatedUser).with {
            it.userRsId += "blah"
            return it
        }
        aeTokenService.marshallTokenForUser(user, impersonatedScopeAccess)

        then:
        thrown(IllegalArgumentException)

        where:
        [user, cache] << [[impersonatorProvisionedUser, impersonatorRackerUser], [true, false]].combinations()
    }

    def createImpersonatedToken(BaseUser impersonator, User impersonated, String tokenString =  UUID.randomUUID().toString(), Date expiration = new DateTime().plusDays(1).toDate(), List<String> authBy = [GlobalConstants.AUTHENTICATED_BY_PASSWORD]) {
        new ImpersonatedScopeAccess().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = expiration
            it.rsImpersonatingRsId = impersonated.id
            it.userRsId = impersonator.id
            it.clientId = staticConfig.getString(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME)
            it.getAuthenticatedBy().addAll(authBy)
            return it
        }
    }
}