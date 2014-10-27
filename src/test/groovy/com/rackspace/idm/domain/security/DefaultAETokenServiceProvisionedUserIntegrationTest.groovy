package com.rackspace.idm.domain.security

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.packers.MessagePackTokenDataPacker
import spock.lang.Shared
import spock.lang.Unroll

class DefaultAETokenServiceProvisionedUserIntegrationTest extends DefaultAETokenServiceBaseIntegrationTest {
    @Shared User hardCodedUser;

    def setupSpec() {
       hardCodedUser = entityFactory.createUser().with {
            it.id = UUID.randomUUID().toString().replaceAll("-", "")
            it.username = "random@random.com"
            return it
        }

        identityUserService.getProvisionedUserById(hardCodedUser.id) >> hardCodedUser
    }

    def "marshall/unmarshall fully populated provisioned user token"() {
        UserScopeAccess originalUSA = createProvisionedUserToken(hardCodedUser).with {
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSCODE)
            return it
        }

        when:
        String webSafeToken = aeTokenService.marshallTokenForUser(hardCodedUser, originalUSA)

        then:
        validateWebSafeToken(webSafeToken)

        when: "unmarshall"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then:
        unmarshalledScopeAccess instanceof UserScopeAccess
        validateUserScopeAccessesEqual(originalUSA, (UserScopeAccess) unmarshalledScopeAccess)
    }

    @Unroll
    def "marshallTokenForUser() - maximize length; run: #run"() {
        UserScopeAccess originalUSA =  new UserScopeAccess().with {
            it.accessTokenString = null //irrelevant
            it.accessTokenExp = new Date()
            it.userRsId = hardCodedUser.id
            it.userRCN = "RCN-000-000-001" //take from sample data
            it.clientId = config.getString(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME)
            it.clientRCN = "RACKSPACE"
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSWORD)
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSCODE)
            it.scope = GlobalConstants.SETUP_MFA_SCOPE
            return it
        }

        when:
        String webSafeToken = aeTokenService.marshallTokenForUser(hardCodedUser, originalUSA)

        then:
        validateWebSafeToken(webSafeToken)

        when: "unmarshall"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then:
        unmarshalledScopeAccess instanceof UserScopeAccess
        validateUserScopeAccessesEqual(originalUSA, (UserScopeAccess) unmarshalledScopeAccess)
    }

    def "marshall/unmarshall auth with nullable field user token"() {
        UserScopeAccess originalUSA =  new UserScopeAccess().with {
            it.accessTokenString = null
            it.accessTokenExp = new Date()
            it.userRsId = hardCodedUser.id
            it.userRCN = null
            it.clientId = config.getString(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME)
            it.clientRCN = null
            return it
        }

        when: "generate token"
        String webSafeToken = aeTokenService.marshallTokenForUser(hardCodedUser, originalUSA)

        then: "generates token"
        validateWebSafeToken(webSafeToken)

        when: "unmarshall token"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then:
        unmarshalledScopeAccess instanceof UserScopeAccess
        validateUserScopeAccessesEqual(originalUSA, (UserScopeAccess) unmarshalledScopeAccess)
    }

    def "marshall/unmarshall multiple auth by user token"() {
        UserScopeAccess originalUSA = createProvisionedUserToken(hardCodedUser).with {
            it.authenticatedBy.add(GlobalConstants.AUTHENTICATED_BY_PASSCODE)
            return it
        }

        when: "generate token with multiple auth by"
        String webSafeToken = aeTokenService.marshallTokenForUser(hardCodedUser, originalUSA)

        then: "generates token"
        validateWebSafeToken(webSafeToken)

        when: "unmarshall token"
        ScopeAccess unmarshalledScopeAccess = aeTokenService.unmarshallToken(webSafeToken)

        then: "get scope access with multiple auth by"
        unmarshalledScopeAccess instanceof UserScopeAccess
        UserScopeAccess unmarshalledUSA = (UserScopeAccess) unmarshalledScopeAccess
        unmarshalledUSA.authenticatedBy.size() == 2
        validateUserScopeAccessesEqual(originalUSA, (UserScopeAccess) unmarshalledScopeAccess)
    }

    def "marshallTokenForUser(provisioned user) - errors thrown appropriately"() {
        when: "null userId in token"
        UserScopeAccess originalUSA = createProvisionedUserToken(hardCodedUser).with {
            it.userRsId = null
            return it
        }
        aeTokenService.marshallTokenForUser(hardCodedUser, originalUSA)

        then:
        thrown(IllegalArgumentException)

        when: "token userId does not match provided user"
        originalUSA = createProvisionedUserToken(hardCodedUser).with {
            it.userRsId += "9"
            return it
        }
        aeTokenService.marshallTokenForUser(hardCodedUser, originalUSA)

        then:
        thrown(IllegalArgumentException)

        when: "token userId does not match provided user id"
        originalUSA = createProvisionedUserToken(hardCodedUser).with {
            it.userRsId += "blah"
            return it
        }
        aeTokenService.marshallTokenForUser(hardCodedUser, originalUSA)

        then:
        thrown(IllegalArgumentException)
    }
}
