package com.rackspace.idm.domain.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.impl.LdapGenericRepository
import com.rackspace.idm.domain.security.encrypters.CacheableKeyCzarCrypterLocator
import com.rackspace.idm.domain.security.encrypters.LDAPKeyCzarCrypterLocator
import com.rackspace.idm.domain.dao.KeyCzarKeyMetadataDao
import com.rackspace.idm.domain.dao.KeyCzarKeyVersionDao
import com.rackspace.idm.domain.entity.LdapKeyMetadata
import com.rackspace.idm.domain.security.signoff.KeyCzarAPINodeSignoffDao
import org.joda.time.DateTime
import org.keyczar.Crypter
import org.keyczar.exceptions.KeyczarException
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.RootIntegrationTest

class KeyCzarLDAPReaderIntegrationTest extends RootIntegrationTest {
    def first_primary= '{"name":"sessionId","purpose":"DECRYPT_AND_ENCRYPT","type":"AES","versions":[{"exportable":false,"status":"PRIMARY","versionNumber":1}],"encrypted":false}'
    def second_primary = '{"name":"sessionId","purpose":"DECRYPT_AND_ENCRYPT","type":"AES","versions":[{"exportable":false,"status":"PRIMARY","versionNumber":2}],"encrypted":false}'
    def two_keys_first_primary = '{"name":"sessionId","purpose":"DECRYPT_AND_ENCRYPT","type":"AES","versions":[{"exportable":false,"status":"PRIMARY","versionNumber":1},{"exportable":false,"status":"ACTIVE","versionNumber":2}],"encrypted":false}'
    def two_keys_second_primary = '{"name":"sessionId","purpose":"DECRYPT_AND_ENCRYPT","type":"AES","versions":[{"exportable":false,"status":"ACTIVE","versionNumber":1},{"exportable":false,"status":"PRIMARY","versionNumber":2}],"encrypted":false}'

    def bad_meta_purpose= '{"name":"sessionId","purpose":"DECRYPT_AND_ENCRYPT_BLAH","type":"AES","versions":[{"exportable":false,"status":"PRIMARY","versionNumber":1}],"encrypted":false}'
    def bad_meta_version= '{"name":"sessionId","purpose":"DECRYPT_AND_ENCRYPT","type":"AES","versions":[{"exportable":false,"status":"PRIMARY","versionNumber":12023}],"encrypted":false}'

    @Autowired
    CacheableKeyCzarCrypterLocator cacheableKeyCzarCrypterLocator

    @Autowired
    KeyCzarKeyVersionDao keyCzarKeyVersionDao

    @Autowired
    KeyCzarKeyMetadataDao keyCzarKeyMetadataDao

    @Autowired
    KeyCzarAPINodeSignoffDao apiNodeSignoffDao

    @Autowired
    IdentityConfig identityConfig;


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

    def "update metadata cache info - no-op when keys don't change"() {
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
        data1.get('retrieved') == data2.get('retrieved')
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
        LdapKeyMetadata metadata = keyCzarKeyMetadataDao.getKeyMetadataByName('meta')
        def original = metadata.data

        def info, encrypt1, decrypt1, encrypt2, decrypt2
        Crypter crypter

        when: "test encryption with v1 key"
        metadata.data = first_primary
        metadata.created = new DateTime(metadata.created).plusMillis(1).toDate()
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

        and: "signoff exists for this version"
        verifyKeySignoff(metadata)

        when: "test encryption with v2 key"
        metadata.data = second_primary
        metadata.created = new DateTime(metadata.created).plusMillis(1).toDate()
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

        and: "signoff exists for this version"
        verifyKeySignoff(metadata)

        when: "test decryption with v1 key (with v2)"
        metadata.data = two_keys_first_primary
        metadata.created = new DateTime(metadata.created).plusMillis(1).toDate()
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

        and: "signoff exists for this version"
        verifyKeySignoff(metadata)

        when: "test decryption with v2 key (with v1)"
        metadata.data = two_keys_second_primary
        metadata.created = new DateTime(metadata.created).plusMillis(1).toDate()
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

        and: "signoff exists for this version"
        verifyKeySignoff(metadata)

        cleanup:
        metadata.data = original
        metadata.created = new DateTime(metadata.created).plusMillis(1).toDate()
        keyMetadataLdapGenericRepository.updateObject(metadata)
    }

