package com.rackspace.idm.aspect

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification


@ContextConfiguration(locations = "classpath:app-config.xml")
class TestAop extends Specification {

    @Autowired
    TestAspectTarget aspectTarget

    def "test aop"() {
        when:
        aspectTarget.foo(TestAspect.INTERCEPT_VALUE)

        then:
        thrown(RuntimeException)

        when:
        def returnValue = aspectTarget.foo(TestAspect.INTERCEPT_VALUE + 1)

        then:
        returnValue == TestAspect.INTERCEPT_VALUE + 1
    }
}
