package com.rackspace.idm.validation

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.DuplicateException
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

import java.util.regex.Pattern

class IdentityProviderValidator20Test extends RootServiceTest {
        @Shared Validator20 service = new Validator20()

        Configuration config

        def setup() {
            mockConfiguration(service)
            mockDomainService(service)
            mockIdentityConfig(service)
            mockRequestContextHolder(service)
            mockFederatedIdentityService(service)
        }

    def "onUpdate: Throws error when supplied name > 255"() {
        def expectedErrorCode = "GEN-002"
        def expectedErrorMessagePattern = Pattern.compile("Error code: 'GEN-002'; name length cannot exceed 255 characters", Pattern.LITERAL)

        IdentityProvider provider = new IdentityProvider().with {
            it.name = RandomStringUtils.randomAlphanumeric(256)
            it
        }

        com.rackspace.idm.domain.entity.IdentityProvider existingProvider = new com.rackspace.idm.domain.entity.IdentityProvider().with {
            it.name = "hello"
            it
        }

        when:
        service.validateIdentityProviderForUpdateForIdentityProviderManager(provider, existingProvider)

        then:
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, BadRequestException, expectedErrorCode, expectedErrorMessagePattern)

        when:
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(provider, existingProvider)

        then:
        Exception ex2 = thrown()
        IdmExceptionAssert.assertException(ex2, BadRequestException, expectedErrorCode, expectedErrorMessagePattern)

        when:
        service.validateIdentityProviderForUpdateForRcnAdmin(provider, existingProvider)

