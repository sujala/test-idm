package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.impl.LdapFederatedRackerRepository
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import org.apache.http.HttpStatus
import org.apache.log4j.Logger
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlCredentialUtils
import testHelpers.saml.v2.FederatedRackerAuthRequestGenerator

import javax.servlet.http.HttpServletResponse

import static com.rackspace.idm.Constants.*
import static com.rackspace.idm.SAMLConstants.TIMESYNCTOKEN_PROTECTED_AUTHCONTEXT_REF_CLASS

@ContextConfiguration(locations = "classpath:app-config.xml")
class FederatedRackerIntegrationTest extends RootIntegrationTest {
    private static final Logger LOG = Logger.getLogger(FederatedRackerIntegrationTest.class)

    @Autowired(required = false)
    LdapFederatedRackerRepository ldapFederatedRackerRepository

    @Autowired
    TenantService tenantService

    @Autowired
    UserService userService

    @Autowired
    IdentityConfig identityConfig

    AuthenticatedByMethodGroup fedAndPasswordGroup = AuthenticatedByMethodGroup.getGroup(AuthenticatedByMethodEnum.FEDERATION, AuthenticatedByMethodEnum.PASSWORD)
    AuthenticatedByMethodGroup fedAndRsaKeyGroup = AuthenticatedByMethodGroup.getGroup(AuthenticatedByMethodEnum.FEDERATION, AuthenticatedByMethodEnum.RSAKEY)

    def "racker populated appropriately from saml and edir w/ no EDIR groups."() {
        given:
        def samlRequest = utils.createFederatedRackerRequest(RACKER_NOGROUP)
        def samlAssertionTokenRequest = utils.createFederatedRackerRequest(RACKER_NOGROUP, null, TIMESYNCTOKEN_PROTECTED_AUTHCONTEXT_REF_CLASS)

        when:
        def samlResponse = utils.authenticateV2FederatedRacker(samlRequest)

        then: "Response contains appropriate content"
        verifyResponseFromSamlRequest(samlResponse, samlRequest.username, fedAndPasswordGroup)

        when:
        samlResponse = utils.authenticateV2FederatedRacker(samlAssertionTokenRequest)

        then: "Response contains appropriate content"
        verifyResponseFromSamlRequest(samlResponse, samlAssertionTokenRequest.username, fedAndRsaKeyGroup)
    }

    def "racker populated appropriately from saml and edir w/ impersonate role. Persist Racker: #persistRacker"() {
        given:
        def samlRequest = utils.createFederatedRackerRequest(RACKER_IMPERSONATE)
        def samlAssertionTokenRequest = utils.createFederatedRackerRequest(RACKER_IMPERSONATE, null, TIMESYNCTOKEN_PROTECTED_AUTHCONTEXT_REF_CLASS)

        when:
        def samlResponse = utils.authenticateV2FederatedRacker(samlRequest)

        then: "Response contains appropriate content"
        verifyResponseFromSamlRequest(samlResponse, samlRequest.username, fedAndPasswordGroup, [identityConfig.getStaticConfig().getRackerImpersonateRoleName()])

        when:
        samlResponse = utils.authenticateV2FederatedRacker(samlAssertionTokenRequest)

        then: "Response contains appropriate content"
        verifyResponseFromSamlRequest(samlResponse, samlAssertionTokenRequest.username, fedAndRsaKeyGroup, [identityConfig.getStaticConfig().getRackerImpersonateRoleName()])
    }

