package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.FederatedUserDao
import groovy.json.JsonSlurper
import org.apache.log4j.Logger
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.openstack.docs.identity.api.v2.UserList
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_OK

class ListUsersIntegrationTest extends RootIntegrationTest {

    @Autowired
    FederatedUserDao federatedUserRepository

    private static final Logger LOG = Logger.getLogger(ListUsersIntegrationTest.class)

    def "federated user can call list users"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def samlToken = authResponse.token.id

        when: "list users with federated user's token"
        def listUsersResponse = cloud20.listUsers(samlToken)

        then: "request successful and only contains the federated user"
        listUsersResponse.status == 200
        def userList = listUsersResponse.getEntity(UserList).value
        userList.user.id.contains(authResponse.user.id)
        userList.user.size() == 1

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "user admin cannot see federated users that are not in their domain"() {
        given:

        def domainId1 = utils.createDomain()
        def domainId2 = utils.createDomain()
        def username1 = testUtils.getRandomUUID("userForSaml")
        def username2 = testUtils.getRandomUUID("userForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion1 = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username1, expSecs, domainId1, null, email);
        def samlAssertion2 = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username2, expSecs, domainId2, null, email);
        def userAdmin1, userAdmin2, users1, users2
        (userAdmin1, users1) = utils.createUserAdminWithTenants(domainId1)
        (userAdmin2, users2) = utils.createUserAdminWithTenants(domainId2)
        def adminToken1 = utils.getToken(userAdmin1.username)
        def adminToken2 = utils.getToken(userAdmin2.username)
        def samlResponse1 = cloud20.samlAuthenticate(samlAssertion1)
        def samlResponse2 = cloud20.samlAuthenticate(samlAssertion2)
        def AuthenticateResponse authResponse1 = samlResponse1.getEntity(AuthenticateResponse).value
        def AuthenticateResponse authResponse2 = samlResponse2.getEntity(AuthenticateResponse).value

        when: "list users with first user admin token"
        def listUsersResponse = cloud20.listUsers(adminToken1)

        then: "request is successful"
        listUsersResponse.status == 200
        def userList1 = listUsersResponse.getEntity(UserList).value

        and: "contains the federated user from domain"
        userList1.user.id.contains(authResponse1.user.id)

        and: "does not contain the federated user from other domains"
        !userList1.user.id.contains(authResponse2.user.id)

        when: "list users with other user admin token"
        listUsersResponse = cloud20.listUsers(adminToken2)

        then: "request is successful"
        listUsersResponse.status == 200
        def userList2 = listUsersResponse.getEntity(UserList).value

        and: "contains the federated user from domain"
        userList2.user.id.contains(authResponse2.user.id)

        and: "does not contain the federated user from other domains"
        !userList2.user.id.contains(authResponse1.user.id)

        cleanup:
        deleteFederatedUserQuietly(username1)
        deleteFederatedUserQuietly(username2)
        utils.deleteUsers(users1)
        utils.deleteUsers(users2)
    }

    @Unroll
    def "user admin with federated user in domain see federated user in list users call - accept: #accept"() {
        given:

        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def adminToken = utils.getToken(userAdmin.username)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value

        when: "list users with user admin token"
        List<User> userList = utils.listUsers(adminToken, accept)

        then: "contains the federated user"
        User fedUser = userList.find() {it.id == authResponse.user.id}
        fedUser != null
        fedUser.federatedIdp == Constants.DEFAULT_IDP_URI

        and: "contains the user-admin"
        userList.id.contains(userAdmin.id)
        userList.size() == 2

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)

        where:
        accept << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    def "identity and service admins are able to see federated users and provisioned users in the list user call"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def userAdminToken = utils.getToken(userAdmin.username)
        def disabledUser = utils.createUser(userAdminToken, testUtils.getRandomUUID(), userAdmin.domainId)
        utils.disableUser(disabledUser)

        def serviceAdmin = utils.createServiceAdmin()
        def serviceAdminToken = utils.getToken(serviceAdmin.username)
        utils.addUserToDomain(serviceAdminToken, serviceAdmin.id, domainId)

