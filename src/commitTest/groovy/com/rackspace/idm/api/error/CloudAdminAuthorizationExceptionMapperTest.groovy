package com.rackspace.idm.api.error

import com.rackspace.idm.exception.CloudAdminAuthorizationException
import spock.lang.Shared
import testHelpers.RootServiceTest

class CloudAdminAuthorizationExceptionMapperTest extends RootServiceTest {
    @Shared CloudAdminAuthorizationExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new CloudAdminAuthorizationExceptionMapper()
    }

    def "exception gets mapped to method not allowed"() {
        when:
        def result = exceptionMapper.toResponse(new CloudAdminAuthorizationException())

        then:
        result.status == 405
    }
}
