package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerifyPhonePinResult
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.commons.collections4.CollectionUtils
import org.apache.http.HttpStatus
import org.apache.log4j.Logger
import org.opensaml.security.credential.Credential
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.SamlFactory
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import javax.mail.internet.MimeMessage
import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.SC_CREATED
import static org.apache.http.HttpStatus.SC_OK

class VerifyPhonePinForFedUserIntegrationTest extends RootIntegrationTest {

    private static final Logger LOG = Logger.getLogger(FederatedUserWithPhonePinIntegrationTest.class)

    @Autowired
    FederatedUserDao federatedUserRepository

    @Autowired
    IdentityUserService identityUserService

    @Shared
    String sharedServiceAdminToken
    @Shared
    String sharedIdentityAdminToken
    @Shared
    IdentityProvider sharedBrokerIdp
    @Shared
    Credential sharedBrokerIdpCredential
    @Shared
    IdentityProvider sharedOriginIdp
    @Shared
    Credential sharedOriginIdpCredential
    @Shared
    User sharedUserAdmin
    @Shared
    FederatedDomainAuthRequestGenerator sharedFederatedDomainAuthRequestGenerator

    def setupSpec() {
        sharedServiceAdminToken = cloud20.authenticateForToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        sharedIdentityAdminToken = cloud20.authenticateForToken(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)

        sharedBrokerIdpCredential = SamlCredentialUtils.generateX509Credential();
        sharedOriginIdpCredential = SamlCredentialUtils.generateX509Credential();
        sharedBrokerIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.BROKER, sharedBrokerIdpCredential)
        sharedOriginIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.DOMAIN, sharedOriginIdpCredential)

        sharedFederatedDomainAuthRequestGenerator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)
        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)
    }

    def createIdpViaRest(IdentityProviderFederationTypeEnum type, Credential cred) {
        def response = cloud20.createIdentityProviderWithCred(sharedServiceAdminToken, type, cred)
        assert (response.status == SC_CREATED)
        return response.getEntity(IdentityProvider)
    }

    def setup() {
        reloadableConfiguration.reset()
    }

    void doCleanupSpec() {
        cloud20.deleteIdentityProvider(sharedServiceAdminToken, sharedBrokerIdp.id)
        cloud20.deleteIdentityProvider(sharedServiceAdminToken, sharedOriginIdp.id)
        cloud20.deleteUser(sharedServiceAdminToken, sharedUserAdmin.id)
    }

    @Unroll
    def "SAML assertion 2.0 - Verify phone pin for a federated user; media = #accept"() {
        given:
        def fedRequest = utils.createFedRequest(sharedUserAdmin, sharedBrokerIdp.issuer, sharedOriginIdp.issuer)
        def samlResponse = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlResponse))
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value
        def fedUserId = authResponse.user.id
        FederatedUser fedUser = federatedUserRepository.getUserById(fedUserId)

        then:
        assert authClientResponse.status == SC_OK

        when: "verify phone pin with default identityAdminToken that has got identity:phone-pin-admin added to it"
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = fedUser.phonePin
            it
        }
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), fedUserId, phonePin)

        then:
        assert response.status == SC_OK

        when: "verify phone pin with SAML auth token"
        response = cloud20.verifyPhonePin(authResponse.token.id, fedUserId, phonePin)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "verify phone pin with cloud userAdmin auth token"
        def userAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)
        response = cloud20.verifyPhonePin(utils.getToken(userAdmin.username), fedUserId, phonePin)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "verify phone pin with cloud userAdmin auth token that has got identity:phone-pin-admin added to it"
        utils.addRoleToUser(userAdmin, Constants.IDENTITY_PHONE_PIN_ADMIN_ROLE_ID)
        response = cloud20.verifyPhonePin(utils.getToken(userAdmin.username), fedUserId, phonePin)

        then:
        assert response.status == SC_OK

        when: "verify phone pin with empty phone pin"
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin emptyPhonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = ""
            it
        }
        response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), fedUserId, emptyPhonePin)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, "Error code: 'PP-001'; Must supply a Phone PIN.")

        when: "verify phone pin with incorrect phone pin"
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin incorrectPhonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = "12345433"
            it
        }
        response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), fedUserId, incorrectPhonePin)

        then: "Returns 200 response"
        response.status == SC_OK

        and: "Response is appropriate"
        VerifyPhonePinResult result = response.getEntity(VerifyPhonePinResult)
        !result.authenticated
        result.failureCode == "PP-003"
        result.failureMessage == "Incorrect Phone PIN."

        cleanup:
        utils.deleteFederatedUserQuietly(fedRequest.username)

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "Locking a phone pin sends an email only when the phone pin becomes locked" () {
        given:
        def userAdmin = utils.createGenericUserAdmin()
        def fedUserId = utils.authenticateFederatedUser(userAdmin.domainId).user.id
        def fedUserEntity = identityUserService.getFederatedUserById(fedUserId)
        def pin = fedUserEntity.phonePin
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = pin + "a"
            it
        }

        when: "Verify the phone pin w/ an invalid pin"
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), fedUserEntity.id, phonePin)

        then: "Returns 200 response"
        response.status == SC_OK
        VerifyPhonePinResult result = response.getEntity(VerifyPhonePinResult)
        !result.authenticated
        CollectionUtils.isEmpty(wiserWrapper.wiserServer.getMessages())

        when: "Verify the phone pin and lock the pin"
        fedUserEntity = identityUserService.getFederatedUserById(fedUserId)
        (GlobalConstants.PHONE_PIN_AUTHENTICATION_FAILURE_LOCKING_THRESHOLD - 2).times {
            fedUserEntity.recordFailedPinAuthentication()
        }
        identityUserService.updateEndUser(fedUserEntity)
        response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), fedUserEntity.id, phonePin)

        then: "The phone pin locked email was sent"
        response.status == SC_OK
        VerifyPhonePinResult result2 = response.getEntity(VerifyPhonePinResult)
        !result2.authenticated
        CollectionUtils.isNotEmpty(wiserWrapper.wiserServer.getMessages())
        MimeMessage message = wiserWrapper.wiserServer.getMessages().get(0).getMimeMessage()
        message.getFrom().length == 1
        message.getFrom()[0].toString() == Constants.PHONE_PIN_LOCKED_EMAIL_FROM
        message.getSubject() == Constants.PHONE_PIN_LOCKED_EMAIL_SUBJECT

        when: "Verify the phone pin again now that it is locked"
        wiserWrapper.getWiser().getMessages().clear()
        response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), fedUserEntity.id, phonePin)

        then: "No emails were sent"
        response.status == SC_OK
        CollectionUtils.isEmpty(wiserWrapper.wiserServer.getMessages())

        cleanup:
        utils.deleteUserQuietly(userAdmin)
    }

}