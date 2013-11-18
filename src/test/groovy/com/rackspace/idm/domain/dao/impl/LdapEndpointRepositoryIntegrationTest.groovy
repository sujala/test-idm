package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.EndpointDao
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.helpers.Cloud20Utils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapEndpointRepositoryIntegrationTest extends Specification {

    @Autowired
    private EndpointDao endpointDao

    @Autowired
    private Cloud20Utils utils

    def "endpoint crud"() {
        given:
        def baseUrlId = utils.getRandomUUID("baseUrlId")
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

    def "calling get global endpoints for us and uk returns correct endpoints"() {
        given:
        def baseUrl1 = getCreateBaseUrl(utils.getRandomUUID("baseUrlId")).with {
            it.region = "ORD"
            it.baseUrlType = "MOSSO"
            it
        }
        def baseUrl2 = getCreateBaseUrl(utils.getRandomUUID("baseUrlId")).with {
            it.region = "ORD"
            it.baseUrlType = "NAST"
            it
        }
        def baseUrl3 = getCreateBaseUrl(utils.getRandomUUID("baseUrlId")).with {
            it.region = "LON"
            it.baseUrlType = "MOSSO"
            it
        }
        def baseUrl4 = getCreateBaseUrl(utils.getRandomUUID("baseUrlId")).with {
            it.region = "LON"
            it.baseUrlType = "NAST"
            it
        }
        endpointDao.addBaseUrl(baseUrl1)
        endpointDao.addBaseUrl(baseUrl2)
        endpointDao.addBaseUrl(baseUrl3)
        endpointDao.addBaseUrl(baseUrl4)

        when:
        List<CloudBaseUrl> usGlobalMossoBaseUrls = endpointDao.getGlobalUSBaseUrlsByBaseUrlType("MOSSO").collect()
        List<CloudBaseUrl> ukGlobalMossoBaseUrls = endpointDao.getGlobalUKBaseUrlsByBaseUrlType("MOSSO").collect()

        then:
        for (CloudBaseUrl baseUrl : usGlobalMossoBaseUrls) {
            assert (baseUrl.baseUrlType == "MOSSO")
            assert (baseUrl.region == "ORD")
        }
        for (CloudBaseUrl baseUrl : ukGlobalMossoBaseUrls) {
            assert (baseUrl.baseUrlType == "MOSSO")
            assert (baseUrl.region == "LON")
        }

        cleanup:
        endpointDao.deleteBaseUrl(baseUrl1.baseUrlId)
        endpointDao.deleteBaseUrl(baseUrl2.baseUrlId)
        endpointDao.deleteBaseUrl(baseUrl3.baseUrlId)
        endpointDao.deleteBaseUrl(baseUrl4.baseUrlId)
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
