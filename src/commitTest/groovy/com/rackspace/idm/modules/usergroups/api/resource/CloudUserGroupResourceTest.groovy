package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import spock.lang.Specification
import spock.lang.Unroll

import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

/**
 * Simply verify the code calls the service appropriately
 */
class CloudUserGroupResourceTest extends Specification {
    CloudUserGroupResource cloudUserGroupResource

    UserGroupCloudService userGroupCloudService

    def setup() {
        userGroupCloudService = Mock()
        cloudUserGroupResource = new CloudUserGroupResource()
        cloudUserGroupResource.userGroupCloudService = userGroupCloudService
    }

    @Unroll
    def "add group overwrites provided domain from path, nulls id: provided id:'#objId'; domain:'#objDomain'"() {
        def mockUriInfo = Mock(UriInfo)
        def mockHttpHeaders = Mock(HttpHeaders)
        def mockResponse = Mock(Response)
        def pathDomainId = "pathDomainId"
        def token = "token"

        UserGroup webgroup = new UserGroup().with {
            it.id = "id"
            it.domainId = "domainId"
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
        def pathDomainId = "pathDomainId"
        def token = "token"

        when:
        cloudUserGroupResource.getGroupById(mockHttpHeaders, token, pathDomainId, groupId)

        then:

        1 * userGroupCloudService.getGroupByIdForDomain(token, groupId, pathDomainId)

        where:
        domainId | groupId
        null | null
        "domain" | "groupId"
    }

}
