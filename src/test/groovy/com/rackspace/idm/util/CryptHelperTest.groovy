package com.rackspace.idm.util

import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification

class CryptHelperTest extends Specification {
    @Shared CryptHelper cryptHelper
    @Shared Configuration config

    def setup() {
        config = Mock()
        cryptHelper = new CryptHelper();
        cryptHelper.config = config
    }

    def "cryptHelper should cache the keyparams"() {
        when:
        def result1 = cryptHelper.getKeyParams()
        def result2 = cryptHelper.getKeyParams()

        then:
        result1.is(result2)
        1 * config.getString("crypto.password") >> "password"
        1 * config.getString("crypto.salt") >> "aa bb"
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

}
