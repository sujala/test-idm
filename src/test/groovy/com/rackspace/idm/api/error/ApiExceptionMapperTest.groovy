package com.rackspace.idm.api.error

import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response

class ApiExceptionMapperTest extends RootServiceTest {

    @Shared ApiExceptionMapper apiExceptionMapper = new ApiExceptionMapper();

    def "Exceptions are mapped to 500s"() {
        when:
        Response response = apiExceptionMapper.toResponse(new RuntimeException())

        then:
        response.status == 500
    }

}
