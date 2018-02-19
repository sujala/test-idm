package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.NotFoundException
import org.apache.commons.lang3.StringUtils
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response

class Cloud20VersionResourceTest extends RootServiceTest {

    @Shared Cloud20VersionResource service

    def setup() {
        service = new Cloud20VersionResource()
        mockConfiguration(service)
        mockCloud20Service(service)
    }

    def "validateOffset null offset sets offset to 0"() {
        when:
        def offset = service.validateMarker(null)

        then:
        offset == 0
    }

    def "validateOffset negative offset throws bad request"() {
        when:
        service.validateMarker(-5)

        then:
        thrown(BadRequestException)
    }

    def "validateOffset valid offset sets offset"() {
        when:
        def offset = service.validateMarker(10)

        then:
        offset == 10
    }

    def "validateLimit null limit sets limit to default"() {
        when:
        config.getInt(_) >> 25
        def limit = service.validateLimit(null)

        then:
        limit == 25
    }

    def "validateLimit negative limit throws bad request"() {
        when:
        service.validateLimit(-5)

        then:
        thrown(BadRequestException)
    }

    def "validateLimit limit is 0 sets to default"() {
        when:
        config.getInt(_) >> 25
        def limit = service.validateLimit(0)

        then:
        limit == 25
    }

    def "validateLimit limit is too large sets to default max"() {
        when:
        config.getInt(_) >> 99
        def value = 100
        def limit = service.validateLimit(value)

        then:
        limit == 99
    }

    def "validateLimit limit is valid sets limit"() {
        when:
        config.getInt(_) >> 100
        def value = 99
        def limit = service.validateLimit(value)

        then:
        limit == value
    }

    @Unroll
    def "get user global roles: Calls appropriate backend service based on whether serviceId provided: serviceId: #serviceId; applyRcnRoles: #applyRcnRoles"() {
        def mockHttpHeaders = Mock(HttpHeaders)
        def token = "token"
        def userId = "userId"

        def response = Response.ok()

        when:
        service.listUserGlobalRoles(mockHttpHeaders, token, userId, serviceId, applyRcnRoles)

        then:
        if (StringUtils.isNotBlank(serviceId)) {
            1 * defaultCloud20Service.listUserGlobalRolesByServiceId(mockHttpHeaders, token, userId, serviceId, applyRcnRoles) >> response
        } else {
            1 * defaultCloud20Service.listUserGlobalRoles(mockHttpHeaders, token, userId, applyRcnRoles) >> response
        }

        where:
        [serviceId, applyRcnRoles] << [["serviceId", "", null], [true, false]].combinations()
    }

    def "grantRolesToUser: Calls appropriate service"() {
        given:
        def mockHttpHeaders = Mock(HttpHeaders)
        mockIdentityConfig(service)
        mockCloud20Service(service)
        def mockResponseBuilder = Mock(Response.ResponseBuilder)

        def userId = "userId"
        def roleAssignments = new RoleAssignments()

        when:
        service.grantRolesToUser(mockHttpHeaders, authToken, userId, roleAssignments)

        then:
        1 * reloadableConfig.isGrantRolesToUserServiceEnabled() >> true
        1 * defaultCloud20Service.grantRolesToUser(mockHttpHeaders, authToken, userId, roleAssignments) >> mockResponseBuilder
    }

    def "grantRolesToUser: test feature flag when set to false"() {
        given:
        mockIdentityConfig(service)

        def userId = "userId"
        def roleAssignments = new RoleAssignments()

        when:
        service.grantRolesToUser(headers, authToken, userId, roleAssignments)

        then:
        1 * reloadableConfig.isGrantRolesToUserServiceEnabled() >> false
        thrown(NotFoundException)
    }
}
