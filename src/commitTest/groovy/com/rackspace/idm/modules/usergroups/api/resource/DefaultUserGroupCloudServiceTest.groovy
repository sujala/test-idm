package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.api.resource.IdmPathUtils
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.modules.usergroups.api.resource.converter.UserGroupConverter
import com.rackspace.idm.modules.usergroups.service.UserGroupService
import org.apache.http.HttpStatus
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

class DefaultUserGroupCloudServiceTest extends RootServiceTest {

    DefaultUserGroupCloudService defaultUserGroupCloudService
    UserGroupAuthorizationService userGroupAuthorizationService
    UserGroupService userGroupService
    UserGroupConverter userGroupConverter
    IdmPathUtils idmPathUtils

    def setup() {
        defaultUserGroupCloudService = new DefaultUserGroupCloudService()

        mockDomainService(defaultUserGroupCloudService)
        mockRequestContextHolder(defaultUserGroupCloudService)
        mockIdmExceptionHandler(defaultUserGroupCloudService)

        userGroupAuthorizationService = Mock()
        defaultUserGroupCloudService.userGroupAuthorizationService = userGroupAuthorizationService

        userGroupService = Mock()
        defaultUserGroupCloudService.userGroupService = userGroupService

        userGroupConverter = Mock()
        defaultUserGroupCloudService.userGroupConverter = userGroupConverter

        idmPathUtils = Mock()
        defaultUserGroupCloudService.idmPathUtils = idmPathUtils

    }

    /**
     * Verifies service calls standard 3 authorization services for user group services and the required exception
     * handler to deal with errors
     */
    def "addUserGroup: Calls appropriate authorization services and exception handler"() {
        given:
        def mockUriInfo = Mock(UriInfo)
        def domainId = "domainId"
        def token = "token"

        UserGroup webgroup = new UserGroup().with {
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        defaultUserGroupCloudService.addGroup(mockUriInfo, token, webgroup)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId) >> {throw new ForbiddenException()}
        1 * idmExceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()
    }

    def "addUserGroup: Converts entity and calls backend service"() {
        given:
        def mockUriInfo = Mock(UriInfo)
        def domainId = "domainId"
        def token = "token"
        def createId = "newId"
        def locationHeader = new URI("http://location")

        UserGroup inputWebGroup = new UserGroup().with {
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }

        UserGroup outputWebGroup = new UserGroup()

        com.rackspace.idm.modules.usergroups.entity.UserGroup entityGroup = new com.rackspace.idm.modules.usergroups.entity.UserGroup().with {
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        Response response = defaultUserGroupCloudService.addGroup(mockUriInfo, token, inputWebGroup)

        then:
        1 * userGroupConverter.fromUserGroupWeb(inputWebGroup) >> entityGroup

        // Add service sets the id on the passed in object. Need to simulate this
        userGroupService.addGroup(entityGroup) >> {entityGroup.id = createId; entityGroup}

        // Generates the path
        1 * idmPathUtils.createLocationHeaderValue(mockUriInfo, createId) >> locationHeader

        // Converts the entity
        1 * userGroupConverter.toUserGroupWeb(entityGroup) >> outputWebGroup

        // Returns the same object from the conversion
        response.entity == outputWebGroup // The response includes the output of the converter

        // Creates a 201 response w/ header set to
        response.status == HttpStatus.SC_CREATED
        response.metadata.get("Location")[0] == locationHeader
    }

    /**
     * Verifies service calls standard 3 authorization services for user group services and the required exception
     * handler to deal with errors
     */
    def "getUserGroup: Calls appropriate authorization services and exception handler"() {
        given:
        def domainId = "domainId"
        def token = "token"
        def groupid = "groupid"

        when:
        defaultUserGroupCloudService.getGroupByIdForDomain(token, groupid, domainId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId) >> {throw new ForbiddenException()}
        1 * idmExceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()
    }

    def "getUserGroup: Converts entity and calls backend service"() {
        given:
        def domainId = "domainId"
        def token = "token"
        def groupId = "groupid"

        com.rackspace.idm.modules.usergroups.entity.UserGroup entityGroup = new com.rackspace.idm.modules.usergroups.entity.UserGroup().with {
            it.id = groupId
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }

        UserGroup outputWebGroup = new UserGroup()

        when:
        Response response = defaultUserGroupCloudService.getGroupByIdForDomain(token, groupId, domainId)

        then:
        1 * userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId) >> entityGroup

        // Converts the entity
        1 * userGroupConverter.toUserGroupWeb(entityGroup) >> outputWebGroup

        // Returns the same object from the conversion
        response.entity == outputWebGroup // The response includes the output of the converter

        // Creates a 200 response w/ header set to
        response.status == HttpStatus.SC_OK
    }

    def "deleteUserGroup: Calls appropriate authorization services and exception handler"() {
        given:
        def domainId = "domainId"
        def token = "token"
        def groupId = "groupid"

        com.rackspace.idm.modules.usergroups.entity.UserGroup entityGroup = new com.rackspace.idm.modules.usergroups.entity.UserGroup().with {
            it.id = groupId
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        defaultUserGroupCloudService.deleteGroup(token, domainId, groupId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId) >> entityGroup
        1 * userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId) >> {throw new ForbiddenException()}
        1 * idmExceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()
    }

    def "deleteUserGroup: calls backend service"() {
        given:
        def domainId = "domainId"
        def token = "token"
        def groupId = "groupid"

        com.rackspace.idm.modules.usergroups.entity.UserGroup entityGroup = new com.rackspace.idm.modules.usergroups.entity.UserGroup().with {
            it.id = groupId
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        Response response = defaultUserGroupCloudService.deleteGroup(token, domainId, groupId)

        then:
        1 * userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId) >> entityGroup
        1 * userGroupService.deleteGroup(entityGroup)

        response.status == HttpStatus.SC_NO_CONTENT
    }

}
