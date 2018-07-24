package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.DuplicateException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotFoundException
import org.apache.commons.lang3.RandomStringUtils
import org.opensaml.core.config.InitializationService
import org.openstack.docs.identity.api.v2.User
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

class UnverifiedUserCloud20ServiceTest extends RootServiceTest {

    @Shared
    DefaultCloud20Service service

    def setupSpec() {
        InitializationService.initialize()

        service = new DefaultCloud20Service()

    }

    def setup() {
        mockRequestContextHolder(service)
        mockAuthorizationService(service)
        mockUserService(service)
        mockDomainService(service)
        mockIdentityUserService(service)
        mockUserConverter(service)
        mockIdentityConfig(service)
        mockExceptionHandler(service)
        mockValidator(service)
    }

    @Unroll
    def "add invite user requires the feature flag to be enabled to create invite users: featureEnabled = #featureEnabled"() {
        given:
        allowUserAccess()
        def user = new User().with {
            it.domainId = RandomStringUtils.randomAlphabetic(8)
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it
        }
        domainService.getDomain(_) >> new Domain().with { it.enabled = true; it}
        identityConfig.getRepositoryConfig().getInvitesSupportedForRCNs() >> ['*']
        validator.isEmailValid(_) >> true
        userConverter.toUser(_, _) >> user

        when:
        service.addInviteUser(headers, uriInfo(), authToken, user)

        then:
        1 * identityConfig.getReloadableConfig().isCreationOfInviteUsersEnabled() >> featureEnabled
        if (featureEnabled) {
            0 * exceptionHandler.exceptionResponse(_)
            1 * userService.addUnverifiedUser(_)
        } else {
            0 * userService.addUnverifiedUser(_)
            1 * exceptionHandler.exceptionResponse(_) >> { args ->
                def exception = args[0]
                assert exception.class == ForbiddenException
                assert exception.message.endsWith(DefaultCloud20Service.ERROR_CREATION_OF_INVITE_USERS_DISABLED)
            }
        }

        where:
        featureEnabled << [true, false]
    }

    @Unroll
    def "user admins and managers can only create invite users in their own domain: sameDomain = #sameDomain, userType = #userType"() {
        given:
        allowUserAccess()
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it
        }
        domainService.getDomain(_) >> new Domain().with { it.enabled = true; it}
        identityConfig.getRepositoryConfig().getInvitesSupportedForRCNs() >> ['*']
        validator.isEmailValid(_) >> true
        userConverter.toUser(_, _) >> user
        def callerDomainId = RandomStringUtils.randomAlphabetic(8)
        def caller = new com.rackspace.idm.domain.entity.User().with {
            it.domainId = callerDomainId
            it
        }
        1 * identityConfig.getReloadableConfig().isCreationOfInviteUsersEnabled() >> true

        when:
        if (sameDomain) {
            user.domainId = callerDomainId
        } else {
            user.domainId = RandomStringUtils.randomAlphabetic(8)
        }
        service.addInviteUser(headers, uriInfo(), authToken, user)

        then:
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        1 * authorizationService.getIdentityTypeRoleAsEnum(caller) >> userType
        if (sameDomain || (IdentityUserTypeEnum.SERVICE_ADMIN == userType || IdentityUserTypeEnum.IDENTITY_ADMIN == userType)) {
            0 * exceptionHandler.exceptionResponse(_)
            1 * userService.addUnverifiedUser(_)
        } else {
            0 * userService.addUnverifiedUser(user)
            1 * exceptionHandler.exceptionResponse(_) >> { args ->
                def exception = args[0]
                assert exception.class == ForbiddenException
                assert exception.message.endsWith(DefaultCloud20Service.ERROR_DOMAIN_USERS_RESTRICTED_TO_SAME_DOMAIN_FOR_INVITE_USERS)
            }
        }

