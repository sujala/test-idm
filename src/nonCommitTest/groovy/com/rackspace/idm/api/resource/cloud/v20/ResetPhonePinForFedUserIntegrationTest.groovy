package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePin
import com.rackspace.idm.Constants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.dao.FederatedUserDao
import org.apache.http.HttpStatus
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.opensaml.security.credential.Credential
import org.openstack.docs.identity.api.v2.AuthenticateResponse
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

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.getRACKER_IMPERSONATE
import static com.rackspace.idm.Constants.getRACKER_IMPERSONATE_PASSWORD
import static org.apache.http.HttpStatus.SC_CREATED
import static org.apache.http.HttpStatus.SC_OK

class ResetPhonePinForFedUserIntegrationTest extends RootIntegrationTest {

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
    def "SAML assertion 2.0 - Get phone pin for a federated user; media = #accept"() {
        given:
        def fedRequest = createFedRequest()
        def samlResponse = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlResponse))
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value
        def fedUserId = authResponse.user.id
        def fedUserToken = authResponse.token.id

        then:
        assert authClientResponse.status == SC_OK

        when: "Service admin token cannot reset the phone pin"
        def response = cloud20.resetPhonePin(utils.getServiceAdminToken(), fedUserId)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        when: "Identity admin token can reset the phone pin"
        def pin1 = utils.getPhonePin(fedUserId, fedUserToken).pin
        response = cloud20.resetPhonePin(utils.getIdentityAdminToken(), fedUserId)
        def pinFromResponse = response.getEntity(PhonePin).pin
        def pin2 = utils.getPhonePin(fedUserId, fedUserToken).pin

        then: "phone pin before and after reset are different"
        assert response.status == SC_OK
        assert pinFromResponse != pin1
        assert pinFromResponse == pin2

        when: "Federated user token can reset the phone pin"
        response = cloud20.resetPhonePin(fedUserToken, fedUserId)
        pinFromResponse = response.getEntity(PhonePin).pin
        pin1 = utils.getPhonePin(fedUserId, fedUserToken).pin

        then: "phone pin before and after reset are different"
        assert response.status == SC_OK
        assert pinFromResponse != pin2
        assert pinFromResponse == pin1

        when: "Racker impersonated token cannot reset the phone pin"
        def rackerToken = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD).token.id
        def federatedUser = utils.getUserById(authResponse.user.id)
        def impersonationResponse = cloud20.impersonate(rackerToken, federatedUser)
        def impersonatedToken = impersonationResponse.getEntity(ImpersonationResponse).token.id

        response = cloud20.resetPhonePin(impersonatedToken, fedUserId)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Impersonation tokens cannot be used to reset the phone PIN.")

        when: "User admin of the federated user cannot reset the phone pin"
        response = cloud20.resetPhonePin(utils.getToken(sharedUserAdmin.username), fedUserId)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        cleanup:
        try {
            deleteFederatedUserQuietly(fedRequest.username)
        } catch (Exception ex) {
            // Eat
        }

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "SAML assertion 1.0 - Get phone pin for a federated user; media = #accept"() {
        given:
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, Constants.DEFAULT_SAML_EXP_SECS, sharedUserAdmin.domainId, null, "test@rackspace.com")

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def fedUserId = authResponse.user.id
        def fedUserToken = authResponse.token.id

        then:
        assert samlResponse.status == SC_OK

        when: "Service admin token able to reset the pin for federated user"
        def response = cloud20.resetPhonePin(utils.getServiceAdminToken(), fedUserId)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

        when: "Identity admin token can reset the phone pin"
        def pin1 = utils.getPhonePin(fedUserId, fedUserToken).pin
        response = cloud20.resetPhonePin(utils.getIdentityAdminToken(), fedUserId)
        def pinFromResponse = response.getEntity(PhonePin).pin
        def pin2 = utils.getPhonePin(fedUserId, fedUserToken).pin

        then: "phone pin before and after reset are different"
        assert response.status == SC_OK
        assert pinFromResponse != pin1
        assert pinFromResponse == pin2

        when: "Federated user token can reset the phone pin"
        response = cloud20.resetPhonePin(fedUserToken, fedUserId)
        pinFromResponse = response.getEntity(PhonePin).pin
        pin1 = utils.getPhonePin(fedUserId, fedUserToken).pin

        then: "phone pin before and after reset are different"
        assert response.status == SC_OK
        assert pinFromResponse != pin2
        assert pinFromResponse == pin1

        when: "Racker impersonated token cannot be used to reset the phone pin"
        def rackerToken = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_IMPERSONATE_PASSWORD).token.id
        def federatedUser = utils.getUserById(authResponse.user.id)
        def impersonationResponse = cloud20.impersonate(rackerToken, federatedUser)
        def impersonatedToken = impersonationResponse.getEntity(ImpersonationResponse).token.id

        response = cloud20.resetPhonePin(impersonatedToken, fedUserId)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Impersonation tokens cannot be used to reset the phone PIN.")

        when: "User admin of the federated user cannot reset the phone pin"
        response = cloud20.resetPhonePin(utils.getToken(sharedUserAdmin.username), fedUserId)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Error code: 'PP-002'; Not Authorized")

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
