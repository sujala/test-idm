package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignment
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import org.apache.commons.lang.RandomStringUtils
import testHelpers.RootIntegrationTest

import static org.apache.http.HttpStatus.SC_NO_CONTENT


class CloudUserGroupResourceIntegrationTest extends RootIntegrationTest {
    void doSetupSpec() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)
    }

    def "Call add user to domain group service w/ an invalid(expired/revoked) token….expect a 401"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getServiceAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = "invalid"

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == 401

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
    }

    def "Call add user to domain group service w/ a token of deleted user….expect a 404"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID()
        def user = utils.createUser(utils.getServiceAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def deletedUsername = testUtils.getRandomUUID()
        def deletedUser = utils.createUser(token, deletedUsername, domainId)
        token = utils.getToken(deletedUsername, Constants.DEFAULT_PASSWORD)

        utils.deleteUser(deletedUser)

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == 404

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
    }

    def "Call add user to domain group service for a non-existing domain….expect a 404"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getServiceAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(token, "doesNotExist", userGroup.id, user.id)

        then:
        response.status == 404

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
        utils.deleteDomain(domainId)
    }

    def "Call add user to domain group service for a non-existing group….expect a 404"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getServiceAdminToken(), username, domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, "doesNotExist", user.id)

        then:
        response.status == 404

        cleanup:
        utils.deleteUser(user)
    }

    def "Call add user to domain group service for a non-existing provisioned user….expect a 404"() {
        given:
        def domainId = utils.createDomain()
        utils.createDomainEntity(domainId)
        def userGroup = utils.createUserGroup(domainId)

        when:
        def response = cloud20.addUserToUserGroup(utils.getIdentityAdminToken(), domainId, userGroup.id, "doesNotExist")

        then:
        response.status == 404

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteDomain(domainId)
    }

    def "Call add user to domain group service for a disabled provisioned user….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getServiceAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        utils.disableUser(user)

        when:
        def response = cloud20.addUserToUserGroup(utils.getServiceAdminToken(), domainId, userGroup.id, user.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
    }

    def "Call add user to domain group service using default user's token….expect a 403"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def defaultUsername = testUtils.getRandomUUID()
        def defaultUser = utils.createUser(token, defaultUsername, domainId)
        token = utils.getToken(defaultUsername, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == 403

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(defaultUser)
        utils.deleteUser(user)
    }

    def "Call add user to domain group service using user admin of other domain….expect a 403"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)

        def otherUsername = testUtils.getRandomUUID()
        def otherDomainId = utils.createDomain()
        def otherUser = utils.createUser(utils.getIdentityAdminToken(), otherUsername, otherDomainId)
        def token = utils.getToken(otherUsername, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == 403

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
        utils.deleteUser(otherUser)
    }

    def "Call add user to domain group service using user manager of other domain….expect a 403"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)

        def otherUsername = testUtils.getRandomUUID()
        def otherDomainId = utils.createDomain()
        def otherUser = utils.createUser(utils.getIdentityAdminToken(), otherUsername, otherDomainId)
        def token = utils.getToken(otherUsername, Constants.DEFAULT_PASSWORD)

        def userManagedUsername = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(token, userManagedUsername, domainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)
        token = utils.getToken(userManagedUsername, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == 403

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(user)
        utils.deleteUser(otherUser)
    }

    def "Call add user to domain group service using rcn admin, not having the domain in the URL as one of domains for its RCN.….expect a 403"() {
        given:
        def username = testUtils.getRandomUUID()
        def rcn = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId = utils.createDomain()
        def rcnDomain = v1Factory.createDomain(rcnDomainId, rcnDomainId)
        rcnDomain.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain)
        def user = utils.createUser(utils.getIdentityAdminToken(), username, rcnDomainId)
        def userGroup = utils.createUserGroup(rcnDomainId)

        def username2 = testUtils.getRandomUUID()
        def rcn2 = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId2 = utils.createDomain()
        def rcnDomain2 = v1Factory.createDomain(rcnDomainId2, rcnDomainId2)
        rcnDomain2.rackspaceCustomerNumber = rcn2
        utils.createDomain(rcnDomain2)
        def user2 = utils.createUser(utils.getIdentityAdminToken(), username2, rcnDomainId2)
        utils.addRoleToUser(user2, Constants.RCN_ADMIN_ROLE_ID)
        def token = utils.getToken(username2, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(token, rcnDomainId, userGroup.id, user.id)

        then:
        response.status == 403

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
        utils.deleteUser(user2)
    }

    def "Call add user to domain group service for a federated user….expect a 403"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)
        def federatedUser = utils.createFederatedUser(domainId)

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, federatedUser.id)

        then:
        response.status == 403

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
        utils.deleteUser(federatedUser)
    }

    def "Call add user to domain group service for a user admin of group's domain, using the same user's token….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
    }

    def "Call add user to domain group service for a user admin of group's domain, using the user manager's token….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def userManagedUsername = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(token, userManagedUsername, domainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)
        token = utils.getToken(userManagedUsername, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(user)
    }

    def "Call add user to domain group service for a user admin of group's domain, using the token of rcn-admin and rcn-admin's domain is same as group's domain….expect a 204"() {
        given:
        def rcn = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId = utils.createDomain()
        def rcnDomain = v1Factory.createDomain(rcnDomainId, rcnDomainId)
        rcnDomain.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain)

        def rcnAdminUsername = testUtils.getRandomUUID()
        def rcnAdmin = utils.createUser(utils.getIdentityAdminToken(), rcnAdminUsername, rcnDomainId)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)
        def rcnAdminToken = utils.getToken(rcnAdminUsername, Constants.DEFAULT_PASSWORD)

        def username = testUtils.getRandomUUID()
        def user = utils.createUser(rcnAdminToken, username, rcnDomainId)

        def userGroup = utils.createUserGroup(rcnDomainId)

        when:
        def response = cloud20.addUserToUserGroup(rcnAdminToken, rcnDomainId, userGroup.id, user.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
        utils.deleteUser(rcnAdmin)
    }

    def "Call add user to domain group service for a user admin of group's domain, using rcn-admin's token and rcn-admin's domain is NOT same as group's domain but the group's domain belongs to rcn-admin's RCN….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def rcn = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId = utils.createDomain()
        def rcnDomain = v1Factory.createDomain(rcnDomainId, rcnDomainId)
        rcnDomain.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain)
        def user = utils.createUser(utils.getIdentityAdminToken(), username, rcnDomainId)
        def userGroup = utils.createUserGroup(rcnDomainId)

        def username2 = testUtils.getRandomUUID()
        def rcnDomainId2 = utils.createDomain()
        def rcnDomain2 = v1Factory.createDomain(rcnDomainId2, rcnDomainId2)
        rcnDomain2.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain2)
        def user2 = utils.createUser(utils.getIdentityAdminToken(), username2, rcnDomainId2)
        utils.addRoleToUser(user2, Constants.RCN_ADMIN_ROLE_ID)
        def token = utils.getToken(username2, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(token, rcnDomainId, userGroup.id, user.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
        utils.deleteUser(user2)
    }

    def "Call add user to domain group service for a user manager of group's domain, using the same user's token….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def userManagedUsername = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(token, userManagedUsername, domainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)
        token = utils.getToken(userManagedUsername, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, userManagedUser.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(user)
    }

    def "Call add user to domain group service for a user manager of group's domain, using the user manager's token….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def userManagedUsername = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(token, userManagedUsername, domainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)
        token = utils.getToken(userManagedUsername, Constants.DEFAULT_PASSWORD)

        def otherUsername = testUtils.getRandomUUID()
        def otherDomainId = utils.createDomain()
        def otherUser = utils.createUser(token, otherUsername, otherDomainId)
        utils.addRoleToUser(otherUser, Constants.USER_MANAGE_ROLE_ID)

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, otherUser.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(otherUser)
        utils.deleteUser(user)
    }

    def "Call add user to domain group service for a user manager of group's domain, using the token of rcn-admin and rcn-admin's domain is same as group's domain….expect a 204"() {
        given:
        def rcn = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId = utils.createDomain()
        def rcnDomain = v1Factory.createDomain(rcnDomainId, rcnDomainId)
        rcnDomain.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain)

        def rcnAdminUsername = testUtils.getRandomUUID()
        def rcnAdmin = utils.createUser(utils.getIdentityAdminToken(), rcnAdminUsername, rcnDomainId)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)
        def rcnAdminToken = utils.getToken(rcnAdminUsername, Constants.DEFAULT_PASSWORD)

        def username = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(rcnAdminToken, username, rcnDomainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)

        def userGroup = utils.createUserGroup(rcnDomainId)

        when:
        def response = cloud20.addUserToUserGroup(rcnAdminToken, rcnDomainId, userGroup.id, userManagedUser.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(rcnAdmin)
    }

    def "Call add user to domain group service for a user manager of group's domain, using rcn-admin's token and rcn-admin's domain is NOT same as group's domain but the group's domain belongs to rcn-admin's RCN….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def rcn = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId = utils.createDomain()
        def rcnDomain = v1Factory.createDomain(rcnDomainId, rcnDomainId)
        rcnDomain.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain)
        def user = utils.createUser(utils.getIdentityAdminToken(), username, rcnDomainId)
        def userGroup = utils.createUserGroup(rcnDomainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def userManagedUsername = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(token, userManagedUsername, rcnDomainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)

        def username2 = testUtils.getRandomUUID()
        def rcnDomainId2 = utils.createDomain()
        def rcnDomain2 = v1Factory.createDomain(rcnDomainId2, rcnDomainId2)
        rcnDomain2.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain2)
        def user2 = utils.createUser(utils.getIdentityAdminToken(), username2, rcnDomainId2)
        utils.addRoleToUser(user2, Constants.RCN_ADMIN_ROLE_ID)
        def rcnAdminToken = utils.getToken(username2, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(rcnAdminToken, rcnDomainId, userGroup.id, userManagedUser.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(user)
        utils.deleteUser(user2)
    }

    def "Call remove user from domain group service for a default user of group's domain, using the same user's token….expect a 403"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def defaultUsername = testUtils.getRandomUUID()
        def defaultUser = utils.createUser(token, defaultUsername, domainId)
        token = utils.getToken(defaultUsername, Constants.DEFAULT_PASSWORD)

        when:
        cloud20.addUserToUserGroup(token, domainId, userGroup.id, defaultUser.id)
        def response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, defaultUser.id)

        then:
        response.status == 403

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(defaultUser)
        utils.deleteUser(user)
    }

    def "Call add user to domain group service for a default user of group's domain, using the user manager's token….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def userManagedUsername = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(token, userManagedUsername, domainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)
        token = utils.getToken(userManagedUsername, Constants.DEFAULT_PASSWORD)

        def defaultUsername = testUtils.getRandomUUID()
        def defaultUser = utils.createUser(token, defaultUsername, domainId)

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, defaultUser.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(defaultUser)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(user)
    }

    def "Call add user to domain group service for a default user of group's domain, using the token of rcn-admin and rcn-admin's domain is same as group's domain….expect a 204"() {
        given:
        def rcn = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId = utils.createDomain()
        def rcnDomain = v1Factory.createDomain(rcnDomainId, rcnDomainId)
        rcnDomain.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain)

        def rcnAdminUsername = testUtils.getRandomUUID()
        def rcnAdmin = utils.createUser(utils.getIdentityAdminToken(), rcnAdminUsername, rcnDomainId)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)
        def rcnAdminToken = utils.getToken(rcnAdminUsername, Constants.DEFAULT_PASSWORD)

        def username = testUtils.getRandomUUID()
        def user = utils.createUser(rcnAdminToken, username, rcnDomainId)

        def userGroup = utils.createUserGroup(rcnDomainId)

        when:
        def response = cloud20.addUserToUserGroup(rcnAdminToken, rcnDomainId, userGroup.id, user.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
        utils.deleteUser(rcnAdmin)
    }

    def "Call add user to domain group service for a default user of group's domain, using rcn-admin's token and rcn-admin's domain is NOT same as group's domain but the group's domain belongs to rcn-admin's RCN….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def rcn = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId = utils.createDomain()
        def rcnDomain = v1Factory.createDomain(rcnDomainId, rcnDomainId)
        rcnDomain.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain)
        def user = utils.createUser(utils.getIdentityAdminToken(), username, rcnDomainId)
        def userGroup = utils.createUserGroup(rcnDomainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def defaultUsername = testUtils.getRandomUUID()
        def defaultUser = utils.createUser(token, defaultUsername, rcnDomainId)

        def username2 = testUtils.getRandomUUID()
        def rcnDomainId2 = utils.createDomain()
        def rcnDomain2 = v1Factory.createDomain(rcnDomainId2, rcnDomainId2)
        rcnDomain2.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain2)
        def user2 = utils.createUser(utils.getIdentityAdminToken(), username2, rcnDomainId2)
        utils.addRoleToUser(user2, Constants.RCN_ADMIN_ROLE_ID)
        token = utils.getToken(username2, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(token, rcnDomainId, userGroup.id, defaultUser.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(defaultUser)
        utils.deleteUser(user)
        utils.deleteUser(user2)
    }

    def "Call add user to domain group service for a rcn admin of group's domain, using the same user's token….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def rcnAdminUsername = testUtils.getRandomUUID()
        def rcnAdmin = utils.createUser(token, rcnAdminUsername, domainId)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)
        def rcnAdminToken = utils.getToken(rcnAdminUsername, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(rcnAdminToken, domainId, userGroup.id, rcnAdmin.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(rcnAdmin)
        utils.deleteUser(user)
    }

    def "Call add user to domain group service for a rcn admin of group's domain, using the user manager's token belonging to rcn-admin's domain….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def rcnAdminUsername = testUtils.getRandomUUID()
        def rcnAdmin = utils.createUser(token, rcnAdminUsername, domainId)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)

        def userManagedUsername = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(token, userManagedUsername, domainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)
        token = utils.getToken(userManagedUsername, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, rcnAdmin.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(rcnAdmin)
        utils.deleteUser(user)
    }

    def "Call add user to domain group service for a rcn admin of group's domain, using the token of user-admin and user-admin's domain is same as rcn-admin's domain….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def rcnAdminUsername = testUtils.getRandomUUID()
        def rcnAdmin = utils.createUser(token, rcnAdminUsername, domainId)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, rcnAdmin.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(rcnAdmin)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service w/ an invalid(expired/revoked) token….expect a 401"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getServiceAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = "invalid"

        when:
        def response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == 401

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service w/ a token of deleted user….expect a 404"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID()
        def user = utils.createUser(utils.getServiceAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def deletedUsername = testUtils.getRandomUUID()
        def deletedUser = utils.createUser(token, deletedUsername, domainId)
        token = utils.getToken(deletedUsername, Constants.DEFAULT_PASSWORD)

        utils.deleteUser(deletedUser)

        when:
        def response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == 404

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service for a non-existing domain….expect a 404"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getServiceAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.removeUserFromUserGroup(token, "doesNotExist", userGroup.id, user.id)

        then:
        response.status == 404

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service for a non-existing group….expect a 404"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getServiceAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.removeUserFromUserGroup(token, domainId, "doesNotExist", user.id)

        then:
        response.status == 404

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service for a non-existing provisioned user….expect a 404"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getServiceAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, "doesNotExist")

        then:
        response.status == 404

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service for a disabled provisioned user….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getServiceAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        utils.disableUser(user)

        when:
        def response = cloud20.removeUserFromUserGroup(utils.getServiceAdminToken(), domainId, userGroup.id, user.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service using default user's token….expect a 403"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def defaultUsername = testUtils.getRandomUUID()
        def defaultUser = utils.createUser(token, defaultUsername, domainId)
        token = utils.getToken(defaultUsername, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == 403

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(defaultUser)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service using rcn admin, not having the domain in the URL as one of domains for its RCN.….expect a 403"() {
        given:
        def username = testUtils.getRandomUUID()
        def rcn = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId = utils.createDomain()
        def rcnDomain = v1Factory.createDomain(rcnDomainId, rcnDomainId)
        rcnDomain.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain)
        def user = utils.createUser(utils.getIdentityAdminToken(), username, rcnDomainId)
        def userGroup = utils.createUserGroup(rcnDomainId)

        def username2 = testUtils.getRandomUUID()
        def rcn2 = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId2 = utils.createDomain()
        def rcnDomain2 = v1Factory.createDomain(rcnDomainId2, rcnDomainId2)
        rcnDomain2.rackspaceCustomerNumber = rcn2
        utils.createDomain(rcnDomain2)
        def user2 = utils.createUser(utils.getIdentityAdminToken(), username2, rcnDomainId2)
        utils.addRoleToUser(user2, Constants.RCN_ADMIN_ROLE_ID)
        def token = utils.getToken(username2, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.removeUserFromUserGroup(token, rcnDomainId, userGroup.id, user2.id)

        then:
        response.status == 403

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
        utils.deleteUser(user2)
    }

    def "Call remove user from domain group service for a federated user….expect a 403"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)
        def federatedUser = utils.createFederatedUser(domainId)

        when:
        def response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, federatedUser.id)

        then:
        response.status == 403

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
        utils.deleteUser(federatedUser)
    }

    def "Call remove user from domain group service for a user admin of group's domain, using the same user's token….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        when:
        cloud20.addUserToUserGroup(token, domainId, userGroup.id, user.id)
        def response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service for a user admin of group's domain, using the user manager's token….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def userManagedUsername = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(token, userManagedUsername, domainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)
        token = utils.getToken(userManagedUsername, Constants.DEFAULT_PASSWORD)

        when:
        cloud20.addUserToUserGroup(token, domainId, userGroup.id, user.id)
        def response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service for a user admin of group's domain, using the token of rcn-admin and rcn-admin's domain is same as group's domain….expect a 204"() {
        given:
        def rcn = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId = utils.createDomain()
        def rcnDomain = v1Factory.createDomain(rcnDomainId, rcnDomainId)
        rcnDomain.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain)

        def rcnAdminUsername = testUtils.getRandomUUID()
        def rcnAdmin = utils.createUser(utils.getIdentityAdminToken(), rcnAdminUsername, rcnDomainId)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)
        def rcnAdminToken = utils.getToken(rcnAdminUsername, Constants.DEFAULT_PASSWORD)

        def username = testUtils.getRandomUUID()
        def user = utils.createUser(rcnAdminToken, username, rcnDomainId)

        def userGroup = utils.createUserGroup(rcnDomainId)

        when:
        cloud20.addUserToUserGroup(rcnAdminToken, rcnDomainId, userGroup.id, user.id)
        def response = cloud20.removeUserFromUserGroup(rcnAdminToken, rcnDomainId, userGroup.id, user.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
        utils.deleteUser(rcnAdmin)
    }

    def "Call remove user from domain group service for a user admin of group's domain, using rcn-admin's token and rcn-admin's domain is NOT same as group's domain but the group's domain belongs to rcn-admin's RCN….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def rcn = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId = utils.createDomain()
        def rcnDomain = v1Factory.createDomain(rcnDomainId, rcnDomainId)
        rcnDomain.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain)
        def user = utils.createUser(utils.getIdentityAdminToken(), username, rcnDomainId)
        def userGroup = utils.createUserGroup(rcnDomainId)

        def username2 = testUtils.getRandomUUID()
        def rcnDomainId2 = utils.createDomain()
        def rcnDomain2 = v1Factory.createDomain(rcnDomainId2, rcnDomainId2)
        rcnDomain2.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain2)
        def user2 = utils.createUser(utils.getIdentityAdminToken(), username2, rcnDomainId2)
        utils.addRoleToUser(user2, Constants.RCN_ADMIN_ROLE_ID)
        def token = utils.getToken(username2, Constants.DEFAULT_PASSWORD)

        when:
        cloud20.addUserToUserGroup(token, rcnDomainId, userGroup.id, user.id)
        def response = cloud20.removeUserFromUserGroup(token, rcnDomainId, userGroup.id, user.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
        utils.deleteUser(user2)
    }

    def "Call remove user from domain group service for a user manager of group's domain, using the same user's token….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def userManagedUsername = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(token, userManagedUsername, domainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)
        token = utils.getToken(userManagedUsername, Constants.DEFAULT_PASSWORD)

        when:
        cloud20.addUserToUserGroup(token, domainId, userGroup.id, userManagedUser.id)
        def response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, userManagedUser.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service for a user manager of group's domain, using the user manager's token….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def userManagedUsername = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(token, userManagedUsername, domainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)
        token = utils.getToken(userManagedUsername, Constants.DEFAULT_PASSWORD)

        def otherUsername = testUtils.getRandomUUID()
        def otherDomainId = utils.createDomain()
        def otherUser = utils.createUser(token, otherUsername, otherDomainId)
        utils.addRoleToUser(otherUser, Constants.USER_MANAGE_ROLE_ID)

        when:
        cloud20.addUserToUserGroup(token, domainId, userGroup.id, otherUser.id)
        def response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, otherUser.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(otherUser)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service for a user manager of group's domain, using the token of rcn-admin and rcn-admin's domain is same as group's domain….expect a 204"() {
        given:
        def rcn = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId = utils.createDomain()
        def rcnDomain = v1Factory.createDomain(rcnDomainId, rcnDomainId)
        rcnDomain.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain)

        def rcnAdminUsername = testUtils.getRandomUUID()
        def rcnAdmin = utils.createUser(utils.getIdentityAdminToken(), rcnAdminUsername, rcnDomainId)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)
        def rcnAdminToken = utils.getToken(rcnAdminUsername, Constants.DEFAULT_PASSWORD)

        def username = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(rcnAdminToken, username, rcnDomainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)

        def userGroup = utils.createUserGroup(rcnDomainId)

        when:
        cloud20.addUserToUserGroup(rcnAdminToken, rcnDomainId, userGroup.id, userManagedUser.id)
        def response = cloud20.removeUserFromUserGroup(rcnAdminToken, rcnDomainId, userGroup.id, userManagedUser.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(rcnAdmin)
    }

    def "Call remove user from domain group service for a user manager of group's domain, using rcn-admin's token and rcn-admin's domain is NOT same as group's domain but the group's domain belongs to rcn-admin's RCN….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def rcn = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId = utils.createDomain()
        def rcnDomain = v1Factory.createDomain(rcnDomainId, rcnDomainId)
        rcnDomain.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain)
        def user = utils.createUser(utils.getIdentityAdminToken(), username, rcnDomainId)
        def userGroup = utils.createUserGroup(rcnDomainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def userManagedUsername = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(token, userManagedUsername, rcnDomainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)

        def username2 = testUtils.getRandomUUID()
        def rcnDomainId2 = utils.createDomain()
        def rcnDomain2 = v1Factory.createDomain(rcnDomainId2, rcnDomainId2)
        rcnDomain2.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain2)
        def user2 = utils.createUser(utils.getIdentityAdminToken(), username2, rcnDomainId2)
        utils.addRoleToUser(user2, Constants.RCN_ADMIN_ROLE_ID)
        def rcnAdminToken = utils.getToken(username2, Constants.DEFAULT_PASSWORD)

        when:
        cloud20.addUserToUserGroup(rcnAdminToken, rcnDomainId, userGroup.id, userManagedUser.id)
        def response = cloud20.removeUserFromUserGroup(rcnAdminToken, rcnDomainId, userGroup.id, userManagedUser.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(user)
        utils.deleteUser(user2)
    }
    def "Call add user to domain group service for a default user of group's domain, using the same user's token….expect a 403"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def defaultUsername = testUtils.getRandomUUID()
        def defaultUser = utils.createUser(token, defaultUsername, domainId)
        token = utils.getToken(defaultUsername, Constants.DEFAULT_PASSWORD)

        when:
        def response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, defaultUser.id)

        then:
        response.status == 403

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(defaultUser)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service for a default user of group's domain, using the user manager's token….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def userManagedUsername = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(token, userManagedUsername, domainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)
        token = utils.getToken(userManagedUsername, Constants.DEFAULT_PASSWORD)

        def defaultUsername = testUtils.getRandomUUID()
        def defaultUser = utils.createUser(token, defaultUsername, domainId)

        when:
        cloud20.addUserToUserGroup(token, domainId, userGroup.id, defaultUser.id)
        def response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, defaultUser.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(defaultUser)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service for a default user of group's domain, using the token of rcn-admin and rcn-admin's domain is same as group's domain….expect a 204"() {
        given:
        def rcn = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId = utils.createDomain()
        def rcnDomain = v1Factory.createDomain(rcnDomainId, rcnDomainId)
        rcnDomain.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain)

        def rcnAdminUsername = testUtils.getRandomUUID()
        def rcnAdmin = utils.createUser(utils.getIdentityAdminToken(), rcnAdminUsername, rcnDomainId)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)
        def rcnAdminToken = utils.getToken(rcnAdminUsername, Constants.DEFAULT_PASSWORD)

        def username = testUtils.getRandomUUID()
        def user = utils.createUser(rcnAdminToken, username, rcnDomainId)

        def userGroup = utils.createUserGroup(rcnDomainId)

        when:
        cloud20.addUserToUserGroup(rcnAdminToken, rcnDomainId, userGroup.id, user.id)
        def response = cloud20.removeUserFromUserGroup(rcnAdminToken, rcnDomainId, userGroup.id, user.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(user)
        utils.deleteUser(rcnAdmin)
    }

    def "Call remove user from domain group service for a default user of group's domain, using rcn-admin's token and rcn-admin's domain is NOT same as group's domain but the group's domain belongs to rcn-admin's RCN….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def rcn = testUtils.getRandomUUID("rcn")[0..16]
        def rcnDomainId = utils.createDomain()
        def rcnDomain = v1Factory.createDomain(rcnDomainId, rcnDomainId)
        rcnDomain.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain)
        def user = utils.createUser(utils.getIdentityAdminToken(), username, rcnDomainId)
        def userGroup = utils.createUserGroup(rcnDomainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def defaultUsername = testUtils.getRandomUUID()
        def defaultUser = utils.createUser(token, defaultUsername, rcnDomainId)

        def username2 = testUtils.getRandomUUID()
        def rcnDomainId2 = utils.createDomain()
        def rcnDomain2 = v1Factory.createDomain(rcnDomainId2, rcnDomainId2)
        rcnDomain2.rackspaceCustomerNumber = rcn
        utils.createDomain(rcnDomain2)
        def user2 = utils.createUser(utils.getIdentityAdminToken(), username2, rcnDomainId2)
        utils.addRoleToUser(user2, Constants.RCN_ADMIN_ROLE_ID)
        token = utils.getToken(username2, Constants.DEFAULT_PASSWORD)

        when:
        cloud20.addUserToUserGroup(token, rcnDomainId, userGroup.id, defaultUser.id)
        def response = cloud20.removeUserFromUserGroup(token, rcnDomainId, userGroup.id, defaultUser.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(defaultUser)
        utils.deleteUser(user)
        utils.deleteUser(user2)
    }

    def "Call remove user from domain group service for a rcn admin of group's domain, using the same user's token….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def rcnAdminUsername = testUtils.getRandomUUID()
        def rcnAdmin = utils.createUser(token, rcnAdminUsername, domainId)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)
        def rcnAdminToken = utils.getToken(rcnAdminUsername, Constants.DEFAULT_PASSWORD)

        when:
        cloud20.addUserToUserGroup(rcnAdminToken, domainId, userGroup.id, rcnAdmin.id)
        def response = cloud20.removeUserFromUserGroup(rcnAdminToken, domainId, userGroup.id, rcnAdmin.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(rcnAdmin)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service for a rcn admin of group's domain, using the user manager's token belonging to rcn-admin's domain….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def rcnAdminUsername = testUtils.getRandomUUID()
        def rcnAdmin = utils.createUser(token, rcnAdminUsername, domainId)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)

        def userManagedUsername = testUtils.getRandomUUID()
        def userManagedUser = utils.createUser(token, userManagedUsername, domainId)
        utils.addRoleToUser(userManagedUser, Constants.USER_MANAGE_ROLE_ID)
        token = utils.getToken(userManagedUsername, Constants.DEFAULT_PASSWORD)

        when:
        cloud20.addUserToUserGroup(token, domainId, userGroup.id, rcnAdmin.id)
        def response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, rcnAdmin.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(userManagedUser)
        utils.deleteUser(rcnAdmin)
        utils.deleteUser(user)
    }

    def "Call remove user from domain group service for a rcn admin of group's domain, using the token of user-admin and user-admin's domain is same as rcn-admin's domain….expect a 204"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def userGroup = utils.createUserGroup(domainId)
        def token = utils.getToken(username, Constants.DEFAULT_PASSWORD)

        def rcnAdminUsername = testUtils.getRandomUUID()
        def rcnAdmin = utils.createUser(token, rcnAdminUsername, domainId)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)

        when:
        cloud20.addUserToUserGroup(token, domainId, userGroup.id, rcnAdmin.id)
        def response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, rcnAdmin.id)

        then:
        response.status == 204

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(rcnAdmin)
        utils.deleteUser(user)
    }

    def "Reconcile user-group tenant roles when tenant is deleted"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def tenant = utils.createTenantInDomain(domainId)
        def userGroup = utils.createUserGroup(domainId)
        def role = utils.createRole(null, RandomStringUtils.randomAlphabetic(8), IdentityUserTypeEnum.USER_MANAGER.roleName)

        utils.grantRoleAssignmentsOnUserGroup(userGroup, v2Factory.createSingleRoleAssignment(role.id, [tenant.id]))

        when: "Verify that the tenant exists within a domain."
        def tenants = utils.listDomainTenants(domainId)

        then:
        assert tenants.tenant.find{it.id == tenant.id}

        when: "List the roles for the user group on the tenant."
        def roleAssignments = utils.listRoleAssignmentsOnUserGroup(userGroup)

        then:
        assert roleAssignments.tenantAssignments.tenantAssignment.find {it.forTenants.contains(tenant.id)}

        when: "Delete the tenant from the domain."
        def response = cloud20.deleteTenantFromDomain(utils.getServiceAdminToken(), domainId, tenant.id)

        then:
        assert response.status == SC_NO_CONTENT

        when: "Verify that all the roles assigned to the user group are deleted."
        roleAssignments = utils.listRoleAssignmentsOnUserGroup(userGroup)

        then:
        assert roleAssignments.tenantAssignments.tenantAssignment.find {it.forTenants.contains(tenant.id)} == null

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteTenant(tenant)
        utils.deleteUser(user)
        utils.deleteRole(role)
        utils.deleteDomain(domainId)
    }

    def "Reconcile user-group tenant roles when tenant is modified"() {
        given:
        def username = testUtils.getRandomUUID()
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), username, domainId)
        def tenant = utils.createTenantInDomain(domainId)
        def userGroup = utils.createUserGroup(domainId)
        def role = utils.createRole(null, RandomStringUtils.randomAlphabetic(8), IdentityUserTypeEnum.USER_MANAGER.roleName)

        def username2 = testUtils.getRandomUUID()
        def domainId2 = utils.createDomain()
        def user2 = utils.createUser(utils.getIdentityAdminToken(), username2, domainId2)

        utils.grantRoleAssignmentsOnUserGroup(userGroup, v2Factory.createSingleRoleAssignment(role.id, [tenant.id]))

        when: "Verify that the tenant exists within a domain."
        def tenants = utils.listDomainTenants(domainId)

        then:
        assert tenants.tenant.find{it.id == tenant.id}

        when: "List the roles for the user group on the tenant."
        def roleAssignments = utils.listRoleAssignmentsOnUserGroup(userGroup)

        then:
        assert roleAssignments.tenantAssignments.tenantAssignment.find {it.forTenants.contains(tenant.id)}

        when: "Add domain to tenant."
        def response = cloud20.addTenantToDomain(utils.getServiceAdminToken(), domainId2, tenant.id)

        then:
        assert response.status == SC_NO_CONTENT

        when: "Verify that all the roles assigned to the user group are deleted."
        roleAssignments = utils.listRoleAssignmentsOnUserGroup(userGroup)

        then:
        assert roleAssignments.tenantAssignments.tenantAssignment.find {it.forTenants.contains(tenant.id)} == null

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteTenant(tenant)
        utils.deleteUser(user)
        utils.deleteRole(role)
        utils.deleteDomain(domainId)
        utils.deleteUser(user2)
        utils.deleteDomain(domainId2)
    }

}