        def identityAdmin = utils.createIdentityAdmin()
        utils.addUserToDomain(serviceAdminToken, identityAdmin.id, domainId)
        def identityAdminToken = utils.getToken(identityAdmin.username)

        when: "list users with service admin token"
        def listUsersResponse = cloud20.listUsers(serviceAdminToken, "0", "100000000")

        then: "request is successful"
        listUsersResponse.status == 200
        def userList = listUsersResponse.getEntity(UserList).value

        and: "contains the federated user"
        userList.user.id.contains(authResponse.user.id)

        and: "contains the user-admin"
        userList.user.id.contains(userAdmin.id)

        and: "contain the disabled user"
        userList.user.id.contains(disabledUser.id)

        when: "list users with identity admin token"
        listUsersResponse = cloud20.listUsers(identityAdminToken, "0", "100000000")

        then: "request is successful"
        listUsersResponse.status == 200
        def userList2 = listUsersResponse.getEntity(UserList).value

        and: "contains the federated user"
        userList2.user.id.contains(authResponse.user.id)

        and: "contains the user-admin"
        userList2.user.id.contains(userAdmin.id)

        and: "contains the disabled user"
        userList2.user.id.contains(disabledUser.id)

        when: "list users in domain"
        def usersInDomain = cloud20.listUsersInDomain(identityAdminToken, domainId, "VERIFIED").getEntity(UserList).value

        then: "see federated user"
        usersInDomain.user.id.contains(authResponse.user.id)

        when: "list enabled users in domain"
        def enabledUsersInDomain = cloud20.listUsersInDomain(identityAdminToken, domainId, "VERIFIED", true).getEntity(UserList).value

        then: "see federated user"
        enabledUsersInDomain.user.id.contains(authResponse.user.id)

        when: "list disabled users in domain"
        def disabledUsersInDomain = cloud20.listUsersInDomain(identityAdminToken, domainId, "VERIFIED",false).getEntity(UserList).value

