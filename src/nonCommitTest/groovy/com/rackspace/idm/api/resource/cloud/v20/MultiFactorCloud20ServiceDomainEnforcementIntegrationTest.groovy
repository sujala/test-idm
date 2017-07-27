package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainMultiFactorEnforcementLevelEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDomain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserMultiFactorEnforcementLevelEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.identity.multifactor.domain.GenericMfaAuthenticationResponse
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecision
import com.rackspace.identity.multifactor.domain.MfaAuthenticationDecisionReason
import com.rackspace.idm.Constants
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.api.resource.cloud.v20.json.writers.JSONWriterForRaxAuthMultiFactorDomain
import com.rackspace.idm.domain.dao.UserDao
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.mockito.Mockito
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Token
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.*

/**
 * Tests the multifactor domain enforcement level
 */
class MultiFactorCloud20ServiceDomainEnforcementIntegrationTest extends RootConcurrentIntegrationTest {

    @Autowired
    UserDao userRepository;

    /**
     * Tests the full "baseline" transition state for domain enforcement such that a user-admin/user-manager can set the
     * MFA user enforcement level on their domain and that when setting to "REQUIRED"
     * existing tokens for all users are revoked. Run the tests each time for the various supported media types.
     *
     * @return
     */
    @Unroll()
    def "Domain Enforcement golden path for #callerUserType: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        //crete user admin and auth
        org.openstack.docs.identity.api.v2.User userAdmin = createUserAdmin()
        String userAdminToken = authenticate(userAdmin.username)

        org.openstack.docs.identity.api.v2.User userManager = createDefaultUser(userAdminToken)
        utils.addRoleToUser(userManager, Constants.USER_MANAGE_ROLE_ID, userAdminToken)
        String userManagerToken = authenticate(userManager.username)

        String callerToken
        String otherUserToken
        User caller
        if (callerUserType == IdentityUserTypeEnum.USER_MANAGER) {
            callerToken = userManagerToken
            otherUserToken = userAdminToken
            caller = userManager
        } else {
            callerToken = userAdminToken
            otherUserToken = userManagerToken
            caller = userAdmin
        }

        String domainId = userAdmin.domainId

        when: "identity admin gets domain"
        Domain domain = utils.getDomain(domainId)

        then: "enforcement is null"
        domain.domainMultiFactorEnforcementLevel == null

        when: "set domain enforcement level to OPTIONAL when user not mfa enabled or set to optional"
        MultiFactorDomain settings = v2Factory.createMultiFactorDomainSettings()
        def response = cloud20.updateMultiFactorDomainSettings(callerToken, domainId, settings, requestContentMediaType, acceptMediaType)

        then: "get forbidden"
        response.status == SC_FORBIDDEN

        when: "set domain enforcement level to OPTIONAL when user not mfa enabled but is optional"
        MultiFactor userMfaSettings = v2Factory.createMultiFactorSettings(false).with {it.enabled = null; it.userMultiFactorEnforcementLevel = UserMultiFactorEnforcementLevelEnum.OPTIONAL; return it}
        utils.updateMultiFactor(callerToken, caller.id, userMfaSettings)

        response = cloud20.updateMultiFactorDomainSettings(callerToken, domainId, settings, requestContentMediaType, acceptMediaType)

        then: "domain enforcement level is changed"
        response.status == SC_NO_CONTENT
        utils.getDomain(userAdmin.getDomainId()).domainMultiFactorEnforcementLevel == DomainMultiFactorEnforcementLevelEnum.OPTIONAL

        when: "when set domain enforcement level to required"
        settings.domainMultiFactorEnforcementLevel = DomainMultiFactorEnforcementLevelEnum.REQUIRED
        response = cloud20.updateMultiFactorDomainSettings(callerToken, domainId, settings, requestContentMediaType, acceptMediaType)

        then: "domain enforcement is changed AND tokens for mfa-required users for mfa protected credential are expired"
        response.status == SC_NO_CONTENT
        utils.getDomain(userAdmin.getDomainId()).domainMultiFactorEnforcementLevel == DomainMultiFactorEnforcementLevelEnum.REQUIRED
        cloud20.getUserById(callerToken, userManager.id).status == SC_OK
        cloud20.getUserById(otherUserToken, userManager.id).status == SC_UNAUTHORIZED

        cleanup:
        deleteUserQuietly(userManager)
        deleteUserQuietly(userAdmin)

