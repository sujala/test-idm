package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.v20.DelegateReference
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotFoundException
import com.unboundid.ldap.sdk.DN
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo

import static org.apache.http.HttpStatus.*

class DefaultDelegationCloudServiceUpdateDaTest extends RootServiceTest {

    @Shared
    DelegationCloudService service

    def setup() {
        service = new DefaultDelegationCloudService()

        mockIdentityConfig(service)
        mockRequestContextHolder(service)
        mockAuthorizationService(service)
        mockDelegationService(service)
        mockUserGroupService(service)
        mockExceptionHandler(service)
        mockIdentityUserService(service)
        mockDelegationAgreementConverter(service)
        mockDomainService(service)
        mockIdmPathUtils(service)
        mockValidator20(service)
        mockRoleAssignmentConverter(service)
    }

    def "updateAgreement: error check"() {
        given:
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "rsId=" + it.id
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        authorizationService.isCallerAuthorizedToManageDelegationAgreement(_) >> true
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.principalDN = new DN(caller.uniqueId)
            it
        }
        delegationService.getDelegationAgreementById(_) >> daEntity

        DelegationAgreement daInvalidWeb = new DelegationAgreement()

        def invalidName = RandomStringUtils.randomAlphabetic(33)
        def validName = RandomStringUtils.randomAlphabetic(32)
        def invalidDescription = RandomStringUtils.randomAlphabetic(256)
        def validDescription = RandomStringUtils.randomAlphabetic(32)

        when: "name exceeding 32"
        daInvalidWeb.setName(invalidName)
        service.updateAgreement(token, daInvalidWeb)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", invalidName, 32) >> {throw new BadRequestException("asd")}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something

        when: "description exceeding 255"
        daInvalidWeb.setName(validName)
        daInvalidWeb.setDescription(invalidDescription)
        service.updateAgreement(token, daInvalidWeb)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", validName, 32)
        1 * validator20.validateStringMaxLength("description", invalidDescription, 255) >> {throw new BadRequestException()}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something
        0 * delegationService.updateDelegationAgreement(_)

        when: "da not found"
        daInvalidWeb.setDescription(validDescription)
        daInvalidWeb.setId("invalid")
        service.updateAgreement(token, daInvalidWeb)

        then:
        1 * delegationService.getDelegationAgreementById("invalid") >> null
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something
        0 * validator20.validateStringNotNullWithMaxLength("name", validName, 32)
        0 * validator20.validateStringMaxLength("description", invalidDescription, 255)
        0 * delegationService.updateDelegationAgreement(_)

        when: "caller is a delegate"
        mockAuthorizationService(service)
        authorizationService.isCallerAuthorizedToManageDelegationAgreement(_) >> false
        daEntity.setPrincipalDN(new DN("rsId=other"))
        service.updateAgreement(token, new DelegationAgreement())

        then:
        1 * delegationService.getDelegationAgreementById(_) >> daEntity
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something
        0 * validator20.validateStringNotNullWithMaxLength("name", validName, 32)
        0 * validator20.validateStringMaxLength("description", invalidDescription, 255)
        0 * delegationService.updateDelegationAgreement(_)
    }
}


