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
        when:
        byte[] encryptedValue = cryptHelper.encrypt("hello", "0")
        String value = cryptHelper.decrypt(encryptedValue, "0")

        then:
        2 * config.getString("crypto.salt") >> "aa bb"
        2 * encryptionPasswordSource.getPassword("0") >> "password"
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
