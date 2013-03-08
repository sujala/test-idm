package com.rackspace.idm.api.error

import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.ws.rs.WebApplicationException

class WebApplicationExceptionMapperTest extends RootServiceTest {
    @Shared WebApplicationExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new WebApplicationExceptionMapper()
    }

    def "exception gets mapped to internal server error"() {
        when:
        def result = exceptionMapper.toResponse(new WebApplicationException())

        then:
        result.status == 500
    }
}
