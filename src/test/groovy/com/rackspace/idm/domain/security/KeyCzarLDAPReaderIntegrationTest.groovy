package com.rackspace.idm.domain.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.rackspace.idm.domain.dao.impl.LdapGenericRepository
import com.rackspace.idm.domain.security.encrypters.CacheableKeyCzarCrypterLocator
import com.rackspace.idm.domain.security.encrypters.keyczar.KeyCzarKeyMetadataDao
import com.rackspace.idm.domain.security.encrypters.keyczar.KeyCzarKeyVersionDao
import com.rackspace.idm.domain.security.encrypters.keyczar.LdapKeyMetadata
import org.keyczar.Crypter
import org.keyczar.exceptions.KeyczarException
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.RootIntegrationTest

class KeyCzarLDAPReaderIntegrationTest extends RootIntegrationTest {

    @Autowired
    CacheableKeyCzarCrypterLocator cacheableKeyCzarCrypterLocator

    @Autowired
    KeyCzarKeyVersionDao keyCzarKeyVersionDao

    @Autowired
    KeyCzarKeyMetadataDao keyCzarKeyMetadataDao

    @Autowired
    LdapGenericRepository<LdapKeyMetadata> keyMetadataLdapGenericRepository

    def "test get metadata cache info"() {
        given:
        def response = devops.getInfo(utils.getServiceAdminToken())
        Thread.sleep(1000)
        def info = cacheableKeyCzarCrypterLocator.getCacheInfo()

        when:
        def data = new ObjectMapper().readValue(response.getEntity(String), Map).get('metadata')

        then:
        response.status == 200
        info.getSize() == data.get('size')
    }

    def "test update metadata cache info"() {
        given:
        def response1 = devops.getInfo(utils.getServiceAdminToken())
        Thread.sleep(1000)
        def response2 = devops.forceUpdateInfo(utils.getServiceAdminToken())

        when:
        def data1 = new ObjectMapper().readValue(response1.getEntity(String), Map).get('metadata')
        def data2 = new ObjectMapper().readValue(response2.getEntity(String), Map).get('metadata')

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

    def "test get metadata from the directory"() {
        when:
        def metadata = keyCzarKeyMetadataDao.getKeyMetadataByName('meta')

        then:
        metadata != null
        metadata.name == 'meta'
    }

    def "test get keys from the directory"() {
        when:
        def versions = keyCzarKeyVersionDao.getKeyVersionsForMetadata('meta')

        then:
        versions != null
        versions.size() == 2
    }

    def "test rotation of keys"() {
        given:
        def first_primary= '{"name":"sessionId","purpose":"DECRYPT_AND_ENCRYPT","type":"AES","versions":[{"exportable":false,"status":"PRIMARY","versionNumber":1}],"encrypted":false}'
        def second_primary = '{"name":"sessionId","purpose":"DECRYPT_AND_ENCRYPT","type":"AES","versions":[{"exportable":false,"status":"PRIMARY","versionNumber":2}],"encrypted":false}'
        def two_keys_first_primary = '{"name":"sessionId","purpose":"DECRYPT_AND_ENCRYPT","type":"AES","versions":[{"exportable":false,"status":"PRIMARY","versionNumber":1},{"exportable":false,"status":"ACTIVE","versionNumber":2}],"encrypted":false}'
        def two_keys_second_primary = '{"name":"sessionId","purpose":"DECRYPT_AND_ENCRYPT","type":"AES","versions":[{"exportable":false,"status":"ACTIVE","versionNumber":1},{"exportable":false,"status":"PRIMARY","versionNumber":2}],"encrypted":false}'

        LdapKeyMetadata metadata = keyCzarKeyMetadataDao.getKeyMetadataByName('meta')
        def original = metadata.data

        def info, encrypt1, decrypt1, encrypt2, decrypt2
        Crypter crypter

        when: "test encryption with v1 key"
        metadata.data = first_primary
        keyMetadataLdapGenericRepository.updateObject(metadata)
        cacheableKeyCzarCrypterLocator.resetCache()
        info = cacheableKeyCzarCrypterLocator.getCacheInfo()
        crypter = cacheableKeyCzarCrypterLocator.getCrypter()

        encrypt1 = crypter.encrypt('foobar')
        decrypt1 = crypter.decrypt(encrypt1)

        then: "test the reload of keys and encryption for v1 key"
        info.size == 1
        encrypt1 != null
        decrypt1 == 'foobar'

        when: "test encryption with v2 key"
        metadata.data = second_primary
        keyMetadataLdapGenericRepository.updateObject(metadata)
        cacheableKeyCzarCrypterLocator.resetCache()
        info = cacheableKeyCzarCrypterLocator.getCacheInfo()
        crypter = cacheableKeyCzarCrypterLocator.getCrypter()

        encrypt2 = crypter.encrypt('foobar')
        decrypt2 = crypter.decrypt(encrypt2)
        try {
            decrypt1 = crypter.decrypt(encrypt1)
        } catch (KeyczarException e) {
            decrypt1 = null
        }

        then: "test the reload of keys and encryption for v2 key"
        info.size == 1
        encrypt2 != null
        decrypt2 == 'foobar'
        decrypt1 == null

        when: "test decryption with v1 key (with v2)"
        metadata.data = two_keys_first_primary
        keyMetadataLdapGenericRepository.updateObject(metadata)
        cacheableKeyCzarCrypterLocator.resetCache()
        info = cacheableKeyCzarCrypterLocator.getCacheInfo()
        crypter = cacheableKeyCzarCrypterLocator.getCrypter()

        decrypt2 = crypter.decrypt(encrypt2)
        decrypt1 = crypter.decrypt(encrypt1)

        then: "test the reload of keys and encryption for v1 key (with v2)"
        info.size == 2
        decrypt2 == 'foobar'
        decrypt1 == 'foobar'

        when: "test decryption with v2 key (with v1)"
        metadata.data = two_keys_second_primary
        keyMetadataLdapGenericRepository.updateObject(metadata)
        cacheableKeyCzarCrypterLocator.resetCache()
        info = cacheableKeyCzarCrypterLocator.getCacheInfo()
        crypter = cacheableKeyCzarCrypterLocator.getCrypter()

        decrypt2 = crypter.decrypt(encrypt2)
        decrypt1 = crypter.decrypt(encrypt1)

        then: "test the reload of keys and encryption for v2 key (with v1)"
        info.size == 2
        decrypt2 == 'foobar'
        decrypt1 == 'foobar'

        cleanup:
        metadata.data = original
        keyMetadataLdapGenericRepository.updateObject(metadata)
    }

}
