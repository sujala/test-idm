package com.rackspace.idm.domain.entity

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import spock.lang.Unroll
import testHelpers.RootServiceTest

class ClientRoleTest extends RootServiceTest {

    @Unroll
    def "getPropagate returns correct value if roleEnum is Propagate - propagate: #propagate; roleType: #roleType"() {
        given:
        ClientRole role = new ClientRole()
        role.setRoleType(roleType)

        expect:
        propagate == role.getPropagate()

        where:
        propagate | roleType
        false     | null
        true      | RoleTypeEnum.PROPAGATE
        false     | RoleTypeEnum.STANDARD
        false     | RoleTypeEnum.RCN
    }
}
