package com.rackspace.idm.api.error

import com.rackspace.idm.exception.DuplicateUsernameException
import spock.lang.Shared
import testHelpers.RootServiceTest

class DuplicateUsernameExceptionMapperTest extends RootServiceTest {
    @Shared DuplicateUsernameExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new DuplicateUsernameExceptionMapper()
    }

    def "exception gets mapped to conflict"() {
        when:
        def result = exceptionMapper.toResponse(new DuplicateUsernameException())

        then:
        result.status == 409
    }
}
