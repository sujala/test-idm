package com.rackspace.idm.domain.entity

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import spock.lang.Unroll
import testHelpers.RootServiceTest

class ClientRoleTest extends RootServiceTest {

    @Unroll
    def "getPropagate returns true if either propagate attribute is true OR roleEnum is Propagate - propagate: #propagate; roleType: #roleType"() {
        given:
        ClientRole role = new ClientRole()
        role.setPropagate(propagate)
        role.setRoleType(roleType)

        expect:
        role.getPropagate()

        where:
        propagate | roleType
        true      | null
        true      | RoleTypeEnum.PROPAGATE
        true      | RoleTypeEnum.STANDARD
        true      | RoleTypeEnum.RCN
        false     | RoleTypeEnum.PROPAGATE
        null      | RoleTypeEnum.PROPAGATE
    }

    def "getPropagate returns false if neither propagate attribute is true nor roleEnum is Propagate - propagate: #propagate; roleType: #roleType"() {
        given:
        ClientRole role = new ClientRole()
        role.setPropagate(propagate)
        role.setRoleType()

        expect:
        !role.getPropagate()

        where:
        propagate | roleType
        false      | RoleTypeEnum.STANDARD
        false      | RoleTypeEnum.RCN
        false      | null
    }

    /**
     * It's impossible to create a _new_ role in this state, but existing roles in the directory may be specified as
     * STANDARD with a propagating attribute set to true. This is because until this story PROPAGATING roles did not exist.
     * @return
     */
    def "Role type reported as PROPAGATE if is propagating"() {
        given:
        ClientRole role = new ClientRole()
        role.setPropagate(propagate)
        role.setRoleType()

        expect:
        !role.getPropagate()

        where:
        propagate | roleType
        false      | RoleTypeEnum.STANDARD
        false      | RoleTypeEnum.RCN
        false      | null
    }

}
