package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.SAMLConstants
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import org.apache.commons.lang.RandomStringUtils
import org.apache.http.HttpStatus
import org.apache.log4j.Logger
import org.joda.time.DateTime
import org.opensaml.security.credential.Credential
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import spock.lang.Shared
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.v2.FederatedRackerAuthGenerationRequest
import testHelpers.saml.v2.FederatedRackerAuthRequestGenerator

import javax.servlet.http.HttpServletResponse

import static com.rackspace.idm.Constants.*
import static org.apache.http.HttpStatus.SC_CREATED

class FederatedRackerV2UserRestIntegrationTest extends RootIntegrationTest {

    private static final Logger LOG = Logger.getLogger(FederatedRackerV2UserRestIntegrationTest.class)

    @Shared String sharedServiceAdminToken
    @Shared String sharedIdentityAdminToken

    /**
     * An identity provider created for this class. No code should modify this provider.
     */
    @Shared IdentityProvider sharedBrokerIdp
    @Shared Credential sharedBrokerIdpCredential
    @Shared IdentityProvider sharedOriginIdp
    @Shared Credential sharedOriginIdpCredential

    @Shared FederatedRackerAuthRequestGenerator sharedRackerAuthRequestGenerator

    @Shared AuthenticatedByMethodGroup fedAndPasswordGroup = AuthenticatedByMethodGroup.getGroup(AuthenticatedByMethodEnum.FEDERATION, AuthenticatedByMethodEnum.PASSWORD)
    @Shared AuthenticatedByMethodGroup fedAndRsaKeyGroup = AuthenticatedByMethodGroup.getGroup(AuthenticatedByMethodEnum.FEDERATION, AuthenticatedByMethodEnum.RSAKEY)

    def setupSpec() {
        sharedServiceAdminToken = cloud20.authenticateForToken(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PASSWORD)
        sharedIdentityAdminToken = cloud20.authenticateForToken(IDENTITY_ADMIN_USERNAME, IDENTITY_ADMIN_PASSWORD)

        sharedBrokerIdpCredential = SamlCredentialUtils.generateX509Credential();
        sharedOriginIdpCredential = SamlCredentialUtils.generateX509Credential();
        sharedBrokerIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.BROKER, sharedBrokerIdpCredential)
        sharedOriginIdp = createIdpViaRest(IdentityProviderFederationTypeEnum.RACKER, sharedOriginIdpCredential)