        where:
        [sameDomain, userType] << [[true, false],
                                   [IdentityUserTypeEnum.SERVICE_ADMIN,
                                    IdentityUserTypeEnum.IDENTITY_ADMIN,
                                    IdentityUserTypeEnum.USER_ADMIN,
                                    IdentityUserTypeEnum.USER_MANAGER]].combinations()
    }

    @Unroll
    def "domain specified on the unverified user must exist: domainExists = #domainExists"() {
        given:
        allowUserAccess()
        def user = new User().with {
            it.domainId = RandomStringUtils.randomAlphabetic(8)
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it
        }
        validator.isEmailValid(_) >> true
        identityConfig.getRepositoryConfig().getInvitesSupportedForRCNs() >> ['*']
        userConverter.toUser(_, _) >> user

        when:
        service.addInviteUser(headers, uriInfo(), authToken, user)

        then:
        1 * identityConfig.getReloadableConfig().isCreationOfInviteUsersEnabled() >> true
        if (domainExists) {
            1 * domainService.getDomain(user.domainId) >> new Domain().with { it.enabled = true; it}
            0 * exceptionHandler.exceptionResponse(_)
            1 * userService.addUnverifiedUser(_)
        } else {
            1 * domainService.getDomain(user.domainId) >> null
            0 * userService.addUnverifiedUser(user)
            1 * exceptionHandler.exceptionResponse(_) >> { args ->
                def exception = args[0]
                assert exception.class == NotFoundException
                assert exception.message.endsWith(DefaultCloud20Service.ERROR_DOMAIN_MUST_EXIST_FOR_UNVERIFIED_USERS)
            }
        }

        where:
        domainExists << [true, false]
    }

    @Unroll
    def "domain specified on the unverified user must be enabled: domainEnabled = #domainEnabled"() {
        given:
        allowUserAccess()
        def user = new User().with {
            it.domainId = RandomStringUtils.randomAlphabetic(8)
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it
        }
        validator.isEmailValid(_) >> true
        identityConfig.getRepositoryConfig().getInvitesSupportedForRCNs() >> ['*']
        userConverter.toUser(_, _) >> user

        when:
        service.addInviteUser(headers, uriInfo(), authToken, user)

        then:
        1 * identityConfig.getReloadableConfig().isCreationOfInviteUsersEnabled() >> true
        1 * domainService.getDomain(user.domainId) >> new Domain().with { it.enabled = domainEnabled; it}
        if (domainEnabled) {
            0 * exceptionHandler.exceptionResponse(_)
            1 * userService.addUnverifiedUser(_)
        } else {
            0 * userService.addUnverifiedUser(user)
            1 * exceptionHandler.exceptionResponse(_) >> { args ->
                def exception = args[0]
                assert exception.class == ForbiddenException
                assert exception.message.endsWith(DefaultCloud20Service.ERROR_DOMAIN_MUST_BE_ENABLED_FOR_UNVERIFIED_USERS)
            }
        }

        where:
        domainEnabled << [true, false]
    }

    @Unroll
    def "domain specified on the unverified user must be in an authorized RCN: rcnAuthorized = #domainAuthorized"() {
        given:
        allowUserAccess()
        def user = new User().with {
            it.domainId = RandomStringUtils.randomAlphabetic(8)
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it
        }
        validator.isEmailValid(_) >> true
        userConverter.toUser(_, _) >> user
        def rcn = RandomStringUtils.randomAlphabetic(8)

        when:
        service.addInviteUser(headers, uriInfo(), authToken, user)

        then:
        1 * identityConfig.getReloadableConfig().isCreationOfInviteUsersEnabled() >> true
        1 * domainService.getDomain(user.domainId) >> new Domain().with { it.enabled = true; it.rackspaceCustomerNumber = rcn; it}
        if (domainAuthorized) {
            1 * identityConfig.getRepositoryConfig().getInvitesSupportedForRCNs() >> [rcn]
            0 * exceptionHandler.exceptionResponse(_)
            1 * userService.addUnverifiedUser(_)
        } else {
            1 * identityConfig.getRepositoryConfig().getInvitesSupportedForRCNs() >> [RandomStringUtils.randomAlphabetic(8)]
            0 * userService.addUnverifiedUser(user)
            1 * exceptionHandler.exceptionResponse(_) >> { args ->
                def exception = args[0]
                assert exception.class == ForbiddenException
                assert exception.message.endsWith(DefaultCloud20Service.ERROR_DOMAIN_NOT_IN_AUTHORIZED_RCN_FOR_INVITE_USERS)
            }
        }

        where:
        domainAuthorized << [true, false]
    }

    @Unroll
    def "unverified users must have an email address: emailProvided = #emailProvided"() {
        given:
        allowUserAccess()
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = RandomStringUtils.randomAlphabetic(8)
            it
        }
        validator.isEmailValid(_) >> true
        userConverter.toUser(_, _) >> user

        when:
        service.addInviteUser(headers, uriInfo(), authToken, user)

        then:
        1 * identityConfig.getReloadableConfig().isCreationOfInviteUsersEnabled() >> true
        1 * domainService.getDomain(user.domainId) >> new Domain().with { it.enabled = true; it}
        1 * identityConfig.getRepositoryConfig().getInvitesSupportedForRCNs() >> ['*']
        if (emailProvided) {
            0 * exceptionHandler.exceptionResponse(_)
            1 * userService.addUnverifiedUser(_)
        } else {
            user.email = null
            0 * userService.addUnverifiedUser(user)
            1 * exceptionHandler.exceptionResponse(_) >> { args ->
                def exception = args[0]
                assert exception.class == BadRequestException
                assert exception.message.endsWith(DefaultCloud20Service.ERROR_UNVERIFIED_USERS_REQUIRE_EMAIL_ADDRESS)
            }
        }

        where:
        emailProvided << [true, false]
    }

    @Unroll
    def "unverified users must have a valid email address: emailValid = #emailValid"() {
        given:
        allowUserAccess()
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = RandomStringUtils.randomAlphabetic(8)
            it
        }
        userConverter.toUser(_, _) >> user

        when:
        service.addInviteUser(headers, uriInfo(), authToken, user)

        then:
        1 * identityConfig.getReloadableConfig().isCreationOfInviteUsersEnabled() >> true
        1 * domainService.getDomain(user.domainId) >> new Domain().with { it.enabled = true; it}
        1 * identityConfig.getRepositoryConfig().getInvitesSupportedForRCNs() >> ['*']
        if (emailValid) {
            1 * validator.isEmailValid(_) >> true
            0 * exceptionHandler.exceptionResponse(_)
            1 * userService.addUnverifiedUser(_)
        } else {
            1 * validator.isEmailValid(_) >> false
            0 * userService.addUnverifiedUser(user)
            1 * exceptionHandler.exceptionResponse(_) >> { args ->
                def exception = args[0]
                assert exception.class == BadRequestException
                assert exception.message.endsWith(DefaultCloud20Service.ERROR_UNVERIFIED_USERS_REQUIRED_VALID_EMAIL_ADDRESS)
            }
        }

        where:
        emailValid << [true, false]
    }

    @Unroll
    def "unverified users must have a unique email address within the domain: = emailUnique = #emailUnique"() {
        given:
        allowUserAccess()
        def user = new User().with {
            it.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
            it.domainId = RandomStringUtils.randomAlphabetic(8)
            it
        }
        userConverter.toUser(_, _) >> user

        when:
        service.addInviteUser(headers, uriInfo(), authToken, user)

        then:
        1 * identityConfig.getReloadableConfig().isCreationOfInviteUsersEnabled() >> true
        1 * domainService.getDomain(user.domainId) >> new Domain().with { it.enabled = true; it.domainId = user.domainId; it}
        1 * identityConfig.getRepositoryConfig().getInvitesSupportedForRCNs() >> ['*']
        1 * validator.isEmailValid(_) >> true
        if (emailUnique) {
            1 * identityUserService.getProvisionedUsersByDomainIdAndEmail(user.domainId, user.email) >> []
            0 * exceptionHandler.exceptionResponse(_)
            1 * userService.addUnverifiedUser(_)
        } else {
            1 * identityUserService.getProvisionedUsersByDomainIdAndEmail(user.domainId, user.email) >> [new com.rackspace.idm.domain.entity.User()]
            0 * userService.addUnverifiedUser(user)
            1 * exceptionHandler.exceptionResponse(_) >> { args ->
                def exception = args[0]
                assert exception.class == DuplicateException
                assert exception.message.endsWith(DefaultCloud20Service.ERROR_UVERIFIED_USERS_MUST_HAVE_UNIQUE_EMAIL_WITHIN_DOMAIN)
            }
        }

        where:
        emailUnique << [true, false]
    }

}
