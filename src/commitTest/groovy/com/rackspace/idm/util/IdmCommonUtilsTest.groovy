package com.rackspace.idm.util

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class IdmCommonUtilsTest extends Specification {

    @Shared
    public IdmCommonUtils idmCommonUtils

    def setupSpec() {
        idmCommonUtils = new IdmCommonUtils();
    }

    @Unroll
    def "test getBoolean; value = #value, expected = #expected"() {
        when:
        def result = idmCommonUtils.getBoolean(value)

        then:
        result == expected

        where:
        value  | expected
        "true" | true
        "TRUE" | true
        "false"| false
        "FALSE"| false
        "0"    | null
        "bad"  | null
        ""     | null
        "  "   | null
    }
}
