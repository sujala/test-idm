package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.api.security.AuthenticationContext
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.idm.domain.entity.ScopeAccess
import org.openstack.docs.identity.api.v2.Token
import spock.lang.Specification
import spock.lang.Unroll

public class TokenConverterCloudV20Test extends Specification {

    JAXBObjectFactories objectFactories
    TokenConverterCloudV20 converter
    IdentityConfig identityConfig
    IdentityConfig.ReloadableConfig reloadableConfig
    AuthenticationContext authContext

    def setup() {
        objectFactories = new JAXBObjectFactories()
        reloadableConfig = Mock(IdentityConfig.ReloadableConfig)
        identityConfig = Mock(IdentityConfig)
        authContext = Mock(AuthenticationContext)
        identityConfig.reloadableConfig >> reloadableConfig
        converter = new TokenConverterCloudV20().with {
            it.objFactories = objectFactories
            it.config = identityConfig
            it.authenticationContext = authContext
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
        reloadableConfig.getEnableIssuedInResponse() >> true

        when:
        Token token = converter.toTokenInternal(scopeAccess, tenantId)

        then:
        token.issued == null
    }

    def "toTokenInternal converts token with created date - feature enabled"() {
        given:
        Date createTimestamp = new Date()
        def tenantId = "tenantId"
        def scopeAccess = new ScopeAccess().with {
            it.createTimestamp = createTimestamp
            it
        }
        reloadableConfig.getEnableIssuedInResponse() >> true

        when:
        Token token = converter.toTokenInternal(scopeAccess, tenantId)

        then:
        token.issued.toGregorianCalendar().getTime() == createTimestamp
    }

    def "toTokenInternal converts token with created date - feature disabled"() {
        given:
        Date createTimestamp = new Date()
        def tenantId = "tenantId"
        def scopeAccess = new ScopeAccess().with {
            it.createTimestamp = createTimestamp
            it
        }
        reloadableConfig.getEnableIssuedInResponse() >> false

        when:
        Token token = converter.toTokenInternal(scopeAccess, tenantId)

        then:
        token.issued== null
    }

    @Unroll
    def "toTokenInternal sets IMPERSONATE in addition to the other auth by values only for impersonation tokens - shouldInclude = #shouldInclude"() {
        given:
        def impToken = new ImpersonatedScopeAccess().with {
            it.authenticatedBy = [AuthenticatedByMethodEnum.PASSWORD.value]
            it
        }
        def token = new ScopeAccess().with {
            it.authenticatedBy = [AuthenticatedByMethodEnum.PASSWORD.value]
            it
        }
        authContext.isIncludeImpersonateInAuthByList() >> shouldInclude

        when: "convert non-imp token"
        def convertedToken = converter.toToken(token, null)

        then:
        convertedToken.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.value)
        !convertedToken.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.IMPERSONATE.value)

        when: "convert imp token"
        convertedToken = converter.toToken(impToken, null)

        then:
        convertedToken.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.PASSWORD.value)
        convertedToken.authenticatedBy.credential.contains(AuthenticatedByMethodEnum.IMPERSONATE.value) == shouldInclude

        where:
        shouldInclude << [true, false]
    }

}
