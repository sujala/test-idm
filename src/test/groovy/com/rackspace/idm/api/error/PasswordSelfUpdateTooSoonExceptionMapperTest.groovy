package com.rackspace.idm.api.error

import com.rackspace.idm.exception.PasswordSelfUpdateTooSoonException
import spock.lang.Shared
import testHelpers.RootServiceTest

class PasswordSelfUpdateTooSoonExceptionMapperTest extends RootServiceTest {
    @Shared PasswordSelfUpdateTooSoonExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new PasswordSelfUpdateTooSoonExceptionMapper()
    }

    def "exception gets mapped to conflict"() {
        when:
        def result = exceptionMapper.toResponse(new PasswordSelfUpdateTooSoonException())

        then:
        result.status == 409
    }
}
