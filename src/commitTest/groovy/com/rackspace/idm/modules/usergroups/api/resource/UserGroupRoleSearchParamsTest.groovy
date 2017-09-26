package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.idm.api.resource.cloud.v20.PaginationParams
import spock.lang.Specification

class UserGroupRoleSearchParamsTest extends Specification {

    def "Default constuctor populates pagination w/ defaults"() {
        when:
        UserGroupRoleSearchParams params = new UserGroupRoleSearchParams(new PaginationParams())

        then:
        params.paginationRequest != null
        params.paginationRequest.limit == null
        params.paginationRequest.marker == null
    }

    def "Pagination constuctor populates pagination request"() {
        given:
        def paginationParams = new PaginationParams(1, 5)

        when:
        UserGroupRoleSearchParams params = new UserGroupRoleSearchParams(paginationParams)

        then:
        params.paginationRequest != null
        params.paginationRequest == paginationParams
    }
}
