package com.rackspace.idm.api.error

import com.rackspace.idm.exception.ForbiddenException
import spock.lang.Shared
import testHelpers.RootServiceTest

class ForbiddenExceptionMapperTest extends RootServiceTest {
    @Shared ForbiddenExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new ForbiddenExceptionMapper()
    }

    def "exception gets mapped to xxx"() {
        when:
        def result = exceptionMapper.toResponse(new ForbiddenException())

        then:
        result.status == 403
    }
}
