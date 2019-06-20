package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.EndUser
import com.rackspace.idm.domain.entity.FederatedUser
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.ForbiddenException
import org.apache.http.HttpStatus
import org.opensaml.core.config.InitializationService
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import javax.ws.rs.core.Response

class UpdateUserServiceTest  extends RootServiceTest {

    @Shared JAXBObjectFactories objFactories
    @Shared DefaultCloud20Service service

    def setupSpec() {
        InitializationService.initialize()
    }

    def setup() {
        //service being tested
        service = new DefaultCloud20Service()
        mockRequestContextHolder(service)
        mockExceptionHandler(service)
        mockAuthorizationService(service)
        mockIdentityUserService(service)
        mockValidator20(service)
        mockAtomHopperClient(service)
        mockUserConverter(service)
        mockPrecedenceValidator(service)
        mockUserService(service)
        mockDomainService(service)
        mockDefaultRegionService(service)
        mockIdentityConfig(service)

        // Since these are generated, just use real ones
        service.jaxbObjectFactories = new JAXBObjectFactories()
    }

    /**
     * This is just basic authorization requirements on the service that a majority of our services must call. Service
     * limited to default users+
     *
     * @return
     */
    def "updateUser: First validates token is valid, caller is not a racker, caller is enabled, and caller has at least default user role"() {
        given:
        def token = "token"
        def exceptionToThrow = new ForbiddenException()

        when:
        service.updateUser(headers, token, "userId", null)

        then:
        1 * requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContextHolder.getRequestContext().verifyEffectiveCallerIsNotARacker()
        1 * requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER) >> {throw exceptionToThrow}