        then:
        Exception ex3 = thrown()
        IdmExceptionAssert.assertException(ex3, BadRequestException, expectedErrorCode, expectedErrorMessagePattern)
    }

    @Unroll
    def "onUpdate: Throws error when supplied name contains invalid characters: invalid name: #name"() {
        def expectedErrorCode = "GEN-005"
        def expectedErrorMessagePattern = Pattern.compile("Error code: 'GEN-005'; Identity provider name must consist of only alphanumeric, '.', and '-' characters.", Pattern.LITERAL)

        IdentityProvider provider = new IdentityProvider().with {
            it.name = name
            it
        }

        com.rackspace.idm.domain.entity.IdentityProvider existingProvider = new com.rackspace.idm.domain.entity.IdentityProvider().with {
            it.name = "hello"
            it
        }

        when:
        service.validateIdentityProviderForUpdateForIdentityProviderManager(provider, existingProvider)

        then:
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, BadRequestException, expectedErrorCode, expectedErrorMessagePattern)

        when:
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(provider, existingProvider)

        then:
        Exception ex2 = thrown()
        IdmExceptionAssert.assertException(ex2, BadRequestException, expectedErrorCode, expectedErrorMessagePattern)

        when:
        service.validateIdentityProviderForUpdateForRcnAdmin(provider, existingProvider)

        then:
        Exception ex3 = thrown()
        IdmExceptionAssert.assertException(ex3, BadRequestException, expectedErrorCode, expectedErrorMessagePattern)

        where:
        name << ["sd%^&adf", "+hi", "12#", "русский"]
    }

    def "onUpdate: Throws error when supplied name is a duplicate"() {
        def newName = RandomStringUtils.randomAlphanumeric(10)
        def expectedErrorCode = "FED_IDP-005"
        def expectedErrorMessagePattern = Pattern.compile("Error code: 'FED_IDP-005'; Identity provider with name $newName already exist.", Pattern.LITERAL)

        IdentityProvider provider = new IdentityProvider().with {
            it.name = newName
            it
        }

        com.rackspace.idm.domain.entity.IdentityProvider existingProvider = new com.rackspace.idm.domain.entity.IdentityProvider().with {
            it.providerId = "existingProvider"
            it.name = "currentName"
            it
        }

        // This represents a different provider in backend with name the same as the requested name for the updated provider
        com.rackspace.idm.domain.entity.IdentityProvider differentProvider = new com.rackspace.idm.domain.entity.IdentityProvider().with {
            it.providerId = "differentProvider"
            it.name = newName
            it
        }

        federatedIdentityService.getIdentityProviderByName(provider.name) >> differentProvider

        when:
        service.validateIdentityProviderForUpdateForIdentityProviderManager(provider, existingProvider)

        then:
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, DuplicateException, expectedErrorCode, expectedErrorMessagePattern)

        when:
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(provider, existingProvider)

        then:
        Exception ex2 = thrown()
        IdmExceptionAssert.assertException(ex2, DuplicateException, expectedErrorCode, expectedErrorMessagePattern)

        when:
        service.validateIdentityProviderForUpdateForRcnAdmin(provider, existingProvider)

        then:
        Exception ex3 = thrown()
        IdmExceptionAssert.assertException(ex3, DuplicateException, expectedErrorCode, expectedErrorMessagePattern)
    }

    def "onUpdate: Throws error when supplied description > 255"() {
        def expectedErrorCode = "GEN-002"
        def expectedErrorMessagePattern = Pattern.compile("Error code: 'GEN-002'; description length cannot exceed 255 characters", Pattern.LITERAL)

        IdentityProvider provider = new IdentityProvider().with {
            it.name = "hello"
            it.description = RandomStringUtils.randomAlphanumeric(256)
            it
        }

        com.rackspace.idm.domain.entity.IdentityProvider existingProvider = new com.rackspace.idm.domain.entity.IdentityProvider().with {
            it.name = "hello"
            it.description = "currentDescription"
            it
        }

        when:
        service.validateIdentityProviderForUpdateForIdentityProviderManager(provider, existingProvider)

        then:
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, BadRequestException, expectedErrorCode, expectedErrorMessagePattern)

        when:
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(provider, existingProvider)

        then:
        Exception ex2 = thrown()
        IdmExceptionAssert.assertException(ex2, BadRequestException, expectedErrorCode, expectedErrorMessagePattern)

        when:
        service.validateIdentityProviderForUpdateForRcnAdmin(provider, existingProvider)

        then:
        Exception ex3 = thrown()
        IdmExceptionAssert.assertException(ex3, BadRequestException, expectedErrorCode, expectedErrorMessagePattern)
    }

    def "onUpdate: No error when name and description meet requirements with no duplicate"() {
        IdentityProvider updateProvider = new IdentityProvider().with {
            it.name = RandomStringUtils.randomAlphanumeric(250) + ".-_:c" // 255 character string with both specials
            it.description = RandomStringUtils.randomAlphanumeric(255)
            it
        }

        com.rackspace.idm.domain.entity.IdentityProvider existingProvider = new com.rackspace.idm.domain.entity.IdentityProvider().with {
            it.providerId = "existingProviderId"
            it.name = "currentName"
            it.description = "currentDescription"
            it
        }


        when:
        service.validateIdentityProviderForUpdateForIdentityProviderManager(updateProvider, existingProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByName(updateProvider.name) >> null
        notThrown()

        when:
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(updateProvider, existingProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByName(updateProvider.name) >> null
        notThrown()

        when:
        service.validateIdentityProviderForUpdateForRcnAdmin(updateProvider, existingProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByName(updateProvider.name) >> null
        notThrown()
    }

    @Unroll
    def "onUpdate: Update calls dup check even when provided name is same (case insensitive): currentName: #currentName; newName: #newName"() {
        IdentityProvider updateProvider = new IdentityProvider().with {
            it.name = newName
            it.description = RandomStringUtils.randomAlphanumeric(255)
            it
        }

        com.rackspace.idm.domain.entity.IdentityProvider existingProvider = new com.rackspace.idm.domain.entity.IdentityProvider().with {
            it.providerId = "existingProviderId"
            it.name = currentName
            it.description = "currentDescription"
            it
        }

        when:
        service.validateIdentityProviderForUpdateForIdentityProviderManager(updateProvider, existingProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByName(newName) >> existingProvider
        notThrown()

        when:
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(updateProvider, existingProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByName(newName) >> existingProvider
        notThrown()

        when:
        service.validateIdentityProviderForUpdateForRcnAdmin(updateProvider, existingProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByName(newName) >> existingProvider
        notThrown()

        where:
        currentName | newName
        "currentName" | "currentName"
        "cur12"       | "cUr12"
    }

}
