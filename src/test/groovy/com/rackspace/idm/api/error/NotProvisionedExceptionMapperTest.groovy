package com.rackspace.idm.api.error

import com.rackspace.idm.exception.NotProvisionedException
import spock.lang.Shared
import testHelpers.RootServiceTest

class NotProvisionedExceptionMapperTest extends RootServiceTest {
    @Shared NotProvisionedExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new NotProvisionedExceptionMapper()
    }

    def "exception gets mapped to to forbidden"() {
        when:
        def result = exceptionMapper.toResponse(new NotProvisionedException())

        then:
        result.status == 403
    }
}
