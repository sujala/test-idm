package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.impl.DefaultDomainService
import com.rackspace.idm.modules.usergroups.service.DefaultUserGroupService
import com.rackspace.idm.validation.Validator20
import com.sun.jersey.api.client.ClientResponse
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang3.RandomStringUtils
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class UpdateUserGroupIntegrationTest extends RootIntegrationTest {

    void doSetupSpec() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)
    }

    @Unroll
    def "test update user group, accept == #accept, request == #request"() {
        given:
        String domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        UserGroup userGroup = utils.createUserGroup(domainId)

        def rcnAdminUsername = testUtils.getRandomUUID()
        def rcnAdmin = utils.createUser(utils.getToken(userAdmin.username), rcnAdminUsername, domainId)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)

        when: "update the user group"
        UserGroup groupData = getRandomUserGroupUpdateData()
        def response = cloud20.updateUserGroup(utils.getToken(userAdmin.username), userGroup.id, domainId, groupData, accept, request)

        then:
        validateUserGroupUpdated(groupData, response)

        when: "get the user group for the domain"
        def getGroup = utils.getUserGroup(userGroup.id, domainId)

        then:
        getGroup.name == groupData.name
        getGroup.description == groupData.description

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(rcnAdmin)
        utils.deleteUser(userAdmin)

        where:
        [accept, request] << [[MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE],
                              [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]].combinations()
    }

    @Unroll
    def "test update user group access, accept == #accept, request == #request"() {
        given:
        String domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        UserGroup userGroup = utils.createUserGroup(domainId)

        def userManage = utils.createUser(utils.getToken(userAdmin.username), RandomStringUtils.randomAlphanumeric(8), domainId)
        utils.addRoleToUser(userManage, Constants.USER_MANAGE_ROLE_ID)
        def rcnAdmin = utils.createUser(utils.getToken(userAdmin.username), RandomStringUtils.randomAlphanumeric(8), domainId)
        utils.addRoleToUser(rcnAdmin, Constants.RCN_ADMIN_ROLE_ID)

        when: "update the user group using service admin token"
        UserGroup groupData = getRandomUserGroupUpdateData()
        def response = cloud20.updateUserGroup(utils.getServiceAdminToken(), userGroup.id, domainId, groupData, accept, request)

        then:
        validateUserGroupUpdated(groupData, response)

        when: "update the user group using identity admin token"
        groupData = getRandomUserGroupUpdateData()
        response = cloud20.updateUserGroup(utils.getIdentityAdminToken(), userGroup.id, domainId, groupData, accept, request)

        then:
        validateUserGroupUpdated(groupData, response)

        when: "update the user group using user admin token"
        groupData = getRandomUserGroupUpdateData()
        response = cloud20.updateUserGroup(utils.getToken(userAdmin.username), userGroup.id, domainId, groupData, accept, request)

        then:
        validateUserGroupUpdated(groupData, response)

        when: "update the user group using rcn admin token"
        groupData = getRandomUserGroupUpdateData()
        response = cloud20.updateUserGroup(utils.getToken(rcnAdmin.username), userGroup.id, domainId, groupData, accept, request)

        then:
        validateUserGroupUpdated(groupData, response)

        when: "update the user group using user manage token"
        groupData = getRandomUserGroupUpdateData()
        response = cloud20.updateUserGroup(utils.getToken(userManage.username), userGroup.id, domainId, groupData, accept, request)

        then:
        validateUserGroupUpdated(groupData, response)

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(rcnAdmin)
        utils.deleteUser(userManage)
        utils.deleteUser(userAdmin)

        where:
        [accept, request] << [[MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE],
                              [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]].combinations()
    }

    def "test update user group using invalid tokens"() {
        given:
        String domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        UserGroup userGroup = utils.createUserGroup(domainId)

        String otherDomainId = utils.createDomain()
        def otherUserAdmin = utils.createUserAdminWithoutIdentityAdmin(otherDomainId)
        def otherRcnAdmin = utils.createUser(utils.getToken(otherUserAdmin.username), RandomStringUtils.randomAlphanumeric(8), domainId)
        utils.addRoleToUser(otherRcnAdmin, Constants.RCN_ADMIN_ROLE_ID)

        UserGroup groupData = getRandomUserGroupUpdateData()

        when: "update the user group using token of user admin in other domain"
        def response = cloud20.updateUserGroup(utils.getToken(otherUserAdmin.username), userGroup.id, domainId, groupData)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, 403, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "update the user group using token of rcn admin in other domain"
        response = cloud20.updateUserGroup(utils.getToken(otherRcnAdmin.username), userGroup.id, domainId, groupData)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, 403, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "update the user group using token of default user belonging to correct domain"
        def defaultUser = utils.createUser(utils.getToken(userAdmin.username))
        response = cloud20.updateUserGroup(utils.getToken(defaultUser.username), userGroup.id, domainId, groupData)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, 403, DefaultCloud20Service.NOT_AUTHORIZED)

        cleanup:
        utils.deleteUserGroup(userGroup)
        utils.deleteUser(otherRcnAdmin)
        utils.deleteUser(otherUserAdmin)
        utils.deleteUser(defaultUser)
        utils.deleteUser(userAdmin)
    }

    def "test update user group that does not exist"() {
        def domainId = utils.createDomain()
        utils.createDomainEntity(domainId)
        def groupId = RandomStringUtils.randomAlphanumeric(8)

        when:
        def groupData = getRandomUserGroupUpdateData()
        def response = cloud20.updateUserGroup(utils.getServiceAdminToken(), groupId, domainId, groupData)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, 404, String.format(DefaultUserGroupService.GROUP_NOT_FOUND_ERROR_MESSAGE, groupId))

        cleanup:
        utils.deleteDomain(domainId)
    }

    @Unroll
    def "get update user group with invalid values, name == #groupName description == #groupDescription"() {
        given:
        String domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        UserGroup userGroup = utils.createUserGroup(domainId)

        when:
        UserGroup groupData = new UserGroup().with {
            it.name = groupName
            it.description = groupDescription
            it
        }
        def response = cloud20.updateUserGroup(utils.getToken(userAdmin.username), userGroup.id, domainId, groupData)

        then:
        if (success) {
            validateUserGroupUpdated(groupData, response)
        } else {
            if (errorCode) {
                IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, 400, errorCode, errorMessage)
            } else {
                IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, 400, errorMessage)
            }
        }

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteUserGroup(userGroup)

        where:
        groupName                                     | groupDescription                              | success | attr   | errorMessage | errorCode
        null                                          | RandomStringUtils.randomAlphanumeric(8) | true    | null   | null | null
        ""                                            | RandomStringUtils.randomAlphanumeric(8) | false   | "name" | String.format(Validator20.REQUIRED_ATTR_MESSAGE, "name")  | ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE
        " "                                           | RandomStringUtils.randomAlphanumeric(8) | false   | "name" | String.format(Validator20.REQUIRED_ATTR_MESSAGE, "name")  | ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE
        "a"                                           | RandomStringUtils.randomAlphanumeric(8) | true    | null   | null | null
        "a" * 64                                      | RandomStringUtils.randomAlphanumeric(8) | true    | null   | null | null
        "a" * 65                                      | RandomStringUtils.randomAlphanumeric(8) | false   | "name" | String.format(Validator20.LENGTH_EXCEEDED_ERROR_MSG, "name", 64) | ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED
        RandomStringUtils.randomAlphanumeric(8) | null                                          | true    | null   | null | null
        RandomStringUtils.randomAlphanumeric(8) | ""                                            | true    | null   | null | null
        RandomStringUtils.randomAlphanumeric(8) | " "                                           | true    | null   | null | null
        RandomStringUtils.randomAlphanumeric(8) | "a"                                           | true    | null   | null | null
        RandomStringUtils.randomAlphanumeric(8) | "a" * 255                                     | true    | null   | null | null
        RandomStringUtils.randomAlphanumeric(8) | "a" * 256                                     | false   | "description" | String.format(Validator20.LENGTH_EXCEEDED_ERROR_MSG, "description", 255) | ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED
    }

    def "test update user group with same name"() {
        given:
        String domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        UserGroup userGroup = utils.createUserGroup(domainId)

        when: "update group to same name different case"
        UserGroup groupData = new UserGroup().with {
            it.name = testUtils.invertStringCase(userGroup.name)
            it.description = RandomStringUtils.randomAlphanumeric(8)
            it
        }
        def response = cloud20.updateUserGroup(utils.getToken(userAdmin.username), userGroup.id, domainId, groupData)

        then:
        validateUserGroupUpdated(groupData, response)

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteUserGroup(userGroup)
    }

    def "test update user group in invalid domain"() {
        given:
        String domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        UserGroup userGroup = utils.createUserGroup(domainId)

        UserGroup groupData = getRandomUserGroupUpdateData()

        when: "update the user group in a domain it does not belong to"
        String otherDomainId = utils.createDomainEntity().id
        def response = cloud20.updateUserGroup(utils.getToken(userAdmin.username), userGroup.id, otherDomainId, groupData)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, 403, DefaultCloud20Service.NOT_AUTHORIZED)

        when: "update the user group in a domain that does not exist"
        String nonexistentDomainId = utils.createDomain()
        response = cloud20.updateUserGroup(utils.getToken(userAdmin.username), userGroup.id, nonexistentDomainId, groupData)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, 404, String.format(DefaultDomainService.DOMAIN_NOT_FOUND_ERROR_MESSGAE, nonexistentDomainId))

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteUserGroup(userGroup)
    }

    def "can update group to have same name as other group in other domain"() {
        given:
        String domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        UserGroup userGroup = utils.createUserGroup(domainId)

        String otherDomainId = utils.createDomain()
        def otherUserAdmin = utils.createUserAdminWithoutIdentityAdmin(otherDomainId)
        UserGroup otherUserGroup = utils.createUserGroup(otherDomainId)

        when:
        def groupData = new UserGroup().with {
            it.name = otherUserGroup.name
            it
        }
        def response = cloud20.updateUserGroup(utils.getToken(userAdmin.username), userGroup.id, domainId, groupData)

        then:
        validateUserGroupUpdated(groupData, response)

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteUser(otherUserAdmin)
        utils.deleteUserGroup(userGroup)
        utils.deleteUserGroup(otherUserGroup)
    }

    def "test update group to have same name as other group in same domain"() {
        given:
        String domainId = utils.createDomain()
        def userAdmin = utils.createUserAdminWithoutIdentityAdmin(domainId)
        UserGroup userGroup = utils.createUserGroup(domainId)

        UserGroup otherUserGroup = utils.createUserGroup(domainId)

        when: "update group to to other group name w/ matching string case"
        def groupData = new UserGroup().with {
            it.name = otherUserGroup.name
            it
        }
        def response = cloud20.updateUserGroup(utils.getToken(userAdmin.username), userGroup.id, domainId, groupData)

        then:
        response.status == 409
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, 409, DefaultUserGroupService.ERROR_MESSAGE_DUPLICATE_GROUP_IN_DOMAIN)

        when: "update group to to other group name w/ non-matching string case"
        groupData = new UserGroup().with {
            it.name = testUtils.invertStringCase(otherUserGroup.name)
            it
        }
        response = cloud20.updateUserGroup(utils.getToken(userAdmin.username), userGroup.id, domainId, groupData)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, 409, DefaultUserGroupService.ERROR_MESSAGE_DUPLICATE_GROUP_IN_DOMAIN)

        cleanup:
        utils.deleteUser(userAdmin)
        utils.deleteUserGroup(userGroup)
        utils.deleteUserGroup(otherUserGroup)
    }

    UserGroup getRandomUserGroupUpdateData() {
        return new UserGroup().with {
            it.name = RandomStringUtils.randomAlphanumeric(8)
            it.description = RandomStringUtils.randomAlphanumeric(8)
            it
        }
    }

    void validateUserGroupUpdated(UserGroup expectedUserGroup, ClientResponse response) {
        assert response.status == 200
        UserGroup groupResponseEntity = response.getEntity(UserGroup)
        if (StringUtils.isNotBlank(expectedUserGroup.name)) {
            assert groupResponseEntity.name == expectedUserGroup.name
        }
        if (StringUtils.isNotBlank(expectedUserGroup.description)) {
            assert groupResponseEntity.description == expectedUserGroup.description
        }
    }

}
