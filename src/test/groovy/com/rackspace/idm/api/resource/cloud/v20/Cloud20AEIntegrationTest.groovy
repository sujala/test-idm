package com.rackspace.idm.api.resource.cloud.v20

import com.fasterxml.jackson.databind.ObjectMapper
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenFormatEnum
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.Cloud20Methods
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.DEFAULT_PASSWORD
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.startOrRestartGrizzly
import static com.rackspace.idm.api.resource.cloud.AbstractAroundClassJerseyTest.stopGrizzly
import static org.apache.http.HttpStatus.SC_CREATED
import static org.apache.http.HttpStatus.SC_OK

class Cloud20AEIntegrationTest extends RootIntegrationTest {

    @Shared def identityAdmin, userAdmin, userManage, defaultUser, users
    @Shared def domainId

    @Autowired
    LdapScopeAccessRepository ldapScopeAccessRepository

    @Autowired
    IdentityConfig config;

    @Autowired
    Cloud20Methods methods

    enum GetTokenVersion {
        v20,
        v11,
        v10
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

    @Unroll
    def "update user - setting token format on user only allowed if ae tokens disabled. Testing with ae tokens enabled: #ae_enabled"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_AE_TOKENS_ENCRYPT, ae_enabled)
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_AE_TOKENS_DECRYPT, ae_enabled)
        def domainId = utils.createDomain()

        def userAdmin
        def users
        (userAdmin, users) = utils.createUserAdmin(domainId)
        assert userAdmin.tokenFormat == null

        when:
        userAdmin.tokenFormat = TokenFormatEnum.AE
        utils.updateUser(userAdmin)
        def retrievedUser = utils.getUserById(userAdmin.id)

        then:
        ae_enabled ? retrievedUser.tokenFormat == TokenFormatEnum.AE : retrievedUser.tokenFormat == null

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        staticIdmConfiguration.reset()

        where:
        ae_enabled  | _
        false       | _
        true        | _
    }

    @Unroll
    def "create user - setting token format on user creation only allowed if ae tokens enabled. Testing with ae tokens enabled: #ae_enabled"() {
        given:
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_AE_TOKENS_ENCRYPT, ae_enabled)
        staticIdmConfiguration.setProperty(IdentityConfig.FEATURE_AE_TOKENS_DECRYPT, ae_enabled)

        def username = testUtils.getRandomUUID()
        User userForCreate = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, null, "Password1")
        userForCreate.setTokenFormat(TokenFormatEnum.AE)

        when:
        def response = cloud20.createUser(utils.getServiceAdminToken(), userForCreate)
        assert response.status == SC_CREATED
        def user = cloud20.getUserByName(utils.getServiceAdminToken(), username).getEntity(User).value

        then:
        ae_enabled ? user.tokenFormat == TokenFormatEnum.AE : user.tokenFormat == null

        cleanup:
        utils.deleteUsers(user)
        staticIdmConfiguration.reset()

        where:
        ae_enabled  | _
        false       | _
        true        | _
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

    def "retrieve and update AE token for a user using JSON"() {
        given:
        def domainId = utils.createDomain()
        def serviceAdminToken = utils.getServiceAdminToken()

        def retrievedUser
        def userList
        def userAdmin
        def users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when: "retrieving an 'null' tokenFormat"
        def nullToken = utils.authenticateUser(userAdmin.username, DEFAULT_PASSWORD)
        retrievedUser = utils.getUserById(userAdmin.id, serviceAdminToken, MediaType.APPLICATION_JSON_TYPE)
        userList = utils.getUsersByEmail(userAdmin.email, serviceAdminToken, MediaType.APPLICATION_JSON_TYPE)

        then: "should default to UUID tokens"
        nullToken.token.id.length() == 32
        retrievedUser.user['RAX-AUTH:tokenFormat'] == null
        userList.users[0]['RAX-AUTH:tokenFormat'] == null

        when: "retrieving an 'AE' tokenFormat"
        utils.updateUser('{"user":{"RAX-AUTH:tokenFormat": "AE"}}', userAdmin.id, MediaType.APPLICATION_JSON_TYPE)
        def aeToken = utils.authenticateUser(userAdmin.username, DEFAULT_PASSWORD)
        retrievedUser = utils.getUserById(userAdmin.id, serviceAdminToken, MediaType.APPLICATION_JSON_TYPE)
        userList = utils.getUsersByEmail(userAdmin.email, serviceAdminToken, MediaType.APPLICATION_JSON_TYPE)

        then: "should be bigger then 32 characters (UUID tokens)"
        aeToken.token.id.length() > 32
        retrievedUser.user['RAX-AUTH:tokenFormat'] == 'AE'
        userList.users[0]['RAX-AUTH:tokenFormat'] == 'AE'

        when: "retrieving an 'UUID' tokenFormat"
        utils.updateUser('{"user":{"RAX-AUTH:tokenFormat": "UUID"}}', userAdmin.id, MediaType.APPLICATION_JSON_TYPE)
        def uuidToken = utils.authenticateUser(userAdmin.username, DEFAULT_PASSWORD)
        retrievedUser = utils.getUserById(userAdmin.id, serviceAdminToken, MediaType.APPLICATION_JSON_TYPE)
        userList = utils.getUsersByEmail(userAdmin.email, serviceAdminToken, MediaType.APPLICATION_JSON_TYPE)

        then: "should return the same original 'null' token"
        uuidToken.token.id == nullToken.token.id
        retrievedUser.user['RAX-AUTH:tokenFormat'] == 'UUID'
        userList.users[0]['RAX-AUTH:tokenFormat'] == 'UUID'

        when: "retrieving an 'DEFAULT' tokenFormat"
        utils.updateUser('{"user":{"RAX-AUTH:tokenFormat": "DEFAULT"}}', userAdmin.id, MediaType.APPLICATION_JSON_TYPE)
        def defaultToken = utils.authenticateUser(userAdmin.username, DEFAULT_PASSWORD)
        retrievedUser = utils.getUserById(userAdmin.id, serviceAdminToken, MediaType.APPLICATION_JSON_TYPE)
        userList = utils.getUsersByEmail(userAdmin.email, serviceAdminToken, MediaType.APPLICATION_JSON_TYPE)

        then: "should return the same as the 'UUID' token"
        defaultToken.token.id == uuidToken.token.id
        retrievedUser.user['RAX-AUTH:tokenFormat'] == 'DEFAULT'
        userList.users[0]['RAX-AUTH:tokenFormat'] == 'DEFAULT'

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
        try { utils.deleteUsers(users) } catch (Exception e) {}
        try { utils.deleteDomain(domainId) } catch (Exception e) {}

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

    def "auth with AE Token + tenant"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        setUserTokenFormat(userAdmin, TokenFormatEnum.AE)
        def userAdminToken = getToken(userAdmin, GetTokenVersion.v20)

        when: "auth w/token and tenant"
        def newToken = utils.getTokenFromAuthWithToken(userAdminToken, domainId)

        then:
        userAdminToken == newToken

        cleanup:
        try { utils.deleteUsers(users) } catch (Exception e) {}
        try { utils.deleteDomain(domainId) } catch (Exception e) {}
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

    def "auth and validate with racker token 'test.ae'"() {
        given:
        def identityAdminToken = utils.getIdentityAdminToken()
        def response
        def rackerToken

        when: "auth v2.0"
        response = cloud20.authenticateRacker('test.ae', 'password')
        rackerToken = response.getEntity(AuthenticateResponse).value.token.id

        then:
        response.status == 200
        rackerToken.length() > 32

        when: "validate v2.0"
        response = cloud20.validateToken(identityAdminToken, rackerToken)

        then:
        response.status == 200
    }

    @Unroll
    def "auth and validate '#authTokenFormat' tokens to verify content"() {
        given:
        def response
        def authToken
        def responseToken

        def identityAdminToken = utils.getIdentityAdminToken()
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]
        setUserTokenFormat(userAdmin, authTokenFormat)
        utils.addApiKeyToUser(userAdmin)

        when: "user auth v2.0"
        response = cloud20.authenticatePassword(userAdmin.username)
        authToken = response.getEntity(AuthenticateResponse).value

        then:
        response.status == 200
        authTokenFormat == TokenFormatEnum.AE ? authToken.token.id.length() > 32 : authToken.token.id.length() == 32

        when: "user validate v2.0"
        response = cloud20.validateToken(identityAdminToken, authToken.token.id)
        responseToken = response.getEntity(AuthenticateResponse).value

        then:
        response.status == 200
        compareTwoTokens(authToken, responseToken)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        authTokenFormat << [TokenFormatEnum.AE, TokenFormatEnum.UUID]
    }

    /**
     * With this change it is expected that all Racker use AE tokens
     * @return
     */
    @Unroll
    def "auth and validate racker AE tokens to verify content"() {
        given:
        def response
        def authToken
        def responseToken

        def identityAdminToken = utils.getIdentityAdminToken()
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]
        setUserTokenFormat(userAdmin, authTokenFormat)
        utils.addApiKeyToUser(userAdmin)

        when: "racker auth v2.0"
        response = cloud20.authenticateRacker('test.ae', 'password')
        authToken = response.getEntity(AuthenticateResponse).value

        then:
        response.status == 200
        authTokenFormat == TokenFormatEnum.AE ? authToken.token.id.length() > 32 : authToken.token.id.length() == 32

        when: "racker validate v2.0"
        response = cloud20.validateToken(identityAdminToken, authToken.token.id)
        responseToken = response.getEntity(AuthenticateResponse).value

        then:
        response.status == 200
        compareTwoTokens(authToken, responseToken)

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        authTokenFormat << [TokenFormatEnum.AE]
    }

    def compareTwoTokens(authToken, responseToken) {
        ObjectMapper mapper = new ObjectMapper()

        def authJson = mapper.writeValueAsString(authToken)
        Map authMap = mapper.readValue(authJson, Map)

        def responseJson = mapper.writeValueAsString(responseToken)
        Map responseMap = mapper.readValue(responseJson, Map)

        return compareTwoTokenMaps(authMap, responseMap)
    }

    def compareTwoTokenMaps(authMap, responseMap) {
        boolean check = true;

        for (String key : authMap.keySet()) {
            def authValue = authMap.get(key)
            def responseValue = responseMap.get(key)
            check = check && compareTwoTokenValues(authValue, responseValue)
        }

        return check;
    }

    def compareTwoTokenValues(authValue, responseValue) {
        if (authValue != null && responseValue != null) { // Some fields are empty on auth or validate
            if (authValue instanceof Map) {
                return compareTwoTokenMaps((Map) authValue, (Map) responseValue)
            } else if (authValue instanceof List) {
                boolean check = true
                for (int i=0; i<((List) authValue).size(); i++) {
                    check = check && compareTwoTokenValues(((List) authValue).get(i), ((List) responseValue).get(i))
                }
                return check
            } else {
                return authValue.equals(responseValue)
            }
        } else {
            return true
        }
    }

    def "verify revoke UUID tokens when AE token config is set"() {
        given:
        def response
        def authToken
        def uuidToken

        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]
        utils.addApiKeyToUser(userAdmin)

        when: "user auth v2.0 (using UUID)"
        response = cloud20.authenticatePassword(userAdmin.username)
        authToken = response.getEntity(AuthenticateResponse).value
        uuidToken = authToken.token.id

        then:
        response.status == 200
        uuidToken.length() == 32

        when: "revoking token after setting AE token"
        setUserTokenFormat(userAdmin, TokenFormatEnum.AE)
        response = cloud20.revokeUserToken(utils.getIdentityAdminToken(), uuidToken)

        then: "it should still revoke it"
        response.status == 204
        ldapScopeAccessRepository.getScopeAccessByAccessToken(uuidToken).accessTokenExpired

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

    def "authenticating with AE token and tenantName"() {
        given:
        def mossoId = testUtils.getRandomInteger()
        def username = testUtils.getRandomUUID("user")
        def apiKey = "0987654231"
        def user = v1Factory.createUser(username, apiKey, mossoId)
        def createdUser = cloud11.createUser(user).getEntity(com.rackspacecloud.docs.auth.api.v1.User)
        def apiUser = cloud20.getUserByName(utils.getServiceAdminToken(), username).getEntity(org.openstack.docs.identity.api.v2.User).value
        setUserTokenFormat(apiUser, TokenFormatEnum.AE)

        def scopeAccess = cloud20.authenticateApiKey(username, apiKey).getEntity(AuthenticateResponse).value
        def authRequestContent = v2Factory.createAuthenticationRequest(scopeAccess.token.id, null, mossoId.toString())

        when:
        def authResponse = cloud20.authenticate(authRequestContent)
        def scopeAccessResponse = authResponse.getEntity(AuthenticateResponse).value
        def ldapTokens = ldapScopeAccessRepository.getScopeAccessesByUserId(apiUser.id)

        then:
        authResponse.status == 200
        compareTwoTokens(scopeAccess, scopeAccessResponse)
        !ldapTokens.iterator().hasNext()

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), createdUser.id)
    }

    def "test global flag combinations"() {
        given:
        def domainId = utils.createDomain()

        def retrievedUser
        def userAdmin
        def users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        retrievedUser = utils.getUserById(userAdmin.id)
        retrievedUser.tokenFormat = TokenFormatEnum.AE
        utils.updateUser(retrievedUser)

        def aeTokenOriginal, aeToken, aeTokenValidateOriginal, aeTokenValidate, response

        when: "all features are enabled"
        staticIdmConfiguration.setProperty('feature.ae.tokens.encrypt', 'true')
        staticIdmConfiguration.setProperty('feature.ae.tokens.decrypt', 'true')
        aeTokenOriginal = utils.authenticateUser(userAdmin.username, DEFAULT_PASSWORD)
        aeTokenValidateOriginal = utils.validateToken(aeTokenOriginal.token.id)

        then: "should be bigger then 32 characters (UUID tokens)"
        config.getFeatureAETokensEncrypt() == true
        config.getFeatureAETokensDecrypt() == true
        aeTokenOriginal.token.id.length() > 32
        aeTokenValidateOriginal.token.id == aeTokenOriginal.token.id

        when: "creation of tokens is disable (but reading is enable)"
        staticIdmConfiguration.setProperty('feature.ae.tokens.encrypt', 'false')
        aeToken = utils.authenticateUser(userAdmin.username, DEFAULT_PASSWORD)
        aeTokenValidate = utils.validateToken(aeToken.token.id)
        aeTokenValidateOriginal = utils.validateToken(aeTokenOriginal.token.id)

        then: "should be equal to 32 (UUID tokens) but it still decrypt the original one"
        config.getFeatureAETokensEncrypt() == false
        aeToken.token.id.length() == 32
        aeTokenValidate.token.id == aeToken.token.id
        aeTokenValidateOriginal.token.id == aeTokenOriginal.token.id

        when: "creation and reading of tokens is disable"
        staticIdmConfiguration.setProperty('feature.ae.tokens.decrypt', 'false')
        aeToken = utils.authenticateUser(userAdmin.username, DEFAULT_PASSWORD)
        aeTokenValidate = utils.validateToken(aeToken.token.id)
        response = methods.validateToken(utils.getServiceAdminToken(), aeTokenOriginal.token.id)

        then:
        config.getFeatureAETokensDecrypt() == false
        response.status != SC_OK
        aeToken.token.id.length() == 32
        aeTokenValidate.token.id == aeToken.token.id

        when: "creation is enable, reading is always enable"
        staticIdmConfiguration.setProperty('feature.ae.tokens.encrypt', 'true')
        staticIdmConfiguration.setProperty('feature.ae.tokens.decrypt', 'false')

        then:
        config.getFeatureAETokensEncrypt() == true
        config.getFeatureAETokensDecrypt() == true

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
    }

}
