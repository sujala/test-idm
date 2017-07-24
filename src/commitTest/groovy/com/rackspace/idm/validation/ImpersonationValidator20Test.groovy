package com.rackspace.idm.validation

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService
import com.rackspace.idm.exception.BadRequestException
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.V2Factory

class ImpersonationValidator20Test extends Specification {
    @Shared Validator20 validator20 = new Validator20();
    @Shared V2Factory v2Factory = new V2Factory();

    Configuration config;

    def setup() {
        config = Mock();
        config.getInt(DefaultScopeAccessService.TOKEN_IMPERSONATED_BY_RACKER_MAX_SECONDS_PROP_NAME) >> 5000
        config.getInt(DefaultScopeAccessService.TOKEN_IMPERSONATED_BY_SERVICE_MAX_SECONDS_PROP_NAME) >> 1000
        config.getInt(Validator20.TOKEN_CLOUD_AUTH_EXPIRATION_SECONDS_PROP_NAME) >> 10000
        validator20.config = config
    }

    def "user is required"() {
        def invalid = validImpersonationRequest().with {
            it.user = null
            return it
        }
        when:
        validator20.validateImpersonationRequestForService(invalid)

        then:
        BadRequestException e = thrown()
        e.getMessage() == Validator20.USER_NULL_IMPERSONATION_ERROR_MSG

        when:
        validator20.validateImpersonationRequestForRacker(invalid)

        then:
        BadRequestException e2 = thrown()
        e2.getMessage() == Validator20.USER_NULL_IMPERSONATION_ERROR_MSG
    }

    def "username must not be not null"() {
        def invalid = validImpersonationRequest().with {
            it.user.username = null
            return it
        }
        when:
        validator20.validateImpersonationRequestForService(invalid)

        then:
        BadRequestException e = thrown()
        e.getMessage() == Validator20.USERNAME_NULL_IMPERSONATION_ERROR_MSG

        when:
        validator20.validateImpersonationRequestForRacker(invalid)

        then:
        BadRequestException e2 = thrown()
        e2.getMessage() == Validator20.USERNAME_NULL_IMPERSONATION_ERROR_MSG
    }

    def "username must not be empty"() {
        def invalid = validImpersonationRequest().with {
            it.user.username = ""
            return it
        }
        when:
        validator20.validateImpersonationRequestForService(invalid)

        then:
        BadRequestException e = thrown()
        e.getMessage() == Validator20.USERNAME_EMPTY_IMPERSONATION_ERROR_MSG

        when:
        validator20.validateImpersonationRequestForRacker(invalid)

        then:
        BadRequestException e2 = thrown()
        e2.getMessage() == Validator20.USERNAME_EMPTY_IMPERSONATION_ERROR_MSG
    }


    def "username must not be blank"() {
        def invalid = validImpersonationRequest().with {
            it.user.username = " "
            return it
        }

        when:
        validator20.validateImpersonationRequestForService(invalid)

        then:
        BadRequestException e = thrown()
        e.getMessage() == Validator20.USERNAME_EMPTY_IMPERSONATION_ERROR_MSG

        when:
        validator20.validateImpersonationRequestForRacker(invalid)

        then:
        BadRequestException e2 = thrown()
        e2.getMessage() == Validator20.USERNAME_EMPTY_IMPERSONATION_ERROR_MSG
    }

    def "expire time must be >=1"() {
        def invalid = validImpersonationRequest().with {
            it.expireInSeconds = 0
            return it
        }

        when:
        validator20.validateImpersonationRequestForService(invalid)

        then:
        BadRequestException e = thrown()
        e.getMessage() == Validator20.EXPIRE_IN_ELEMENT_LESS_ONE_IMPERSONATION_ERROR_MSG

        when:
        validator20.validateImpersonationRequestForRacker(invalid)

        then:
        BadRequestException e2 = thrown()
        e2.getMessage() == Validator20.EXPIRE_IN_ELEMENT_LESS_ONE_IMPERSONATION_ERROR_MSG
    }

    def "expire time must be < max service/racker time"() {
        def invalid = validImpersonationRequest().with {
            it.expireInSeconds = 10000
            return it
        }

        when:
        validator20.validateImpersonationRequestForService(invalid)

        then:
        BadRequestException e = thrown()
        e.getMessage() == String.format(Validator20.EXPIRE_IN_ELEMENT_EXCEEDS_MAX_IMPERSONATION_ERROR_MSG, config.getInt(DefaultScopeAccessService.TOKEN_IMPERSONATED_BY_SERVICE_MAX_SECONDS_PROP_NAME))

        when:
        validator20.validateImpersonationRequestForRacker(invalid)

        then:
        BadRequestException e2 = thrown()
        e2.getMessage() == String.format(Validator20.EXPIRE_IN_ELEMENT_EXCEEDS_MAX_IMPERSONATION_ERROR_MSG, config.getInt(DefaultScopeAccessService.TOKEN_IMPERSONATED_BY_RACKER_MAX_SECONDS_PROP_NAME))
    }

    def ImpersonationRequest validImpersonationRequest() {
        def request = new ImpersonationRequest().with {
            it.user = v2Factory.createUser("userId", "username")
            it.expireInSeconds = 100   //make this below both mocked maximums
            return it
        }
    }


}
