package com.rackspace.idm.domain.entity

import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 4/22/13
 * Time: 10:58 AM
 * To change this template use File | Settings | File Templates.
 */
class UserGroovyTest extends RootServiceTest{

    def "Get user password version" () {
        given:
        User user = entityFactory.createUser()
        user.setPasswordVersion(1)

        when:
        Integer version = user.getPasswordVersion()

        then:
        version == 1
    }

}
