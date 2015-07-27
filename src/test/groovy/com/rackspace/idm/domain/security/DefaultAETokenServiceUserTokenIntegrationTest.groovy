package com.rackspace.idm.domain.security

import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.BaseUser
import com.rackspace.idm.domain.entity.EndUser
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.packers.MessagePackTokenDataPacker
import spock.lang.Shared
import spock.lang.Unroll

/**
 * This test is meant to test issuing a regular token to a user. "Regular" is whatever type of token is appropriate for
 * the user being authenticationed (e.g. - UserScopeAccess for a provisioned or federated user, RackerScopeAccess for a racker).
 *
 * This test does not include verifying the AE format for ImpersonatedScopeAccess tokens. For those,
 * see {@link DefaultAETokenServiceImpersonationIntegrationTest)
 */
class DefaultAETokenServiceUserTokenIntegrationTest extends DefaultAETokenServiceBaseIntegrationTest {
    @Shared User hardCodedProvisionedUser;
    @Shared Racker hardCodedRackerUser;

    def setupSpec() {
        hardCodedProvisionedUser = entityFactory.createUser().with {
            it.id = UUID.randomUUID().toString().replaceAll("-", "")
            it.username = "random@random.com"
            return it
        }
        hardCodedRackerUser = entityFactory.createRacker().with {
            it.rackerId = UUID.randomUUID().toString().replaceAll("-", "")
            it.username = "impersonatorRackerUser"
            return it
        }

        identityUserService.getBaseUserById(hardCodedProvisionedUser.id) >> hardCodedProvisionedUser
        identityUserService.getBaseUserById(hardCodedRackerUser.id) >> hardCodedRackerUser
    }

    @Unroll
    def "marshall/unmarshall fully populated #methodDesc token"() {
        ScopeAccess originalUSA = createUserToken(hardCodedUser)

        when:
        String webSafeToken = aeTokenService.marshallTokenForUser(hardCodedUser, originalUSA)

        then:
        validateWebSafeToken(webSafeToken)

        when: "unmarshall"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then:
        validateScopeAccessesEqual(originalUSA, unmarshalledScopeAccess)

        where:
        hardCodedUser | methodDesc
        hardCodedProvisionedUser | "Provisioned User"
        hardCodedRackerUser | "Racker"
    }

    def "marshallTokenForUser() - maximize provisioned user token length; run: #run"() {
        UserScopeAccess originalUSA =  new UserScopeAccess().with {
            it.accessTokenString = null //irrelevant
            it.accessTokenExp = new Date()
            it.userRsId = hardCodedProvisionedUser.id
            it.clientId = config.getString(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME)
            it.clientRCN = "RACKSPACE"
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSWORD)
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSCODE)
            it.scope = GlobalConstants.SETUP_MFA_SCOPE
            return it
        }

        when:
        String webSafeToken = aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, originalUSA)

        then:
        validateWebSafeToken(webSafeToken)

        when: "unmarshall"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then:
        unmarshalledScopeAccess instanceof UserScopeAccess
        validateUserScopeAccessesEqual(originalUSA, (UserScopeAccess) unmarshalledScopeAccess)
    }

    @Unroll
    def "can marshall/unmarshall scope access of type #methodDesc with null token string"() {
        ScopeAccess originalUSA =  createUserToken(hardCodedUser).with {
            it.accessTokenString = null
            return it
        }

        when: "generate token"
        String webSafeToken = aeTokenService.marshallTokenForUser(hardCodedUser, originalUSA)

        then: "generates token"
        validateWebSafeToken(webSafeToken)

        when: "unmarshall token"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then:
        validateScopeAccessesEqual(originalUSA, unmarshalledScopeAccess)

        where:
        hardCodedUser | methodDesc
        hardCodedProvisionedUser | "Provisioned User"
        hardCodedRackerUser | "Racker"
    }

    @Unroll
    def "marshall/unmarshall multiple authby #methodDesc token"() {
        ScopeAccess originalUSA = createUserToken(hardCodedUser, Arrays.asList(GlobalConstants.AUTHENTICATED_BY_PASSCODE))

        when: "generate token with multiple auth by"
        String webSafeToken = aeTokenService.marshallTokenForUser(hardCodedUser, originalUSA)

        then: "generates token"
        validateWebSafeToken(webSafeToken)

        when: "unmarshall token"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then: "get scope access with multiple auth by"
        unmarshalledScopeAccess.authenticatedBy.size() == 2
        validateScopeAccessesEqual(originalUSA, unmarshalledScopeAccess)

        where:
        hardCodedUser | methodDesc
        hardCodedProvisionedUser | "Provisioned User"
        hardCodedRackerUser | "Racker"
    }

    def "marshallTokenForUser throws errors appropriately for provisioned user token"() {
        when: "null userId in token"
        UserScopeAccess originalUSA = createProvisionedUserToken(hardCodedProvisionedUser).with {
            it.userRsId = null
            return it
        }
        aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, originalUSA)

        then:
        thrown(IllegalArgumentException)

        when: "token userId does not match provided user"
        originalUSA = createProvisionedUserToken(hardCodedProvisionedUser).with {
            it.userRsId += "9"
            return it
        }
        aeTokenService.marshallTokenForUser(hardCodedProvisionedUser, originalUSA)

        then:
        thrown(IllegalArgumentException)
    }

    def "marshallTokenForUser throws errors appropriately for racker user token"() {
        when: "null userId in token"
        ScopeAccess originalUSA = createUserToken(hardCodedRackerUser).with {
            it.rackerId = null
            return it
        }
        aeTokenService.marshallTokenForUser(hardCodedRackerUser, originalUSA)

        then:
        thrown(IllegalArgumentException)

        when: "token rackerId does not match provided user"
        originalUSA = createUserToken(hardCodedRackerUser).with {
            it.rackerId += "9"
            return it
        }
        aeTokenService.marshallTokenForUser(hardCodedRackerUser, originalUSA)

        then:
        thrown(IllegalArgumentException)
    }

    def ScopeAccess createUserToken(BaseUser user, List<String> authBy = Arrays.asList(AuthenticatedByMethodEnum.PASSWORD.value)) {
        if (user instanceof EndUser) {
            return createProvisionedUserToken((User)user).with {
                it.authenticatedBy.addAll(authBy)
                return it
            }
        } else if (user instanceof Racker) {
            return createRackerToken((Racker) user).with {
                it.authenticatedBy.addAll(authBy)
                return it
            }
        }
    }

}
