package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.exception.BadRequestException
import spock.lang.Specification
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

/**
 * Simply verify the code calls the service appropriately
 */
class CloudUserGroupResourceTest extends RootServiceTest {
    CloudUserGroupResource cloudUserGroupResource

    UserGroupCloudService userGroupCloudService

    def setup() {
        cloudUserGroupResource = new CloudUserGroupResource()

        userGroupCloudService = Mock()
        cloudUserGroupResource.userGroupCloudService = userGroupCloudService

        mockIdentityConfig(cloudUserGroupResource)
    }

    @Unroll
    def "add group overwrites provided domain from path, nulls id: provided id:'#objId'; domain:'#objDomain'"() {
        def mockUriInfo = Mock(UriInfo)
        def mockHttpHeaders = Mock(HttpHeaders)
        def mockResponse = Mock(Response)
        def pathDomainId = "pathDomainId"
        def token = "token"

        UserGroup webgroup = new UserGroup().with {
            it.id = objId
            it.domainId = objDomain
            it.name = "name"
            it.description = "description"
            it
        }

        def passedUserGroup = null

        when:
        cloudUserGroupResource.addGroup(mockHttpHeaders, mockUriInfo, token, pathDomainId, webgroup)

        then:
        1 * userGroupCloudService.addGroup(mockUriInfo, token, _) >> {
                args -> passedUserGroup=args[2]
                mockResponse
        }

        passedUserGroup == webgroup
        passedUserGroup.id == null
        passedUserGroup.domainId == pathDomainId
        passedUserGroup.name == webgroup.name
        passedUserGroup.description == webgroup.description

        where:
        objId | objDomain
        null | null
        "id" | "domain"
    }

    @Unroll
    def "get group passes values as-is to service: domainId:'#domainId'; groupId:'#groupId'"() {
        def mockHttpHeaders = Mock(HttpHeaders)
        def token = "token"

        when:
        cloudUserGroupResource.getGroupById(mockHttpHeaders, token, domainId, groupId)

        then:
        1 * userGroupCloudService.getGroupByIdForDomain(token, groupId, domainId)

        where:
        domainId | groupId
        null | null
        "domain" | "groupId"
    }

    @Unroll
    def "setRoleAssignments: get group passes values as-is to service: domainId:'#domainId'; groupId:'#groupId'"() {
        def mockHttpHeaders = Mock(HttpHeaders)
        def token = "token"
        def roleAssignments = new RoleAssignments()

        when:
        cloudUserGroupResource.grantRolesToGroup(mockHttpHeaders, token, domainId, groupId, roleAssignments)

        then:
        1 * userGroupCloudService.grantRolesToGroup(token, domainId, groupId, roleAssignments)

        where:
        domainId | groupId
        null | null
        "domain" | "groupId"
    }

    @Unroll
    def "listRoleAssignments: list group roles passes values appropriately to backend service: domainId:'#domainId'; groupId:'#groupId'; marker:'#marker'; limit:'#limit'"() {
        def mockUriInfo = Mock(UriInfo)
        def mockHttpHeaders = Mock(HttpHeaders)
        def token = "token"

        identityConfig.getStaticConfig().getLdapPagingDefault() >> 5
        identityConfig.getStaticConfig().getLdapPagingMaximum() >> 1000

        when:
        cloudUserGroupResource.getRolesOnGroup(mockHttpHeaders, mockUriInfo, token, domainId, groupId, marker, limit)

        then:
        1 * userGroupCloudService.listRoleAssignmentsOnGroup(mockUriInfo, token, domainId, groupId, _ ) >> {args ->
            UserGroupRoleSearchParams params = (UserGroupRoleSearchParams) args[4]
            assert params.paginationRequest != null
            assert params.paginationRequest.marker == marker
            assert params.paginationRequest.limit == limit

            null // Return null. No testing performed on response
        }

        where:
        domainId | groupId | marker | limit
        "domain" | "groupId" | 5 | 10
        "domain" | "groupId" | 0 | 10
    }

    /**
     * Verify business logic surrounding pagination is enforced.
     *
     * Marker
     * 1. The default, if not supplied, is 0
     *
     * Limit
     * 1. The default, if not supplied, is controlled by a config property
     * 2. If the supplied value == 0, then set to config default
     * 2. The max allowed value is controlled by a different config property
     * 3. If the supplied value > max allowed value, the max allowed value is used
     *
     * @return
     */
    @Unroll
    def "listRoleAssignments: list group roles enforces marker/limit values: domainId:'#domainId'; groupId:'#groupId'; marker:'#marker'; limit:'#limit'"() {
        def mockUriInfo = Mock(UriInfo)
        def mockHttpHeaders = Mock(HttpHeaders)
        def token = "token"

        identityConfig.getStaticConfig().getLdapPagingDefault() >> limitDefault
        identityConfig.getStaticConfig().getLdapPagingMaximum() >> limitMax

        when:
        cloudUserGroupResource.getRolesOnGroup(mockHttpHeaders, mockUriInfo, token, "domainId", "groupId", providedMarker, providedLimit)

        then:
        1 * userGroupCloudService.listRoleAssignmentsOnGroup(mockUriInfo, token, _, _, _ ) >> {args ->
            UserGroupRoleSearchParams params = (UserGroupRoleSearchParams) args[4]
            assert params.paginationRequest != null
            assert params.paginationRequest.marker == effectiveMarker
            assert params.paginationRequest.limit == effectiveLimit

            null // Return null. No testing performed on response
        }

        where:
        limitDefault    | limitMax | providedMarker  | effectiveMarker   | providedLimit | effectiveLimit
        5               | 100       | 10              | 10                | 20            | 20
        5               | 10        | null            | 0                 | null          | 5
        5               | 10        | null            | 0                 | 100           | 10
        5               | 10        | null            | 0                 | 10            | 10
        5               | 10        | null            | 0                 | 0             | 5
    }

    def "listRoleAssignments: list group roles throws exceptions for negative marker/limit values"() {
        def mockUriInfo = Mock(UriInfo)
        def mockHttpHeaders = Mock(HttpHeaders)
        def token = "token"

        identityConfig.getStaticConfig().getLdapPagingDefault() >> 5
        identityConfig.getStaticConfig().getLdapPagingMaximum() >> 10

        when:
        cloudUserGroupResource.getRolesOnGroup(mockHttpHeaders, mockUriInfo, token, "domainId", "groupId", -1, 10)

        then:
        thrown(BadRequestException)

        when:
        cloudUserGroupResource.getRolesOnGroup(mockHttpHeaders, mockUriInfo, token, "domainId", "groupId", 5, -10)

        then:
        thrown(BadRequestException)
    }


}
