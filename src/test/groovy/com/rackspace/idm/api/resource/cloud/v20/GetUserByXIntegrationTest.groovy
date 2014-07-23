package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.BooleanUtils
import org.apache.log4j.Logger
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.saml.SamlAssertionFactory

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.*

/**
 * Testing the v2.0/users endpoint
 */
class GetUserByXIntegrationTest extends RootConcurrentIntegrationTest {
    private static final Logger LOG = Logger.getLogger(GetUserByXIntegrationTest.class)

    @Shared def identityAdminToken

    @Autowired def ScopeAccessService scopeAccessService
    @Autowired def TenantService tenantService
    @Autowired def UserService userService
    @Autowired def Configuration config
    @Autowired def EndpointService endpointService

    @Autowired
    LdapFederatedUserRepository ldapFederatedUserRepository

    @Autowired
    private FederatedUserDao federatedUserDao

    User userAdmin
    def domainId
    def userAdminEmail

    /**
     * Setup involves creating both types of users (provisioned and federated) for subsequent retrieval
     * @return
     */
    def setup() {
        identityAdminToken = utils.getIdentityAdminToken()
        String adminUsername = USER_ADMIN_USERNAME_PREFIX + getNormalizedRandomString()
        domainId = getNormalizedRandomString()
        userAdminEmail = adminUsername + "@rackspace.com"

        //create user-admin
        def response = cloud20.createUser(specificationIdentityAdminToken, v2Factory.createUserForCreate(adminUsername, "display", userAdminEmail, true, null, domainId, DEFAULT_PASSWORD))
        userAdmin = response.getEntity(User).value
    }

    def cleanup() {
        utils.deleteUser(userAdmin)
    }

    /**
     * http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/GET_getUserById_users_User_Calls.html
     *
     * @return
     */
    @Unroll
    def "Retrieve federated user by id response accept type: #mediaType"() {
        //create a federated user
        def fedUsername = testUtils.getRandomUUID("subUserForSaml")
        def fedEmail = "fedIntTest@invalid.rackspace.com"

        //specify assertion with no roles
        def samlAssertion = new SamlAssertionFactory().generateSamlAssertion(DEFAULT_IDP_URI, fedUsername, 5, userAdmin.domainId, null, fedEmail);
        AuthenticateResponse samlAuthResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value
        UserForAuthenticateResponse samlUser = samlAuthResponse.user

        when: "Retrieve federated user"
        User retrievedFedUser = getUserById(samlUser.id, mediaType)

        then: "the user is returned"
        retrievedFedUser != null
        retrievedFedUser.domainId == domainId
        retrievedFedUser.defaultRegion == userAdmin.defaultRegion
        retrievedFedUser.email == fedEmail
        retrievedFedUser.username == samlUser.name
        BooleanUtils.isTrue(retrievedFedUser.isEnabled())
        BooleanUtils.isNotTrue(retrievedFedUser.isMultiFactorEnabled())
        retrievedFedUser.roles == null
        retrievedFedUser.groups == null
        retrievedFedUser.secretQA == null
        retrievedFedUser.federatedIdp == DEFAULT_IDP_URI

        cleanup:
            deleteFederatedUserQuietly(samlUser)

        where:
            mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    /**
     * http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/GET_getUserById_users_User_Calls.html
     *
     * @return
     */
    @Unroll
    def "Retrieve provisioned user by id response accept type: #mediaType"() {
        when: "Retrieve provisioned user"
        User retrievedFedUser = getUserById(userAdmin.id, mediaType)

        then: "the user is returned"
        retrievedFedUser != null
        retrievedFedUser.domainId == domainId
        retrievedFedUser.defaultRegion == userAdmin.defaultRegion
        retrievedFedUser.email == userAdminEmail
        retrievedFedUser.username == userAdmin.username
        BooleanUtils.isTrue(retrievedFedUser.isEnabled())
        BooleanUtils.isNotTrue(retrievedFedUser.isMultiFactorEnabled())
        retrievedFedUser.roles == null
        retrievedFedUser.groups == null
        retrievedFedUser.secretQA == null
        retrievedFedUser.federatedIdp == null

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def User getUserById(String userId, MediaType mediaType) {
        def responseEntity = cloud20.getUserById(identityAdminToken, userId, mediaType).getEntity(User)
        return mediaType == MediaType.APPLICATION_XML_TYPE ? responseEntity.value : responseEntity
    }

    def deleteFederatedUserQuietly(UserForAuthenticateResponse samlUser) {
        if (samlUser == null || samlUser.id == null) {
            return
        }
        try {
            def federatedUser = ldapFederatedUserRepository.getUserByUsernameForIdentityProviderName(samlUser.id, DEFAULT_IDP_NAME)
            if (federatedUser != null) {
                ldapFederatedUserRepository.deleteObject(federatedUser)
            }
        } catch (Exception e) {
            //eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", samlUser.id), e)
        }
    }

}