        where:
        requestContentMediaType           | acceptMediaType                     | callerUserType
//        MediaType.APPLICATION_XML_TYPE    |   MediaType.APPLICATION_XML_TYPE    | IdentityUserTypeEnum.USER_ADMIN
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_JSON_TYPE   | IdentityUserTypeEnum.USER_MANAGER
    }

    @Unroll()
    def "updateMultiFactorDomain: Not allowed when #callerType does not have MFA enabled and user mfa enforcement is not set to Optional"() {
        //crete user admin and auth
        org.openstack.docs.identity.api.v2.User userAdmin = createUserAdmin()
        String userAdminPwdToken = authenticate(userAdmin.username)

        String domainId = userAdmin.domainId

        org.openstack.docs.identity.api.v2.User userManager = createDefaultUser(userAdminPwdToken)
        utils.addRoleToUser(userManager, Constants.USER_MANAGE_ROLE_ID, userAdminPwdToken)

        User caller
        if (callerUserType == IdentityUserTypeEnum.USER_MANAGER) {
            caller = userManager
        } else {
            caller = userAdmin
        }

        String callerPwdToken = authenticate(caller.username)

        com.rackspace.idm.domain.entity.User callerEntity = userRepository.getUserById(caller.getId())
        String callerApiToken = utils.getTokenFromApiKeyAuth(callerEntity.username, callerEntity.apiKey)

        MultiFactorDomain settingsOptional = v2Factory.createMultiFactorDomainSettings()
        MultiFactorDomain settingsRequired = v2Factory.createMultiFactorDomainSettings(DomainMultiFactorEnforcementLevelEnum.REQUIRED)

        when: "When user not mfa enabled and user enforcement is null"
        def response = cloud20.updateMultiFactorDomainSettings(callerPwdToken, domainId, settingsOptional)

        then: "get forbidden"
        response.status == SC_FORBIDDEN

        when: "when enabled and user enforcement is null, can set level"
        enableMfaOnUser(callerPwdToken, caller)
        response = cloud20.updateMultiFactorDomainSettings(callerApiToken, domainId, settingsOptional)

        then: "domain enforcement level is changed"
        response.status == SC_NO_CONTENT
        utils.getDomain(userAdmin.getDomainId()).domainMultiFactorEnforcementLevel == settingsOptional.domainMultiFactorEnforcementLevel

        when: "When user not mfa enabled and user enforcement is DEFAULT"
        disableMfaOnUser(callerApiToken, caller)
        setUserMfaEnforcementLevel(callerApiToken, caller.id, UserMultiFactorEnforcementLevelEnum.DEFAULT)
        response = cloud20.updateMultiFactorDomainSettings(callerApiToken, domainId, settingsRequired)

        then: "get forbidden"
        response.status == SC_FORBIDDEN

        when: "When user not mfa enabled and user enforcement is REQUIRED"
        setUserMfaEnforcementLevel(callerApiToken, caller.id, UserMultiFactorEnforcementLevelEnum.REQUIRED)
        response = cloud20.updateMultiFactorDomainSettings(callerApiToken, domainId, settingsRequired)

        then: "get forbidden"
        response.status == SC_FORBIDDEN

        when: "When user not mfa enabled and user enforcement is OPTIONAL"
        setUserMfaEnforcementLevel(callerApiToken, caller.id, UserMultiFactorEnforcementLevelEnum.OPTIONAL)
        response = cloud20.updateMultiFactorDomainSettings(callerApiToken, domainId, settingsRequired)

        then: "domain enforcement level is changed"
        response.status == SC_NO_CONTENT
        utils.getDomain(userAdmin.getDomainId()).domainMultiFactorEnforcementLevel == settingsRequired.domainMultiFactorEnforcementLevel

        cleanup:
        deleteUserQuietly(userManager)
        deleteUserQuietly(userAdmin)

        where:
        callerUserType                      | _
        IdentityUserTypeEnum.USER_ADMIN     | _
        IdentityUserTypeEnum.USER_MANAGER   | _
    }

    def "updateMultiFactorDomainSettings - When set to required, appropriate tokens are revoked"() {
        //crete user admin with regular token
        org.openstack.docs.identity.api.v2.User userAdmin = createUserAdmin()
        String userAdminToken = authenticate(userAdmin.username)

        //create user manager pwd and API token (default enforcement)
        org.openstack.docs.identity.api.v2.User userManager = createDefaultUser(userAdminToken)
        utils.addRoleToUser(userManager, Constants.USER_MANAGE_ROLE_ID, userAdminToken)
        String userManagerPwdToken = authenticate(userManager.username)

        com.rackspace.idm.domain.entity.User callerEntity = userRepository.getUserById(userManager.getId())
        String userManagerApiToken = utils.getTokenFromApiKeyAuth(callerEntity.username, callerEntity.apiKey)

        //create default user with MFA enabled
        org.openstack.docs.identity.api.v2.User userDefault = createDefaultUser(userAdminToken)
        enableMfaOnUser(userAdminToken, userDefault)
        String defaultUserMfaToken = getMfaTokenForUser(userDefault.username)

        when: "update domain to REQUIRED"
        MultiFactor userMfaSettings = v2Factory.createMultiFactorSettings(false).with {it.enabled = null; it.userMultiFactorEnforcementLevel = UserMultiFactorEnforcementLevelEnum.OPTIONAL; return it}
        utils.updateMultiFactor(userAdminToken, userAdmin.id, userMfaSettings)
        MultiFactorDomain settingsRequired = v2Factory.createMultiFactorDomainSettings(DomainMultiFactorEnforcementLevelEnum.REQUIRED)
        cloud20.updateMultiFactorDomainSettings(userAdminToken, userAdmin.getDomainId(), settingsRequired)

        then: "mfa protected credentials on users required to use mfa that currently do not are revoked"
        cloud20.validateToken(specificationIdentityAdminToken, userAdminToken).status == SC_OK //user mfa level == OPTIONAL
        cloud20.validateToken(specificationIdentityAdminToken, userManagerPwdToken).status == SC_NOT_FOUND
        cloud20.validateToken(specificationIdentityAdminToken, userManagerApiToken).status == SC_OK // API token not protected
        cloud20.validateToken(specificationIdentityAdminToken, defaultUserMfaToken).status == SC_OK // MFA token

        cleanup:
        deleteUserQuietly(userDefault)
        deleteUserQuietly(userManager)
        deleteUserQuietly(userAdmin)
    }

    def String getMfaTokenForUser(username) {
        def response = cloud20.authenticate(username, DEFAULT_PASSWORD)
        String wwwHeader = response.getHeaders().getFirst(DefaultMultiFactorCloud20Service.HEADER_WWW_AUTHENTICATE)
        String encryptedSessionId = utils.extractSessionIdFromWwwAuthenticateHeader(wwwHeader)

        def mfaServiceResponse = new GenericMfaAuthenticationResponse(MfaAuthenticationDecision.ALLOW, MfaAuthenticationDecisionReason.ALLOW, null, null)
        Mockito.when(mockMultiFactorAuthenticationService.mock.verifyPasscodeChallenge(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(mfaServiceResponse)
        def mfaAuthResponse = cloud20.authenticateMFAWithSessionIdAndPasscode(encryptedSessionId, "1234")
        Token token = mfaAuthResponse.getEntity(AuthenticateResponse).value.token
        return token.id
    }

    def void setUserMfaEnforcementLevel(String authToken, String userId, UserMultiFactorEnforcementLevelEnum level) {
        MultiFactor userMfaSettings = v2Factory.createMultiFactorSettings(false).with {it.enabled = null; it.userMultiFactorEnforcementLevel = level; return it}
        utils.updateMultiFactor(authToken, userId, userMfaSettings)
    }

    def "Setting domain enforcement level to invalid value returns 400"() {
        org.openstack.docs.identity.api.v2.User userAdmin = createUserAdmin()
        String userAdminToken = authenticate(userAdmin.username)

        MultiFactorDomain settings = v2Factory.createMultiFactorDomainSettings()
        JSONWriterForRaxAuthMultiFactorDomain writer = new JSONWriterForRaxAuthMultiFactorDomain()
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        writer.writeTo(settings, null, null, null, null, null, out)

        JsonSlurper jsonParser = new JsonSlurper()
        def settingsAsJson = jsonParser.parseText(out.toString())
        settingsAsJson[JSONConstants.RAX_AUTH_MULTIFACTOR_DOMAIN].domainMultiFactorEnforcementLevel = "INVALID"

        JsonBuilder invalidSettings = new JsonBuilder(settingsAsJson)

        when: "set level to invalid value"

        def response = cloud20.updateMultiFactorDomainSettings(specificationIdentityAdminToken, userAdmin.id, invalidSettings.toString())

        then: "get bad request"
        response.status == SC_BAD_REQUEST

        cleanup:
        deleteUserQuietly(userAdmin)
    }

    def void enableMfaOnUser(String authToken, User user) {
        MobilePhone responsePhone = utils.addPhone(authToken, user.id)
        utils.sendVerificationCodeToPhone(authToken, user.id, responsePhone.id)
        VerificationCode constantVerificationCode = v2Factory.createVerificationCode(Constants.MFA_DEFAULT_PIN)
        utils.verifyPhone(authToken, user.id, responsePhone.id, constantVerificationCode)

        MultiFactor settings = v2Factory.createMultiFactorSettings(true)
        cloud20.updateMultiFactorSettings(authToken, user.id, settings)
    }

    def void disableMfaOnUser(String authToken, User user) {
        MultiFactor settings = v2Factory.createMultiFactorSettings(false)
        cloud20.updateMultiFactorSettings(authToken, user.id, settings)
    }

}
