package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroups
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.service.impl.DefaultDomainService
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

    @Shared User sharedUserAdmin
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminCloudTenant
    @Shared org.openstack.docs.identity.api.v2.Tenant sharedUserAdminFilesTenant

    void doSetupSpec() {
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
    def "List groups; mediaType = #mediaType"() {
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

        when: "List user groups"
        def getGroupsResponse = cloud20.listUserGroups(sharedIdentityAdminToken, sharedUserAdmin.domainId, null, mediaType)
        UserGroups groups = getGroupsResponse.getEntity(UserGroups)

        then:
        getGroupsResponse.status == HttpStatus.SC_OK
        groups.userGroup.size() > 0

        when: "List user groups with name query param"
        getGroupsResponse = cloud20.listUserGroups(sharedIdentityAdminToken, sharedUserAdmin.domainId, group.name, mediaType)
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

        cleanup:
        utils.deleteUserGroup(created)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

    @Unroll
    def "Error check: list user groups; #mediaType"() {
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
        def getGroupsResponse = cloud20.listUserGroups(sharedIdentityAdminToken, "invalid")
        String errMsg = String.format(DefaultDomainService.DOMAIN_NOT_FOUND_ERROR_MESSGAE, "invalid")

        then:
        IdmAssert.assertOpenStackV2FaultResponse(getGroupsResponse, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, errMsg)

        when: "Invalid name query param"
        getGroupsResponse = cloud20.listUserGroups(sharedIdentityAdminToken, sharedUserAdmin.domainId, "invalid")
        UserGroups userGroups = getGroupsResponse.getEntity(UserGroups)

        then:
        userGroups.userGroup.size() == 0

        when: "Invalid auth token"
        getGroupsResponse = cloud20.listUserGroups("invalid", created.domainId)
        errMsg = "No valid token provided. Please use the 'X-Auth-Token' header with a valid token."

        then:
        IdmAssert.assertOpenStackV2FaultResponse(getGroupsResponse, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED, errMsg)

        when: "Unauthorized token"
        getGroupsResponse = cloud20.listUserGroups(utils.getToken(defaultUser.username), created.domainId)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(getGroupsResponse, ForbiddenFault, HttpStatus.SC_FORBIDDEN, "Not Authorized")

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domainId)
        utils.deleteUserGroup(created)

        where:
        mediaType << [MediaType.APPLICATION_XML_TYPE, MediaType.APPLICATION_JSON_TYPE]
    }

}
