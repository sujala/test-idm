package com.rackspace.idm.api.error

import com.rackspace.idm.exception.UserDisabledException
import spock.lang.Shared
import testHelpers.RootServiceTest

class UserDisabledExceptionMapperTest extends RootServiceTest {
    @Shared UserDisabledExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new UserDisabledExceptionMapper()
    }

    def "exception gets mapped to xxx"() {
        when:
        def result = exceptionMapper.toResponse(new UserDisabledException())

        then:
        result.status == 403
    }
}
