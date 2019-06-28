package com.rackspace.idm.api.resource.cloud.v20

import com.fasterxml.jackson.databind.ObjectMapper
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenFormatEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.Cloud20Methods
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.SC_CREATED

class Cloud20AEIntegrationTest extends RootIntegrationTest {

    @Shared def identityAdmin, userAdmin, userManage, defaultUser, users
    @Shared def domainId

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
    def "update user - setting token format on user is allowed"() {
        given:
        utils.resetServiceAdminToken()
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
        retrievedUser.tokenFormat == TokenFormatEnum.AE

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        staticIdmConfiguration.reset()
        reloadableConfiguration.reset()
    }

    @Unroll
    def "create user - setting token format on user creation allowed when setting to AE"() {
        given:
        utils.resetServiceAdminToken()

        def username = testUtils.getRandomUUID()
        User userForCreate = v2Factory.createUserForCreate(username, "display", "email@email.com", true, null, null, "Password1")
        userForCreate.setTokenFormat(TokenFormatEnum.AE)

        when:
        def response = cloud20.createUser(utils.getServiceAdminToken(), userForCreate)
        assert response.status == SC_CREATED
        def user = cloud20.getUserByName(utils.getServiceAdminToken(), username).getEntity(User).value

        then:
        user.tokenFormat == TokenFormatEnum.AE

        cleanup:
        utils.deleteUsers(user)
        staticIdmConfiguration.reset()
        reloadableConfiguration.reset()
    }

    def "create user - setting token format only allowed to be set by service or identity admins"() {
        given:
        utils.resetServiceAdminToken()

        when: "service admin create user"
        User userForCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("identityAdmin"), "display", "email@email.com", true, null, null, "Password1")
        userForCreate.setTokenFormat(TokenFormatEnum.AE)
        def response = cloud20.createUser(utils.getServiceAdminToken(), userForCreate)
        assert response.status == SC_CREATED
        def userResponse = response.getEntity(User).value
        def identityAdmin = cloud20.getUserByName(utils.getServiceAdminToken(), userResponse.username).getEntity(User).value

        then:
        identityAdmin.tokenFormat == TokenFormatEnum.AE

        when: "identity admin create user-admin"
        def domainId = utils.createDomain()
        userForCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userAdmin"), "display", "email@email.com", true, null, domainId, Constants.DEFAULT_PASSWORD)
        userForCreate.setTokenFormat(TokenFormatEnum.AE)
        response = cloud20.createUser(utils.getIdentityAdminToken(), userForCreate)
        assert response.status == SC_CREATED
        userResponse = response.getEntity(User).value
        def userAdmin = cloud20.getUserByName(utils.getIdentityAdminToken(), userResponse.username).getEntity(User).value

        then: "token format set"
        userAdmin.tokenFormat == TokenFormatEnum.AE

        when: "identity admin create user-manage"
        userForCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userManage"), "display", "email@email.com", true, null, userAdmin.domainId, Constants.DEFAULT_PASSWORD)
        userForCreate.roles = v2Factory.createRoleList([v2Factory.createRole(IdentityUserTypeEnum.USER_MANAGER.getRoleName())].asList())
        userForCreate.setTokenFormat(TokenFormatEnum.AE)
        response = cloud20.createUser(utils.getIdentityAdminToken(), userForCreate)
        assert response.status == SC_CREATED
        userResponse = response.getEntity(User).value
        def userManage = cloud20.getUserByName(utils.getIdentityAdminToken(), userResponse.username).getEntity(User).value

        then: "token format set"
        userManage.tokenFormat == TokenFormatEnum.AE

        when: "identity admin create default user"
        userForCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userManage"), "display", "email@email.com", true, null, userAdmin.domainId, Constants.DEFAULT_PASSWORD)
        userForCreate.roles = v2Factory.createRoleList([v2Factory.createRole(IdentityUserTypeEnum.DEFAULT_USER.getRoleName())].asList())
        userForCreate.setTokenFormat(TokenFormatEnum.AE)
        response = cloud20.createUser(utils.getIdentityAdminToken(), userForCreate)
        assert response.status == SC_CREATED
        userResponse = response.getEntity(User).value
        def defaultUser = cloud20.getUserByName(utils.getIdentityAdminToken(), userResponse.username).getEntity(User).value

        then: "token format set"
        defaultUser.tokenFormat == TokenFormatEnum.AE

