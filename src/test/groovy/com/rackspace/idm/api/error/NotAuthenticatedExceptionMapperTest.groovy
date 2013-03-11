package com.rackspace.idm.api.error

import com.rackspace.idm.exception.NotAuthenticatedException
import spock.lang.Shared
import testHelpers.RootServiceTest

class NotAuthenticatedExceptionMapperTest extends RootServiceTest {
    @Shared NotAuthenticatedExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new NotAuthenticatedExceptionMapper()
    }

    def "exception gets mapped to unauthorized"() {
        when:
        def result = exceptionMapper.toResponse(new NotAuthenticatedException())

        then:
        result.status == 401
    }
}
