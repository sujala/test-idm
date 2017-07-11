package com.rackspace.idm.domain.service.federation.v2

import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.config.IdentityConfigHolder
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.exception.BadRequestException
import org.apache.commons.lang3.RandomStringUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.joda.time.DateTime
import org.opensaml.core.config.InitializationService
import org.opensaml.security.credential.Credential
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import java.security.Security
import java.util.regex.Pattern

import static com.rackspace.idm.Constants.DEFAULT_FED_EMAIL

class FederatedDomainAuthRequestTest extends Specification
{
    @Shared IdentityConfig identityConfig
    @Shared IdentityConfig.StaticConfig staticConfig
    @Shared IdentityConfig.ReloadableConfig reloadableConfig

    @Shared FederatedDomainAuthRequestGenerator sharedDomainRequestGenerator
    @Shared Credential sharedBrokerIdpCredential
    @Shared Credential sharedOriginIdpCredential

    @Shared IdentityConfig oldIdentityConfig

    def setupSpec() {
        Security.addProvider(new BouncyCastleProvider())
        InitializationService.initialize()
        sharedBrokerIdpCredential = SamlCredentialUtils.generateX509Credential()
        sharedOriginIdpCredential = SamlCredentialUtils.generateX509Credential()
        sharedDomainRequestGenerator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)

