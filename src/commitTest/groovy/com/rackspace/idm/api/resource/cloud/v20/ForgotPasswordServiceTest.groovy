package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordReset
import com.rackspace.idm.domain.entity.TokenScopeEnum
import com.rackspace.idm.domain.entity.UserScopeAccess
import org.apache.http.HttpStatus
import org.opensaml.core.config.InitializationService
import spock.lang.Shared
import testHelpers.RootServiceTest

class ForgotPasswordServiceTest extends RootServiceTest {

    @Shared
    DefaultCloud20Service service

    def setup(){
        InitializationService.initialize()

        service = new DefaultCloud20Service()

        // Setup mocks
        mockValidator(service)
        mockUserService(service)
        mockExceptionHandler(service)
        mockRequestContextHolder(service)
        mockAuthorizationService(service)
    }

    def "passwordReset - can successfully reset password"() {
        given:
        def passwordReset = new PasswordReset().with {
            it.password = "password"
            it
        }
        def userScopeAccess = new UserScopeAccess().with {
            it.scope = TokenScopeEnum.PWD_RESET.scope
            it.userRsId = "userId"
            it
        }

        when:
        def response = service.passwordReset(headers, authToken, passwordReset)

        then:
        response.build().status == HttpStatus.SC_NO_CONTENT

        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * securityContext.getEffectiveCallerToken() >> userScopeAccess
        1 * userService.checkAndGetUserById(userScopeAccess.userRsId) >> entityFactory.createUser()
        1 * userService.updateUser(_)
    }
}
