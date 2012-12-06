package com.rackspace.idm.api.converter.cloudv11

import com.rackspacecloud.docs.auth.api.v1.BaseURLRef
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList
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
