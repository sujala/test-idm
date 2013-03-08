package com.rackspace.idm.api.error

import com.rackspace.idm.exception.NotAuthorizedException
import spock.lang.Shared
import testHelpers.RootServiceTest

class NotAuthorizedExceptionMapperTest extends RootServiceTest {
    @Shared NotAuthorizedExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new NotAuthorizedExceptionMapper()
    }

    def "exception gets mapped to unauthorized"() {
        when:
        def result = exceptionMapper.toResponse(new NotAuthorizedException())

        then:
        result.status == 401
    }
}