        when: "user admin create default user"
        userForCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("userManage"), "display", "email@email.com", true, null, userAdmin.domainId, Constants.DEFAULT_PASSWORD)
        userForCreate.setTokenFormat(TokenFormatEnum.AE)
        response = cloud20.createUser(utils.getToken(userAdmin.username), userForCreate)
        assert response.status == SC_CREATED
        userResponse = response.getEntity(User).value
        def defaultUser2 = cloud20.getUserByName(utils.getIdentityAdminToken(), userResponse.username).getEntity(User).value

        then: "token format NOT set"
        defaultUser2.tokenFormat == null

        cleanup:
        utils.deleteUsers(defaultUser2, defaultUser, userManage, userAdmin, identityAdmin)
    }

    def "retrieve AE token for a user"() {
        given:
        def domainId = utils.createDomain()

        def retrievedUser
        def userAdmin
        def users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when: "retrieving an 'null' tokenFormat"
        def nullToken = utils.authenticateUser(userAdmin.username, Constants.DEFAULT_PASSWORD)

        then: "should default to AE tokens"
        nullToken.token.id.length() > 32

        when: "retrieving an 'AE' tokenFormat"
        retrievedUser = utils.getUserById(userAdmin.id)
        retrievedUser.tokenFormat = TokenFormatEnum.AE
        utils.updateUser(retrievedUser)
        def aeToken = utils.authenticateUser(userAdmin.username, Constants.DEFAULT_PASSWORD)

        then: "should be an AE token"
        aeToken.token.id.length() > 32

        when: "retrieving an 'DEFAULT' tokenFormat"
        retrievedUser = utils.getUserById(userAdmin.id)
        retrievedUser.tokenFormat = TokenFormatEnum.DEFAULT
        utils.updateUser(retrievedUser)
        def defaultToken = utils.authenticateUser(userAdmin.username, Constants.DEFAULT_PASSWORD)

        then: "should be an AE token"
        defaultToken.token.id.length() > 32

        when: "validating an 'AE' token"
        def aeTokenValidate = utils.validateToken(aeToken.token.id)

        then: "should be valid"
        aeTokenValidate.token.id == aeToken.token.id

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        staticIdmConfiguration.reset()
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
        def nullToken = utils.authenticateUser(userAdmin.username, Constants.DEFAULT_PASSWORD)
        retrievedUser = utils.getUserById(userAdmin.id, serviceAdminToken, MediaType.APPLICATION_JSON_TYPE)
        userList = utils.getUsersByEmail(userAdmin.email, serviceAdminToken, MediaType.APPLICATION_JSON_TYPE)

        then: "should be AE token"
        nullToken.token.id.length() > 32
        retrievedUser.tokenFormat == null
        userList[0].tokenFormat == null

        when: "retrieving an 'AE' tokenFormat"
        utils.updateUser('{"user":{"RAX-AUTH:tokenFormat": "AE"}}', userAdmin.id, MediaType.APPLICATION_JSON_TYPE)
        def aeToken = utils.authenticateUser(userAdmin.username, Constants.DEFAULT_PASSWORD)
        retrievedUser = utils.getUserById(userAdmin.id, serviceAdminToken, MediaType.APPLICATION_JSON_TYPE)
        userList = utils.getUsersByEmail(userAdmin.email, serviceAdminToken, MediaType.APPLICATION_JSON_TYPE)

        then: "should be bigger then 32 characters"
        aeToken.token.id.length() > 32
        retrievedUser.tokenFormat == TokenFormatEnum.AE
        userList[0].tokenFormat == TokenFormatEnum.AE

        when: "retrieving an 'DEFAULT' tokenFormat"
        utils.updateUser('{"user":{"RAX-AUTH:tokenFormat": "DEFAULT"}}', userAdmin.id, MediaType.APPLICATION_JSON_TYPE)
        def defaultToken = utils.authenticateUser(userAdmin.username, Constants.DEFAULT_PASSWORD)
        retrievedUser = utils.getUserById(userAdmin.id, serviceAdminToken, MediaType.APPLICATION_JSON_TYPE)
        userList = utils.getUsersByEmail(userAdmin.email, serviceAdminToken, MediaType.APPLICATION_JSON_TYPE)

        then: "should return the same as the 'AE' token"
        defaultToken.token.id.length() > 32
        retrievedUser.tokenFormat == TokenFormatEnum.DEFAULT
        userList[0].tokenFormat == TokenFormatEnum.DEFAULT

        when: "validating an 'AE' token"
        def aeTokenValidate = utils.validateToken(aeToken.token.id)

        then: "should be valid"
        aeTokenValidate.token.id == aeToken.token.id

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        staticIdmConfiguration.reset()
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
        TokenFormatEnum.AE   | TokenFormatEnum.AE   | GetTokenVersion.v11
        TokenFormatEnum.AE   | TokenFormatEnum.AE   | GetTokenVersion.v10
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
        if (authTokenFormat == TokenFormatEnum.AE) {
            assert authToken.token.id.length() > 32
        }

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
        authTokenFormat << [TokenFormatEnum.AE]
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

        def aeTokenOriginal, uuidToken, aeTokenValidateOriginal, uuidTokenValidate, response

        when: "all features are enabled"
        aeTokenOriginal = utils.authenticateUser(userAdmin.username, Constants.DEFAULT_PASSWORD)
        aeTokenValidateOriginal = utils.validateToken(aeTokenOriginal.token.id)

        then: "should be bigger then 32 characters (UUID tokens)"
        aeTokenOriginal.token.id.length() > 32
        aeTokenValidateOriginal.token.id == aeTokenOriginal.token.id

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        reloadableConfiguration.reset()
    }
}
