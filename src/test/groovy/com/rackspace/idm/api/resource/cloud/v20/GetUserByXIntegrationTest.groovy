package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import groovy.json.JsonSlurper
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.BooleanUtils
import org.apache.log4j.Logger
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse
import org.openstack.docs.identity.api.v2.UserList
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


    @Unroll
    def "test get user shows contact ID only for Service or Identity Admins, userType = #userType, request = #request, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def contactId = testUtils.getRandomUUID("contactId")
        def email = testUtils.getRandomUUID() + "@example.com"
        def username = testUtils.getRandomUUID("defaultUser")
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]
        def userForCreate = v2Factory.createUserForCreate(username, "display", email, true, null, domainId, DEFAULT_PASSWORD).with {
            it.contactId = contactId
            it
        }
        def user = cloud20.createUser(utils.getIdentityAdminToken(), userForCreate).getEntity(User).value

        when: "get user by ID"
        def token
        switch(userType) {
            case IdentityUserTypeEnum.SERVICE_ADMIN:
                token = utils.getServiceAdminToken()
                break
            case IdentityUserTypeEnum.IDENTITY_ADMIN:
                token = utils.getIdentityAdminToken()
                break
            case IdentityUserTypeEnum.USER_ADMIN:
                token = utils.getToken(userAdmin.username)
                break
            case IdentityUserTypeEnum.USER_MANAGER:
                token = utils.getToken(userManage.username)
                break
        }
        def getUserByIdResponse = cloud20.getUserById(token, user.id, accept)

        then:
        getUserByIdResponse.status == 200
        def returnedContactId = getContactIdFromResponse(getUserByIdResponse, accept)
        if(attrVisible) {
            assert returnedContactId == contactId
        } else {
            assert returnedContactId == null
        }

        when: "get user by name"
        def getByNameResponse = cloud20.getUserByName(token, username, accept)
        returnedContactId = getContactIdFromResponse(getByNameResponse, accept)

        then:
        getByNameResponse.status == 200
        if(attrVisible) {
            assert returnedContactId == contactId
        } else {
            assert returnedContactId == null
        }

        when: "get users by email"
        def getUsersByEmail = cloud20.getUsersByEmail(token, email, accept)
        returnedContactId = getContactIdFromResponse(getUsersByEmail, user.id, accept)

        then:
        getUsersByEmail.status == 200
        if(attrVisible) {
            assert returnedContactId == contactId
        } else {
            assert returnedContactId == null
        }

        cleanup:
        cloud20.deleteUser(utils.getServiceAdminToken(), user.id)
        utils.deleteUsers(users)

        where:
        userType                            | attrVisible  | accept                          | request
        IdentityUserTypeEnum.SERVICE_ADMIN  | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.SERVICE_ADMIN  | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

        IdentityUserTypeEnum.IDENTITY_ADMIN | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.IDENTITY_ADMIN | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

        IdentityUserTypeEnum.USER_ADMIN     | false        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_ADMIN     | false        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.USER_ADMIN     | false        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_ADMIN     | false        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

        IdentityUserTypeEnum.USER_MANAGER   | false        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_MANAGER   | false        | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.USER_MANAGER   | false        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_MANAGER   | false        | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    def getContactIdFromResponse(userResponse, accept) {
        if(MediaType.APPLICATION_XML_TYPE == accept) {
            def user = userResponse.getEntity(User).value
            return user.contactId
        } else {
            def user = new JsonSlurper().parseText(userResponse.getEntity(String)).user
            if(user.hasProperty('RAX-AUTH:contactId')) {
                assert user['RAX-AUTH:contactId'] != null
            }
            return user['RAX-AUTH:contactId']
        }
    }

    def getContactIdFromResponse(userResponse, userId, accept) {
        if(MediaType.APPLICATION_XML_TYPE == accept) {
            UserList userList = userResponse.getEntity(UserList).value
            return userList.getUser().find{ user -> user.id == userId }.contactId
        } else {
            def userList = new JsonSlurper().parseText(userResponse.getEntity(String)).users
            def userWithContactId = userList.find{ user -> user.id == userId }
            if(userWithContactId.hasProperty('RAX-AUTH:contactId') != null) {
                assert userWithContactId['RAX-AUTH:contactId'] != null
            }
            return userWithContactId['RAX-AUTH:contactId']
        }
    }

}
