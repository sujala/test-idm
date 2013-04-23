package com.rackspace.idm.util

import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification

class CryptHelperTest extends Specification {
    @Shared CryptHelper cryptHelper
    @Shared Configuration config
    @Shared EncryptionPasswordSource encryptionPasswordSource


    def setup() {
       setupMock()
    }

    def "crypHelper performance test" (){
        when:
        def startTime = System.nanoTime()
        for(int i = 0; i < 1000; i++){
            cryptHelper.getKeyParams("password","aa bb")
        }
        def endTime = System.nanoTime()

        then:
        endTime - startTime > 1
    }

    def "Encypt value with password version uses the version" () {
        given:
        def version = "0"
        def salt = "aa bb"

        when:
        byte[] encryptedValue = cryptHelper.encrypt("hello", version, salt)
        String value = cryptHelper.decrypt(encryptedValue, version, salt)

        then:
        0 * config.getString("crypto.salt") >> salt
        2 * encryptionPasswordSource.getPassword(version) >> "password"
        value == "hello"
    }

    def setupMock(){
        cryptHelper = new CryptHelper()

        config = Mock()
        cryptHelper.config = config

        encryptionPasswordSource = Mock()
        cryptHelper.encryptionPasswordSource = encryptionPasswordSource
    }
}
