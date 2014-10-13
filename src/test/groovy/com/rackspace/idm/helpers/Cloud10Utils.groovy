package com.rackspace.idm.helpers

import com.rackspace.idm.api.resource.cloud.v10.Cloud10VersionResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import testHelpers.Cloud10Methods

import javax.annotation.PostConstruct

import static com.rackspace.idm.Constants.*

import static org.apache.http.HttpStatus.SC_NO_CONTENT

@Component
class Cloud10Utils {

    @Autowired
    Cloud10Methods methods

    def authenticate(String username, String key) {
        def response = methods.authenticate(username, key)
        assert (response.status == SC_NO_CONTENT)
        response.headers
    }

    def getToken(def username, def key = DEFAULT_API_KEY) {
        def headers = authenticate(username, key)
        return headers[Cloud10VersionResource.HEADER_AUTH_TOKEN][0]
    }

    static def removeTenantFromEndpoint(String endpoint){
        return endpoint.substring(0,endpoint.lastIndexOf("/"));
    }
}
