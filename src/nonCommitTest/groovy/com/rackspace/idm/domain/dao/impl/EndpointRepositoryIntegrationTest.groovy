package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.EndpointDao
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.helpers.CloudTestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import spock.lang.Specification

import static com.rackspace.idm.GlobalConstants.*

@WebAppConfiguration
@ContextConfiguration(locations = "classpath:app-config.xml")
class EndpointRepositoryIntegrationTest extends Specification {

    @Autowired
    private EndpointDao endpointDao

    @Autowired
    private CloudTestUtils testUtils


    def "endpoint crud"() {
        given:
        def baseUrlId = testUtils.getRandomUUID("baseUrlId")
        def adminUrlId = testUtils.getRandomUUID()
        def internalUrlId = testUtils.getRandomUUID()
        def publicUrlId = testUtils.getRandomUUID()
        def baseUrlToCreate = getCreateBaseUrl(baseUrlId, adminUrlId, internalUrlId, publicUrlId)
        def baseUrlToUpdate = getUpdateBaseUrl(baseUrlId, adminUrlId, internalUrlId, publicUrlId)

        when:
        endpointDao.addBaseUrl(baseUrlToCreate)
        def createdBaseUrl = endpointDao.getBaseUrlById(baseUrlId)

        baseUrlToUpdate.uniqueId = createdBaseUrl.uniqueId
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
        def baseUrl1 = getCreateBaseUrl(testUtils.getRandomUUID("baseUrlId")).with {
            it.region = "ORD"
            it.baseUrlType = "MOSSO"
            it
        }
        def baseUrl2 = getCreateBaseUrl(testUtils.getRandomUUID("baseUrlId")).with {
            it.region = "ORD"
            it.baseUrlType = "NAST"
            it
        }
        def baseUrl3 = getCreateBaseUrl(testUtils.getRandomUUID("baseUrlId")).with {
            it.region = "LON"
            it.baseUrlType = "MOSSO"
            it
        }
        def baseUrl4 = getCreateBaseUrl(testUtils.getRandomUUID("baseUrlId")).with {
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
            assert (baseUrl.region == "ORD" || baseUrl.region == "DFW")
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

    def "tenantAlias defaults to the value of 'TENANT_ALIAS_PATTERN'"() {
        given:
        def baseUrlId = testUtils.getRandomUUID("baseUrlId")
        def baseUrl = getCreateBaseUrl(baseUrlId).with {
            it.tenantAlias = null
            it
        }

        when:
        endpointDao.addBaseUrl(baseUrl)
        def createdBaseUrl = endpointDao.getBaseUrlById(baseUrlId)

        then:
        createdBaseUrl.tenantAlias == TENANT_ALIAS_PATTERN

        cleanup:
        endpointDao.deleteBaseUrl(baseUrlId)
    }

    def getCreateBaseUrl(
            baseUrlId,
            adminUrlId = testUtils.getRandomUUID(),
            internalUrlId = testUtils.getRandomUUID(),
            publicUrlId = testUtils.getRandomUUID()) {
        return new CloudBaseUrl().with {
            it.baseUrlId = baseUrlId
            it.publicUrl = "publicUrl"
            it.baseUrlType = "baseUrlType"
            it.def = true
            it.enabled = true
            it.internalUrl = "internalUrl"
            it.openstackType = "openstackType"
            it.adminUrl = "adminUrl"
            it.global = true
            it.region = "ORD"
            it.serviceName = "serviceName"
            it.versionId = "versionId"
            it.versionInfo = "versionInfo"
            it.versionList = "versionList"
            it.tenantAlias = "prefix${TENANT_ALIAS_PATTERN}"
            it.adminUrlId =  adminUrlId
            it.internalUrlId = internalUrlId
            it.publicUrlId = publicUrlId
            it.clientId = "18e7a7032733486cd32f472d7bd58f709ac0d221"
            return it
        }
    }

    def getUpdateBaseUrl(baseUrlId, adminUrlId, internalUrlId, publicUrlId) {
        return new CloudBaseUrl().with {
            it.baseUrlId = baseUrlId
            it.publicUrl = "publicUrl2"
            it.baseUrlType = "baseUrlType2"
            it.def = false
            it.enabled = false
            it.internalUrl = "internalUrl2"
            it.openstackType = "openstackType2"
            it.adminUrl = "adminUrl2"
            it.global = false
            it.region = "DFW"
            it.serviceName = "serviceName2"
            it.versionId = "versionId2"
            it.versionInfo = "versionInfo2"
            it.versionList = "versionList2"
            it.tenantAlias = TENANT_ALIAS_PATTERN
            it.adminUrlId =  adminUrlId
            it.internalUrlId = internalUrlId
            it.publicUrlId = publicUrlId
            it.clientId = "18e7a7032733486cd32f472d7bd58f709ac0d221"
            return it
        }
    }

}
