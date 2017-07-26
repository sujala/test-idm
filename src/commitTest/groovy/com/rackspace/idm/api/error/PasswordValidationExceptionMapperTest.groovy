package com.rackspace.idm.api.error

import com.rackspace.idm.exception.PasswordValidationException
import spock.lang.Shared
import testHelpers.RootServiceTest

class PasswordValidationExceptionMapperTest extends RootServiceTest {
    @Shared PasswordValidationExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new PasswordValidationExceptionMapper()
    }

    def "exception gets mapped to bad request"() {
        when:
        def result = exceptionMapper.toResponse(new PasswordValidationException())

        then:
        result.status == 400
    }
}
