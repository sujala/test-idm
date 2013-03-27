package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.AuthenticatedBy
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import spock.lang.Shared
import testHelpers.RootServiceTest
import org.openstack.docs.identity.api.v2.Token

import javax.xml.bind.JAXBElement

class TokenConverterCloudV20Test extends RootServiceTest {

    @Shared TokenConverterCloudV20 converter

    def setupSpec() {
        converter = new TokenConverterCloudV20()
        converter.objFactories = new JAXBObjectFactories()
    }

    def "toToken adds authenticatedBy to the other attributes"() {
        when:
        def scopeAccess = createScopeAccess().with {
            it.authenticatedBy = authenticatedBy
            it
        }
        Token result = converter.toToken(scopeAccess)

        then:
        JAXBElement<AuthenticatedBy> authenticatedByEntity = result.any.get(0)
        expected as Set == authenticatedByEntity.getValue().credential as Set

        where:
        authenticatedBy     | expected
        ["RSA"]             | ["RSA"]
        ["Password"]        | ["Password"]
        ["RSA", "Password"] | ["RSA", "Password"]
    }
}
