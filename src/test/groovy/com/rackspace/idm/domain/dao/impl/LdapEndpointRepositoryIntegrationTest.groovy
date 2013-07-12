package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.EndpointDao
import com.rackspace.idm.domain.entity.CloudBaseUrl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapEndpointRepositoryIntegrationTest extends Specification {

    @Shared def randomness = UUID.randomUUID()
    @Shared def random
    @Shared def baseUrlId

    @Autowired
    private EndpointDao endpointDao

    def setupSpec() {
        random = ("$randomness").replace('-',"")
        baseUrlId = "baseUrlId${random}"
    }

    def cleanupSpec() {
    }

    def "endpoint crud"() {
        given:
        def baseUrlToCreate = getCreateBaseUrl(baseUrlId)
        def baseUrlToUpdate = getUpdateBaseUrl(baseUrlId)

        when:
        endpointDao.addBaseUrl(baseUrlToCreate)
        def createdBaseUrl = endpointDao.getBaseUrlById(baseUrlId)

        baseUrlToUpdate.ldapEntry = createdBaseUrl.ldapEntry
        endpointDao.updateCloudBaseUrl(baseUrlToUpdate)
        def updatedBaseUrl = endpointDao.getBaseUrlById(baseUrlId)

        endpointDao.deleteBaseUrl(baseUrlId)
        def deletedBaseUrl = endpointDao.getBaseUrlById(baseUrlId)

        then:
        baseUrlToCreate == createdBaseUrl
        baseUrlToUpdate == updatedBaseUrl
        deletedBaseUrl == null
    }

    def getCreateBaseUrl(baseUrlId) {
        return new CloudBaseUrl().with {
            it.baseUrlId = baseUrlId
            it.publicUrl = "publicUrl"
            it.baseUrlType = "baseUrlType"
            it.cn = "name"
            it.def = true
            it.enabled = true
            it.internalUrl = "internalUrl"
            it.openstackType = "openstackType"
            it.policyList = ["1"].asList()
            it.adminUrl = "adminUrl"
            it.global = true
            it.region = "region"
            it.serviceName = "serviceName"
            it.versionId = "versionId"
            it.versionInfo = "versionInfo"
            it.versionList = "versionList"
            return it
        }
    }

    def getUpdateBaseUrl(baseUrlId) {
        return new CloudBaseUrl().with {
            it.baseUrlId = baseUrlId
            it.publicUrl = "publicUrl2"
            it.baseUrlType = "baseUrlType2"
            it.cn = "name2"
            it.def = false
            it.enabled = false
            it.internalUrl = "internalUrl2"
            it.openstackType = "openstackType2"
            it.policyList = ["1", "2"].asList()
            it.adminUrl = "adminUrl2"
            it.global = false
            it.region = "region2"
            it.serviceName = "serviceName2"
            it.versionId = "versionId2"
            it.versionInfo = "versionInfo2"
            it.versionList = "versionList2"
            return it
        }
    }

}