        and: "Exceptions routed through exception handler"
        1 * exceptionHandler.exceptionResponse(exceptionToThrow)
    }

    def "updateUser: Unverified users can not be updated by non identity:admin+"() {
        given:
        UserForCreate userToUpdate = new UserForCreate().with {
            it.id = "2"
            it.enabled = false
            it.email = "someEmail@rackspace.com"
            it
        }

        User existingUnverifiedUser = entityFactory.createUnverifiedUser().with {
            it.id = userToUpdate.id
            it
        }

        def otherCaller = entityFactory.createUser().with {
            it.id = "id"
            it
        }

        when: "Call by non-admin, non-self"
        service.updateUser(headers, authToken, userToUpdate.id, userToUpdate)

        then: "Fails"
        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> otherCaller
        1 * identityUserService.getEndUserById(userToUpdate.id) >> existingUnverifiedUser
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> false

        and:
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            Exception exception = args[0]
            IdmExceptionAssert.assertException(exception, ForbiddenException, null, "Not Authorized")
        }

        when: "Call by non-admin, self"
        service.updateUser(headers, authToken, userToUpdate.id, userToUpdate)

        then: "Fails"
        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> existingUnverifiedUser
        1 * identityUserService.getEndUserById(userToUpdate.id) >> existingUnverifiedUser
        1 * authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> false

        and:
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            Exception exception = args[0]
            IdmExceptionAssert.assertException(exception, ForbiddenException, null, "Not Authorized")
        }
    }

    def "updateUser Unverified user can be updated by identity:admins"() {
        given:
        UserForCreate userToUpdate = new UserForCreate().with {
            it.id = "2"
            it.enabled = false
            it.email = "someEmail@rackspace.com"
            it
        }

        User existingUnverifiedUser = entityFactory.createUnverifiedUser().with {
            it.id = userToUpdate.id
            it
        }

        def caller = entityFactory.createUser().with {
            it.id = "id"
            it
        }

        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(userToUpdate.id) >> existingUnverifiedUser
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true

        when:
        Response response = service.updateUser(headers, authToken, userToUpdate.id, userToUpdate).build()

        then:
        response.status == HttpStatus.SC_OK
    }

    def "updateUser: Fed users can update self"() {
        given:

        UserForCreate userToUpdate = createUserForUpdateApi("2")
        EndUser existingUser = entityFactory.createFederatedUser().with {
            it.id = userToUpdate.id
            it.domainId = "sameDomain"
            it.phonePin = "654987"
            it
        }

        EndUser caller = createFedCaller(existingUser.id, existingUser.domainId)

        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(userToUpdate.id) >> existingUser
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> false

        when: "Update self"
        Response response = service.updateUser(headers, authToken, userToUpdate.id, userToUpdate).build()

        then: "Succeeds"
        response.status == HttpStatus.SC_OK
    }

    @Unroll
    def "updateUser Error: Fed users are forbidden from updating other fed users"() {
        given:
        UserForCreate userToUpdate = createUserForUpdateApi("2")
        EndUser existingUser = entityFactory.createFederatedUser().with {
            it.id = userToUpdate.id
            it.domainId = "sameDomain"
            it.phonePin = "654987"
            it
        }

        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(userToUpdate.id) >> existingUser
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> false

        when:
        service.updateUser(headers, authToken, userToUpdate.id, userToUpdate)

        then:
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            Exception exception = args[0]
            IdmExceptionAssert.assertException(exception, ForbiddenException, null, "Not Authorized")
        }

        where:
        caller | _
        createFedCaller("3", "sameDomain") | _ // Fed user in same domain
        createFedCaller("3", "otherDomain") | _ // Fed user in different domain
    }

    def "updateUser Error: Provisioned user targets use precedence validator to verify access when caller is not identity:admin or self "() {
        given:
        UserForCreate userToUpdate = createUserForUpdateApi("2")
        EndUser existingUser = entityFactory.createUser().with {
            it.id = userToUpdate.id
            it.domainId = "sameDomain"
            it.phonePin = "654987"
            it
        }
        EndUser caller = entityFactory.createUser().with {
            it.id = "3"
            it.domainId = existingUser.domainId
            it
        }

        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(userToUpdate.id) >> existingUser
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> false

        when:
        service.updateUser(headers, authToken, userToUpdate.id, userToUpdate)

        then:
        1 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(_) >> {throw new ForbiddenException()} // just throw exception to end test
        1 * exceptionHandler.exceptionResponse(_)
    }

    def "updateUser: Provisioned users bypass precedence validator when updating self "() {
        given:
        UserForCreate userToUpdate = createUserForUpdateApi("2")
        EndUser existingUser = entityFactory.createUser().with {
            it.id = userToUpdate.id
            it.domainId = "sameDomain"
            it.phonePin = "654987"
            it
        }
        EndUser caller = entityFactory.createUser().with {
            it.id = userToUpdate.id
            it.domainId = existingUser.domainId
            it
        }

        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(userToUpdate.id) >> existingUser
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> false

        when:
        Response response = service.updateUser(headers, authToken, userToUpdate.id, userToUpdate).build()

        then:
        0 * precedenceValidator.verifyEffectiveCallerPrecedenceOverUser(_)
        1 * userService.updateUser(_)
        response.status == HttpStatus.SC_OK
    }

    def "updateUser: Provisioned user updating own phone pin resets failure count"(){
        given:
        UserForCreate userToUpdate = createUserForUpdateApi("2")
        EndUser existingUser = entityFactory.createUser().with {
            it.id = userToUpdate.id
            it.domainId = "sameDomain"
            it.phonePinAuthenticationFailureCount = GlobalConstants.PHONE_PIN_AUTHENTICATION_FAILURE_LOCKING_THRESHOLD - 1
            it.phonePin = "654987"
            it
        }
        EndUser caller = entityFactory.createUser().with {
            it.id = userToUpdate.id
            it.domainId = existingUser.domainId
            it
        }

        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(userToUpdate.id) >> existingUser
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> false

        when:
        service.updateUser(headers, authToken, userToUpdate.id, userToUpdate)

        then:
        1 * userConverter.fromUser(userToUpdate) >> { args ->
            UserForCreate fromUser = args[0]
            assert fromUser.phonePin == userToUpdate.phonePin

            // Create the new user and copy over details (like converter would do)
            new User().with {
                it.phonePin = fromUser.phonePin
                it
            }
        }

        1 * userService.updateUser(_) >> {args ->
            User userStateForUpdate = args[0]
            assert userStateForUpdate.phonePin == userToUpdate.phonePin
            assert userStateForUpdate.phonePinAuthenticationFailureCount == 0
        }
    }

    def "updateUser: Federated user updating own phone pin resets failure count"(){
        given:
        UserForCreate userToUpdate = createUserForUpdateApi("2")
        EndUser existingUser = entityFactory.createFederatedUser().with {
            it.id = userToUpdate.id
            it.domainId = "sameDomain"
            it.phonePinAuthenticationFailureCount = GlobalConstants.PHONE_PIN_AUTHENTICATION_FAILURE_LOCKING_THRESHOLD - 1
            it.phonePin = "654987"
            it
        }
        EndUser caller = entityFactory.createUser().with {
            it.id = userToUpdate.id
            it.domainId = existingUser.domainId
            it
        }

        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(userToUpdate.id) >> existingUser
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> false

        when:
        service.updateUser(headers, authToken, userToUpdate.id, userToUpdate)

        then:
        1 * identityUserService.updateFederatedUser(_) >> {args ->
            FederatedUser userStateForUpdate = args[0]
            assert userStateForUpdate.phonePin == userToUpdate.phonePin
            assert userStateForUpdate.phonePinAuthenticationFailureCount == 0
        }
    }

    @Unroll
    def "updateUser: Self is not allowed to update phone pin if locked. type: #existingUser.class.name"(){
        given:
        UserForCreate userToUpdate = createUserForUpdateApi("2")
        existingUser.with {
            it.id = userToUpdate.id
            it.domainId = "sameDomain"
            it.phonePin = "654987"
            it.phonePinAuthenticationFailureCount = GlobalConstants.PHONE_PIN_AUTHENTICATION_FAILURE_LOCKING_THRESHOLD
            it
        }

        // Caller is "self"
        EndUser caller = existingUser

        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(userToUpdate.id) >> existingUser
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> false

        when:
        service.updateUser(headers, authToken, userToUpdate.id, userToUpdate)

        then:
        1 * exceptionHandler.exceptionResponse(_) >> {args ->
            Exception exception = args[0]
            IdmExceptionAssert.assertException(exception, ForbiddenException, "PP-004", "User's PIN is locked.")
        }
        0 * userService.updateUser(_)
        0 * identityUserService.updateFederatedUser(_)

        where:
        existingUser << [new User(), new FederatedUser()]
    }

    @Unroll
    def "updateUser: Phone pin can not be updated by other users. Admin: #admin"(){
        given:
        UserForCreate userToUpdate = createUserForUpdateApi("2")
        User existingUser = entityFactory.createUser().with {
            it.id = userToUpdate.id
            it.domainId = "sameDomain"
            it.phonePin = "654987"
            it.phonePinAuthenticationFailureCount = GlobalConstants.PHONE_PIN_AUTHENTICATION_FAILURE_LOCKING_THRESHOLD - 1
            it
        }

        EndUser caller = entityFactory.createUser().with {
            it.id = "4" // different than the user being updated
            it.domainId = existingUser.domainId
            it
        }

        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(userToUpdate.id) >> existingUser
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> admin

        when:
        service.updateUser(headers, authToken, userToUpdate.id, userToUpdate)

        then:
        1 * userConverter.fromUser(userToUpdate) >> { args ->
            UserForCreate fromUser = args[0]
            assert fromUser.phonePin == null

            // Create the new user and copy over details (like converter would do)
            new User().with {
                it.phonePin = fromUser.phonePin
                it
            }
        }

        1 * userService.updateUser(_) >> {args ->
            User userStateForUpdate = args[0]
            assert userStateForUpdate.phonePin == null // Phone pin is not updated
            assert userStateForUpdate.phonePinAuthenticationFailureCount == null // Failure count is not updated
        }

        where:
        admin << [true, false]
    }

    @Unroll
    def "updateUser: updating user's region calls correct services - feature.enable.use.domain.type.for.update.user = #flag"(){
        given:
        Domain domain = entityFactory.createDomain().with {
            it.type = GlobalConstants.DOMAIN_TYPE_RACKSPACE_CLOUD_US
            it
        }
        UserForCreate userToUpdate = createUserForUpdateApi("1").with {
            it.defaultRegion = "DFW"
            it
        }
        User existingUser = entityFactory.createUser().with {
            it.id = userToUpdate.id
            it.domainId = domain.domainId
            it.region = "ORD"
            it
        }

        EndUser caller = entityFactory.createUser()

        // Setup mocks
        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(userToUpdate.id) >> existingUser
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true

        when:
        service.updateUser(headers, authToken, userToUpdate.id, userToUpdate)

        then:
        1 * userConverter.fromUser(userToUpdate) >> new User().with {
            it.id = userToUpdate.id
            it.domainId = domain.domainId
            it.region = userToUpdate.defaultRegion
            it
        }
        1 * identityConfig.repositoryConfig.shouldUseDomainTypeForUpdateUser() >> flag
        1 * userService.updateUser(_) >> { args ->
            User userStateForUpdate = args[0]
            assert userStateForUpdate.region == "DFW"
        }

        if (flag) {
            1 * defaultRegionService.validateComputeRegionForUser(userToUpdate.defaultRegion, existingUser)
        } else {
            1 * defaultRegionService.validateDefaultRegion(userToUpdate.defaultRegion, existingUser)
        }

        where:
        flag << [true, false]
    }

    def "updateUser: error check - feature.enable.use.domain.type.for.update.user = true"(){
        given:
        Domain domain = entityFactory.createDomain().with {
            it.type = GlobalConstants.DOMAIN_TYPE_RACKSPACE_CLOUD_US
            it
        }
        UserForCreate userToUpdate = createUserForUpdateApi("1").with {
            it.defaultRegion = "DFW"
            it
        }
        User existingUser = entityFactory.createUser().with {
            it.id = userToUpdate.id
            it.domainId = domain.domainId
            it.region = "ORD"
            it
        }

        EndUser caller = entityFactory.createUser()

        // Setup mocks
        identityConfig.repositoryConfig.shouldUseDomainTypeForUpdateUser() >> true
        requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled() >> caller
        identityUserService.getEndUserById(userToUpdate.id) >> existingUser
        authorizationService.authorizeEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.IDENTITY_ADMIN, null) >> true

        when: "invalid region for user"
        service.updateUser(headers, authToken, userToUpdate.id, userToUpdate)

        then:
        1 * userConverter.fromUser(userToUpdate) >> new User().with {
            it.id = userToUpdate.id
            it.domainId = domain.domainId
            it.region = userToUpdate.defaultRegion
            it
        }
        1 * defaultRegionService.validateComputeRegionForUser(userToUpdate.defaultRegion, existingUser) >> {throw new BadRequestException()}
        1 * exceptionHandler.exceptionResponse(_)
    }

    EndUser createFedCaller(String id, String domainId) {
        entityFactory.createFederatedUser().with {
            it.id = id
            it.domainId = domainId
            it
        }
    }

    UserForCreate createUserForUpdateApi(String id) {
        new UserForCreate().with {
            it.id = id
            it.enabled = true
            it.phonePin = "123654"
            it.email = "someEmail@rackspace.com"
            it
        }
    }
}
