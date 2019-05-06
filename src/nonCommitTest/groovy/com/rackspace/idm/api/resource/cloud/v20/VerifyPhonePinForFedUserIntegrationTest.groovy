package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.entity.FederatedUser
import org.apache.http.HttpStatus
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.opensaml.security.credential.Credential
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.SamlFactory
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.*

class VerifyPhonePinForFedUserIntegrationTest extends RootIntegrationTest {

    private static final Logger LOG = Logger.getLogger(FederatedUserWithPhonePinIntegrationTest.class)

    @Autowired
    FederatedUserDao federatedUserRepository

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
        def fedRequest = createFedRequest()
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
        assert response.status == SC_NO_CONTENT

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
        assert response.status == SC_NO_CONTENT

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

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, "Error code: 'PP-001'; Incorrect Phone PIN.")

        cleanup:
        try {
            deleteFederatedUserQuietly(fedRequest.username)
            utils.deleteUser(userAdmin)
        } catch (Exception ex) {
            // Eat
        }

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "SAML assertion 1.0 - Verify phone pin for a federated user; media = #accept"() {
        given:
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, Constants.DEFAULT_SAML_EXP_SECS, sharedUserAdmin.domainId, null, "test@rackspace.com")

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def fedUserId = authResponse.user.id
        FederatedUser fedUser = federatedUserRepository.getUserById(fedUserId)

        then:
        assert samlResponse.status == SC_OK

        when: "verify phone pin with default identityAdminToken that has got identity:phone-pin-admin added to it"
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin phonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = fedUser.phonePin
            it
        }
        def response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), fedUserId, phonePin)

        then:
        assert response.status == SC_NO_CONTENT

        when: "verify phone pin with SAML auth token"
        response = cloud20.verifyPhonePin(authResponse.token.id, fedUserId, phonePin)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "verify phone pin with incorrect phone pin"
        com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin incorrectPhonePin = new com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin().with {
            it.pin = "12345433"
            it
        }
        response = cloud20.verifyPhonePin(utils.getIdentityAdminToken(), fedUserId, incorrectPhonePin)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, "Error code: 'PP-001'; Incorrect Phone PIN.")

        cleanup:
        try {
            deleteFederatedUserQuietly(username)
        } catch (Exception ex) {
            // Eat
        }

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def deleteFederatedUserQuietly(username) {
        try {
            def federatedUser = federatedUserRepository.getUserByUsernameForIdentityProviderId(username, Constants.DEFAULT_IDP_ID)
            if (federatedUser != null) {
                federatedUserRepository.deleteObject(federatedUser)
            }
        } catch (Exception e) {
            // Eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", username), e)
        }
    }

    def createFedRequest(userAdmin = sharedUserAdmin) {
        new FederatedDomainAuthGenerationRequest().with {
            it.domainId = userAdmin.domainId
            it.validitySeconds = 100
            it.brokerIssuer = sharedBrokerIdp.issuer
            it.originIssuer = sharedOriginIdp.issuer
            it.email = Constants.DEFAULT_FED_EMAIL
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass =  SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = UUID.randomUUID().toString()
            it.roleNames = [] as Set
            it
        }
    }
}