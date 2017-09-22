package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.idm.api.resource.IdmPathUtils
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.modules.usergroups.api.resource.converter.RoleAssignmentConverter
import com.rackspace.idm.modules.usergroups.service.UserGroupService
import org.apache.http.HttpStatus
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response

class RevokeRoleAssignmentFromUserGroupCloudServiceTest extends RootServiceTest {

    DefaultUserGroupCloudService defaultUserGroupCloudService
    UserGroupAuthorizationService userGroupAuthorizationService
    UserGroupService userGroupService
    RoleAssignmentConverter roleAssignmentConverter
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

        roleAssignmentConverter = Mock()
        defaultUserGroupCloudService.roleAssignmentConverter = roleAssignmentConverter

        idmPathUtils = Mock()
        defaultUserGroupCloudService.idmPathUtils = idmPathUtils
    }

    /**
     * Verifies service calls standard 3 authorization services for user group services and the required exception
     * handler to deal with errors
     */
    def "revokeRoleFromGroup: Calls appropriate authorization services and exception handler"() {
        given:
        def domainId = "domainId"
        def token = "token"
        def groupid = "groupid"
        def roleId = "roleId"

        when:
        defaultUserGroupCloudService.revokeRoleFromGroup(token, domainId, groupid, roleId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId) >> {throw new ForbiddenException()}
        1 * idmExceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()
    }

    def "revokeRoleFromGroup: Calls backend service and returns appropriate response"() {
        given:
        def domainId = "domainId"
        def token = "token"
        def groupid = "groupid"
        def roleId = "roleId"
        com.rackspace.idm.modules.usergroups.entity.UserGroup entityGroup = new com.rackspace.idm.modules.usergroups.entity.UserGroup().with {
            it.id = groupid
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        Response response = defaultUserGroupCloudService.revokeRoleFromGroup(token, domainId, groupid, roleId)

        then:
        1 * userGroupService.checkAndGetGroupByIdForDomain(groupid, domainId) >> entityGroup
        1 * userGroupService.revokeRoleAssignmentFromGroup(entityGroup, roleId)

        // Creates a 204 response w/ not entity
        response.status == HttpStatus.SC_NO_CONTENT
        response.entity == null
    }
}
