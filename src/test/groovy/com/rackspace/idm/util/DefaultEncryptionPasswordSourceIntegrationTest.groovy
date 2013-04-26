package com.rackspace.idm.util

import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 4/19/13
 * Time: 10:39 AM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultEncryptionPasswordSourceIntegrationTest extends Specification{
    @Autowired
    DefaultEncryptionPasswordSource encryptionPasswordSource

    @Autowired
    Configuration config

    @Shared String password

    def setupSpec(){
        password = "this is a super secret key!"
    }

    def setup(){
    }

    def cleanup(){
    }

    def "getPassword with version returns password" (){
        given:
        encryptionPasswordSource.init()

        when:
        String password = encryptionPasswordSource.getPassword("0")

        then:
        password == this.password
    }

    def "getPassword without version returns password" (){
        given:
        encryptionPasswordSource.init()

        when:
        String password = encryptionPasswordSource.getPassword()

        then:
        password == encryptionPasswordSource.getPassword(encryptionPasswordSource.currentVersion)
    }

    def "Password version does not exist - returns IllegalStateException" () {
        given:
        encryptionPasswordSource.init()

        when:
        encryptionPasswordSource.getPassword("-1")

        then:
        thrown(IllegalStateException.class)
    }

    def "Read passwords retrieves all passwords" () {
        given:
        Integer size = config.getStringArray("crypto.password").size()


        when:
        encryptionPasswordSource.readPasswords()

        then:
        encryptionPasswordSource.map.size() == size
        encryptionPasswordSource.map.size() > 0
    }
}
