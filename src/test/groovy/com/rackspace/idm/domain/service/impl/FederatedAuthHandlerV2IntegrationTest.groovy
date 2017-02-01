package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.dao.IdentityProviderDao
import com.rackspace.idm.domain.decorator.SAMLAuthContext
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.domain.entity.FederatedBaseUser
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.SamlAuthResponse
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.federation.v2.FederatedAuthHandlerV2
import com.rackspace.idm.domain.service.federation.v2.FederatedDomainRequestHandler
import com.rackspace.idm.domain.service.federation.v2.FederatedRackerRequestHandler
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ForbiddenException
import org.apache.commons.lang.RandomStringUtils
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.opensaml.saml.saml2.core.Response
import org.opensaml.saml.saml2.core.impl.ResponseBuilder
import org.opensaml.security.credential.Credential
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Shared
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.v2.FederatedDomainAuthGenerationRequest
import testHelpers.saml.v2.FederatedDomainAuthRequestGenerator

import java.security.cert.X509Certificate

import static org.apache.http.HttpStatus.SC_CREATED
import static org.apache.http.HttpStatus.SC_OK

/**
 * Tests the various error/validity checks performed on an incoming SAML Response for a fed request.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FederatedAuthHandlerV2IntegrationTest extends RootIntegrationTest {

    private static final Logger LOG = Logger.getLogger(FederatedAuthHandlerV2IntegrationTest.class)

    @Autowired
    FederatedAuthHandlerV2 federatedAuthHandlerV2

    @Autowired
    IdentityProviderDao ldapIdentityProviderRepository

    @Autowired
    UserService userService

    FederatedBaseUser fUser = new FederatedUser()
    FederatedBaseUser fRacker = new Racker()

    @Shared IdentityProvider sharedBrokerIdp;
    @Shared Credential sharedBrokerIdpCredential;

    @Shared IdentityProvider sharedOriginIdp;
    @Shared Credential sharedOriginIdpCredential;

    @Shared String serviceAdminToken;

    def setupSpec() {
        def response = cloud20.authenticatePassword(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        assert (response.status == SC_OK)
        serviceAdminToken = response.getEntity(AuthenticateResponse).value.token.id

        sharedBrokerIdpCredential = SamlCredentialUtils.generateX509Credential();
        sharedOriginIdpCredential = SamlCredentialUtils.generateX509Credential();

        sharedBrokerIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.BROKER, sharedBrokerIdpCredential)
        sharedOriginIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.DOMAIN, sharedOriginIdpCredential)
    }

    def cleanupSpec() {
        cloud20.deleteIdentityProvider(serviceAdminToken, sharedBrokerIdp.id)
        cloud20.deleteIdentityProvider(serviceAdminToken, sharedOriginIdp.id)
    }

    def setup() {
        federatedAuthHandlerV2.federatedDomainRequestHandler = Mock(FederatedDomainRequestHandler)
        federatedAuthHandlerV2.federatedRackerRequestHandler = Mock(FederatedRackerRequestHandler)

        federatedAuthHandlerV2.federatedDomainRequestHandler.processAuthRequest(_) >> new SamlAuthResponse(fUser, null, null, null)
        federatedAuthHandlerV2.federatedDomainRequestHandler.processAuthRequest(_) >> new SamlAuthResponse(fRacker, null, null, null)
    }

    /**
     * Tests the golden case where all values are provided appropriately, IDP exists in correct state in LDAP, etc
     * @return
     */
    def "Valid Request"() {
        given:
        FederatedDomainAuthRequestGenerator generator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)
        FederatedDomainAuthGenerationRequest req = createValidFedRequest()
        def samlResponse = generator.createSignedSAMLResponse(req);

        when:
        def samlAuthResponse = federatedAuthHandlerV2.authenticate(samlResponse)

        then:
        samlAuthResponse.user == fUser
    }

    def "Error when missing response issuer"() {
        given:
        ResponseBuilder responseBuilder = new ResponseBuilder();
        Response samlResponse = responseBuilder.buildObject();

        when:
        federatedAuthHandlerV2.authenticate(samlResponse)

        then:
        def ex = thrown(BadRequestException)
        ex.errorCode == ErrorCodes.ERROR_CODE_FEDERATION2_MISSING_RESPONSE_ISSUER
    }

    def "Error when missing Response Signature"() {
        given:
        FederatedDomainAuthRequestGenerator generator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)

        FederatedDomainAuthGenerationRequest req = createValidFedRequest()
        def samlResponse = generator.createSignedSAMLResponse(req);

        //remove signature from Response
        samlResponse.setSignature(null)

        when:
        federatedAuthHandlerV2.authenticate(samlResponse)

        then:
        def ex = thrown(BadRequestException)
        ex.errorCode == ErrorCodes.ERROR_CODE_FEDERATION2_MISSING_RESPONSE_SIGNATURE
    }

    def "Error when don't have any assertions"() {
        given:
        FederatedDomainAuthRequestGenerator generator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)

        FederatedDomainAuthGenerationRequest req = createValidFedRequest()
        def samlResponse = generator.createSignedSAMLResponse(req);

        //there are 2 assertions so remove them both
        samlResponse.getAssertions().remove(samlResponse.getAssertions().get(0))
        samlResponse.getAssertions().remove(samlResponse.getAssertions().get(0))

        when:
        federatedAuthHandlerV2.authenticate(samlResponse)

        then:
        def ex = thrown(BadRequestException)
        ex.errorCode == ErrorCodes.ERROR_CODE_FEDERATION2_MISSING_BROKER_ASSERTION
    }

    def "Error when missing broker assertion issuer"() {
        given:
        FederatedDomainAuthRequestGenerator generator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)

        FederatedDomainAuthGenerationRequest req = createValidFedRequest()
        def samlResponse = generator.createSignedSAMLResponse(req);

        //remove issuer from broker assertion
        samlResponse.getAssertions().get(0).setIssuer(null)

        when:
        federatedAuthHandlerV2.authenticate(samlResponse)

        then:
        def ex = thrown(BadRequestException)
        ex.errorCode == ErrorCodes.ERROR_CODE_FEDERATION2_MISSING_BROKER_ISSUER
    }

    def "Error when missing Origin Assertion Signature"() {
        given:
        FederatedDomainAuthRequestGenerator generator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)

        FederatedDomainAuthGenerationRequest req = createValidFedRequest()
        def samlResponse = generator.createSignedSAMLResponse(req);

        //remove signature from origin assertion
        samlResponse.getAssertions().get(1).setSignature(null)

        when:
        federatedAuthHandlerV2.authenticate(samlResponse)

        then:
        def ex = thrown(BadRequestException)
        ex.errorCode == ErrorCodes.ERROR_CODE_FEDERATION2_MISSING_ORIGIN_ASSERTION_SIGNATURE
    }

    /**
     * In a Response, the 1st assertion is always considered the "broker" assertion, while the 2nd assertion is
     * always considered the "origin" assertion. So if there is only 1 assertion, it means there is a broker assertion,
     * but not an origin assertion.
     *
     * @return
     */
    def "Error when missing origin assertion"() {
        given:
        FederatedDomainAuthRequestGenerator generator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)
        FederatedDomainAuthGenerationRequest req = createValidFedRequest()
        def samlResponse = generator.createSignedSAMLResponse(req);

        /*
        remove origin assertion. The concrete class returned by getAssertions does not support removing by index so need
        to remove object explicitly
         */

        samlResponse.getAssertions().remove(samlResponse.getAssertions().get(1))

        when:
        federatedAuthHandlerV2.authenticate(samlResponse)

        then:
        def ex = thrown(BadRequestException)
        ex.errorCode == ErrorCodes.ERROR_CODE_FEDERATION2_MISSING_ORIGIN_ASSERTION
    }

    def "Error when response issuer is not a broker IDP"() {
        given:
        FederatedDomainAuthRequestGenerator generator = new FederatedDomainAuthRequestGenerator(sharedOriginIdpCredential, sharedBrokerIdpCredential)
        FederatedDomainAuthGenerationRequest req = createValidFedRequest().with {
            it.brokerIssuer = sharedOriginIdp.issuer
            it.originIssuer = sharedBrokerIdp.issuer
            it
        }
        def samlResponse = generator.createSignedSAMLResponse(req);

        when:
        federatedAuthHandlerV2.authenticate(samlResponse)

        then:
        def ex = thrown(ForbiddenException)
        ex.errorCode == ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_BROKER_ISSUER
    }

    def "Error when broker assertion issuer is not same as response issuer"() {
        given:
        FederatedDomainAuthRequestGenerator generator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)
        FederatedDomainAuthGenerationRequest req = createValidFedRequest()
        def samlResponse = generator.createSignedSAMLResponse(req);

        /*
         Hack the returned response to change the assertion issuer since the generator sets both response and broker assertion
         to same value from request
          */
        samlResponse.getAssertions().get(0).setIssuer(generator.createIssuer(sharedOriginIdp.getIssuer()))

        when:
        federatedAuthHandlerV2.authenticate(samlResponse)

        then:
        def ex = thrown(BadRequestException)
        ex.errorCode == ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_BROKER_ASSERTION
    }

    def "Error when broker and origin issuers are the same"() {
        given:
        FederatedDomainAuthRequestGenerator generator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)
        FederatedDomainAuthGenerationRequest req = createValidFedRequest().with {
            it.originIssuer = it.brokerIssuer
            it
        }
        def samlResponse = generator.createSignedSAMLResponse(req);

        when:
        federatedAuthHandlerV2.authenticate(samlResponse)

        then:
        def ex = thrown(BadRequestException)
        ex.errorCode == ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_ORIGIN_ASSERTION
    }

    def "Error when origin IDP is a broker other than the response broker"() {
        given:
        def brokerIdpCredential = SamlCredentialUtils.generateX509Credential();
        def brokerIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.BROKER, brokerIdpCredential)

        FederatedDomainAuthRequestGenerator generator = new FederatedDomainAuthRequestGenerator(sharedBrokerIdpCredential, brokerIdpCredential)
        FederatedDomainAuthGenerationRequest req = createValidFedRequest().with {
            it.originIssuer = brokerIdp.issuer
            it
        }
        def samlResponse = generator.createSignedSAMLResponse(req);

        when:
        federatedAuthHandlerV2.authenticate(samlResponse)

        then:
        def ex = thrown(ForbiddenException)
        ex.errorCode == ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_ORIGIN_ISSUER

        cleanup:
        cloud20.deleteIdentityProvider(serviceAdminToken, sharedBrokerIdp.id)
    }

    def createIdp(IdentityProviderFederationTypeEnum type, Credential cred) {
        List<X509Certificate> certs = [cred.entityCertificate]
        com.rackspace.idm.domain.entity.IdentityProvider provider = entityFactory.createIdentityProviderWithCertificates(certs).with {
            it.federationType = type.value()
            it
        }
        ldapIdentityProviderRepository.addIdentityProvider(provider)

        return provider
    }

    def createIdpViaRest(IdentityProviderFederationTypeEnum type, Credential cred) {
        def response = cloud20.createIdentityProviderWithCred(serviceAdminToken, type, cred)
        assert (response.status == SC_CREATED)
        return response.getEntity(IdentityProvider)
    }

    def createValidFedRequest() {
        new FederatedDomainAuthGenerationRequest().with {
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it.validitySeconds = 100
            it.brokerIssuer = sharedBrokerIdp.issuer
            it.originIssuer = sharedOriginIdp.issuer
            it.email = Constants.DEFAULT_FED_EMAIL
            it.requestIssueInstant = new DateTime()
            it.samlAuthContext = SAMLAuthContext.PASSWORD
            it.username = UUID.randomUUID()
            it.roleNames = ["admin", "observer"] as Set
            it
        }
    }
}