        then: "no federated user"
        !disabledUsersInDomain.user.id.contains(authResponse.user.id)

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUser(disabledUser)
        utils.deleteUsers(users)
    }

    @Ignore("This test will currently fail due to federated users not being deleted when a domain is disabled. This test should pass once these updates are made")
    def "federated users in disabled domain are not returned in list users call"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def serviceAdminToken = utils.getServiceAdminToken()

        when: "list users"
        def listUsersResponse = cloud20.listUsers(serviceAdminToken, "0", "100000")

        then: "request is successful"
        listUsersResponse.status == 200
        def userList = listUsersResponse.getEntity(UserList).value

        and: "contains the federated user"
        userList.user.id.contains(authResponse.user.id)

        and: "contains the user-admin"
        userList.user.id.contains(userAdmin.id)

        when: "disable the domain for the federated user"
        def domainToUpdate = v1Factory.createDomain().with {
            it.id = userAdmin.domainId
            it.enabled = false
            it
        }
        utils.updateDomain(userAdmin.domainId, domainToUpdate)
        listUsersResponse = cloud20.listUsers(serviceAdminToken, "0", "100000")

        then: "request is successful"
        listUsersResponse.status == 200
        def userList2 = listUsersResponse.getEntity(UserList).value

        and: "does not contain the federated user"
        !userList2.user.id.contains(authResponse.user.id)

        and: "contains the user-admin"
        userList2.user.id.contains(userAdmin.id)

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    @Ignore("This test will currently fail due to federated users not being deleted when a domain has its last user admin disabled. This test should pass once these updates are made")
    def "federated users with all user admins in domain are disabled should not be returned in list users call"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, userAdmin2, users, users2
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        (userAdmin2, users2) = utils.createUserAdmin(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def serviceAdminToken = utils.getServiceAdminToken()

        when: "list users"
        def listUsersResponse = cloud20.listUsers(serviceAdminToken, "0", "100000")

        then: "request is successful"
        listUsersResponse.status == 200
        def userList = listUsersResponse.getEntity(UserList).value

        and: "contains the federated user"
        userList.user.id.contains(authResponse.user.id)

        and: "contains the first user-admin"
        userList.user.id.contains(userAdmin.id)

        and: "contains the other user-admin"
        userList.user.id.contains(userAdmin2.id)

        when: "disable the first user admin for the federated user's domain"
        utils.disableUser(userAdmin)
        listUsersResponse = cloud20.listUsers(serviceAdminToken, "0", "100000")

        then: "request is successful"
        listUsersResponse.status == 200
        def userList2 = listUsersResponse.getEntity(UserList).value

        and: "contains the federated user"
        userList2.user.id.contains(authResponse.user.id)

        and: "does not contain the first user-admin"
        !userList2.user.id.contains(userAdmin.id)

        and: "contains the other user-admin"
        userList2.user.id.contains(userAdmin2.id)

        when: "disable the other user admin for the federated user's domain"
        utils.disableUser(userAdmin2)
        listUsersResponse = cloud20.listUsers(serviceAdminToken, "0", "100000")

        then: "request is successful"
        listUsersResponse.status == 200
        def userList3 = listUsersResponse.getEntity(UserList).value

        and: "does not contain the federated user"
        !userList3.user.id.contains(authResponse.user.id)

        and: "does not contain the first user-admin"
        !userList3.user.id.contains(userAdmin.id)

        and: "does not contain the other user-admin"
        !userList3.user.id.contains(userAdmin2.id)

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
        utils.deleteUsers(users2)
        staticIdmConfiguration.reset()
    }

    @Unroll
    def "list users in domain returns federated and provisioned users: accept = #accept"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def username2 = testUtils.getRandomUUID("userForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def samlAssertion2 = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username2, expSecs, domainId2, null, email);
        def userAdmin, userAdmin2, users, users2
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        (userAdmin2, users2) = utils.createUserAdminWithTenants(domainId2)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def samlResponse2 = cloud20.samlAuthenticate(samlAssertion2)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def AuthenticateResponse authResponse2 = samlResponse2.getEntity(AuthenticateResponse).value
        def serviceAdminToken = utils.getServiceAdminToken()

        when: "list users with domain id (not specifying enabled flag)"
        def response = cloud20.getUsersByDomainId(serviceAdminToken, domainId, accept)

        then: "the request was successful"
        response.status == 200
        def userList
        def userIds
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            userList = response.getEntity(UserList).value.user
            userIds = userList.id
        } else {
            userList = new JsonSlurper().parseText(response.getEntity(String))['users']
            userIds = userList['id']
        }

        and: "returns the federated user for the domain"
        def federatedUser
        for (def curUser : userList) {
            if (curUser.id == authResponse.user.id) {
                federatedUser = curUser
            }
        }
        userIds.contains(authResponse.user.id)

        and: "federated user has the RAX-AUTH:federatedIdp attribute"
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            assert federatedUser.federatedIdp == Constants.DEFAULT_IDP_URI
        } else {
            assert federatedUser.'RAX-AUTH:federatedIdp' == Constants.DEFAULT_IDP_URI
        }

        and: "returns the provisioned user for the domain"
        userIds.contains(userAdmin.id)

        and: "does not return the provisioned user in the other domain"
        !userIds.contains(authResponse2.user.id)

        and: "does not return the federated user in the other domain"
        !userIds.contains(userAdmin2.id)

        cleanup:
        deleteFederatedUserQuietly(username)
        deleteFederatedUserQuietly(username2)
        utils.deleteUsers(users)
        utils.deleteUsers(users2)

        where:
        accept | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    @Unroll
    def "list users in domain and with enabled returns federated and provisioned users: accept = #accept"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)
        def domainId = utils.createDomain()
        def domainId2 = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def username2 = testUtils.getRandomUUID("userForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def samlAssertion2 = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username2, expSecs, domainId2, null, email);
        def userAdmin, userAdmin2, users, users2
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        (userAdmin2, users2) = utils.createUserAdminWithTenants(domainId2)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def samlResponse2 = cloud20.samlAuthenticate(samlAssertion2)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def AuthenticateResponse authResponse2 = samlResponse2.getEntity(AuthenticateResponse).value
        def serviceAdminToken = utils.getServiceAdminToken()

        when: "list users with domain id"
        def response = cloud20.getUsersByDomainIdAndEnabledFlag(serviceAdminToken, domainId, true, accept)

        then: "the request was successful"
        response.status == 200
        def userList
        def userIds
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            userList = response.getEntity(UserList).value.user
            userIds = userList.id
        } else {
            userList = new JsonSlurper().parseText(response.getEntity(String))['users']
            userIds = userList['id']
        }

        and: "returns the federated user for the domain"
        def federatedUser
        for (def curUser : userList) {
            if (curUser.id == authResponse.user.id) {
                federatedUser = curUser
            }
        }
        userIds.contains(authResponse.user.id)

        and: "federated user has the RAX-AUTH:federatedIdp attribute"
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            assert federatedUser.federatedIdp == Constants.DEFAULT_IDP_URI
        } else {
            assert federatedUser.'RAX-AUTH:federatedIdp' == Constants.DEFAULT_IDP_URI
        }

        and: "returns the provisioned user for the domain"
        userIds.contains(userAdmin.id)

        and: "does not return the provisioned user in the other domain"
        !userIds.contains(authResponse2.user.id)

        and: "does not return the federated user in the other domain"
        !userIds.contains(userAdmin2.id)

        cleanup:
        deleteFederatedUserQuietly(username)
        deleteFederatedUserQuietly(username2)
        utils.deleteUsers(users)
        utils.deleteUsers(users2)

        where:
        accept | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def "list users in domain returns enabled or disabled users based on enabled flag"() {
        given:
        staticIdmConfiguration.setProperty("domain.restricted.to.one.user.admin.enabled", false)
        def domainId = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def userAdminToken = utils.getToken(userAdmin.username)
        def disabledUser = utils.createUser(userAdminToken)
        utils.disableUser(disabledUser)

        when: "list disabled users with domain id"
        def response = cloud20.getUsersByDomainIdAndEnabledFlag(utils.getServiceAdminToken(), domainId, false)
        def userList = response.getEntity(UserList).value.user

        then: "the disabled user was returned"
        userList.id.contains(disabledUser.id)

        and: "the enabled user was not returned"
        !userList.id.contains(userAdmin.id)

        when: "list enabled users with domain id"
        response = cloud20.getUsersByDomainIdAndEnabledFlag(utils.getServiceAdminToken(), domainId, true)
        userList = response.getEntity(UserList).value.user

        then: "the disabled user was not returned"
        !userList.id.contains(disabledUser.id)

        and: "the enabled user was returned"
        userList.id.contains(userAdmin.id)

        cleanup:
        utils.deleteUser(disabledUser)
        utils.deleteUsers(users)
    }

    @Unroll
    def "Assert contactId is exposed on list users when using correct access level, accept = #acceptContentType"() {
        given:
        def domainId = utils.createDomain()
        def contactId = testUtils.getRandomUUID("contactId")
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin]

        when: "Update users contactId"
        for(def user : users) {
            def userForUpdate = new User().with {
                it.contactId = contactId
                it
            }
            cloud20.updateUser(utils.getIdentityAdminToken(), user.id, userForUpdate)
        }

        then: "List Users"
        for (def user : users) {
            def token = utils.getToken(user.username)
            def listUsersResponse = cloud20.listUsers(token, acceptContentType)
            listUsersResponse.status == 200
            def userList = getUsersFromListUsers(listUsersResponse)
            for(def usr : userList.user) {
                if (acceptContentType == MediaType.APPLICATION_JSON_TYPE){
                    assert usr["RAX-AUTH:contactId"] == contactId
                } else {
                    assert usr.contactId == contactId
                }
            }
        }

        cleanup:
        utils.deleteUsers(users)

        where:
        acceptContentType               | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def "List users - users filtered by caller type"() {
        given:

        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def userManage2 = utils.createUserWithUser(userAdmin, testUtils.getRandomUUID(), domainId)
        utils.addRoleToUser(userManage2, Constants.USER_MANAGE_ROLE_ID)
        def users = [defaultUser, userManage, userAdmin, userManage2]

        when: "User-admin"
        def returnedUsers = utils.listUsers(utils.getToken(userAdmin.username))

        then: "new behavior filter still returns admin and all managers"
        returnedUsers.find {it.id == userAdmin.id} != null
        returnedUsers.find {it.id == userManage.id} != null
        returnedUsers.find {it.id == userManage2.id} != null
        returnedUsers.find {it.id == defaultUser.id} != null

        when: "User-manager"
        returnedUsers = utils.listUsers(utils.getToken(userManage.username))

        then: "new behavior filters user-admins"
        returnedUsers.find {it.id == userAdmin.id} == null
        returnedUsers.find {it.id == userManage.id} != null
        returnedUsers.find {it.id == userManage2.id} != null
        returnedUsers.find {it.id == defaultUser.id} != null

        when: "default user w/ flag enabled"
        returnedUsers = utils.listUsers(utils.getToken(defaultUser.username))

        then: "new behavior filters all users but caller"
        returnedUsers.find {it.id == userAdmin.id} == null
        returnedUsers.find {it.id == userManage.id} == null
        returnedUsers.find {it.id == userManage2.id} == null
        returnedUsers.find {it.id == defaultUser.id} != null

        cleanup:
        utils.deleteUsersQuietly(users)
    }

    def "List users by email - users filtered by caller type"() {
        given:

        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def userManage2 = utils.createUserWithUser(userAdmin, testUtils.getRandomUUID(), domainId)
        utils.addRoleToUser(userManage2, Constants.USER_MANAGE_ROLE_ID)
        def users = [defaultUser, userManage, userAdmin, userManage2]

        def commonEmail = testUtils.getRandomUUID() + "@rackspace.com"
        users.each {
            it.email = commonEmail
            def updatedUser = utils.updateUser(it, it.id)
        }

        when: "User-admin"
        def returnedUsers = utils.getUsersByEmail(commonEmail, utils.getToken(userAdmin.username))

        then: "new behavior filter still returns all users in domain"
        returnedUsers.find {it.id == userAdmin.id} != null
        returnedUsers.find {it.id == userManage.id} != null
        returnedUsers.find {it.id == userManage2.id} != null
        returnedUsers.find {it.id == defaultUser.id} != null

        when: "User-manager"
        returnedUsers = utils.getUsersByEmail(commonEmail, utils.getToken(userManage.username))

        then: "new behavior filters out user-admins"
        returnedUsers.find {it.id == userAdmin.id} == null
        returnedUsers.find {it.id == userManage.id} != null
        returnedUsers.find {it.id == userManage2.id} != null
        returnedUsers.find {it.id == defaultUser.id} != null

        when: "default user"
        def response = cloud20.getUsersByEmail(utils.getToken(defaultUser.username), commonEmail)

        then: "default users still not allowed to use"
        response.status == SC_FORBIDDEN

        cleanup:
        utils.deleteUsersQuietly(users)
    }

    // This test assumes that users already exist in the backend.
    def "list users restricts users to own domain"() {
        given:
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        // Move the identity admin to the same domain as the other users
        utils.addUserToDomain(identityAdmin.id, domainId)
        identityAdmin = utils.getUserByName(identityAdmin.username)
        def serviceAdminToken = utils.getToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        def identityAdminToken = utils.getToken(identityAdmin.username)
        def userAdminToken = utils.getToken(userAdmin.username)
        def serviceAdmin = utils.getUserByName(Constants.SERVICE_ADMIN_USERNAME, serviceAdminToken)
        assert serviceAdmin.domainId != null

        when: "Own domain feature enabled"
        List<User> saListEnabled = utils.listUsers(serviceAdminToken)
        List<User> iaListEnabled = utils.listUsers(identityAdminToken)
        List<User> uaListEnabled = utils.listUsers(userAdminToken)

        then: "saList contains only users within own domain"
        // Number of users is unknown, but verifying that all exist within own domain is sufficient
        saListEnabled.find {it.domainId != serviceAdmin.domainId} == null

        and: "iaList contains only users within own domain"
        iaListEnabled.size() == 4
        iaListEnabled.find { it.domainId != identityAdmin.domainId } == null

        and: "uaList still contains all 4 users in own domain, including identity admin"
        uaListEnabled.size() == 4
        uaListEnabled.find { it.id == identityAdmin.id } != null
        uaListEnabled.find { it.id == userManage.id } != null
        uaListEnabled.find { it.id == defaultUser.id } != null
        uaListEnabled.find { it.id == defaultUser.id } != null
    }

    def "List users by name - filtered by caller type"() {
        given:

        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def userManage2 = utils.createUserWithUser(userAdmin, testUtils.getRandomUUID(), domainId)
        utils.addRoleToUser(userManage2, Constants.USER_MANAGE_ROLE_ID)
        def users = [defaultUser, userManage, userAdmin, userManage2]

        when: "User-admin"
        def token = utils.getToken(userAdmin.username)
        def userAdminResponse = cloud20.getUserByName(token, userAdmin.username)
        def userManageResponse = cloud20.getUserByName(token, userManage.username)
        def userManage2Response = cloud20.getUserByName(token, userManage2.username)
        def defaultUserResponse = cloud20.getUserByName(token, defaultUser.username)

        then: "new behavior returns all users"
        userAdminResponse.status == SC_OK
        userManageResponse.status == SC_OK
        userManage2Response.status == SC_OK
        defaultUserResponse.status == SC_OK

        when: "User-manager"
        token = utils.getToken(userManage.username)
        userAdminResponse = cloud20.getUserByName(token, userAdmin.username)
        userManageResponse = cloud20.getUserByName(token, userManage.username)
        userManage2Response = cloud20.getUserByName(token, userManage2.username)
        defaultUserResponse = cloud20.getUserByName(token, defaultUser.username)

        then: "new behavior only allows self, default users and user-manage"
        userAdminResponse.status == SC_FORBIDDEN
        userManageResponse.status == SC_OK
        userManage2Response.status == SC_OK
        defaultUserResponse.status == SC_OK

        when: "default user"
        token = utils.getToken(defaultUser.username)
        userAdminResponse = cloud20.getUserByName(token, userAdmin.username)
        userManageResponse = cloud20.getUserByName(token, userManage.username)
        userManage2Response = cloud20.getUserByName(token, userManage2.username)
        defaultUserResponse = cloud20.getUserByName(token, defaultUser.username)

        then: "new behavior returns only self"
        userAdminResponse.status == SC_FORBIDDEN
        userManageResponse.status == SC_FORBIDDEN
        userManage2Response.status == SC_FORBIDDEN
        defaultUserResponse.status == SC_OK

        cleanup:
        utils.deleteUsersQuietly(users)
    }

    /**
     * This test assumes the 'test.impersonate' base racker use has the impersonation `role cloud-identity-impersonate`
     * which is mapped via implicit role mappings to include the `v2_0_list_users_global` identity role.
     */
    def "listUsers: Rackers w/ implicit role identity:v2_0_list_users_global can call list users"() {
        given:
        def userAdmin = utils.createCloudAccount(utils.getIdentityAdminToken())
        def domainId = userAdmin.domainId

        def rackerToken = utils.authenticateRacker(Constants.RACKER_IMPERSONATE, Constants.RACKER_IMPERSONATE_PASSWORD).token.id
        def rackerNoAccessToken = utils.authenticateRacker(Constants.RACKER_NOGROUP, Constants.RACKER_NOGROUP_PASSWORD).token.id

        when: "Get user by name"
        def response = cloud20.getUserByName(rackerToken, userAdmin.username)
        def responseNoAccess = cloud20.getUserByName(rackerNoAccessToken, userAdmin.username)

        then: "Allowed"
        response.status == SC_OK

        and: "Racker w/o implicit role is denied"
        responseNoAccess.status == SC_FORBIDDEN

        when: "Get user by email"
        response = cloud20.getUsersByEmail(rackerToken, userAdmin.email)
        responseNoAccess = cloud20.getUsersByEmail(rackerNoAccessToken, userAdmin.email)

        then: "Allowed"
        response.status == SC_OK

        and: "Racker w/o implicit role is denied"
        responseNoAccess.status == SC_FORBIDDEN

        when: "Get user by domainId"
        ListUsersSearchParams params = new ListUsersSearchParams(null, null, null, domainId, null, null, null)
        response = cloud20.listUsersWithSearchParams(rackerToken, params)
        responseNoAccess = cloud20.listUsersWithSearchParams(rackerNoAccessToken, params)

        then: "Allowed"
        response.status == SC_OK

        and: "Racker w/o implicit role is denied"
        responseNoAccess.status == SC_FORBIDDEN

        when: "Get users by tenantId"
        params = new ListUsersSearchParams(null, null, domainId, null, null, null, null)
        response = cloud20.listUsersWithSearchParams(rackerToken, params)
        responseNoAccess = cloud20.listUsersWithSearchParams(rackerNoAccessToken, params)

        then: "Allowed"
        response.status == SC_OK

        and: "Racker w/o implicit role is denied"
        responseNoAccess.status == SC_FORBIDDEN

        when: "Get admin for domain"
        params = new ListUsersSearchParams(null, null, null, domainId, true, null, null)
        response = cloud20.listUsersWithSearchParams(rackerToken, params)
        responseNoAccess = cloud20.listUsersWithSearchParams(rackerNoAccessToken, params)

        then: "Allowed"
        response.status == SC_OK

        and: "Racker w/o implicit role is denied"
        responseNoAccess.status == SC_FORBIDDEN
    }

    def "listUsers: Federated racker w/ implicit identity:v2_0_list_users_global can call list users"() {
        given:
        def userAdmin = utils.createCloudAccount()
        def domainId = userAdmin.domainId
        AuthenticateResponse fedResponse = utils.authenticateFederatedRacker(Constants.RACKER_IMPERSONATE)

        def fedToken = fedResponse.token.id

        when: "Get user by name"
        def response = cloud20.getUserByName(fedToken, userAdmin.username)

        then: "Allowed"
        response.status == SC_OK

        when: "Get user by email"
        response = cloud20.getUsersByEmail(fedToken, userAdmin.email)

        then: "Allowed"
        response.status == SC_OK

        when: "Get user by domainId"
        ListUsersSearchParams params = new ListUsersSearchParams(null, null, null, domainId, null, null, null)
        response = cloud20.listUsersWithSearchParams(fedToken, params)

        then: "Allowed"
        response.status == SC_OK

        when: "Get users by tenantId"
        params = new ListUsersSearchParams(null, null, domainId, null, null, null, null)
        response = cloud20.listUsersWithSearchParams(fedToken, params)

        then: "Allowed"
        response.status == SC_OK

        when: "Get admin for domain"
        params = new ListUsersSearchParams(null, null, null, domainId, true, null, null)
        response = cloud20.listUsersWithSearchParams(fedToken, params)

        then: "Allowed"
        response.status == SC_OK
    }

    @Unroll
    def "get admins for user: feature.enable.user.admin.look.up.by.domain = #featureEnabled"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_ADMIN_LOOK_UP_BY_DOMAIN_PROP, featureEnabled)
        def domainId = utils.createDomain()
        def identityAdmin, userAdmin, userManage, defaultUser
        (identityAdmin, userAdmin, userManage, defaultUser) = utils.createUsers(domainId)
        def users = [defaultUser, userManage, userAdmin, identityAdmin]

        when:
        def response = cloud20.getAdminsForUser(utils.identityAdminToken, defaultUser.id)
        UserList userList = response.getEntity(UserList).value

        then:
        response.status == SC_OK
        userList.user.size() == 1
        userList.user.get(0).id == userAdmin.id

        cleanup:
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(domainId)
        reloadableConfiguration.reset()

        where:
        featureEnabled << [true, false]
    }

    def getUsersFromListUsers(response) {
        if(response.getType() == MediaType.APPLICATION_XML_TYPE) {
            return response.getEntity(UserList).value
        }

        UserList userList = new UserList()
        userList.user.addAll(new JsonSlurper().parseText(response.getEntity(String))["users"])

        return userList
    }

    def deleteFederatedUserQuietly(username) {
        try {
            def federatedUser = federatedUserRepository.getUserByUsernameForIdentityProviderId(username, Constants.DEFAULT_IDP_ID)
            if (federatedUser != null) {
                federatedUserRepository.deleteUser(federatedUser)
            }
        } catch (Exception e) {
            //eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", username), e)
        }
    }
}
