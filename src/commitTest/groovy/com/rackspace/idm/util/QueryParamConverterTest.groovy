package com.rackspace.idm.util

import com.rackspace.idm.domain.entity.User.UserType
import spock.lang.Specification
import spock.lang.Unroll

class QueryParamConverterTest extends Specification {

    @Unroll
    def "test that invalid param value returns default as VERIFIED for: #invalidInput" () {

        when: "invalid or unexpected input is passed"
        UserType userType= QueryParamConverter.convertUserTypeParamToEnum(invalidInput)

        then: "default user type VERIFIED is return"
        UserType.VERIFIED == userType
        UserType.ALL != userType
        UserType.UNVERIFIED != userType

        and: "return value is never null"
        null != userType

        where:
        invalidInput << ["", null, "null", "garbage#;-)", "Verifieded", "23234551", "funInLife"]
    }

    @Unroll
    def "test that expected enum type is return against case insensitive : #queryParams"(){

        when: "case insensitive input is passed"
        UserType userType= QueryParamConverter.convertUserTypeParamToEnum(queryParams)

        then: "expected user type enum value is returned"
        userType == expectedUserType

        where:
        queryParams  | expectedUserType
        "VERIFIED"   | UserType.VERIFIED
        "verified"   | UserType.VERIFIED
        "VErIFeEd"   | UserType.VERIFIED

        "UNVERIFIED" | UserType.UNVERIFIED
        "unverified" | UserType.UNVERIFIED
        "uNverIfied" | UserType.UNVERIFIED

        "ALL"        | UserType.ALL
        "All"        | UserType.ALL
        "all"        | UserType.ALL
        "aLL"        | UserType.ALL
    }
}
