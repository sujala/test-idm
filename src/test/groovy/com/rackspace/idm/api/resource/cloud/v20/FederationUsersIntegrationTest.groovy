package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.dao.DomainDao
import com.rackspace.idm.domain.dao.FederatedTokenDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import org.apache.commons.lang.BooleanUtils
import org.apache.log4j.Logger
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlAssertionFactory

import javax.servlet.http.HttpServletResponse

import static com.rackspace.idm.Constants.DEFAULT_IDP_NAME
import static com.rackspace.idm.Constants.DEFAULT_IDP_URI

@ContextConfiguration(locations = "classpath:app-config.xml")
class FederationUsersIntegrationTest extends RootIntegrationTest {
    private static final Logger LOG = Logger.getLogger(FederationUsersIntegrationTest.class)

    @Autowired
    FederatedTokenDao federatedTokenDao

    @Autowired
    LdapFederatedUserRepository ldapFederatedUserRepository

    @Autowired
    TenantService tenantService

    @Autowired
    UserService userService

    @Autowired
    DomainDao domainDao

    def "user populated appropriately from saml"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expDays = 5
        def email = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminEntity = userService.getUserById(userAdmin.id)

        when:
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)

        then: "Response contains appropriate content"
        samlResponse.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value

        //check the user object
        authResponse.user.id != null
        authResponse.user.federatedIdp == DEFAULT_IDP_URI
        authResponse.user.defaultRegion == userAdmin.defaultRegion

        //check the token
        authResponse.token.id != null
        authResponse.token.authenticatedBy.credential.contains(GlobalConstants.AUTHENTICATED_BY_FEDERATION)

        //check the roles (assigned identity default role as well as compute:default,object-store:default (propagating roles) by default
        //should query the useradmin to figure out the roles, but
        authResponse.user.getRoles().role.find{r -> r.name == 'identity:default'} != null
        def userAdminRoles = tenantService.getTenantRolesForUser(userAdminEntity)
        userAdminRoles.each() { userAdminRole ->
            if (BooleanUtils.isTrue(userAdminRole.propagate)) {
                authResponse.user.getRoles().role.find{r -> r.name == userAdminRole.name && r.id == userAdminRole.roleRsId} != null
            }
        }

        //TODO: Service catalog checks
        authResponse.serviceCatalog != null
        authResponse.serviceCatalog.service.size() > 0

        when: "retrieve user from backend"
        FederatedUser fedUser = ldapFederatedUserRepository.getUserById(authResponse.user.id)

        then: "reflects current state"
        fedUser.id == authResponse.user.id
        fedUser.username == username
        fedUser.domainId == domainId
        fedUser.email == email
        fedUser.region == userAdminEntity.region

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "passing multiple saml requests with same info references same user"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, null);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)

        when: "init"
        def samlResponse1 = cloud20.samlAuthenticate(samlAssertion)
        def samlResponse2 = cloud20.samlAuthenticate(samlAssertion)

        then:
        samlResponse1.status == HttpServletResponse.SC_OK
        samlResponse2.status == HttpServletResponse.SC_OK
        AuthenticateResponse authResponse1 = samlResponse1.getEntity(AuthenticateResponse).value
        AuthenticateResponse authResponse2 = samlResponse2.getEntity(AuthenticateResponse).value

        authResponse1.user.id == authResponse2.user.id

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "passing multiple saml requests with same user, but different id throws error"() {
        given:
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def username = testUtils.getRandomUUID("samlUser")
        def expDays = 5
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId, ["identity:default"].asList());
        def samlAssertion2 = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, username, expDays, domainId2, ["identity:default"].asList());
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdmin2, users2
        (userAdmin2, users2) = utils.createUserAdminWithTenants(domainId2)


        when: "init"
        def samlResponse1 = cloud20.samlAuthenticate(samlAssertion)
        def samlResponse2 = cloud20.samlAuthenticate(samlAssertion2)

        then:
        samlResponse1.status == HttpServletResponse.SC_OK
        samlResponse2.status == HttpServletResponse.SC_CONFLICT

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
        utils.deleteUsers(users2)
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