    def "Validating token received matches initial federated auth response."() {
        given:
        def samlRequest = utils.createFederatedRackerRequest(RACKER_IMPERSONATE)

        when:
        def samlResponse = utils.authenticateV2FederatedRacker(samlRequest)

        then: "Response contains appropriate content"
        assert samlResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.FEDERATION.getValue())
        assert samlResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())

        when: "validate token"
        AuthenticateResponse validationAuthResponse = utils.validateToken(samlResponse.token.id)

        then:
        validationAuthResponse.token.id == samlResponse.token.id
        validationAuthResponse.token.authenticatedBy.credential.size() == samlResponse.token.authenticatedBy.credential.size()
        assert validationAuthResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.FEDERATION.getValue())
        assert validationAuthResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())

        validationAuthResponse.user.id == samlResponse.user.id
        validationAuthResponse.user.defaultRegion == samlResponse.user.defaultRegion
        validationAuthResponse.user.name == samlResponse.user.name
        validationAuthResponse.user.federatedIdp == samlResponse.user.federatedIdp
        validationAuthResponse.serviceCatalog == null

        //every role in original token response is returned in validation response
        samlResponse.user.roles.role.each {expectedRole -> assert validationAuthResponse.user.roles.role.find {it.name == expectedRole.name} != null}
    }

    def "Federated racker auth matches regular racker auth."() {
        given:
        def samlRequest = utils.createFederatedRackerRequest(RACKER_IMPERSONATE)
        def rackerAuthResponse = utils.authenticateRacker(RACKER_IMPERSONATE, RACKER_PASSWORD)

        when:
        def samlResponse = utils.authenticateV2FederatedRacker(samlRequest)

        then: "responses match other than token id"
        rackerAuthResponse.token.authenticatedBy.credential.size() == samlResponse.token.authenticatedBy.credential.size() - 1

        assert samlResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.FEDERATION.getValue())
        assert samlResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())
        assert rackerAuthResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())

        rackerAuthResponse.user.id == samlResponse.user.id
        rackerAuthResponse.user.defaultRegion == samlResponse.user.defaultRegion
        rackerAuthResponse.user.name == samlResponse.user.name
        rackerAuthResponse.user.federatedIdp == null
        samlResponse.user.federatedIdp == IDP_V2_RACKER_URI
        (rackerAuthResponse.serviceCatalog.service == samlResponse.serviceCatalog.service || rackerAuthResponse.serviceCatalog.service.size() == samlResponse.serviceCatalog.service.size())

        //every role in original token response is returned in validation response
        rackerAuthResponse.user.roles.role.each {expectedRole -> assert samlResponse.user.roles.role.find {it.name == expectedRole.name} != null}
    }

    def "Federated racker w/ impersonation role can impersonate a user-admin and have token imp token validated."() {
        given:
        def samlRequest = utils.createFederatedRackerRequest(RACKER_IMPERSONATE)
        def samlResponse = utils.authenticateV2FederatedRacker(samlRequest)

        def userAdmin = utils.createCloudAccount()

        when:
        ImpersonationResponse impResponse = utils.impersonateWithToken(samlResponse.token.id, userAdmin)

        then: "has a token"
        impResponse.token.id != null

        and: "can be validated"
        AuthenticateResponse response = utils.validateToken(impResponse.token.id)
        assert response.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.FEDERATION.getValue())
    }

    def "Invalid SAML signature results in 400"() {
        given:
        def rackerIdpCred = SamlCredentialUtils.generateX509Credential()
        def brokerIdpCred = SamlCredentialUtils.generateX509Credential()
        utils.createIdentityProviderWithCred(utils.serviceAdminToken, IdentityProviderFederationTypeEnum.RACKER, rackerIdpCred)
        utils.createIdentityProviderWithCred(utils.serviceAdminToken, IdentityProviderFederationTypeEnum.BROKER, brokerIdpCred)

        FederatedRackerAuthRequestGenerator federatedRackerAuthRequestGenerator = new FederatedRackerAuthRequestGenerator(brokerIdpCred, rackerIdpCred)
        def samlRequest = utils.createFederatedRackerRequest(RACKER_NOGROUP)
        samlRequest.brokerIssuer = IDP_V2_DOMAIN_URI
        def inputSamlResponseStr = federatedRackerAuthRequestGenerator.convertResponseToString(federatedRackerAuthRequestGenerator.createSignedSAMLResponse(samlRequest))

        when:
        def samlResponse = cloud20.authenticateV2FederatedUser(inputSamlResponseStr)

        then: "Response contains appropriate content"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(samlResponse, BadRequestFault, HttpStatus.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_FEDERATION_INVALID_BROKER_SIGNATURE)
    }

    def "When receive samlResponse for non-existant racker, throw 400"() {
        given:
        FederatedRackerAuthRequestGenerator federatedRackerAuthRequestGenerator = new FederatedRackerAuthRequestGenerator(DEFAULT_BROKER_IDP_PUBLIC_KEY, DEFAULT_BROKER_IDP_PRIVATE_KEY, RACKER_IDP_PUBLIC_KEY, RACKER_IDP_PRIVATE_KEY)
        def samlRequest = utils.createFederatedRackerRequest("non-existant_racker")
        def inputSamlResponseStr = federatedRackerAuthRequestGenerator.convertResponseToString(federatedRackerAuthRequestGenerator.createSignedSAMLResponse(samlRequest))

        when:
        def samlResponse = cloud20.authenticateV2FederatedUser(inputSamlResponseStr)

        then: "Response contains appropriate content"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(samlResponse, BadRequestFault, HttpServletResponse.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_FEDERATION2_INVALID_REQUIRED_ATTRIBUTE)
    }

    void verifyResponseFromSamlRequest(AuthenticateResponse authResponse, expectedUserName, AuthenticatedByMethodGroup authByGroup, List<String> expectedEDirRoleNames = Collections.EMPTY_LIST) {
        //check the user object
        assert authResponse.user.id != null
        assert authResponse.user.name == expectedUserName
        assert authResponse.user.federatedIdp == Constants.IDP_V2_RACKER_URI

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
}
