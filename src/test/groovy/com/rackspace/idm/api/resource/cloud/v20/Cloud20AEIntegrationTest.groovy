package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenFormatEnum
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.DEFAULT_PASSWORD
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly

@ContextConfiguration(locations = ["classpath:app-config.xml"
        , "classpath:com/rackspace/idm/api/resource/cloud/v20/MultifactorSessionIdKeyLocation-context.xml"])
class Cloud20AEIntegrationTest extends RootIntegrationTest {

    @Shared def identityAdmin, userAdmin, userManage, defaultUser, users
    @Shared def domainId

    enum GetTokenVersion {
        v20,
        v11,
        v10
    }

    @Override
    public void doSetupSpec() {
        ClassPathResource resource = new ClassPathResource("/com/rackspace/idm/api/resource/cloud/v20/keys");
        resource.exists()
        this.resource = startOrRestartGrizzly("classpath:app-config.xml " +
                "classpath:com/rackspace/idm/api/resource/cloud/v20/MultifactorSessionIdKeyLocation-context.xml")
    }

    @Override
    public void doCleanupSpec() {
        stopGrizzly();
    }

    def "update token format on user"() {
        given:
        def domainId = utils.createDomain()

        def userAdmin
        def users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when:
        userAdmin.tokenFormat = TokenFormatEnum.AE
        utils.updateUser(userAdmin)
        def retrievedUser = utils.getUserById(userAdmin.id)

        then:
        retrievedUser.tokenFormat == TokenFormatEnum.AE

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "retrieve AE token for a user"() {
        given:
        def domainId = utils.createDomain()

        def retrievedUser
        def userAdmin
        def users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when: "retrieving an 'null' tokenFormat"
        def nullToken = utils.authenticateUser(userAdmin.username, DEFAULT_PASSWORD)

        then: "should default to UUID tokens"
        nullToken.token.id.length() == 32

        when: "retrieving an 'AE' tokenFormat"
        retrievedUser = utils.getUserById(userAdmin.id)
        retrievedUser.tokenFormat = TokenFormatEnum.AE
        utils.updateUser(retrievedUser)
        def aeToken = utils.authenticateUser(userAdmin.username, DEFAULT_PASSWORD)

        then: "should be bigger then 32 characters (UUID tokens)"
        aeToken.token.id.length() > 32

        when: "retrieving an 'UUID' tokenFormat"
        retrievedUser = utils.getUserById(userAdmin.id)
        retrievedUser.tokenFormat = TokenFormatEnum.UUID
        utils.updateUser(retrievedUser)
        def uuidToken = utils.authenticateUser(userAdmin.username, DEFAULT_PASSWORD)

        then: "should return the same original 'null' token"
        uuidToken.token.id == nullToken.token.id

        when: "retrieving an 'DEFAULT' tokenFormat"
        retrievedUser = utils.getUserById(userAdmin.id)
        retrievedUser.tokenFormat = TokenFormatEnum.DEFAULT
        utils.updateUser(retrievedUser)
        def defaultToken = utils.authenticateUser(userAdmin.username, DEFAULT_PASSWORD)

        then: "should return the same as the 'UUID' token"
        defaultToken.token.id == uuidToken.token.id

        when: "validating an 'AE' token"
        def aeTokenValidate = utils.validateToken(aeToken.token.id)

        then: "should be valid"
        aeTokenValidate.token.id == aeToken.token.id

        when: "validating an 'UUID' token"
        def uuidTokenValidate = utils.validateToken(uuidToken.token.id)

        then: "should be valid"
        uuidTokenValidate.token.id == uuidToken.token.id

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    @Unroll
    def "validate AE token X-Auth-Token = #authTokenFormat validateToken = #validateTokenFormat getTokenVersion = #getTokenVersion"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]
        setUserTokenFormat(identityAdmin, authTokenFormat)
        setUserTokenFormat(userAdmin, validateTokenFormat)
        utils.addApiKeyToUser(identityAdmin)
        utils.addApiKeyToUser(userAdmin)
        def identityAdminToken = getToken(identityAdmin, getTokenVersion)
        def userAdminToken = getToken(userAdmin, getTokenVersion)

        when: "v2.0"
        def response = cloud20.validateToken(identityAdminToken, userAdminToken)

        then:
        response.status == 200

        when: "v1.1"
        response = cloud11.validateToken(userAdminToken)

        then:
        response.status == 200

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        authTokenFormat      | validateTokenFormat  | getTokenVersion
        TokenFormatEnum.AE   | TokenFormatEnum.AE   | GetTokenVersion.v20
        TokenFormatEnum.AE   | TokenFormatEnum.UUID | GetTokenVersion.v20
        TokenFormatEnum.UUID | TokenFormatEnum.AE   | GetTokenVersion.v20
        TokenFormatEnum.UUID | TokenFormatEnum.UUID | GetTokenVersion.v20

        TokenFormatEnum.AE   | TokenFormatEnum.AE   | GetTokenVersion.v11
        TokenFormatEnum.AE   | TokenFormatEnum.UUID | GetTokenVersion.v11
        TokenFormatEnum.UUID | TokenFormatEnum.AE   | GetTokenVersion.v11
        TokenFormatEnum.UUID | TokenFormatEnum.UUID | GetTokenVersion.v11

        TokenFormatEnum.AE   | TokenFormatEnum.AE   | GetTokenVersion.v10
        TokenFormatEnum.AE   | TokenFormatEnum.UUID | GetTokenVersion.v10
        TokenFormatEnum.UUID | TokenFormatEnum.AE   | GetTokenVersion.v10
        TokenFormatEnum.UUID | TokenFormatEnum.UUID | GetTokenVersion.v10

    }

    def setUserTokenFormat(user, tokenFormat) {
        def retrievedUser = utils.getUserById(user.id)
        retrievedUser.tokenFormat = tokenFormat
        utils.updateUser(retrievedUser)
    }

    def getToken(user, version) {
        if(version == GetTokenVersion.v20) {
           return utils.getToken(user.username)
        } else if(version == GetTokenVersion.v11) {
            return utils11.getToken(user.username)
        } else {
            return utils10.getToken(user.username)
        }
    }

}
