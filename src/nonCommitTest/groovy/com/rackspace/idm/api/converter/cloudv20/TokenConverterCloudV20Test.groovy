package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.ScopeAccess
import org.openstack.docs.identity.api.v2.Token
import spock.lang.Specification

public class TokenConverterCloudV20Test extends Specification {

    JAXBObjectFactories objectFactories
    TokenConverterCloudV20 converter
    IdentityConfig identityConfig
    IdentityConfig.ReloadableConfig reloadableConfig

    def setup() {
        objectFactories = new JAXBObjectFactories()
        reloadableConfig = Mock(IdentityConfig.ReloadableConfig)
        identityConfig = Mock(IdentityConfig)
        identityConfig.reloadableConfig >> reloadableConfig
        converter = new TokenConverterCloudV20().with {
            it.objFactories = objectFactories
            it.config = identityConfig
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
        reloadableConfig.getEnableIssuedAtInResponse() >> true

        when:
        Token token = converter.toTokenInternal(scopeAccess, tenantId)

        then:
        token.issuedAt == null
    }

    def "toTokenInternal converts token with created date - feature enabled"() {
        given:
        Date createTimestamp = new Date()
        def tenantId = "tenantId"
        def scopeAccess = new ScopeAccess().with {
            it.createTimestamp = createTimestamp
            it
        }
        reloadableConfig.getEnableIssuedAtInResponse() >> true

        when:
        Token token = converter.toTokenInternal(scopeAccess, tenantId)

        then:
        token.issuedAt.toGregorianCalendar().getTime() == createTimestamp
    }

    def "toTokenInternal converts token with created date - feature disabled"() {
        given:
        Date createTimestamp = new Date()
        def tenantId = "tenantId"
        def scopeAccess = new ScopeAccess().with {
            it.createTimestamp = createTimestamp
            it
        }
        reloadableConfig.getEnableIssuedAtInResponse() >> false

        when:
        Token token = converter.toTokenInternal(scopeAccess, tenantId)

        then:
        token.issuedAt == null
    }

}