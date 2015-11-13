package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.config.RepositoryProfileResolver
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import com.rackspace.idm.domain.dao.FederatedUserDao
import groovy.json.JsonSlurper
import org.apache.log4j.Logger
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.UserList
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory

import javax.ws.rs.core.MediaType

import static com.rackspace.idm.Constants.*

class ListUsersIntegrationTest extends RootIntegrationTest {

    @Autowired
    FederatedUserDao federatedUserRepository

    private static final Logger LOG = Logger.getLogger(ListUsersIntegrationTest.class)

    def "federated user can call list users"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def expDays = 500
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
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
        def expDays = 500
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion1 = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username1, expDays, domainId1, null, email);
        def samlAssertion2 = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username2, expDays, domainId2, null, email);
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

    def "user admin with federated user in domain see federated user in list users call"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def expDays = 500
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def adminToken = utils.getToken(userAdmin.username)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value

        when: "list users with user admin token"
        def listUsersResponse = cloud20.listUsers(adminToken)

        then: "request is successful"
        listUsersResponse.status == 200
        def userList = listUsersResponse.getEntity(UserList).value

        and: "contains the federated user"
        userList.user.id.contains(authResponse.user.id)

        and: "contains the user-admin"
        userList.user.id.contains(userAdmin.id)
        userList.user.size() == 2

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)
    }

    def "identity and service admins are able to see federated users and provisioned users in the list user call"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def expDays = 500
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def userAdminToken = utils.getToken(userAdmin.username)
        def disabledUser = utils.createUser(userAdminToken, testUtils.getRandomUUID(), userAdmin.domainId)
        utils.disableUser(disabledUser)
        def identityAdminToken = utils.getIdentityAdminToken()
        def serviceAdminToken = utils.getServiceAdminToken()

        when: "list users with service admin token"
        def listUsersResponse = cloud20.listUsers(serviceAdminToken, "0", "100000000")

        then: "request is successful"
        listUsersResponse.status == 200
        def userList = listUsersResponse.getEntity(UserList).value

        and: "contains the federated user"
        userList.user.id.contains(authResponse.user.id)

        and: "contains the user-admin"
        userList.user.id.contains(userAdmin.id)

        and: "does not contain the disabled user"
        !userList.user.id.contains(disabledUser.id)

        when: "list users with identity admin token"
        listUsersResponse = cloud20.listUsers(identityAdminToken, "0", "100000000")

        then: "request is successful"
        listUsersResponse.status == 200
        def userList2 = listUsersResponse.getEntity(UserList).value

        and: "contains the federated user"
        userList2.user.id.contains(authResponse.user.id)

        and: "contains the user-admin"
        userList2.user.id.contains(userAdmin.id)

        and: "does not contain the disabled user"
        !userList2.user.id.contains(disabledUser.id)

        when: "list users in domain"
        def usersInDomain = cloud20.listUsersInDomain(identityAdminToken, domainId).getEntity(UserList).value

        then: "see federated user"
        usersInDomain.user.id.contains(authResponse.user.id)

        when: "list enabled users in domain"
        def enabledUsersInDomain = cloud20.listUsersInDomain(identityAdminToken, domainId, true).getEntity(UserList).value

        then: "see federated user"
        enabledUsersInDomain.user.id.contains(authResponse.user.id)

        when: "list disabled users in domain"
        def disabledUsersInDomain = cloud20.listUsersInDomain(identityAdminToken, domainId, false).getEntity(UserList).value

        then: "no federated user"
        !disabledUsersInDomain.user.id.contains(authResponse.user.id)

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUser(disabledUser)
        utils.deleteUsers(users)
    }

    @Unroll
    def "list users contains federated IDP attribute for federated users: accept=#accept"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def expDays = 500
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        def AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def serviceAdminToken = utils.getServiceAdminToken()

        when:
        def listUsersResponse = cloud20.listUsers(serviceAdminToken, "0", "100000", accept)
        def userList
        if (accept == MediaType.APPLICATION_XML_TYPE) {
            userList = listUsersResponse.getEntity(UserList).value.user
        } else {
            userList = new JsonSlurper().parseText(listUsersResponse.getEntity(String))['users']
        }
        def federatedUser
        for (def curUser : userList) {
            if (curUser.id == authResponse.user.id) {
                federatedUser = curUser
            }
        }

        then:
        listUsersResponse.status == 200
        federatedUser != null
        if(accept == MediaType.APPLICATION_XML_TYPE) {
            assert federatedUser.federatedIdp == DEFAULT_IDP_URI
        } else {
            assert federatedUser.'RAX-AUTH:federatedIdp' == DEFAULT_IDP_URI
        }

        cleanup:
        deleteFederatedUserQuietly(username)
        utils.deleteUsers(users)

        where:
        accept | _
        MediaType.APPLICATION_XML_TYPE | _
        MediaType.APPLICATION_JSON_TYPE | _

    }

    @Ignore("This test will currently fail due to federated users not being deleted when a domain is disabled. This test should pass once these updates are made")
    def "federated users in disabled domain are not returned in list users call"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userForSaml")
        def expDays = 500
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
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
        def expDays = 500
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
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
        def expDays = 500
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def samlAssertion2 = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username2, expDays, domainId2, null, email);
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
            assert federatedUser.federatedIdp == DEFAULT_IDP_URI
        } else {
            assert federatedUser.'RAX-AUTH:federatedIdp' == DEFAULT_IDP_URI
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
        def expDays = 500
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username, expDays, domainId, null, email);
        def samlAssertion2 = new SamlFactory().generateSamlAssertionStringForFederatedUser(DEFAULT_IDP_URI, username2, expDays, domainId2, null, email);
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
            assert federatedUser.federatedIdp == DEFAULT_IDP_URI
        } else {
            assert federatedUser.'RAX-AUTH:federatedIdp' == DEFAULT_IDP_URI
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

    def deleteFederatedUserQuietly(username) {
        try {
            def federatedUser = federatedUserRepository.getUserByUsernameForIdentityProviderName(username, DEFAULT_IDP_NAME)
            if (federatedUser != null) {
                if (RepositoryProfileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
                    federatedUser = federatedUserRepository.findOneByUsernameAndFederatedIdpName(username, DEFAULT_IDP_NAME)
                    federatedUserRepository.delete(federatedUser)
                } else {
                    federatedUserRepository.deleteObject(federatedUser)
                }
            }
        } catch (Exception e) {
            //eat but log
            LOG.warn(String.format("Error cleaning up federatedUser with username '%s'", username), e)
        }
    }
}
