package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.entity.FederatedUser
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.opensaml.security.credential.Credential
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.SamlFactory
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import javax.servlet.http.HttpServletResponse

import static org.apache.http.HttpStatus.SC_CREATED

class FederatedUserWithPhonePinIntegrationTest extends RootIntegrationTest {

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
    def "SAML assertion 2.0 - Create a federated user with phone PIN - featureEnabled == #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, featureEnabled)

        def fedRequest = createFedRequest()
        def samlResponse = sharedFederatedDomainAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedFederatedDomainAuthRequestGenerator.convertResponseToString(samlResponse))

        then:
        assert authClientResponse.status == HttpServletResponse.SC_OK

        when:
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value
        FederatedUser fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then:
        assert fedUser.id == authResponse.user.id
        assert fedUser.username == fedRequest.username
        assert fedUser.domainId == fedRequest.domainId

        if (featureEnabled) {
            assert fedUser.phonePin != null
            assert fedUser.encryptedPhonePin != null
            assert fedUser.salt != null
            assert fedUser.phonePin.size() == GlobalConstants.PHONE_PIN_SIZE
            assert fedUser.phonePin.isNumber()
        } else {
            assert fedUser.phonePin == null
        }

        cleanup:
        try {
            deleteFederatedUserQuietly(fedRequest.username)
        } catch (Exception ex) {
            // Eat
        }

        where:
        featureEnabled << [true, false]
    }

    @Unroll
    def "SAML assertion 1.0 - Create a federated user with phone PIN - featureEnabled == #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PHONE_PIN_ON_USER_PROP, featureEnabled)

        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "test@rackspace.com"

        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, sharedUserAdmin.domainId, null, email)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse.status == HttpServletResponse.SC_OK

        when:
        def authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def fedUser = federatedUserRepository.getUserById(authResponse.user.id)

        then:

        if (featureEnabled) {
            assert fedUser.phonePin != null
            assert fedUser.encryptedPhonePin != null
            assert fedUser.salt != null
            assert fedUser.phonePin.size() == GlobalConstants.PHONE_PIN_SIZE
            assert fedUser.phonePin.isNumber()
        } else {
            assert fedUser.phonePin == null
        }

        cleanup:
        try {
            deleteFederatedUserQuietly(username)
        } catch (Exception ex) {
            // Eat
        }

        where:
        featureEnabled << [true, false]
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
