package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.DomainDao
import com.rackspace.idm.domain.dao.FederatedTokenDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedRackerRepository
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.security.ConfigurableTokenFormatSelector
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import org.apache.log4j.Logger
import org.opensaml.saml2.core.Response
import org.opensaml.xml.signature.Signature
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlAssertionFactory

import javax.servlet.http.HttpServletResponse

import static com.rackspace.idm.Constants.RACKER_IDP_URI

@ContextConfiguration(locations = "classpath:app-config.xml")
class FederatedRackerIntegrationTest extends RootIntegrationTest {
    private static final Logger LOG = Logger.getLogger(FederatedRackerIntegrationTest.class)

    @Autowired
    LdapFederatedRackerRepository ldapFederatedRackerRepository

    @Autowired
    TenantService tenantService

    @Autowired
    UserService userService

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    ConfigurableTokenFormatSelector configurableTokenFormatSelector

    private static final String EDIR_ROLE_NAME = "team-cloud-identity"

    def setup() {
    }

    def cleanup() {
    }

    def "racker populated appropriately from saml and edir w/ no EDIR groups"() {
        given:
        def username = Constants.RACKER_NOGROUP
        def expDays = 5

        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedRacker(RACKER_IDP_URI, username, expDays);

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, username)

        when: "retrieve user from backend"
        Racker fedUser = ldapFederatedRackerRepository.getUserByUsernameForIdentityProviderUri(authResponse.user.id, authResponse.user.federatedIdp)

        then: "reflects current state"
        fedUser.federatedUserName == authResponse.user.id
        fedUser.federatedUserName == authResponse.user.name
        fedUser.federatedIdpUri == RACKER_IDP_URI

