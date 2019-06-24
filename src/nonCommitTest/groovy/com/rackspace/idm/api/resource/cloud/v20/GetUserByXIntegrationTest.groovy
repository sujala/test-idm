package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PhonePinStateEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.FederatedUserDao
import com.rackspace.idm.domain.dao.impl.LdapFederatedUserRepository
import com.rackspace.idm.domain.service.*
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import groovy.json.JsonSlurper
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.BooleanUtils
import org.apache.http.HttpStatus
import org.apache.log4j.Logger
import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.openstack.docs.identity.api.v2.UserForAuthenticateResponse
import org.openstack.docs.identity.api.v2.UserList
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.saml.SamlFactory

import javax.ws.rs.core.MediaType
import javax.xml.datatype.XMLGregorianCalendar

/**
 * Testing the v2.0/users endpoint
 */
class GetUserByXIntegrationTest extends RootConcurrentIntegrationTest {
    private static final Logger LOG = Logger.getLogger(GetUserByXIntegrationTest.class)

    @Shared def identityAdminToken

    @Autowired ScopeAccessService scopeAccessService
    @Autowired TenantService tenantService
    @Autowired UserService userService
    @Autowired Configuration config
    @Autowired EndpointService endpointService

    @Autowired(required = false)
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
        def response = cloud20.createUser(specificationIdentityAdminToken, v2Factory.createUserForCreate(adminUsername, "display", userAdminEmail, true, null, domainId, Constants.DEFAULT_PASSWORD))
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
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, fedUsername, 500, userAdmin.domainId, null, fedEmail);
        AuthenticateResponse samlAuthResponse = null
        for (int i=0; samlAuthResponse == null && i<10; i++) { // Workaround random failures in Jenkins
            try {
                samlAuthResponse = cloud20.samlAuthenticate(samlAssertion).getEntity(AuthenticateResponse).value
            } catch (GroovyCastException e) {
                samlAuthResponse = null
                sleep(500)
            }
        }
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
        retrievedFedUser.federatedIdp == Constants.DEFAULT_IDP_URI
        retrievedFedUser.created != null
        retrievedFedUser.phonePinState == PhonePinStateEnum.ACTIVE

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
        User user = getUserById(userAdmin.id, mediaType)

        then: "the user is returned"
        user != null
        user.domainId == domainId
        user.defaultRegion == userAdmin.defaultRegion
        user.email == userAdminEmail
        user.username == userAdmin.username
        BooleanUtils.isTrue(user.isEnabled())
        BooleanUtils.isNotTrue(user.isMultiFactorEnabled())
        user.roles == null
        user.groups == null
        user.secretQA == null
        user.federatedIdp == null
        user.created != null
        user.phonePinState == PhonePinStateEnum.ACTIVE

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Test get user by name/id return created attribute: Accept: #accept"() {
        given:
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domainId)

        when: "Get user by name"
        def response = cloud20.getUserByName(utils.identityAdminToken, userAdmin.username, accept)
        def byNameEntity = getEntity(response, User)

        then: "Assert created date"
        response.status == HttpStatus.SC_OK
        byNameEntity.created != null

        when: "Get user by id"
        response = cloud20.getUserById(utils.identityAdminToken, userAdmin.id, accept)
        def byIdEntity = getEntity(response, User)

        then: "Assert created date"
        response.status == HttpStatus.SC_OK
        byIdEntity.created != null

        then: "Assert created dates match"
        byNameEntity.created == byIdEntity.created

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
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
            def federatedUser = ldapFederatedUserRepository.getUserByUsernameForIdentityProviderId(samlUser.id, Constants.DEFAULT_IDP_ID)
            if (federatedUser != null) {
                ldapFederatedUserRepository.deleteObject(federatedUser)
            }
        } catch (Exception e) {
            //eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", samlUser.id), e)
        }
    }


    @Unroll
    def "test get user shows contact ID, userType = #userType, request = #request, accept = #accept"() {
        given:
        def domainId = utils.createDomain()
        def contactId = testUtils.getRandomUUID("contactId")
        def users = utils.createUsers(domainId).reverse()
        def defaultUser = users.find({it.username =~ /^defaultUser.*/})
        def userForUpdate = new User().with {
            it.id = defaultUser.id
            it.contactId = contactId
            it
        }
        utils.updateUser(userForUpdate)

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
                def userAdmin = users.find({it.username =~ /^userAdmin.*/})
                token = utils.getToken(userAdmin.username)
                break
            case IdentityUserTypeEnum.USER_MANAGER:
                def userManage = users.find({it.username =~ /^userManage.*/})
                token = utils.getToken(userManage.username)
                break
        }
        def getUserByIdResponse = cloud20.getUserById(token, defaultUser.id, accept)

        then:
        getUserByIdResponse.status == 200
        def returnedContactId = getContactIdFromResponse(getUserByIdResponse, accept)
        if(attrVisible) {
            assert returnedContactId == contactId
        } else {
            assert returnedContactId == null
        }

        when: "get user by name"
        def getByNameResponse = cloud20.getUserByName(token, defaultUser.username, accept)
        returnedContactId = getContactIdFromResponse(getByNameResponse, accept)

        then:
        getByNameResponse.status == 200
        if(attrVisible) {
            assert returnedContactId == contactId
        } else {
            assert returnedContactId == null
        }

        when: "get users by email"
        def getUsersByEmail = cloud20.getUsersByEmail(token, defaultUser.email, accept)
        returnedContactId = getContactIdFromResponse(getUsersByEmail, defaultUser.id, accept)

        then:
        getUsersByEmail.status == 200
        if(attrVisible) {
            assert returnedContactId == contactId
        } else {
            assert returnedContactId == null
        }

        cleanup:
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

        IdentityUserTypeEnum.USER_ADMIN     | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_ADMIN     | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.USER_ADMIN     | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_ADMIN     | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE

        IdentityUserTypeEnum.USER_MANAGER   | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_MANAGER   | true         | MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE
        IdentityUserTypeEnum.USER_MANAGER   | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        IdentityUserTypeEnum.USER_MANAGER   | true         | MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "get user by name/id returns password expiration if password policy exists for domain - accept == #accept, featureEnabled == #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_INCLUDE_PASSWORD_EXPIRATION_DATE_PROP, featureEnabled)
        def userAdmin = utils.createCloudAccount()
        def userAdminWithoutPwdPolicy = utils.createCloudAccount()
        utils.updateDomainPasswordPolicy(userAdmin.domainId)
        def userEntity = userService.getUserById(userAdmin.id)
        def expectedExpTime = userService.getPasswordExpiration(userEntity)

        when: "get user w/ pwd policy by ID"
        def getUserByIdResponse = utils.getUserById(userAdmin.id, utils.getServiceAdminToken(), accept)
        def returnedExpTime = getUserByIdResponse.passwordExpiration != null ? new DateTime(((XMLGregorianCalendar) getUserByIdResponse.passwordExpiration).toGregorianCalendar().getTime()) : null

        then: "the returned expiration time matches the expiration time we expect from the pw policy"
        if (featureEnabled) {
            assert returnedExpTime != null
            assert returnedExpTime == expectedExpTime
        } else {
            assert returnedExpTime == null
        }

        when: "get user w/o pwd policy by ID"
        getUserByIdResponse = utils.getUserById(userAdminWithoutPwdPolicy.id, utils.getServiceAdminToken(), accept)

        then:
        getUserByIdResponse.passwordExpiration == null

        when: "get user w/ pwd policy by name"
        def getUserByNameResponse = utils.getUserByName(userAdmin.username, utils.getServiceAdminToken(), accept)
       returnedExpTime = getUserByNameResponse.passwordExpiration != null ? new DateTime(((XMLGregorianCalendar) getUserByNameResponse.passwordExpiration).toGregorianCalendar().getTime()) : null

        then:
        if (featureEnabled) {
            assert returnedExpTime != null
            assert returnedExpTime == expectedExpTime
        } else {
            assert returnedExpTime == null
        }

        when: "get user w/o pwd policy by name"
        getUserByNameResponse = utils.getUserByName(userAdminWithoutPwdPolicy.username, utils.getServiceAdminToken(), accept)

        then:
        getUserByNameResponse.passwordExpiration == null

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteUser(userAdminWithoutPwdPolicy)

        where:
        [accept, featureEnabled] << [[MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE], [true, false]].combinations()
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