    def "failure to load new keys to cache leaves old keys in place"() {
        LdapKeyMetadata metadata = keyCzarKeyMetadataDao.getKeyMetadataByName('meta')
        def original = metadata.data

        def originalCache, afterResetCache, encrypt1, decrypt1, encrypt2, decrypt2
        Crypter crypter

        when: "test encryption with v1 key"
        metadata.data = first_primary
        metadata.created = new DateTime(metadata.created).plusMillis(1).toDate()
        keyMetadataLdapGenericRepository.updateObject(metadata)
        cacheableKeyCzarCrypterLocator.resetCache()
        originalCache = cacheableKeyCzarCrypterLocator.getCacheInfo()

        //test encrypting/decrypting
        crypter = cacheableKeyCzarCrypterLocator.getCrypter()
        encrypt1 = crypter.encrypt('foobar')
        decrypt1 = crypter.decrypt(encrypt1)

        then: "can encrypt and decrypt with v1 key"
        originalCache.size == 1
        encrypt1 != null
        decrypt1 == 'foobar'

        and: "signoff exists for this version"
        verifyKeySignoff(metadata)

        when: "try to reset with bad data"
        LdapKeyMetadata currentMetaData = keyCzarKeyMetadataDao.getKeyMetadataByName('meta')

        metadata.data = bad_meta_purpose
        metadata.created = new DateTime(metadata.created).plusMillis(1).toDate()
        keyMetadataLdapGenericRepository.updateObject(metadata)

        //try to reload
        cacheableKeyCzarCrypterLocator.resetCache()
        afterResetCache = cacheableKeyCzarCrypterLocator.getCacheInfo()

        then: "cache wasn't updated"
        originalCache.created == afterResetCache.created
        originalCache.retrieved == afterResetCache.retrieved
        originalCache.size == afterResetCache.size
        originalCache.getKey().get(0).version == afterResetCache.getKey().get(0).version

        and: "and can still decrypt previously generated keys"
        cacheableKeyCzarCrypterLocator.getCrypter().decrypt(encrypt1) == 'foobar'

        and: "signoff exists for the old version of key"
        verifyKeySignoff(currentMetaData)

        cleanup:
        metadata.data = original
        metadata.created = new DateTime(metadata.created).plusMillis(1).toDate()
        keyMetadataLdapGenericRepository.updateObject(metadata)
        cacheableKeyCzarCrypterLocator.resetCache()
    }

    def "disable of signoff causes updates not to happen"() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_AE_SYNC_SIGNOFF_ENABLED_PROP, true)
        cacheableKeyCzarCrypterLocator.resetCache()

        LdapKeyMetadata metadata = keyCzarKeyMetadataDao.getKeyMetadataByName('meta')
        assert metadata != null
        verifyKeySignoff(metadata)

        LdapKeyMetadata metaForUpdate = new LdapKeyMetadata().with {
            it.name = metadata.name
            it.created = metadata.created
            it.data = metadata.data
            it.uniqueId = metadata.uniqueId
            it
        }

        def signoffObj = apiNodeSignoffDao.getByNodeAndMetaName(LDAPKeyCzarCrypterLocator.DN_META, identityConfig.getReloadableConfig().getAENodeNameForSignoff())
        assert signoffObj != null
        metaForUpdate.data = two_keys_first_primary
        metaForUpdate.created = new DateTime(metadata.created).plusMillis(1).toDate()
        keyMetadataLdapGenericRepository.updateObject(metaForUpdate)

        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_AE_SYNC_SIGNOFF_ENABLED_PROP, false)
        cacheableKeyCzarCrypterLocator.resetCache()

        then: "signoff is not updated"
        verifyKeySignoff(metadata) //compare to original

        cleanup:
        reloadableConfiguration.reset()
        keyMetadataLdapGenericRepository.updateObject(metadata)
    }

    def void verifyKeySignoff(LdapKeyMetadata metadata) {
        def signoffObj = apiNodeSignoffDao.getByNodeAndMetaName(LDAPKeyCzarCrypterLocator.DN_META, identityConfig.getReloadableConfig().getAENodeNameForSignoff())
        assert signoffObj != null
        assert signoffObj.cachedMetaCreatedDate != null
        assert new DateTime(signoffObj.cachedMetaCreatedDate).equals(new DateTime(metadata.created))
    }
}
