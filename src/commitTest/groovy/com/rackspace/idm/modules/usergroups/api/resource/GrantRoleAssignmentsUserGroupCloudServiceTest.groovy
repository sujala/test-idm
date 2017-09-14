package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.api.resource.IdmPathUtils
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.modules.usergroups.api.resource.converter.RoleAssignmentConverter
import com.rackspace.idm.modules.usergroups.api.resource.converter.UserGroupConverter
import com.rackspace.idm.modules.usergroups.service.UserGroupService
import org.apache.http.HttpStatus
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response

class GrantRoleAssignmentsUserGroupCloudServiceTest extends RootServiceTest {

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
    def "grantRoleAssignments: Calls appropriate authorization services and exception handler"() {
        given:
        def domainId = "domainId"
        def token = "token"
        def groupid = "groupid"
        def roleAssignments = new RoleAssignments()

        when:
        defaultUserGroupCloudService.grantRolesToGroup(token, domainId, groupid, roleAssignments)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId) >> {throw new ForbiddenException()}
        1 * idmExceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()
    }

    def "grantRoleAssignments: Converts entity and calls backend service"() {
        given:
        def domainId = "domainId"
        def token = "token"
        def groupid = "groupid"
        def roleAssignments = new RoleAssignments()

        com.rackspace.idm.modules.usergroups.entity.UserGroup entityGroup = new com.rackspace.idm.modules.usergroups.entity.UserGroup().with {
            it.id = groupid
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }
        TenantRole tenantRole = new TenantRole().with {
            it.name = "roleName"
            it.roleRsId = "roleId"
            it
        }
        def groupRoles = [tenantRole]

        RoleAssignments outputWebRoleAssignments = new RoleAssignments()

        when:
        Response response = defaultUserGroupCloudService.grantRolesToGroup(token, domainId, groupid, roleAssignments)

        then:
        1 * userGroupService.checkAndGetGroupByIdForDomain(groupid, domainId) >> entityGroup
        1 * userGroupService.replaceRoleAssignmentsOnGroup(entityGroup, roleAssignments) >> [tenantRole]
        1 * userGroupService.getRoleAssignmentsOnGroup(groupid) >> groupRoles

        // Converts the roles using converter
        1 * roleAssignmentConverter.toRoleAssignmentsWeb(groupRoles) >> outputWebRoleAssignments

        // Returns the same object from the conversion
        response.entity == outputWebRoleAssignments // The response includes the output of the converter

        // Creates a 200 response w/ header set to
        response.status == HttpStatus.SC_OK
    }

    def "grantRoleAssignments: Returns fault if passed null role assignments"() {
        given:
        def domainId = "domainId"
        def token = "token"
        def groupid = "groupid"
        def roleAssignments = null

        securityContext.getAndVerifyEffectiveCallerToken(token)
        requestContext.getAndVerifyEffectiveCallerIsEnabled()
        userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId)

        when:
        defaultUserGroupCloudService.grantRolesToGroup(token, domainId, groupid, roleAssignments)

        then:
        1 * idmExceptionHandler.exceptionResponse(_ as BadRequestException) >> Response.serverError()
    }

}
