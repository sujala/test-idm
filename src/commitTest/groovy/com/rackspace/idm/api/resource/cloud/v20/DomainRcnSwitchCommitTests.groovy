package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.api.security.RequestContext
import com.rackspace.idm.api.security.RequestContextHolder
import com.rackspace.idm.api.security.SecurityContext
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.DomainService
import com.rackspace.idm.domain.service.TenantService
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ExceptionHandler
import com.rackspace.idm.validation.Validator20
import org.apache.commons.lang3.RandomStringUtils
import org.opensaml.core.config.InitializationService
import spock.lang.Shared
import spock.lang.Specification

class DomainRcnSwitchCommitTests extends Specification {

    @Shared Cloud20Service cloud20Service
    @Shared def authorizationService
    @Shared def requestContextHolder
    @Shared def requestContext
    @Shared def securityContext
    @Shared def exceptionHandler
    @Shared def validator20
    @Shared def domainService
    @Shared def tenantService
    @Shared def applicationService
    @Shared def atomHopperClient

    def setupSpec() {
        InitializationService.initialize()
    }

    def setup() {
        cloud20Service = new DefaultCloud20Service()

        authorizationService = Mock(AuthorizationService)
        cloud20Service.authorizationService = authorizationService

        requestContextHolder = Mock(RequestContextHolder)
        requestContext = Mock(RequestContext)
        securityContext = Mock(SecurityContext)
        requestContextHolder.getRequestContext() >> requestContext
        requestContext.getSecurityContext() >> securityContext
        cloud20Service.requestContextHolder = requestContextHolder

        exceptionHandler = Mock(ExceptionHandler)
        cloud20Service.exceptionHandler = exceptionHandler

        validator20 = Mock(Validator20)
        cloud20Service.validator20 = validator20

        domainService = Mock(DomainService)
        cloud20Service.domainService = domainService

        tenantService = Mock(TenantService)
        cloud20Service.tenantService = tenantService

        applicationService = Mock(ApplicationService)
        cloud20Service.applicationService = applicationService

        atomHopperClient = Mock(AtomHopperClient)
        cloud20Service.atomHopperClient = atomHopperClient
    }

    def "test switch RCN on a domain"() {
        given:
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def token = RandomStringUtils.randomAlphanumeric(8)
        def destinationRcn = RandomStringUtils.randomAlphanumeric(8)
        def domain = new Domain().with {
            it.domainId = domainId
            it
        }
        def user1 = new User().with {
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it
        }
        def user2 = new User().with {
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it
        }
        def rcnClientRole = new ClientRole().with {
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it
        }
        def rcnTenantRole = new TenantRole().with {
            it
        }

        when:
        cloud20Service.switchDomainRcn(token, domainId, destinationRcn)

        then: "verified that a valid token for a user with the rcn switch role is provided"
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token)
        1 * authorizationService.verifyEffectiveCallerHasRoleByName(IdentityRole.DOMAIN_RCN_SWITCH.getRoleName())

        and: "the RCN is validated"
        1 * validator20.validateDomainRcn(destinationRcn)

        and: "the domain is loaded using the 'get and check' method"
        1 * domainService.checkAndGetDomain(domainId) >> domain

        and: "the users within the provided domain are loaded"
        1 * domainService.getUsersByDomainId(domainId) >> [user1, user2]

        and: "the RCN roles are loaded"
        1 * applicationService.getClientRolesByRoleType(RoleTypeEnum.RCN) >> [rcnClientRole]

        and: "for each user we load the RCN roles assigned to that user"
        1 * tenantService.getTenantRolesForUserWithId(user1, _) >> [rcnTenantRole]
        1 * tenantService.getTenantRolesForUserWithId(user2, _) >> []

        and: "delete the role from that user if they have any RCN roles"
        1 * tenantService.deleteTenantRole(rcnTenantRole)
        1 * atomHopperClient.asyncPost(user1, AtomHopperConstants.ROLE)
        0 * atomHopperClient.asyncPost(user2, AtomHopperConstants.ROLE)

        and: "update the RCN on the domain to the new RCN"
        1 * domainService.updateDomain(*_) >> { arguments ->
            assert arguments[0].rackspaceCustomerNumber == destinationRcn
        }
    }

    def "bad request exception is thrown when an invalid RCN is provided"() {
        given:
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def token = RandomStringUtils.randomAlphanumeric(8)

        when:
        def destinationRcn = ""
        cloud20Service.switchDomainRcn(token, domainId, destinationRcn)

        then:
        exceptionHandler.exceptionResponse(*_) >> { arguments ->
            assert arguments[0] instanceof BadRequestException
            assert arguments[0].message == DefaultCloud20Service.ERROR_SWITCH_RCN_ON_DOMAIN_MISSING_RCN
        }

        when:
        destinationRcn = "       "
        cloud20Service.switchDomainRcn(token, domainId, destinationRcn)

        then:
        exceptionHandler.exceptionResponse(*_) >> { arguments ->
            assert arguments[0] instanceof BadRequestException
            assert arguments[0].message == DefaultCloud20Service.ERROR_SWITCH_RCN_ON_DOMAIN_MISSING_RCN
        }

        when:
        destinationRcn = null
        cloud20Service.switchDomainRcn(token, domainId, destinationRcn)

        then:
        exceptionHandler.exceptionResponse(*_) >> { arguments ->
            assert arguments[0] instanceof BadRequestException
            assert arguments[0].message == DefaultCloud20Service.ERROR_SWITCH_RCN_ON_DOMAIN_MISSING_RCN
        }
    }

    def "when domain contains an RCN tenant an exception is thrown"() {
        given:
        def domainId = RandomStringUtils.randomAlphanumeric(8)
        def token = RandomStringUtils.randomAlphanumeric(8)
        def destinationRcn = RandomStringUtils.randomAlphanumeric(8)
        domainService.checkAndGetDomain(domainId) >> new Domain().with {
            it.rackspaceCustomerNumber = RandomStringUtils.randomAlphanumeric(8)
            it
        }

        when:
        tenantService.countTenantsWithTypeInDomain(GlobalConstants.TENANT_TYPE_RCN, domainId) >> 1
        cloud20Service.switchDomainRcn(token, domainId, destinationRcn)

        then:
        1 * exceptionHandler.exceptionResponse(*_) >> { arguments ->
            assert arguments[0] instanceof BadRequestException
            assert arguments[0].message == DefaultCloud20Service.ERROR_SWITCH_RCN_ON_DOMAIN_CONTAINING_RCN_TENANT
        }
    }

}
