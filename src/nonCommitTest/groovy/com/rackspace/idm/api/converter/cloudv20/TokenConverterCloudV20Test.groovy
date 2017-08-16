package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.ScopeAccess
import org.openstack.docs.identity.api.v2.Token
import spock.lang.Specification

public class TokenConverterCloudV20Test extends Specification {

    JAXBObjectFactories objectFactories
    TokenConverterCloudV20 converter

    def setup() {
        objectFactories = new JAXBObjectFactories()
        converter = new TokenConverterCloudV20().with {
            it.objFactories = objectFactories
            it
        }
    }

    def "toTokenInternal converts token with null creating date"() {
        given:
        def tenantId = "tenantId"
        def scopeAccess = new ScopeAccess().with {
            it.createTimestamp = null
            it
        }

        when:
        Token token = converter.toTokenInternal(scopeAccess, tenantId)

        then:
        token.issuedAt == null
    }

}