        cleanup:
        deleteFederatedRackerQuietly(String.format("%s@%s", username, RACKER_IDP_URI))
    }

    def "racker populated appropriately from saml and edir w/ impersonate role"() {
        given:
        def username = Constants.RACKER_IMPERSONATE
        def expDays = 5

        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedRacker(RACKER_IDP_URI, username, expDays);

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        verifyResponseFromSamlRequest(authResponse, username, [identityConfig.getStaticConfig().getRackerImpersonateRoleName()])

        when: "retrieve user from backend"
        Racker fedUser = ldapFederatedRackerRepository.getUserByUsernameForIdentityProviderUri(authResponse.user.id, authResponse.user.federatedIdp)

        then: "reflects current state"
        fedUser.federatedUserName == authResponse.user.id
        fedUser.federatedUserName == authResponse.user.name
        fedUser.federatedIdpUri == RACKER_IDP_URI

        cleanup:
        deleteFederatedRackerQuietly(String.format("%s@%s", username, RACKER_IDP_URI))
    }

    def "Validating token received matches initial federated auth response"() {
        given:
        def username = Constants.RACKER_IMPERSONATE

        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedRacker(RACKER_IDP_URI, username, 1);

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value

        when: "validate token"
        AuthenticateResponse validationAuthResponse = utils.validateToken(authResponse.token.id)

        then:
        validationAuthResponse.token.id == authResponse.token.id
        validationAuthResponse.token.authenticatedBy.credential.size() == 1
        assert authResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.FEDERATION.getValue())

        validationAuthResponse.user.id == authResponse.user.id
        validationAuthResponse.user.defaultRegion == authResponse.user.defaultRegion
        validationAuthResponse.user.name == authResponse.user.name
        validationAuthResponse.user.federatedIdp == authResponse.user.federatedIdp
        validationAuthResponse.serviceCatalog == null

        //every role in original token response is returned in validation response
        authResponse.user.roles.role.each {expectedRole -> assert validationAuthResponse.user.roles.role.find {it.name == expectedRole.name} != null}

        cleanup:
        deleteFederatedRackerQuietly(String.format("%s@%s", username, RACKER_IDP_URI))
    }

    def "Federated racker auth matches regular racker auth"() {
        given:
        def username = Constants.RACKER_IMPERSONATE
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedRacker(RACKER_IDP_URI, username, 1);

        AuthenticateResponse rackerAuthResponse = utils.authenticateRacker(username, Constants.RACKER_IMPERSONATE_PASSWORD)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse samlAuthResponse = samlResponse.getEntity(AuthenticateResponse).value

        then: "responses match other than token id"
        rackerAuthResponse.token.authenticatedBy.credential.size() == samlAuthResponse.token.authenticatedBy.credential.size()

        assert samlAuthResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.FEDERATION.getValue())
        assert rackerAuthResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.getValue())

        rackerAuthResponse.user.id == samlAuthResponse.user.id
        rackerAuthResponse.user.defaultRegion == samlAuthResponse.user.defaultRegion
        rackerAuthResponse.user.name == samlAuthResponse.user.name
        rackerAuthResponse.user.federatedIdp == null
        samlAuthResponse.user.federatedIdp == RACKER_IDP_URI
        (rackerAuthResponse.serviceCatalog.service == samlAuthResponse.serviceCatalog.service || rackerAuthResponse.serviceCatalog.service.size() == samlAuthResponse.serviceCatalog.service.size())

        //every role in original token response is returned in validation response
        rackerAuthResponse.user.roles.role.each {expectedRole -> assert samlAuthResponse.user.roles.role.find {it.name == expectedRole.name} != null}

        cleanup:
        deleteFederatedRackerQuietly(String.format("%s@%s", username, RACKER_IDP_URI))
    }

    def "Federated racker w/ impersonation role can impersonate a user-admin and have token imp token validated"() {
        given:
        def username = Constants.RACKER_IMPERSONATE
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertionStringForFederatedRacker(RACKER_IDP_URI, username, 1);

        AuthenticateResponse rackerAuthResponse = utils.authenticateRacker(username, Constants.RACKER_IMPERSONATE_PASSWORD)

        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse samlAuthResponse = samlResponse.getEntity(AuthenticateResponse).value

        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when:
        ImpersonationResponse impResponse = utils.impersonateWithToken(samlAuthResponse.token.id, userAdmin)

        then: "has a token"
        impResponse.token.id != null

        and: "can be validated"
        AuthenticateResponse response = utils.validateToken(impResponse.token.id)
        assert response.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.FEDERATION.getValue())

        cleanup:
        deleteFederatedRackerQuietly(String.format("%s@%s", username, RACKER_IDP_URI))
    }

    def "Invalid SAML signature results in 400"() {
        given:
        def username = Constants.RACKER_NOGROUP
        def expDays = 5
        def samlFactor = new SamlAssertionFactory()

        Response samlAssertion = samlFactor.generateSamlAssertionResponseForFederatedRacker(RACKER_IDP_URI, username, expDays);
        Response samlAssertion2 = samlFactor.generateSamlAssertionResponseForFederatedRacker(RACKER_IDP_URI, username, expDays+1);

        //replace first assertion with second to make an invalid assertion
        Signature sig = samlAssertion2.getSignature()
        sig.detach()
        samlAssertion.setSignature(sig)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlFactor.convertResponseToString(samlAssertion))

        then: "Response contains appropriate content"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(samlResponse, BadRequestFault, HttpServletResponse.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_FEDERATION_INVALID_SIGNATURE)
    }

    def "When receive samlResponse for non-existant racker, throw 400"() {
        given:
        def username = "non-existant_racker"
        def expDays = 5
        def samlFactor = new SamlAssertionFactory()

        String samlAssertion = samlFactor.generateSamlAssertionStringForFederatedRacker(RACKER_IDP_URI, username, expDays);

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains appropriate content"
        IdmAssert.assertOpenStackV2FaultResponseWithErrorCode(samlResponse, BadRequestFault, HttpServletResponse.SC_BAD_REQUEST, ErrorCodes.ERROR_CODE_FEDERATION_RACKER_NON_EXISTANT_RACKER)
    }

    def void verifyResponseFromSamlRequest(AuthenticateResponse authResponse, expectedUserName, List<String> expectedEDirRoleNames = Collections.EMPTY_LIST) {
        //check the user object
        assert authResponse.user.id != null
        assert authResponse.user.name == expectedUserName
        assert authResponse.user.federatedIdp == RACKER_IDP_URI

        //check the token
        assert authResponse.token.id != null
        assert authResponse.token.authenticatedBy.credential.size() == 1
        assert authResponse.token.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.FEDERATION.getValue())

        assert authResponse.user.roles.role.size() == expectedEDirRoleNames.size() + 1

        assert authResponse.user.getRoles().role.find{r -> r.name == GlobalConstants.ROLE_NAME_RACKER} != null

        expectedEDirRoleNames.each() { expectedRoleName ->
            assert authResponse.user.getRoles().role.find{r -> r.name == expectedRoleName} != null
        }

        assert authResponse.serviceCatalog.service.size() == 0
    }

    def deleteFederatedRackerQuietly(id) {
        try {
            def federatedRacker = ldapFederatedRackerRepository.getUserById(id)
            if (federatedRacker != null) {
                ldapFederatedRackerRepository.deleteObject(federatedRacker)
            }
        } catch (Exception e) {
            //eat but log
            LOG.warn(String.format("Error cleaning up federatedRacker with id '%s'", id), e)
        }
    }
}
