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

        when: "description is empty string"
        daInvalidWeb.setName(validName)
        daInvalidWeb.setDescription("")
        service.updateAgreement(token, daInvalidWeb)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", validName, 32)
        1 * validator20.validateAttributeIsNotEmpty("description", "") >> {throw new BadRequestException()}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something
        0 * delegationService.updateDelegationAgreement(_)

        when: "description exceeding 255"
        daInvalidWeb.setName(validName)
        daInvalidWeb.setDescription(invalidDescription)
        service.updateAgreement(token, daInvalidWeb)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", validName, 32)
        1 * validator20.validateAttributeIsNotEmpty("description", invalidDescription)
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

        when: "caller is not a principal"
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

    def "SubAgreementsAllowed and nest level are mutually exclusive"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "rsId=" + it.id
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.principalDN = new DN(caller.uniqueId)
            it.id = "daId"
            it
        }
        delegationService.getDelegationAgreementById(daEntity.id) >> daEntity
        authorizationService.isCallerAuthorizedToManageDelegationAgreement(daEntity) >> true

        DelegationAgreement daInvalidWeb = new DelegationAgreement().with {
            it.id = daEntity.id
            it
        }

        when: "When both provided"
        daInvalidWeb.setAllowSubAgreements(true)
        daInvalidWeb.setSubAgreementNestLevel(BigInteger.ONE)
        service.updateAgreement(token, daInvalidWeb)

        then:
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], BadRequestException, ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST, DefaultDelegationCloudService.ERROR_MSG_SUBAGREEMENT_MUTUAL_EXCLUSION)
            Response.status(SC_BAD_REQUEST)
        }
    }

    @Unroll
    def "updateAgreement: Root agreement nest level is validated against max allowed: max: #maxNestLevel; tested: #daNestLevel"() {
        reloadableConfig.getMaxDelegationAgreementNestingLevel() >> maxNestLevel

        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "rsId=" + it.id
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        identityUserService.getEndUserById(caller.id) >> caller
        identityUserService.getProvisionedUserById(caller.id) >> caller // Delegate call

        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.principalDN = new DN(caller.uniqueId)
            it.id = "daId"
            it
        }
        delegationService.getDelegationAgreementById(daEntity.id) >> daEntity
        authorizationService.isCallerAuthorizedToManageDelegationAgreement(daEntity) >> true

        DelegationAgreement daInvalidWeb = new DelegationAgreement().with {
            it.id = daEntity.id
            it
        }

        when:
        daInvalidWeb.setSubAgreementNestLevel(BigInteger.valueOf(daNestLevel))
        service.updateAgreement(token, daInvalidWeb)

        then:
        1 * validator20.validateIntegerMinMax("subAgreementNestLevel", daNestLevel, 0, maxNestLevel) >> {throw new BadRequestException("asd")}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something

        where:
        [maxNestLevel, daNestLevel] << [[1, -1], [1,2], [5,6], [0, 1]]
    }

    @Unroll
    def "updateAgreement: Nested agreement nest level is validated against parents nest level: parentNestLevel: #parentNestLevel; tested: #daNestLevel"() {
        reloadableConfig.getMaxDelegationAgreementNestingLevel() >> parentNestLevel + 1 // make property 1 higher than parent

        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        DN callerDN = new DN("com=rax")
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = callerDN.toString()
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        identityUserService.getEndUserById(caller.id) >> caller
        identityUserService.getProvisionedUserById(caller.id) >> caller // Delegate call

        com.rackspace.idm.domain.entity.DelegationAgreement daParent = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.name = "parent"
            it.id = "id"
            it.subAgreementNestLevel = parentNestLevel
            it.domainId = caller.domainId
            it.delegates.add(callerDN)
            it
        }
        com.rackspace.idm.domain.entity.DelegationAgreement daNestedEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.id = "daNestedId"
            it.parentDelegationAgreementId = daParent.id
            it.principalDN = new DN(caller.uniqueId)
            it
        }
        delegationService.getDelegationAgreementById(daParent.id) >> daParent
        delegationService.getDelegationAgreementById(daNestedEntity.id) >> daNestedEntity
        authorizationService.isCallerAuthorizedToManageDelegationAgreement(daNestedEntity) >> true

        DelegationAgreement nestedWebDa = new DelegationAgreement().with {
            it.id = "daNestedId"
            it.parentDelegationAgreementId = daParent.id
            it.subAgreementNestLevel = daNestLevel
            it
        }

        when:
        service.updateAgreement(token, nestedWebDa)

        then: "nest level validated to be no more than 1 less than parent nest level"
        1 * validator20.validateIntegerMinMax("subAgreementNestLevel", daNestLevel, 0, parentNestLevel-1) >> {throw new BadRequestException("asd")}

        // Test is that the validator will be called appropriately. Once that tested, we just force exception to be thrown
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST)

        where:
        [parentNestLevel, daNestLevel] << [[1, -1], [1,2], [5,6], [6,6]]
    }

    @Unroll
    def "updateAgreement: Nested agreement can not be updated to support nesting with allowSubAgreements if parent has a nesting level of 1"() {
        reloadableConfig.getMaxDelegationAgreementNestingLevel() >> 2 // make property 1 higher than parent

        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        DN callerDN = new DN("com=rax")
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = callerDN.toString()
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        identityUserService.getEndUserById(caller.id) >> caller
        identityUserService.getProvisionedUserById(caller.id) >> caller // Delegate call

        com.rackspace.idm.domain.entity.DelegationAgreement daParent = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.name = "parent"
            it.id = "id"
            it.subAgreementNestLevel = 1
            it.domainId = caller.domainId
            it.delegates.add(callerDN)
            it
        }
        com.rackspace.idm.domain.entity.DelegationAgreement daNestedEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.id = "daNestedId"
            it.parentDelegationAgreementId = daParent.id
            it.allowSubAgreements = false
            it.subAgreementNestLevel = 0
            it.principalDN = new DN(caller.uniqueId)
            it
        }
        delegationService.getDelegationAgreementById(daParent.id) >> daParent
        delegationService.getDelegationAgreementById(daNestedEntity.id) >> daNestedEntity
        authorizationService.isCallerAuthorizedToManageDelegationAgreement(daNestedEntity) >> true

        DelegationAgreement nestedWebDa = new DelegationAgreement().with {
            it.id = "daNestedId"
            it.parentDelegationAgreementId = daParent.id
            it.allowSubAgreements = true
            it
        }

        when:
        service.updateAgreement(token, nestedWebDa)

        then: "nest level validated to be no more than 1 less than parent nest level"
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], BadRequestException, ErrorCodes.ERROR_CODE_INVALID_VALUE, "Agreement can not be updated to support subagreements.")
            Response.status(SC_BAD_REQUEST)
        }
    }

    @Unroll
    def "updateAgreement: Sets allowSubAgreement when only nest provided: nestLevel: #nestLevel"() {
        reloadableConfig.getMaxDelegationAgreementNestingLevel() >> 5

        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "rs=hi"
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(caller.id) >> caller

        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.principalDN = new DN(caller.uniqueId)
            it.id = "daId"
            it
        }
        delegationService.getDelegationAgreementById(daEntity.id) >> daEntity
        authorizationService.isCallerAuthorizedToManageDelegationAgreement(daEntity) >> true

        DelegationAgreement daWeb = new DelegationAgreement().with {
            it.id = daEntity.id
            it.subAgreementNestLevel = BigInteger.valueOf(nestLevel)
            it
        }

        when:
        service.updateAgreement(token, daWeb)

        then:
        1 * delegationService.updateDelegationAgreement(_) >> { args ->
            def da = args[0]
            assert nestLevel > 0 ? da.getAllowSubAgreements() : !da.getAllowSubAgreements()
            assert da.getSubAgreementNestLevel().intValue() == nestLevel
            null
        }

        where:
        nestLevel << [0, 1, 4]
    }

    @Unroll
    def "updateAgreement: If allowSubAgreements specified, only changes root agreement nest level when changing nesting: existingAllowSubAgreements: #existingAllowSubAgreements; existingNestLevel: #existingNestLevel; newAllowSubAgreements: #newAllowSubAgreements; expectedNestLevel: #expectedNestLevel"() {
        reloadableConfig.getMaxDelegationAgreementNestingLevel() >> 5

        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "rs=hi"
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(caller.id) >> caller

        com.rackspace.idm.domain.entity.DelegationAgreement daEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.principalDN = new DN(caller.uniqueId)
            it.id = "daId"
            it.allowSubAgreements = existingAllowSubAgreements
            it.subAgreementNestLevel = existingNestLevel
            it
        }
        delegationService.getDelegationAgreementById(daEntity.id) >> daEntity
        authorizationService.isCallerAuthorizedToManageDelegationAgreement(daEntity) >> true

        DelegationAgreement daWeb = new DelegationAgreement().with {
            it.id = daEntity.id
            it.allowSubAgreements = newAllowSubAgreements
            it
        }
        when:
        service.updateAgreement(token, daWeb)

        then:
        1 * delegationService.updateDelegationAgreement(_) >> { args ->
            com.rackspace.idm.domain.entity.DelegationAgreement da = args[0]
            assert da.allowSubAgreements == newAllowSubAgreements
            assert da.subAgreementNestLevel == expectedNestLevel
            null
        }
        where:
        [existingAllowSubAgreements, existingNestLevel, newAllowSubAgreements, expectedNestLevel] << [[true, 1, false, 0], [false, 0, true, 5], [true, 2, true, 2], [false, 0, false, 0]]
    }

    @Unroll
    def "addAgreement: If changing allowSubAgreements to true, sets nested agreement nest level to parent's nest level - 1: nestedAllowSubAgreement: #nestedAllowSubAgreement; parentNestLevel: #parentNestLevel"() {
        reloadableConfig.getMaxDelegationAgreementNestingLevel() >> parentNestLevel

        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        DN callerDN = new DN("com=rax")
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = callerDN.toString()
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        identityUserService.getEndUserById(caller.id) >> caller
        identityUserService.getProvisionedUserById(caller.id) >> caller // Delegate call

        com.rackspace.idm.domain.entity.DelegationAgreement daParent = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.name = "parent"
            it.id = "id"
            it.subAgreementNestLevel = parentNestLevel
            it.domainId = caller.domainId
            it.delegates.add(callerDN)
            it
        }
        com.rackspace.idm.domain.entity.DelegationAgreement daNestedEntity = new com.rackspace.idm.domain.entity.DelegationAgreement().with {
            it.id = "daNestedId"
            it.parentDelegationAgreementId = daParent.id
            it.principalDN = new DN(caller.uniqueId)
            it.allowSubAgreements = false
            it
        }
        delegationService.getDelegationAgreementById(daParent.id) >> daParent
        delegationService.getDelegationAgreementById(daNestedEntity.id) >> daNestedEntity
        authorizationService.isCallerAuthorizedToManageDelegationAgreement(daNestedEntity) >> true

        DelegationAgreement nestedWebDa = new DelegationAgreement().with {
            it.id = daNestedEntity.id
            it.parentDelegationAgreementId = daNestedEntity.parentDelegationAgreementId
            it.allowSubAgreements = nestedAllowSubAgreement
            it
        }

        when:
        service.updateAgreement(token, nestedWebDa)

        then: "nest level of subagreement defaults to 1 less than parent"
        1 * delegationService.updateDelegationAgreement(_) >> { args ->
            com.rackspace.idm.domain.entity.DelegationAgreement da = args[0]
            assert da.allowSubAgreements == nestedAllowSubAgreement
            if (nestedAllowSubAgreement) {
                assert da.subAgreementNestLevel.intValue() == parentNestLevel - 1
            } else {
                assert da.subAgreementNestLevel.intValue() == 0
            }
        }

        where:
        [nestedAllowSubAgreement, parentNestLevel] << [[true, 5], [false, 5], [true, 2]]
    }

}


