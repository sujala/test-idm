package com.rackspace.idm.domain.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.rackspace.idm.domain.security.encrypters.CacheableKeyCzarCrypterLocator
import org.keyczar.Crypter
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.RootIntegrationTest

class KeyCzarLDAPReaderIntegrationTest extends RootIntegrationTest {

    @Autowired
    CacheableKeyCzarCrypterLocator cacheableKeyCzarCrypterLocator

    def "test get metadata cache info"() {
        given:
        def response = devops.getInfo(utils.getServiceAdminToken())
        Thread.sleep(1000)
        def info = cacheableKeyCzarCrypterLocator.getCacheInfo()

        when:
        def data = new ObjectMapper().readValue(response.getEntity(String), Map)

        then:
        response.status == 200
        info.get('size') == data.get('size')
        info.get('updated') == data.get('updated')
        info.get('retrieved') != data.get('retrieved')
    }

    def "test update metadata cache info"() {
        given:
        def response1 = devops.getInfo(utils.getServiceAdminToken())
        Thread.sleep(1000)
        def response2 = devops.forceUpdateInfo(utils.getServiceAdminToken())

        when:
        def data1 = new ObjectMapper().readValue(response1.getEntity(String), Map)
        def data2 = new ObjectMapper().readValue(response2.getEntity(String), Map)

        then:
        response1.status == 200
        response2.status == 200

        data1.get('size') == data2.get('size')
        data1.get('updated') == data2.get('updated')
        data1.get('retrieved') != data2.get('retrieved')
    }

    def "test encryption and decryption"() {
        given:
        Crypter crypter = cacheableKeyCzarCrypterLocator.getCrypter()

        when:
        def encrypt = crypter.encrypt('foobar')

        then:
        encrypt != null

        when:
        def decrypt = crypter.decrypt(encrypt)

        then:
        decrypt == 'foobar'
    }

}
