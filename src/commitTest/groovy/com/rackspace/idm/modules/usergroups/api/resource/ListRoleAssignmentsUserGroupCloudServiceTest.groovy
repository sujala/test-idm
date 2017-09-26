package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.idm.api.resource.IdmPathUtils
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import com.rackspace.idm.domain.entity.PaginatorContext
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotAuthorizedException
import com.rackspace.idm.modules.usergroups.api.resource.converter.RoleAssignmentConverter
import com.rackspace.idm.modules.usergroups.service.UserGroupService
import org.apache.http.HttpStatus
import org.springframework.http.HttpHeaders
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

class ListRoleAssignmentsUserGroupCloudServiceTest extends RootServiceTest {

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
    def "listRoleAssignmentsOnGroup: Calls appropriate authorization services and exception handler"() {
        given:
        def mockUriInfo = Mock(UriInfo)
        def domainId = "domainId"
        def token = "token"
        def groupid = "groupid"
        def searchParams = new UserGroupRoleSearchParams(new PaginationParams())

        when:
        defaultUserGroupCloudService.listRoleAssignmentsOnGroup(mockUriInfo, token, domainId, groupid, searchParams)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId) >> {throw new ForbiddenException()}
        1 * idmExceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()
    }

    @Unroll
    def "listRoleAssignmentsOnGroup: Converts entity and calls backend service w/ query params"() {
        given:
        def mockUriInfo = Mock(UriInfo)
        def domainId = "domainId"
        def token = "token"
        def groupid = "groupid"
        def searchParams = new UserGroupRoleSearchParams(new PaginationParams())

        com.rackspace.idm.modules.usergroups.entity.UserGroup entityGroup = new com.rackspace.idm.modules.usergroups.entity.UserGroup().with {
            it.id = groupid
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }
        List<TenantRole> tenantRoles = [new TenantRole().with {
            it.name = "roleName"
            it.roleRsId = "roleId"
            it
        }]

        def pc = new PaginatorContext<TenantRole>()
        pc.update(tenantRoles, searchParams.paginationRequest.getEffectiveMarker(), searchParams.paginationRequest.getEffectiveLimit())

        RoleAssignments outputWebRoleAssignments = new RoleAssignments()

        when:
        Response response = defaultUserGroupCloudService.listRoleAssignmentsOnGroup(mockUriInfo, token, domainId, groupid, searchParams)

        then:
        // Verifies will throw NotFoundException if group doesn't exist
        1 * userGroupService.checkAndGetGroupByIdForDomain(groupid, domainId) >> entityGroup

        // Verifies the call to backend service is based on parameters supplied in request
        1 * userGroupService.getRoleAssignmentsOnGroup(entityGroup, searchParams) >> pc

        // Converts the roles using converter
        1 * roleAssignmentConverter.toRoleAssignmentsWeb(tenantRoles) >> outputWebRoleAssignments

        // Calls path utils to generate the link header
        1 * idmPathUtils.createLinkHeader(mockUriInfo, pc) >> "something"

        // Returns the converted object
        response.entity == outputWebRoleAssignments

        // Returns a 200 response
        response.status == HttpStatus.SC_OK

        // Returns a link header
        response.metadata.get(HttpHeaders.LINK)[0] == "something"
    }

    def "listRoleAssignmentsOnGroup: Returns fault if passed in invalid arguments"() {
        given:
        def mockUriInfo = Mock(UriInfo)
        def domainId = "domainId"
        def token = "token"
        def groupid = "groupid"
        def searchParams = new UserGroupRoleSearchParams()

        securityContext.getAndVerifyEffectiveCallerToken(token)
        requestContext.getAndVerifyEffectiveCallerIsEnabled()
        userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId)

        when: "url info is null"
        defaultUserGroupCloudService.listRoleAssignmentsOnGroup(null, token, domainId, groupid, searchParams)

        then:
        1 * idmExceptionHandler.exceptionResponse(_ as IllegalArgumentException) >> Response.serverError()

        when: "token is null"
        defaultUserGroupCloudService.listRoleAssignmentsOnGroup(mockUriInfo, null, domainId, groupid, searchParams)

        then: "caught by security context check"
        1 * securityContext.getAndVerifyEffectiveCallerToken(null) >> {throw new NotAuthorizedException()}
        1 * idmExceptionHandler.exceptionResponse(_ as NotAuthorizedException) >> Response.serverError()

        when: "domainId is null"
        defaultUserGroupCloudService.listRoleAssignmentsOnGroup(mockUriInfo, token, null, groupid, searchParams)

        then:
        1 * idmExceptionHandler.exceptionResponse(_ as IllegalArgumentException) >> Response.serverError()

        when: "domainId is whitespace"
        defaultUserGroupCloudService.listRoleAssignmentsOnGroup(mockUriInfo, token, " ", groupid, searchParams)

        then:
        1 * idmExceptionHandler.exceptionResponse(_ as IllegalArgumentException) >> Response.serverError()

        when: "groupId is null"
        defaultUserGroupCloudService.listRoleAssignmentsOnGroup(mockUriInfo, token, domainId, null, searchParams)

        then:
        1 * idmExceptionHandler.exceptionResponse(_ as IllegalArgumentException) >> Response.serverError()

        when: "groupId is whitespace"
        defaultUserGroupCloudService.listRoleAssignmentsOnGroup(mockUriInfo, token, domainId, " ", searchParams)

        then:
        1 * idmExceptionHandler.exceptionResponse(_ as IllegalArgumentException) >> Response.serverError()

        when: "search params is null"
        defaultUserGroupCloudService.listRoleAssignmentsOnGroup(mockUriInfo, token, domainId, groupid, null)

        then:
        1 * idmExceptionHandler.exceptionResponse(_ as IllegalArgumentException) >> Response.serverError()
    }

}
