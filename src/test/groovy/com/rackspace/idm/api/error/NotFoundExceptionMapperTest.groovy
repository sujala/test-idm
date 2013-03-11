package com.rackspace.idm.api.error

import com.rackspace.idm.exception.NotFoundException
import spock.lang.Shared
import testHelpers.RootServiceTest

class NotFoundExceptionMapperTest extends RootServiceTest {
    @Shared NotFoundExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new NotFoundExceptionMapper()
    }

    def "exception gets mapped to not found"() {
        when:
        def result = exceptionMapper.toResponse(new NotFoundException())

        then:
        result.status == 404
    }
}
