package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotAuthenticatedException
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

import javax.ws.rs.WebApplicationException


class DefaultMultiFactorCloud20ServiceTest extends RootServiceTest {
    @Shared DefaultMultiFactorCloud20Service service

    def setupSpec() {
        service = new DefaultMultiFactorCloud20Service()
    }

    def setup() {
        mockConfiguration(service)
        mockCloud20Service(service)
        mockUserService(service)
        mockMultiFactorService(service)
        mockExceptionHandler(service)
        mockPhoneCoverterCloudV20(service)
    }

    def "listDevicesForUser validates x-auth-token"() {
        when:
        allowMultiFactorAccess()
        service.listDevicesForUser(null, "token", null)

        then:
        defaultCloud20Service.getScopeAccessForValidToken(_) >> { throw new NotAuthenticatedException() }
    }

    def "validateListDevicesForUser verifies the user is the same"() {
        given:
        def user = entityFactory.createUser().with {
            it.id = id1
            it
        }
        def userId = id2

        when:
        boolean exceptionThrown = false
        try {
            service.validateListDevicesForUser(user, userId)
        } catch (ForbiddenException) {
            exceptionThrown = true
        }

        then:
        exceptionThrown == expectedResult

        where:
        id1     | id2   | expectedResult
        "id"    | "id"  | false
        "id"    | "not" | true
    }


}