        oldIdentityConfig = IdentityConfigHolder.IDENTITY_CONFIG;
    }

    void cleanupSpec() {
        /**
         * This is a complete hack. THe IdentityConfigHolder is a hack in and of itself to allow non spring injected beans
         * access to the spring loaded IdentityConfig bean into a static context. However, this test uses this class which
         * requires the IdentifyConfig set so injects it with Mock. However, if Spring context was loaded for an integration
         * test prior to running this component test then the spring loaded config will be replaced with a mock and never
         * replaced. So this test needs to reset the IdentifyConfigHolder to the old value.
         */
        IdentityConfigHolder.IDENTITY_CONFIG = oldIdentityConfig
    }

    void setup() {
        identityConfig = Mock()
        staticConfig = Mock()
        reloadableConfig = Mock()
        identityConfig.getStaticConfig() >> staticConfig
        identityConfig.getReloadableConfig() >> reloadableConfig
        IdentityConfigHolder.IDENTITY_CONFIG = identityConfig
        reloadableConfig.shouldV2FederationValidateOriginIssueInstant() >> true
    }

    def "Can process a valid racker request"() {
        given:
        FederatedDomainAuthGenerationRequest genRequest = createValidFederatedDomainRequest()
        def samlResponse = sharedDomainRequestGenerator.createSignedSAMLResponse(genRequest)

        when: "process valid request"
        FederatedDomainAuthRequest req = new FederatedDomainAuthRequest(samlResponse)

        then: "values copied from saml as expected"
        req.originIssuer == genRequest.originIssuer
        req.brokerIssuer == genRequest.brokerIssuer
        req.username == genRequest.username
        req.authenticatedByForRequest == AuthenticatedByMethodEnum.PASSWORD

        /*
         Date is equal to or before the requested expiration (need 'before' and 'equal' test to avoid race condition since
         a clock tick may or may not have elapsed between time generated request was originally created, saml response created, and then
         when racker request was finally created.
         */
        req.requestedTokenExpiration.equals(new DateTime().plusSeconds(genRequest.validitySeconds)) || req.requestedTokenExpiration.isBefore(new DateTime().plusSeconds(genRequest.validitySeconds))
        req.getWrappedSamlResponse().samlResponse == samlResponse
    }

    @Unroll
    def "AuthContextClassRef other than mapped results in OTHER auth by. Tested: #ref"() {
        given:
        FederatedDomainAuthGenerationRequest genRequest = createValidFederatedDomainRequest().with {
            it.authContextRefClass = ref
            it
        }
        def samlResponse = sharedDomainRequestGenerator.createSignedSAMLResponse(genRequest)

        when:
        FederatedDomainAuthRequest req = new FederatedDomainAuthRequest(samlResponse)

        then: "error"
        req.authenticatedByForRequest == AuthenticatedByMethodEnum.OTHER

        where:
        ref | _
        null | _
        "" | _
        RandomStringUtils.randomAlphanumeric(10) | _
    }

    def "Missing AuthContextClassRef results in OTHER auth by"() {
        given:
        FederatedDomainAuthGenerationRequest genRequest = createValidFederatedDomainRequest()
        def unSignedSamlResponse = sharedDomainRequestGenerator.createUnsignedSAMLResponse(genRequest)
        unSignedSamlResponse.getAssertions().get(0).getAuthnStatements().get(0).getAuthnContext().setAuthnContextClassRef(null)
        def samlResponse = sharedDomainRequestGenerator.signSAMLResponse(unSignedSamlResponse, sharedBrokerIdpCredential)

        when:
        FederatedDomainAuthRequest req = new FederatedDomainAuthRequest(samlResponse)

        then: "error"
        req.authenticatedByForRequest == AuthenticatedByMethodEnum.OTHER
    }

    def "Expiration time must be in future"() {
        given:
        FederatedDomainAuthGenerationRequest genRequest = createValidFederatedDomainRequest().with {
            it.validitySeconds = -1
            it
        }
        def samlResponse = sharedDomainRequestGenerator.createSignedSAMLResponse(genRequest)

        when:
        FederatedDomainAuthRequest req = new FederatedDomainAuthRequest(samlResponse)

        then: "error"
        BadRequestException ex = thrown()
        IdmExceptionAssert.assertException(ex, BadRequestException, ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_REQUESTED_TOKEN_EXP, Pattern.compile("Error code: 'FED2-013'; Token expiration date must be in future"))
    }

    def "Issuer must be provided"() {
        given:
        FederatedDomainAuthGenerationRequest genRequest = createValidFederatedDomainRequest().with {
            it.brokerIssuer = null // Will generate the response issuer to be null
            it
        }
        def samlResponse = sharedDomainRequestGenerator.createSignedSAMLResponse(genRequest)

        when:
        FederatedDomainAuthRequest req = new FederatedDomainAuthRequest(samlResponse)

        then: "error"
        BadRequestException ex = thrown()
        IdmExceptionAssert.assertException(ex, BadRequestException, ErrorCodes.ERROR_CODE_FEDERATION2_MISSING_RESPONSE_ISSUER, Pattern.compile("Error code: 'FED2-000'; Issuer is a required field"))
    }

    def "Origin Issuer must be provided"() {
        given:
        FederatedDomainAuthGenerationRequest genRequest = createValidFederatedDomainRequest().with {
            it.originIssuer = null
            it
        }
        def samlResponse = sharedDomainRequestGenerator.createSignedSAMLResponse(genRequest)

        when:
        FederatedDomainAuthRequest req = new FederatedDomainAuthRequest(samlResponse)

        then: "error"
        BadRequestException ex = thrown()
        IdmExceptionAssert.assertException(ex, BadRequestException, ErrorCodes.ERROR_CODE_FEDERATION2_MISSING_ORIGIN_ISSUER, Pattern.compile("Error code: 'FED2-002'; The origin assertion must specify an issuer"))
    }

    def createValidFederatedDomainRequest(username = UUID.randomUUID()) {

        new FederatedDomainAuthGenerationRequest().with {
            it.domainId = RandomStringUtils.randomAlphanumeric(10)
            it.validitySeconds = 100
            it.brokerIssuer = RandomStringUtils.randomAlphanumeric(16)
            it.originIssuer = RandomStringUtils.randomAlphanumeric(16)
            it.email = DEFAULT_FED_EMAIL
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass = SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = username
            it.roleNames = [] as Set
            it
        }
    }

}