        sharedRackerAuthRequestGenerator = new FederatedRackerAuthRequestGenerator(sharedBrokerIdpCredential, sharedOriginIdpCredential)
    }

    def createIdpViaRest(IdentityProviderFederationTypeEnum type, Credential cred) {
        def response = cloud20.createIdentityProviderWithCred(sharedServiceAdminToken, type, cred)
        if (response.status != SC_CREATED) {
            LOG.debug(response.getEntity(String))
        }
        assert (response.status == SC_CREATED)
        return response.getEntity(IdentityProvider)
    }

    def setup() {
        reloadableConfiguration.reset()
        staticIdmConfiguration.reset()
    }

    def cleanup() {
    }

    /**
     * This test case assumes an existing Racker existing by default within the docker eDir environment
     */
    def "Valid Racker auth no roles in eDir for authmethod: #authMethod"() {
        given:
        def username = RACKER_NOGROUP
        def fedRequest = createFedRequest(username).with {
            it.authContextRefClass = authContextClassRef
            it
        }

        def samlResponse = sharedRackerAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedRackerAuthRequestGenerator.convertResponseToString(samlResponse))

        then: "Response contains appropriate content"
        authClientResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value
        verifyAuthenticateResult(fedRequest, authResponse, expectedAuthByGroup, Collections.EMPTY_LIST)

        where:
        authMethod | authContextClassRef | expectedAuthByGroup
        "Password" | SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS | fedAndPasswordGroup
        "RSAKey" | SAMLConstants.TIMESYNCTOKEN_PROTECTED_AUTHCONTEXT_REF_CLASS | fedAndRsaKeyGroup
    }

    /**
     * This test case assumes an existing Racker existing by default within the docker eDir environment
     */
    def "Valid Racker auth with group in eDir"() {
        given:
        def username = RACKER_IMPERSONATE
        def fedRequest = createFedRequest(username)

        def samlResponse = sharedRackerAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedRackerAuthRequestGenerator.convertResponseToString(samlResponse))

        then: "Response contains appropriate content"
        authClientResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value
        verifyAuthenticateResult(fedRequest, authResponse, fedAndPasswordGroup, [identityConfig.getStaticConfig().getRackerImpersonateRoleName()])
    }

    def "Error: BadRequest against non existent user"() {
        given:
        def username = RandomStringUtils.randomAlphabetic(10)
        def fedRequest = createFedRequest(username)

        def samlResponse = sharedRackerAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedRackerAuthRequestGenerator.convertResponseToString(samlResponse))

        then: "Response contains appropriate content"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(authClientResponse, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE)
    }

    def "Error: BadRequest invalid auth context"() {
        given:
        def username = RandomStringUtils.randomAlphabetic(10)
        def fedRequest = createFedRequest(username).with {
            it.authContextRefClass = "invalid"
            it
        }

        def samlResponse = sharedRackerAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedRackerAuthRequestGenerator.convertResponseToString(samlResponse))

        then: "Response contains appropriate content"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(authClientResponse, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_AUTH_CONTEXT)
    }

    def "Validate racker token received matches initial federated auth response."() {
        given:
        def username = RACKER_IMPERSONATE
        def fedRequest = createFedRequest(username)

        def samlResponse = sharedRackerAuthRequestGenerator.createSignedSAMLResponse(fedRequest)

        when:
        def authClientResponse = cloud20.federatedAuthenticateV2(sharedRackerAuthRequestGenerator.convertResponseToString(samlResponse))

        then: "Response contains appropriate content"
        authClientResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = authClientResponse.getEntity(AuthenticateResponse).value
        verifyAuthenticateResult(fedRequest, authResponse, fedAndPasswordGroup, [identityConfig.getStaticConfig().getRackerImpersonateRoleName()])

        when: "validate token"
        AuthenticateResponse validationAuthResponse = utils.validateToken(authResponse.token.id)

        then:
        validationAuthResponse.token.id == authResponse.token.id
        validationAuthResponse.token.authenticatedBy.credential.size() == authResponse.token.authenticatedBy.credential.size()
        assert validationAuthResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.FEDERATION.getValue())
        assert validationAuthResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())

        validationAuthResponse.user.id == authResponse.user.id
        validationAuthResponse.user.name == authResponse.user.name
        validationAuthResponse.user.federatedIdp == authResponse.user.federatedIdp
        validationAuthResponse.serviceCatalog == null

        //every role in original token response is returned in validation response
        authResponse.user.roles.role.each {expectedRole -> assert validationAuthResponse.user.roles.role.find {it.name == expectedRole.name} != null}
    }

    def void verifyAuthenticateResult(FederatedRackerAuthGenerationRequest request, AuthenticateResponse authResponse, AuthenticatedByMethodGroup authByGroup, List<String> expectedEDirRoleNames = Collections.EMPTY_LIST) {
        //check the user object
        assert authResponse.user.id != null
        assert authResponse.user.name == request.username
        assert authResponse.user.federatedIdp == request.originIssuer
        assert authResponse.user.sessionInactivityTimeout == null

        //check the token
        assert authResponse.token.id != null

        List<String> expectedAuthByVals = authByGroup.authenticatedByMethodsAsValues
        assert authResponse.token.authenticatedBy.credential.size() == expectedAuthByVals.size()
        expectedAuthByVals.each {
            assert authResponse.token.authenticatedBy.credential.contains(it)
        }

        assert authResponse.user.roles.role.size() == expectedEDirRoleNames.size() + 1

        assert authResponse.user.getRoles().role.find{r -> r.name == GlobalConstants.ROLE_NAME_RACKER} != null

        expectedEDirRoleNames.each() { expectedRoleName ->
            assert authResponse.user.getRoles().role.find{r -> r.name == expectedRoleName} != null
        }

        assert authResponse.serviceCatalog.service.size() == 0
    }

    /**
     * Creates a fed request specifying shared broker/origin as the issuers and no requested roles
     * @return
     */
    def createFedRequest(String username) {
        new FederatedRackerAuthGenerationRequest().with {
            it.validitySeconds = 100
            it.brokerIssuer = sharedBrokerIdp.issuer
            it.originIssuer = sharedOriginIdp.issuer
            it.responseIssueInstant = new DateTime()
            it.authContextRefClass = SAMLConstants.PASSWORD_PROTECTED_AUTHCONTEXT_REF_CLASS
            it.username = username
            it
        }
    }
}