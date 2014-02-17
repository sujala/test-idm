package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.TenantService
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification

class DefaultMultifactorCloud20Test extends Specification {

    @Shared tenantService
    @Shared config
    @Shared service

    def setup() {
        service = new DefaultMultiFactorCloud20Service()
        tenantService = Mock(TenantService)
        service.tenantService = tenantService
        config = Mock(Configuration)
        service.config = config
    }

    def "isMultiFactorEnabled checks multifactor feature flag"() {
        when:
        service.isMultiFactorEnabled()

        then:
        1 * config.getBoolean("multifactor.services.enabled", false)
    }

    def "isMultiFactorEnabledForUser returns false if multifactor services are disabled"() {
        when:
        def response = service.isMultiFactorEnabled()

        then:
        1 * config.getBoolean("multifactor.services.enabled", false) >> false
        response == false
    }

    def "isMultiFactorEnabledForUser returns false if multifactor services are enabled and in beta and user does not have MFA beta role"() {
        given:
        def user = new User()
        def betaRoleName = "mfaBetaRoleName"
        def roles = []
        roles << new TenantRole().with {
            it.name = "notMfaBetaRole"
            it
        }

        when:
        def response = service.isMultiFactorEnabledForUser(user)

        then:
        1 * config.getBoolean("multifactor.services.enabled", false) >> true
        1 * config.getBoolean("multifactor.beta.enabled", false) >> true
        1 * config.getString("cloudAuth.multiFactorBetaRoleName") >> betaRoleName
        1 * tenantService.getGlobalRolesForUser(user) >> roles
        response == false
    }

    def "isMultiFactorEnabledForUser returns true if multifactor services are enabled and in beta and user has MFA beta role"() {
        given:
        def user = new User()
        def betaRoleName = "mfaBetaRoleName"
        def roles = []
        roles << new TenantRole().with {
            it.name = betaRoleName
            it
        }

        when:
        def response = service.isMultiFactorEnabledForUser(user)

        then:
        1 * config.getBoolean("multifactor.services.enabled", false) >> true
        1 * config.getBoolean("multifactor.beta.enabled", false) >> true
        1 * config.getString("cloudAuth.multiFactorBetaRoleName") >> betaRoleName
        1 * tenantService.getGlobalRolesForUser(user) >> roles
        response == true
    }

}
