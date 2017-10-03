package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.api.converter.cloudv20.UserConverterCloudV20
import com.rackspace.idm.api.resource.IdmPathUtils
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import com.rackspace.idm.domain.entity.EndUser
import com.rackspace.idm.domain.entity.PaginatorContext
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.modules.usergroups.api.resource.converter.RoleAssignmentConverter
import com.rackspace.idm.modules.usergroups.api.resource.converter.UserGroupConverter
import com.rackspace.idm.modules.usergroups.service.UserGroupService
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.ObjectFactory
import org.openstack.docs.identity.api.v2.UserList
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo
import javax.xml.bind.JAXBElement

class DefaultUserGroupCloudServiceTest extends RootServiceTest {

    DefaultUserGroupCloudService defaultUserGroupCloudService
    UserGroupAuthorizationService userGroupAuthorizationService
    UserGroupService userGroupService
    UserGroupConverter userGroupConverter
    IdmPathUtils idmPathUtils
    RoleAssignmentConverter roleAssignmentConverter

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

        roleAssignmentConverter = Mock()
        defaultUserGroupCloudService.roleAssignmentConverter = roleAssignmentConverter
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

    def "listGroupsForDomain: Calls appropriate authorization services and exception handler"() {
        given:
        def domainId = "domainId"
        def token = "token"

        when:
        defaultUserGroupCloudService.listGroupsForDomain(token, domainId, new UserGroupSearchParams(null))

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId) >> {throw new ForbiddenException()}
        1 * idmExceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()
    }

    @Unroll
    def "listGroupsForDomain: calls backend service; name=#name"() {
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

        List<com.rackspace.idm.modules.usergroups.entity.UserGroup> entityGroups = new ArrayList<>()
        entityGroups.add(entityGroup)

        when:
        Response response = defaultUserGroupCloudService.listGroupsForDomain(token, domainId, new UserGroupSearchParams(name))

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId)
        if (name == null) {
            1 * userGroupService.getGroupsForDomain(domainId) >> entityGroups
        } else {
            1 * userGroupService.getGroupByNameForDomain(name, domainId) >> entityGroup
        }
        response.status == HttpStatus.SC_OK

        where:
        name << [null, "name"]
    }

    def "getUsersInGroup: Calls appropriate authorization services and exception handler"() {
        given:
        def mockUriInfo = Mock(UriInfo)
        def domainId = "domainId"
        def token = "token"
        def groupId = "groupid"

        when:
        defaultUserGroupCloudService.getUsersInGroup(mockUriInfo,  token, domainId, groupId, new UserSearchCriteria(new PaginationParams()))

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId) >> {throw new ForbiddenException()}
        1 * idmExceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()
    }

    def "getUsersInGroup: calls backend service"() {
        given:
        def mockUriInfo = Mock(UriInfo)
        def idmPathUtils = Mock(IdmPathUtils)
        defaultUserGroupCloudService.idmPathUtils = idmPathUtils
        def objFactories = Mock(JAXBObjectFactories)
        def openStackIdentityV2Factory = Mock(ObjectFactory)
        objFactories.getOpenStackIdentityV2Factory() >> openStackIdentityV2Factory
        defaultUserGroupCloudService.objFactories = objFactories
        def userConverter = Mock(UserConverterCloudV20)
        defaultUserGroupCloudService.userConverterCloudV20 = userConverter

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
        UserSearchCriteria searchCriteria = new UserSearchCriteria(new PaginationParams())
        User user = entityFactory.createUser("username", "id", domainId, "ORD")

        when:
        Response response = defaultUserGroupCloudService.getUsersInGroup(mockUriInfo, token, domainId, groupId, searchCriteria)

        then:
        1 * userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId) >> entityGroup
        1 * userGroupService.getUsersInGroup(entityGroup, searchCriteria) >> { args ->
            UserSearchCriteria searchParams = args[1]
            assert searchParams.paginationRequest.effectiveMarker == 0
            assert searchParams.paginationRequest.effectiveLimit == 1000
            def pc = new PaginatorContext<EndUser>()
            pc.update([user].asList(), 0, 1000)
            pc
        }
        1 * idmPathUtils.createLinkHeader(_,_) >> "links"
        1 * openStackIdentityV2Factory.createUsers(_) >> new JAXBElement<UserList>(ObjectFactory._Users_QNAME, UserList.class, null, [user].asList())
        1 * userConverter.toUserList(_) >> new UserList()

        response.status == HttpStatus.SC_OK
    }

    def "getRoleOnGroup: Calls appropriate authorization services and exception handler"() {
        given:
        def domainId = "domainId"
        def token = "token"
        def groupId = "groupId"
        def roleId = "roleId"

        when:
        defaultUserGroupCloudService.getRoleOnGroup(token, domainId, groupId, roleId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId) >> {throw new ForbiddenException()}
        1 * idmExceptionHandler.exceptionResponse(_ as ForbiddenException) >> Response.serverError()
    }

    @Unroll
    def "getRoleOnGroup: calls backend service"() {
        given:
        def domainId = "domainId"
        def token = "token"
        def groupId = "groupid"
        def roleId = "roleId"

        com.rackspace.idm.modules.usergroups.entity.UserGroup entityGroup = new com.rackspace.idm.modules.usergroups.entity.UserGroup().with {
            it.id = groupId
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

        TenantAssignment tenantAssignment = new TenantAssignment()

        when:
        Response response = defaultUserGroupCloudService.getRoleOnGroup(token, domainId, groupId, roleId)

        then:
        1 * securityContext.getAndVerifyEffectiveCallerToken(token)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * userGroupAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId)

        1 * userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId) >> entityGroup
        1 * userGroupService.getRoleAssignmentOnGroup(entityGroup, roleId) >> tenantRole

        1 * roleAssignmentConverter.toRoleAssignmentWeb(tenantRole) >> tenantAssignment

        response.entity == tenantAssignment
        response.status == HttpStatus.SC_OK
    }


    def "getRoleOnGroup: throws NotFoundException if given retrieved tenantRole is null"() {
        given:
        def domainId = "domainId"
        def token = "token"
        def groupId = "groupid"
        def roleId = "roleId"

        com.rackspace.idm.modules.usergroups.entity.UserGroup entityGroup = new com.rackspace.idm.modules.usergroups.entity.UserGroup().with {
            it.id = groupId
            it.domainId = domainId
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        defaultUserGroupCloudService.getRoleOnGroup(token, domainId, groupId, roleId)

        then:
        1 * userGroupService.checkAndGetGroupByIdForDomain(groupId, domainId) >> entityGroup
        1 * userGroupService.getRoleAssignmentOnGroup(entityGroup, roleId) >> null

        1 * idmExceptionHandler.exceptionResponse(_ as NotFoundException) >> Response.serverError()
    }
}
