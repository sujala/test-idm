package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroups
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.impl.DefaultDomainService
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.modules.usergroups.service.DefaultUserGroupService
import org.apache.commons.lang.RandomStringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.openstack.docs.identity.api.v2.Tenants
import org.openstack.docs.identity.api.v2.UnauthorizedFault
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class DefaultUserGroupCloudServiceRestIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Shared
    def sharedIdentityAdminToken

    @Shared
    User sharedUserAdmin
    @Shared
    org.openstack.docs.identity.api.v2.Tenant sharedUserAdminCloudTenant
    @Shared
    org.openstack.docs.identity.api.v2.Tenant sharedUserAdminFilesTenant

    void doSetupSpec() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_SUPPORT_USER_GROUPS_GLOBALLY_PROP, true)

        def authResponse = cloud20.authenticatePassword(Constants.IDENTITY_ADMIN_USERNAME, Constants.IDENTITY_ADMIN_PASSWORD)
        assert authResponse.status == HttpStatus.SC_OK
        sharedIdentityAdminToken = authResponse.getEntity(AuthenticateResponse).value.token.id

        sharedUserAdmin = cloud20.createCloudAccount(sharedIdentityAdminToken)

        Tenants tenants = cloud20.getDomainTenants(sharedIdentityAdminToken, sharedUserAdmin.domainId).getEntity(Tenants).value
        sharedUserAdminCloudTenant = tenants.tenant.find {
            it.id == sharedUserAdmin.domainId
        }
        sharedUserAdminFilesTenant = tenants.tenant.find() {
            it.id != sharedUserAdmin.domainId
        }
    }

    @Unroll
    def "CRUD group; mediaType = #mediaType"() {
        when:
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "addTest_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def response = cloud20.createUserGroup(sharedIdentityAdminToken, group, mediaType)

        then:
        response.status == HttpStatus.SC_CREATED
        UserGroup created = response.getEntity(UserGroup)

        and:
        created.domainId == group.domainId
        created.id != null
        created.description == group.description
        created.name == group.name

        when:
        def getResponse = cloud20.getUserGroup(sharedIdentityAdminToken, created, mediaType)

        then:
        getResponse.status == HttpStatus.SC_OK
        UserGroup retrievedEntity = getResponse.getEntity(UserGroup)

        and:
        retrievedEntity.domainId == group.domainId
        retrievedEntity.id == created.id
        retrievedEntity.description == group.description
        retrievedEntity.name == group.name

        when:
        def deleteResponse = cloud20.deleteUserGroup(sharedIdentityAdminToken, retrievedEntity)

        then:
        deleteResponse.status == HttpStatus.SC_NO_CONTENT

        when:
        getResponse = cloud20.getUserGroup(sharedIdentityAdminToken, retrievedEntity)

        then:
        getResponse.status == HttpStatus.SC_NOT_FOUND

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Get User Group: Returns error when invalid groupId provided in url: id: #groupId"() {
        def group = new UserGroup().with {
            id = groupId
            domainId = sharedUserAdmin.domainId
            it
        }

        expect:
        cloud20.getUserGroup(sharedIdentityAdminToken, group).status == HttpStatus.SC_NOT_FOUND

        where:
        groupId << ["null", RandomStringUtils.randomAlphanumeric(100)]
    }

    @Unroll
    def "Error check: delete user group; #mediaType"() {
        given:
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "deleteTest_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }

        def domainId = utils.createDomain()
        def defaultUser, users
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when: "Create user group"
        def response = cloud20.createUserGroup(sharedIdentityAdminToken, group, mediaType)
        UserGroup created = response.getEntity(UserGroup)

        then:
        response.status == HttpStatus.SC_CREATED

        when: "Invalid domain"
        UserGroup invalidGroup = new UserGroup().with {
            it.domainId = "invalid"
            it.id = created.id
            it
        }
        def deleteResponse = cloud20.deleteUserGroup(sharedIdentityAdminToken, invalidGroup)
        String errMsg = String.format(DefaultUserGroupService.GROUP_NOT_FOUND_ERROR_MESSAGE, invalidGroup.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(deleteResponse, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, errMsg)

        when: "Invalid group"
        invalidGroup = new UserGroup().with {
            it.domainId = created.domainId
            it.id = "invalid"
            it
        }
        deleteResponse = cloud20.deleteUserGroup(sharedIdentityAdminToken, invalidGroup)
        errMsg = String.format(DefaultUserGroupService.GROUP_NOT_FOUND_ERROR_MESSAGE, invalidGroup.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(deleteResponse, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, errMsg)

        when: "Invalid auth token"
        deleteResponse = cloud20.deleteUserGroup("invalid", created)
        errMsg = "No valid token provided. Please use the 'X-Auth-Token' header with a valid token."

        then:
        IdmAssert.assertOpenStackV2FaultResponse(deleteResponse, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED, errMsg)

        when: "Unauthorized token"
        deleteResponse = cloud20.deleteUserGroup(utils.getToken(defaultUser.username), created)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(deleteResponse, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteUserGroup(created)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "List groups for domain; mediaType = #mediaType"() {
        when: "Create user group"
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "listTest_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def response = cloud20.createUserGroup(sharedIdentityAdminToken, group, mediaType)

        then:
        response.status == HttpStatus.SC_CREATED
        UserGroup created = response.getEntity(UserGroup)

        when: "List user groups for domain"
        def getGroupsResponse = cloud20.listUserGroupsForDomain(sharedIdentityAdminToken, sharedUserAdmin.domainId, null, mediaType)
        UserGroups groups = getGroupsResponse.getEntity(UserGroups)

        then:
        getGroupsResponse.status == HttpStatus.SC_OK
        groups.userGroup.size() > 0
        groups.userGroup.find { it.name == created.name } != null

        when: "List user groups for domain with name query param"
        getGroupsResponse = cloud20.listUserGroupsForDomain(sharedIdentityAdminToken, sharedUserAdmin.domainId, new UserGroupSearchParams(created.name, null), mediaType)
        groups = getGroupsResponse.getEntity(UserGroups)

        then:
        getGroupsResponse.status == HttpStatus.SC_OK

        and:
        groups.userGroup.size() == 1
        UserGroup userGroup = groups.userGroup.get(0)
        userGroup.id == created.id
        userGroup.name == created.name
        userGroup.domainId == created.domainId
        userGroup.description == created.description

        when: "Adding user to user group"
        response = cloud20.addUserToUserGroup(utils.getToken(sharedUserAdmin.username), sharedUserAdmin.domainId, created.id, sharedUserAdmin.id)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "List user groups for domain with userId query param"
        getGroupsResponse = cloud20.listUserGroupsForDomain(sharedIdentityAdminToken, sharedUserAdmin.domainId, new UserGroupSearchParams(null, sharedUserAdmin.id), mediaType)
        groups = getGroupsResponse.getEntity(UserGroups)

        then:
        getGroupsResponse.status == HttpStatus.SC_OK

        and:
        groups.userGroup.size() == 1
        UserGroup userGroup2 = groups.userGroup.get(0)
        userGroup2.id == created.id
        userGroup2.name == created.name
        userGroup2.domainId == created.domainId
        userGroup2.description == created.description

        when: "List user groups for domain with name and userId query param"
        getGroupsResponse = cloud20.listUserGroupsForDomain(sharedIdentityAdminToken, sharedUserAdmin.domainId, new UserGroupSearchParams(created.name, sharedUserAdmin.id), mediaType)
        groups = getGroupsResponse.getEntity(UserGroups)

        then:
        getGroupsResponse.status == HttpStatus.SC_OK

        and:
        groups.userGroup.size() == 1
        UserGroup userGroup3 = groups.userGroup.get(0)
        userGroup3.id == created.id
        userGroup3.name == created.name
        userGroup3.domainId == created.domainId
        userGroup3.description == created.description

        cleanup:
        utils.removeUserFromUserGroup(sharedUserAdmin.id, created)
        utils.deleteUserGroup(created)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "List user groups with default user; #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def defaultUser, users
        (defaultUser, users) = utils.createDefaultUser(domainId)

        UserGroup group = new UserGroup().with {
            it.domainId = defaultUser.domainId
            it.name = "listTest_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }
        def defaultUserToken = utils.getToken(defaultUser.username)

        when: "Create user group"
        def response = cloud20.createUserGroup(sharedIdentityAdminToken, group, mediaType)
        UserGroup created = response.getEntity(UserGroup)

        then:
        response.status == HttpStatus.SC_CREATED

        when: "Adding defaultUser to userGroup"
        response = cloud20.addUserToUserGroup(sharedIdentityAdminToken, domainId, created.id, defaultUser.id)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        when: "Using defaultUser token without userId query param"
        def getGroupsResponse = cloud20.listUserGroupsForDomain(defaultUserToken, defaultUser.domainId)

        then: "Assert 403"
        IdmAssert.assertOpenStackV2FaultResponse(getGroupsResponse, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "Using defaultUser token with userId query param"
        getGroupsResponse = cloud20.listUserGroupsForDomain(defaultUserToken, defaultUser.domainId, new UserGroupSearchParams(null, defaultUser.id))
        UserGroups groups = getGroupsResponse.getEntity(UserGroups)

        then:
        getGroupsResponse.status == HttpStatus.SC_OK

        and:
        groups.userGroup.size() == 1
        UserGroup userGroup = groups.userGroup.get(0)
        userGroup.id == created.id
        userGroup.name == created.name
        userGroup.domainId == created.domainId
        userGroup.description == created.description

        cleanup:
        utils.deleteUsers(users)
        // Deleting a domain also removed all associated userGroups
        utils.deleteDomain(domainId)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error check: list user groups for domain; #mediaType"() {
        given:
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "listTest_" + RandomStringUtils.randomAlphanumeric(10)
            it
        }

        def domainId = utils.createDomain()
        def defaultUser, users
        (defaultUser, users) = utils.createDefaultUser(domainId)

        when: "Create user group"
        def response = cloud20.createUserGroup(sharedIdentityAdminToken, group, mediaType)
        UserGroup created = response.getEntity(UserGroup)

        then:
        response.status == HttpStatus.SC_CREATED

        when: "Invalid domain"
        def getGroupsResponse = cloud20.listUserGroupsForDomain(sharedIdentityAdminToken, "invalid")
        String errMsg = String.format(DefaultDomainService.DOMAIN_NOT_FOUND_ERROR_MESSGAE, "invalid")

        then:
        IdmAssert.assertOpenStackV2FaultResponse(getGroupsResponse, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, errMsg)

        when: "Invalid name query param"
        getGroupsResponse = cloud20.listUserGroupsForDomain(sharedIdentityAdminToken, sharedUserAdmin.domainId, new UserGroupSearchParams("invalid", null))
        UserGroups userGroups = getGroupsResponse.getEntity(UserGroups)

        then:
        userGroups.userGroup.size() == 0

        when: "query param name with special characters"
        getGroupsResponse = cloud20.listUserGroupsForDomain(sharedIdentityAdminToken, sharedUserAdmin.domainId, new UserGroupSearchParams("иииии", null))
        userGroups = getGroupsResponse.getEntity(UserGroups)

        then:
        userGroups.userGroup.size() == 0

        when: "Invalid auth token"
        getGroupsResponse = cloud20.listUserGroupsForDomain("invalid", created.domainId)
        errMsg = "No valid token provided. Please use the 'X-Auth-Token' header with a valid token."

        then:
        IdmAssert.assertOpenStackV2FaultResponse(getGroupsResponse, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED, errMsg)

        when: "Unauthorized token"
        getGroupsResponse = cloud20.listUserGroupsForDomain(utils.getToken(defaultUser.username), created.domainId)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(getGroupsResponse, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "token of user from different domain"
        def anotherdomainId = utils.createDomain()
        def anotherDomain = v2Factory.createDomain(anotherdomainId, anotherdomainId)
        def anotherDomainCreated = utils.createDomain(anotherDomain)
        def userAdminToken = utils.getToken(sharedUserAdmin.username)

        def listGroupsResponse = cloud20.listUserGroupsForDomain(userAdminToken, anotherDomainCreated.id)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(listGroupsResponse, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        when: "token of user from different domain with query param specified"
        getGroupsResponse = cloud20.listUserGroupsForDomain(userAdminToken, anotherDomainCreated.id, new UserGroupSearchParams("test", null))

        then:
        IdmAssert.assertOpenStackV2FaultResponse(getGroupsResponse, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteUserGroup(created)
        utils.deleteDomain(anotherDomainCreated.id)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error check: list user groups for domain with invalid mediatype; mediatype = #mediaType"() {
        given:
        def domainId = utils.createDomain()
        def domain = v2Factory.createDomain(domainId, domainId)
        def domainCreated = utils.createDomain(domain)

        when: "Invalid media type, mediaType - #mediaType"
        def getGroupsResponse = cloud20.listUserGroupsForDomain(sharedIdentityAdminToken, domainCreated.id,null, mediaType)

        then:
        getGroupsResponse.status == HttpStatus.SC_NOT_ACCEPTABLE

        cleanup:
        utils.deleteDomain(domainId)

        where:
        mediaType << [MediaType.TEXT_PLAIN_TYPE, GlobalConstants.TEXT_YAML_TYPE]
    }

    @Unroll
    @Ignore("This test for special characters in user-group's name, currently fails at the gate")
    def "Create user group with name having special characters and list user groups with query param works, mediaType - #mediaType"() {
        when:
        UserGroup group = new UserGroup().with {
            it.domainId = sharedUserAdmin.domainId
            it.name = "SpecialCharTest_" + RandomStringUtils.randomAlphanumeric(10) + "и"
            it
        }
        def response = cloud20.createUserGroup(sharedIdentityAdminToken, group, mediaType)

        then:
        response.status == HttpStatus.SC_CREATED
        UserGroup created = response.getEntity(UserGroup)

        and:
        created.domainId == group.domainId
        created.id != null
        created.description == group.description
        created.name == group.name

        when:
        def getResponse = cloud20.getUserGroup(sharedIdentityAdminToken, created, mediaType)

        then:
        getResponse.status == HttpStatus.SC_OK
        UserGroup retrievedEntity = getResponse.getEntity(UserGroup)

        and:
        retrievedEntity.domainId == group.domainId
        retrievedEntity.id == created.id
        retrievedEntity.description == group.description
        retrievedEntity.name == group.name

        when: "list groups with query param"
        def getGroupsResponse = cloud20.listUserGroupsForDomain(sharedIdentityAdminToken, sharedUserAdmin.domainId, group.name, mediaType)
        def userGroups = getGroupsResponse.getEntity(UserGroups)

        then:
        getGroupsResponse.status == HttpStatus.SC_OK
        userGroups.userGroup.size() == 1

        and:
        UserGroup userGroup = userGroups.userGroup.get(0)
        userGroup.domainId == group.domainId
        userGroup.id == created.id
        userGroup.description == group.description
        userGroup.name == group.name

        when: "query param set to group name modified with different special character"
        def queryParam = group.name[0..-2] + 'ü'

        getGroupsResponse = cloud20.listUserGroupsForDomain(sharedIdentityAdminToken, sharedUserAdmin.domainId, queryParam, mediaType)
        userGroups = getGroupsResponse.getEntity(UserGroups)

        then:
        getGroupsResponse.status == HttpStatus.SC_OK
        userGroups.userGroup.size() == 0

        when: "query param set to group name appended with another special character"
        queryParam = group.name + 'и'

        getGroupsResponse = cloud20.listUserGroupsForDomain(sharedIdentityAdminToken, sharedUserAdmin.domainId, queryParam, mediaType)
        userGroups = getGroupsResponse.getEntity(UserGroups)

        then:
        getGroupsResponse.status == HttpStatus.SC_OK
        userGroups.userGroup.size() == 0

        cleanup:
        utils.deleteUserGroup(created)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }
}