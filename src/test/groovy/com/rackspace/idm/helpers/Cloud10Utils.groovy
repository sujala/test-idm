package com.rackspace.idm.helpers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import testHelpers.Cloud10Methods

import javax.annotation.PostConstruct

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

    static def removeTenantFromEndpoint(String endpoint){
        return endpoint.substring(0,endpoint.lastIndexOf("/"));
    }
}
