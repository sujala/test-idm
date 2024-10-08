package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.EndUser
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.modules.usergroups.entity.UserGroup
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

class DefaultDelegationCloudServiceAddAgreementTest extends RootServiceTest {

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
    }

    @Unroll
    def "addAgreement: Validates name and description length"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        DelegationAgreement daInvalidWeb = new DelegationAgreement()

        def invalidName = RandomStringUtils.randomAlphabetic(33)
        def validName = RandomStringUtils.randomAlphabetic(32)
        def invalidDescription = RandomStringUtils.randomAlphabetic(256)

        when: "Don't Provide name"
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", null, 32) >> {throw new BadRequestException()}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something

        when: "Provide name exceeding 32"
        daInvalidWeb.setName(invalidName)
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", invalidName, 32) >> {throw new BadRequestException("asd")}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something

        when: "Provide empty string description"
        daInvalidWeb.setName(validName)
        daInvalidWeb.setDescription("")
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", validName, 32)
        1 * validator20.validateAttributeIsNotEmpty("description", "") >> {throw new BadRequestException()}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something

        when: "Provide description exceeding 255"
        daInvalidWeb.setName(validName)
        daInvalidWeb.setDescription(invalidDescription)
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", validName, 32)
        1 * validator20.validateStringMaxLength("description", invalidDescription, 255) >> {throw new BadRequestException()}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something

        when: "Provide empty string parentDelegationAgreementId"
        daInvalidWeb.setName(validName)
        daInvalidWeb.setParentDelegationAgreementId("")
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * validator20.validateStringNotNullWithMaxLength("name", validName, 32)
        1 * validator20.validateAttributeIsNotEmpty("parentDelegationAgreementId", "") >> {throw new BadRequestException()}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something

    }

    def "addAgreement: Success when set the USER principal to be the same user as the caller"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        reloadableConfig.getDelegationMaxNumberOfDaPerPrincipal() >> 5
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getProvisionedUserById(caller.id) >> caller // Delegate call
        Domain callerDomain = new Domain().with {
            it.domainId = caller.domainId
            it
        }

        delegationService.addDelegationAgreement(_) >> new com.rackspace.idm.domain.entity.DelegationAgreement()
        domainService.getDomain(callerDomain.domainId) >> callerDomain
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain

        def capturedException
        exceptionHandler.exceptionResponse(_) >> { args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN) }

        when: "add agreement with principal as caller"
        DelegationAgreement daValidWeb = new DelegationAgreement().with {
            it.principalType = PrincipalType.USER
            it.principalId = caller.id
            it
        }
        def response = service.addAgreement(uriInfo, token, daValidWeb)

        then:
        1 * identityUserService.getEndUserById(caller.id) >> caller
        1 * delegationAgreementConverter.fromDelegationAgreementWeb(_) >> new com.rackspace.idm.domain.entity.DelegationAgreement()
        1 * delegationAgreementConverter.toDelegationAgreementWeb(_) >> new DelegationAgreement()
        response.status == SC_CREATED
    }

    @Unroll
    def "addAgreement Error: Error when specify a USER principal that is other than caller"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = "callerDomain"
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getProvisionedUserById(caller.id) >> caller // Delegate call
        Domain callerDomain = new Domain().with {
            it.domainId = caller.domainId
            it
        }

        domainService.getDomain(callerDomain.domainId) >> callerDomain
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain

        when:
        DelegationAgreement daInvalidWeb = new DelegationAgreement().with {
            it.principalType = PrincipalType.USER
            it.principalId = principalUser.id
            it
        }
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * identityUserService.getEndUserById(principalUser.id) >> principalUser
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, DefaultDelegationCloudService.ERROR_MSG_PRINCIPAL_NOT_FOUND)
            return Response.status(SC_BAD_REQUEST)
        }

        where:
        principalUser << [new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = "callerDomain"
            it
        },new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = "domain2"
            it
        },new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = null
            it
        }
        ]
    }

    def "addAgreement Error: Error when specified USER principal does not exist"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = "callerDomain"
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getProvisionedUserById(caller.id) >> caller // Delegate call
        Domain callerDomain = new Domain().with {
            it.domainId = caller.domainId
            it
        }

        domainService.getDomain(callerDomain.domainId) >> callerDomain
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain

        DelegationAgreement daInvalidWeb = new DelegationAgreement().with {
            it.principalType = PrincipalType.USER
            it.principalId = RandomStringUtils.randomAlphabetic(10)
            it
        }

        when:
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * identityUserService.getEndUserById(daInvalidWeb.principalId) >> null
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, DefaultDelegationCloudService.ERROR_MSG_PRINCIPAL_NOT_FOUND)
            return Response.status(SC_BAD_REQUEST)
        }
    }

    def "addAgreement: Can specify user group principal when caller is member"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        reloadableConfig.getDelegationMaxNumberOfDaPerPrincipal() >> 5
        UserGroup ug = new UserGroup().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it.uniqueId = "group=${RandomStringUtils.randomAlphanumeric(8)}"
            it
        }

        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.getUserGroupDNs().add(ug.getGroupDn())
            it.domainId = ug.domainId
            it
        }

        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        delegationService.getDelegateByReference(_) >> caller // Delegate call
        Domain callerDomain = new Domain().with {
            it.domainId = caller.domainId
            it
        }

        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        delegationService.addDelegationAgreement(_) >> new com.rackspace.idm.domain.entity.DelegationAgreement()
        domainService.getDomain(callerDomain.domainId) >> callerDomain
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain

        DelegationAgreement daValidWeb = new DelegationAgreement().with {
            it.principalType = PrincipalType.USER_GROUP
            it.principalId = ug.id
            it
        }

        when: "add agreement with user group principal that member of"
        def response = service.addAgreement(uriInfo, token, daValidWeb)

        then: "created successfully"
        1 * userGroupService.getGroupById(ug.id) >> ug
        1 * delegationAgreementConverter.fromDelegationAgreementWeb(_) >> new com.rackspace.idm.domain.entity.DelegationAgreement()
        1 * delegationAgreementConverter.toDelegationAgreementWeb(_) >> new DelegationAgreement()
        response.status == SC_CREATED
    }

    @Unroll
    def "addAgreement user group errors: Verifies USER GROUP principal must be valid for caller"() {
        def principalId = userGroup != null ? userGroup.id : "group=agroupOther"

        userGroupService.getGroupById(principalId) >> userGroup

        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true

        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = "callerDomain"
            it.userGroupDNs.add("group=agroup)")
            it
        }

        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getProvisionedUserById(caller.id) >> caller // Delegate call
        Domain callerDomain = new Domain().with {
            it.domainId = caller.domainId
            it
        }

        domainService.getDomain(callerDomain.domainId) >> callerDomain
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain

        when: "add agreement with user group principal that user is not member of"
        DelegationAgreement daValidWeb = new DelegationAgreement().with {
            it.principalType = PrincipalType.USER_GROUP
            it.principalId = principalId
            it
        }
        service.addAgreement(uriInfo, token, daValidWeb)

        then: "Get error"
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, DefaultDelegationCloudService.ERROR_MSG_PRINCIPAL_NOT_FOUND)
            return Response.status(SC_BAD_REQUEST)
        }

        where:
        userGroup << [
                // Group in different domain
                new UserGroup().with {
                    it.id = RandomStringUtils.randomAlphabetic(10)
                    it.domainId = RandomStringUtils.randomAlphabetic(10)
                    it.uniqueId = "group=agroup"
                    it
                },
                // Different group
                new UserGroup().with {
                    it.id = RandomStringUtils.randomAlphabetic(10)
                    it.domainId = "callerDomain"
                    it.uniqueId = "group=agroupOther"
                    it
                },
                // group doesn't exist
                null
        ]
    }

    @Unroll
    def "addAgreement: Root agreement nest level is validated against max allowed: max: #maxNestLevel; tested: #daNestLevel"() {
        reloadableConfig.getMaxDelegationAgreementNestingLevel() >> maxNestLevel

        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller

        identityUserService.getEndUserById(caller.id) >> caller
        identityUserService.getProvisionedUserById(caller.id) >> caller // Delegate call

        DelegationAgreement daInvalidWeb = new DelegationAgreement()

        when:
        daInvalidWeb.setSubAgreementNestLevel(BigInteger.valueOf(daNestLevel))
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * validator20.validateIntegerMinMax("subAgreementNestLevel", daNestLevel, 0, maxNestLevel) >> {throw new BadRequestException("asd")}
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST) // Just need to return something

        where:
        [maxNestLevel, daNestLevel] << [[1, -1], [1,2], [5,6], [0, 1]]
    }

    @Unroll
    def "addAgreement: Nested agreement nest level is validated against parents nest level: parentNestLevel: #parentNestLevel; tested: #daNestLevel"() {
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
        delegationService.getDelegationAgreementById(daParent.id) >> daParent

        DelegationAgreement daInvalidWeb = new DelegationAgreement().with {
            it.name = "nested"
            it.parentDelegationAgreementId = daParent.id
            it.subAgreementNestLevel = daNestLevel
            it
        }

        when:
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then: "nest level validated to be no more than 1 less than parent nest level"
        1 * validator20.validateIntegerMinMax("subAgreementNestLevel", daNestLevel, 0, parentNestLevel-1) >> {throw new BadRequestException("asd")}

        // Test is that the validator will be called appropriately. Once that tested, we just force exception to be thrown
        1 * exceptionHandler.exceptionResponse(_) >> Response.status(SC_BAD_REQUEST)

        where:
        [parentNestLevel, daNestLevel] << [[1, -1], [1,2], [5,6], [6,6]]
    }

    def "addDelegationAgreement: Error when maximum number of DAs are created for principal"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        DelegationAgreement daWeb = new DelegationAgreement().with {
            it
        }
        def token = "token"
        User caller = new User().with {
            it.id = "callerId"
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        identityUserService.getEndUserById(caller.id) >> caller

        def callerDomain = new Domain().with {
            it.domainId = caller.domainId
            it
        }

        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain
        reloadableConfig.areDelegationAgreementsEnabledForRcn(callerDomain.rackspaceCustomerNumber) >> true
        reloadableConfig.getDelegationMaxNumberOfDaPerPrincipal() >> 1

        when:
        service.addAgreement(uriInfo, token, daWeb)

        then:
        1 * delegationService.countNumberOfDelegationAgreementsByPrincipal(_) >> 1
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0]
                    , BadRequestException
                    , ErrorCodes.ERROR_CODE_THRESHOLD_REACHED
                    , "Maximum number of delegation agreements has been reached for principal")
            return Response.status(SC_BAD_REQUEST)
        }
    }

    def "addAgreement: Returns error when parent agreement does not exist"() {
        reloadableConfig.getDelegationMaxNumberOfDaPerPrincipal() >> 5

        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        identityUserService.getEndUserById(caller.id) >> caller
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        requestContext.getEffectiveCallerDomain() >> new Domain()

        DelegationAgreement daInvalidWeb = new DelegationAgreement().with {
            it.setSubAgreementNestLevel(BigInteger.ONE)
            it.parentDelegationAgreementId = "nonexistant"
            it
        }

        when:
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * delegationService.getDelegationAgreementById(daInvalidWeb.getParentDelegationAgreementId()) >> null
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, "The specified parent agreement does not exist for the requested principal")
            Response.status(SC_BAD_REQUEST)
        }
    }

    @Unroll
    def "addAgreement: Returns error when subagreement principal not a delegate of parent agreement"() {
        reloadableConfig.getDelegationMaxNumberOfDaPerPrincipal() >> 5

        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        identityUserService.getEndUserById(caller.id) >> caller
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        requestContext.getEffectiveCallerDomain() >> new Domain()

        DelegationAgreement daInvalidWeb = new DelegationAgreement().with {
            it.setSubAgreementNestLevel(BigInteger.ONE)
            it.parentDelegationAgreementId = "nonexistant"
            it
        }

        com.rackspace.idm.domain.entity.DelegationAgreement parentAgreement = Mock()

        when:
        service.addAgreement(uriInfo, token, daInvalidWeb)

        then:
        1 * delegationService.getDelegationAgreementById(daInvalidWeb.getParentDelegationAgreementId()) >> parentAgreement
        1 * parentAgreement.isExplicitDelegate(_) >> {args ->
            EndUser user = (EndUser) args[0]
            assert user.id == caller.id
            false
        }
        1 * parentAgreement.isEffectiveDelegate(caller) >> false
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            IdmExceptionAssert.assertException(args[0], NotFoundException, ErrorCodes.ERROR_CODE_NOT_FOUND, "The specified parent agreement does not exist for the requested principal")
            Response.status(SC_BAD_REQUEST)
        }
    }

    @Unroll
    def "addAgreement: When creating subagreement parent is accepted if subagreement principal is effective delegate"() {
        reloadableConfig.getDelegationMaxNumberOfDaPerPrincipal() >> 5

        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess

        reloadableConfig.areDelegationAgreementsEnabledForRcn(_) >> true
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }
        Domain callerDomain = new Domain().with {
            it.domainId = caller.getDomainId()
            it.rackspaceCustomerNumber = "myRcn"
            it
        }

        identityUserService.getEndUserById(caller.id) >> caller
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        requestContext.getEffectiveCallerDomain() >> callerDomain

        com.rackspace.idm.domain.entity.DelegationAgreement parentAgreement = Mock()

        DelegationAgreement daExplicitDelegate = new DelegationAgreement().with {
            it.setSubAgreementNestLevel(BigInteger.ONE)
            it.parentDelegationAgreementId = "exists"
            it
        }

        DelegationAgreement daEffectiveDelegate = new DelegationAgreement().with {
            it.setSubAgreementNestLevel(BigInteger.ONE)
            it.parentDelegationAgreementId = "exists"
            it
        }

        when: "Is explicit delegate"
        service.addAgreement(uriInfo, token, daExplicitDelegate)

        then: "Parent is accepted and DA is created"
        1 * delegationService.getDelegationAgreementById(daExplicitDelegate.getParentDelegationAgreementId()) >> parentAgreement
        1 * parentAgreement.isExplicitDelegate(_) >> true
        0 * parentAgreement.isEffectiveDelegate(caller)
        1 * parentAgreement.getDomainId() >> callerDomain.getDomainId()
        (1.._) * parentAgreement.getSubAgreementNestLevelNullSafe() >> 2 // sub agreement setting to 1, so this must be >1
        1 * delegationAgreementConverter.fromDelegationAgreementWeb(_) >> new com.rackspace.idm.domain.entity.DelegationAgreement()
        1 * delegationService.addDelegationAgreement(_) >> new com.rackspace.idm.domain.entity.DelegationAgreement()

        and: "Principal defaults to caller when not specified"
        daExplicitDelegate.principalType == PrincipalType.USER
        daExplicitDelegate.principalId == caller.id

        when: "Is effective delegate"
        service.addAgreement(uriInfo, token, daEffectiveDelegate)

        then: "Parent is accepted and DA is created"
        1 * delegationService.getDelegationAgreementById(daEffectiveDelegate.getParentDelegationAgreementId()) >> parentAgreement
        1 * parentAgreement.isExplicitDelegate(_) >> false
        1 * parentAgreement.isEffectiveDelegate(caller) >> true
        1 * parentAgreement.getDomainId() >> callerDomain.getDomainId()
        (1.._) * parentAgreement.getSubAgreementNestLevelNullSafe() >> 2 // sub agreement setting to 1, so this must be >1
        1 * delegationAgreementConverter.fromDelegationAgreementWeb(_) >> new com.rackspace.idm.domain.entity.DelegationAgreement()
        1 * delegationService.addDelegationAgreement(_) >> new com.rackspace.idm.domain.entity.DelegationAgreement()
    }

    @Unroll
    def "addAgreement: test for feature.enable.global.root.da.creation with flag: #flag"() {
        UriInfo uriInfo = Mock()
        ScopeAccess tokenScopeAccess = new UserScopeAccess()
        def token = "token"
        securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(token) >> tokenScopeAccess
        reloadableConfig.isGlobalRootDelegationAgreementCreationEnabled() >> flag
        User caller = new User().with {
            it.id = RandomStringUtils.randomAlphabetic(10)
            it.domainId = RandomStringUtils.randomAlphabetic(10)
            it
        }

        requestContext.getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getProvisionedUserById(caller.id) >> caller // Delegate call
        Domain callerDomain = new Domain().with {
            it.domainId = caller.domainId
            it
        }

        domainService.getDomain(callerDomain.domainId) >> callerDomain
        requestContextHolder.getRequestContext().getEffectiveCallerDomain() >> callerDomain

        def capturedException
        exceptionHandler.exceptionResponse(_) >> { args -> capturedException = args[0]; return Response.status(HttpServletResponse.SC_FORBIDDEN) }

        DelegationAgreement daValidWeb = new DelegationAgreement().with {
            it.principalType = PrincipalType.USER
            it.principalId = caller.id
            it
        }
        when: "add agreement with principal as caller"
        service.addAgreement(uriInfo, token, daValidWeb)

        then:
        if(flag){
            1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER)
        }else{
            1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_ADMIN)
        }

        where:
        flag << [false, true]
    }
}


