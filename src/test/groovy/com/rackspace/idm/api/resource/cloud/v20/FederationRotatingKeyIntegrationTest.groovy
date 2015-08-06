package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.DomainDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.dao.impl.LdapIdentityProviderRepository
import com.rackspace.idm.domain.entity.IdentityProvider
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import org.apache.log4j.Logger
import org.opensaml.xml.security.credential.Credential
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import testHelpers.EntityFactory
import testHelpers.saml.CredentialSamlAssertionFactory
import testHelpers.saml.SamlCredentialUtils

import javax.servlet.http.HttpServletResponse
import java.security.cert.X509Certificate

import static com.rackspace.idm.Constants.DEFAULT_IDP_NAME

@ContextConfiguration(locations = "classpath:app-config.xml")
class FederationRotatingKeyIntegrationTest extends RootConcurrentIntegrationTest {
    private static final Logger LOG = Logger.getLogger(FederationRotatingKeyIntegrationTest.class)

    @Autowired
    LdapFederatedUserRepository ldapFederatedUserRepository

    @Autowired
    TenantService tenantService

    @Autowired
    RoleService roleService

    @Autowired
    UserService userService

    @Autowired
    DomainDao domainDao

    @Autowired
    LdapIdentityProviderRepository ldapIdentityProviderRepository

    EntityFactory entityFactory = new EntityFactory()

    def setup() {
    }

    /**
     * Tests that samlResponses that validate against different certs on the same provider can be processed successfully.
     * Solely tests that the appropriate http status code is returned - relying on other existing tests to verify that the actual
     * backend processing (and response body) is correct when the samlResponse can be property verified.
     */
    def "verify key rotation"() {
        //add identity provider w/ 2 keys
        Credential cred1 = SamlCredentialUtils.generateX509Credential();
        Credential cred2 = SamlCredentialUtils.generateX509Credential();
        List<X509Certificate> certs = [cred1.entityCertificate, cred2.entityCertificate]
        IdentityProvider provider = entityFactory.createIdentityProviderWithCertificates(certs)
        ldapIdentityProviderRepository.addObject(provider)

        //first credential assertino factory
        def cred1Factory = new CredentialSamlAssertionFactory(cred1)
        def cred2Factory = new CredentialSamlAssertionFactory(cred2)

        org.openstack.docs.identity.api.v2.User userAdmin = createUserAdmin()

        def domainId = userAdmin.domainId
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"

        when: "Generate samlResponse using first certificate"
        def samlAssertion = cred1Factory.generateSamlAssertion(provider.name, username, expDays, domainId, null, email);
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Was processed successfully"
        samlResponse.status == HttpServletResponse.SC_OK

        when: "Generate samlResponse using second certificate"
        def samlAssertion2 = cred2Factory.generateSamlAssertion(provider.name, username, expDays, domainId, null, email);
        def samlResponse2 = cloud20.samlAuthenticate(samlAssertion2)

        then: "Was processed successfully"
        samlResponse2.status == HttpServletResponse.SC_OK

        when: "Remove first certificate"
        provider.removeUserCertificate(cred1.entityCertificate)
        ldapIdentityProviderRepository.updateObject(provider)
        def samlResponseInvalid = cloud20.samlAuthenticate(samlAssertion)
        def samlResponse2Again = cloud20.samlAuthenticate(samlAssertion2)

        then: "Can not use samlResponse generated for first cert, but can still verify samlResponse for second certificate"
        samlResponseInvalid.status == HttpServletResponse.SC_BAD_REQUEST
        samlResponse2Again.status == HttpServletResponse.SC_OK

        when: "Remove remaining certificate"
        provider.removeUserCertificate(cred2.entityCertificate)
        ldapIdentityProviderRepository.updateObject(provider)
        def samlResponseInvalid2 = cloud20.samlAuthenticate(samlAssertion)
        def samlResponse2Invalid = cloud20.samlAuthenticate(samlAssertion2)

        then: "Can not validate either samlResponses"
        samlResponseInvalid2.status == HttpServletResponse.SC_BAD_REQUEST
        samlResponse2Invalid.status == HttpServletResponse.SC_BAD_REQUEST

        cleanup:
        ldapIdentityProviderRepository.deleteObject(provider)

    }

    def deleteFederatedUserQuietly(username) {
        try {
            def federatedUser = ldapFederatedUserRepository.getUserByUsernameForIdentityProviderName(username, DEFAULT_IDP_NAME)
            if (federatedUser != null) {
                ldapFederatedUserRepository.deleteObject(federatedUser)
            }
        } catch (Exception e) {
            //eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", username), e)
        }
    }

}
