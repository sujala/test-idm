package com.rackspace.idm.api.converter.cloudv11

import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList
import com.rackspacecloud.docs.auth.api.v1.UserType
import spock.lang.Specification
import com.rackspace.idm.domain.entity.OpenstackEndpoint

class EndpointConverterCloudV11Test extends Specification {

    def EndpointConverterCloudV11 endpointConverterCloudV11

    def setup() {
        endpointConverterCloudV11 = new EndpointConverterCloudV11();
    }

    def "remove duplicates removes duplicates"() {
        given:
        BaseURLRefList baseURLRefList = new BaseURLRefList()
        BaseURLRef baseURLRef1 = getBaseUrlRef(7)
        BaseURLRef baseURLRef2 = getBaseUrlRef(7)

        baseURLRefList.baseURLRef.add(baseURLRef1)
        baseURLRefList.baseURLRef.add(baseURLRef2)

        when:
        def result = endpointConverterCloudV11.removeDuplicates(baseURLRefList)

        then:
        result.baseURLRef.size() == 1
    }

    def "getting openstack baseurls calls removeduplicates"() {
        given:
        def removeDuplicates_called = false
        def endpointConverterCloudV11 = [ removeDuplicates: { removeDuplicates_called = true; null } ] as EndpointConverterCloudV11

        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>()
        endpoints.add(getOpenstackEndpoint())

        when:
        endpointConverterCloudV11.openstackToBaseUrlRefs(endpoints)

        then:
        removeDuplicates_called == true
    }

    def "converter trims the urls and baseUrlType"() {
        given:
        def adminUrl = "admin"
        def internalUrl = "internal"
        def publicUrl = "public"
        def baseUrlType = "MOSSO"

        def cloudBaseUrl = new CloudBaseUrl().with {
            it.baseUrlId = 1
            it.adminUrl = "$adminUrl "
            it.internalUrl = "$internalUrl "
            it.publicUrl = "$publicUrl "
            it.baseUrlType = "$baseUrlType "
            return it
        }

        when:
        def result = endpointConverterCloudV11.toBaseUrl(cloudBaseUrl)

        then:
        result.adminURL == adminUrl
        result.publicURL == publicUrl
        result.internalURL == internalUrl
        result.userType == UserType.MOSSO
    }

    def getBaseUrlRef(id) {
        new BaseURLRef().with {
            it.id = id
            return it
        }
    }

    def getOpenstackEndpoint() {
        new OpenstackEndpoint().with {
            it.baseUrls = new ArrayList<BaseURLRef>()
            return it
        }
    }
}
