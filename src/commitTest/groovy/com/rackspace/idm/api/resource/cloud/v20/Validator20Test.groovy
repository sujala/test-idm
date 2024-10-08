package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.EmailDomains
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdpPolicyFormatEnum
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.DuplicateException
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.helpers.CloudTestUtils
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.validation.JsonValidator
import com.rackspace.idm.validation.Validator20
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.IdmExceptionAssert
import testHelpers.RootServiceTest

class Validator20Test extends RootServiceTest {

    @Shared Validator20 service

    @Shared JsonValidator jsonValidator

    @Shared CloudTestUtils testUtils

    def setupSpec() {
        service = new Validator20()
    }

    def setup() {
        jsonValidator = Mock()
        service.jsonValidator = jsonValidator
        mockIdentityConfig(service)
        mockFederatedIdentityService(service)
        mockPasswordBlacklistService(service)
        testUtils = new CloudTestUtils()
    }

    @Unroll
    def "Validate default policy file: type = #type" () {
        when:
        service.validateIdpPolicy(policy, IdpPolicyFormatEnum.valueOf(type))

        then:
        1 * identityConfig.getReloadableConfig().getIdpPolicyMaxSize() >> 2
        jsonValidatorCalls * jsonValidator.isValidJson(policy) >> true

        where:
        policy                         | type   | jsonValidatorCalls
        '{"policy": {"name":"name"}"}' | "JSON" | 1
        "--- policy: name: name"       | "YAML" | 0
    }

    def "validateIdentityProviderForCreation: validate email domains"() {
        given:
        IdentityProvider identityProvider = new IdentityProvider().with {
            it.name = "name"
            it.enabled = true
            it.issuer = "issuer"
            it.authenticationUrl = "authenticationUrl"
            it.approvedDomainGroup = ApprovedDomainGroupEnum.GLOBAL
            it.federationType = IdentityProviderFederationTypeEnum.DOMAIN
            it.emailDomains = new EmailDomains().with {
                it.emailDomain = ["emailDomain"].asList()
                it
            }
            it
        }
        federatedIdentityService.getIdentityProviderByName(identityProvider.name)
        federatedIdentityService.getIdentityProviderByIssuer(identityProvider.getIssuer()) >> null

        when: "Only one email domain"
        service.validateIdentityProviderForCreation(identityProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(identityProvider.emailDomains.emailDomain.get(0))

        when: "Two email domains"
        identityProvider.emailDomains.emailDomain.add("emailDomain2")
        service.validateIdentityProviderForCreation(identityProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByEmailDomain("emailDomain")
        1 * federatedIdentityService.getIdentityProviderByEmailDomain("emailDomain2")

        when: "Duplicate email domains"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add("emailDomain2")
        identityProvider.emailDomains.emailDomain.add("emailDomain2")
        identityProvider.emailDomains.emailDomain.add("emailDomain2".toUpperCase())
        service.validateIdentityProviderForCreation(identityProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByEmailDomain("emailDomain2")
    }

    def "validateIdentityProviderForCreation: emailDomains error check"() {
        given:
        IdentityProvider identityProvider = new IdentityProvider().with {
            it.name = "name"
            it.enabled = true
            it.issuer = "issuer"
            it.authenticationUrl = "authenticationUrl"
            it.approvedDomainGroup = ApprovedDomainGroupEnum.GLOBAL
            it.federationType = IdentityProviderFederationTypeEnum.DOMAIN
            it.emailDomains = new EmailDomains().with {
                it.emailDomain = [].asList()
                it
            }
            it
        }
        federatedIdentityService.getIdentityProviderByName(identityProvider.name)
        federatedIdentityService.getIdentityProviderByIssuer(identityProvider.getIssuer()) >> null

        when: "Max size exceeded"
        identityProvider.emailDomains.emailDomain.add(testUtils.getRandomUUIDOfLength("emailDomain", 256))
        service.validateIdentityProviderForCreation(identityProvider)

        then:
        thrown(BadRequestException)

        when: "Empty emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add("")
        service.validateIdentityProviderForCreation(identityProvider)

        then:
        thrown(BadRequestException)

        when: "Blank emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add("      ")
        service.validateIdentityProviderForCreation(identityProvider)

        then:
        thrown(BadRequestException)

        when: "EmailDomain containing whitespace characters"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add("bad      ")
        service.validateIdentityProviderForCreation(identityProvider)

        then:
        thrown(BadRequestException)

        when: "Empty emailDomains"
        identityProvider.emailDomains.emailDomain.clear()
        service.validateIdentityProviderForCreation(identityProvider)

        then:
        thrown(BadRequestException)

        when: "emailDomains only contain null values"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add(null)
        service.validateIdentityProviderForCreation(identityProvider)

        then:
        thrown(BadRequestException)

        when: "Duplicate emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add("emailDomain")
        service.validateIdentityProviderForCreation(identityProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(_) >> new com.rackspace.idm.domain.entity.IdentityProvider()
        thrown(DuplicateException)
    }

    def "ValidateIdentityProviderForUpdate: validate email domains"() {
        given:
        IdentityProvider identityProvider = new IdentityProvider().with {
            it.name = "name"
            it.authenticationUrl = "authenticationUrl"
            it.emailDomains = new EmailDomains().with {
                it.emailDomain = [].asList()
                it
            }
            it
        }
        com.rackspace.idm.domain.entity.IdentityProvider existingProvider = new com.rackspace.idm.domain.entity.IdentityProvider().with {
            it.name = "name"
            it.enabled = true
            it.authenticationUrl = "authenticationUrl"
            it.approvedDomainGroup = ApprovedDomainGroupEnum.GLOBAL
            it.federationType = IdentityProviderFederationTypeEnum.DOMAIN
            it.emailDomains = [].asList()
            it
        }
        def emailDomain = "emailDomain"
        def emailDomain2 = "emailDomain2"

        when: "Manager: one email domain"
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        service.validateIdentityProviderForUpdateForIdentityProviderManager(identityProvider, existingProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(emailDomain) >> null

        when: "Manager: existing IDP with same emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        existingProvider.emailDomains.add(emailDomain)
        service.validateIdentityProviderForUpdateForIdentityProviderManager(identityProvider, existingProvider)

        then:
        0 * federatedIdentityService.getIdentityProviderByEmailDomain(_)

        when: "Manager: replace IDP's emailDomains"
        identityProvider.emailDomains.emailDomain.clear()
        existingProvider.emailDomains.clear()
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(emailDomain2)
        existingProvider.emailDomains.add(emailDomain)
        service.validateIdentityProviderForUpdateForIdentityProviderManager(identityProvider, existingProvider)

        then: "Assert newly added emailDomain does not belong to another IDP"
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(emailDomain2) >> null

        when: "Manager: duplicate emailDomains (case insensitive)"
        identityProvider.emailDomains.emailDomain.clear()
        existingProvider.emailDomains.clear()
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(emailDomain2)
        identityProvider.emailDomains.emailDomain.add(emailDomain2)
        identityProvider.emailDomains.emailDomain.add(emailDomain2.toUpperCase())
        existingProvider.emailDomains.add(emailDomain.toUpperCase())
        service.validateIdentityProviderForUpdateForIdentityProviderManager(identityProvider, existingProvider)

        then: "Assert duplicates are ignored"
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(emailDomain2) >> null

        when: "Manager: ignore null values for emailDomains"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(null)
        existingProvider.emailDomains.add(emailDomain)
        service.validateIdentityProviderForUpdateForIdentityProviderManager(identityProvider, existingProvider)

        then:
        0 * federatedIdentityService.getIdentityProviderByEmailDomain(_)

        when: "RcnAdmin: one email domain"
        identityProvider.emailDomains.emailDomain.clear()
        existingProvider.emailDomains.clear()
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        service.validateIdentityProviderForUpdateForRcnAdmin(identityProvider, existingProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(emailDomain) >> null

        when: "RcnAdmin: existing IDP with same emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        existingProvider.emailDomains.add(emailDomain)
        service.validateIdentityProviderForUpdateForRcnAdmin(identityProvider, existingProvider)

        then:
        0 * federatedIdentityService.getIdentityProviderByEmailDomain(_)

        when: "RcnAdmin: replace IDP's emailDomains"
        identityProvider.emailDomains.emailDomain.clear()
        existingProvider.emailDomains.clear()
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(emailDomain2)
        existingProvider.emailDomains.add(emailDomain)
        service.validateIdentityProviderForUpdateForRcnAdmin(identityProvider, existingProvider)

        then: "Assert newly added emailDomain does not belong to another IDP"
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(emailDomain2) >> null

        when: "RcnAdmin: duplicate emailDomains (case insensitive)"
        identityProvider.emailDomains.emailDomain.clear()
        existingProvider.emailDomains.clear()
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(emailDomain2)
        identityProvider.emailDomains.emailDomain.add(emailDomain2)
        identityProvider.emailDomains.emailDomain.add(emailDomain2.toUpperCase())
        existingProvider.emailDomains.add(emailDomain.toUpperCase())
        service.validateIdentityProviderForUpdateForRcnAdmin(identityProvider, existingProvider)

        then: "Assert duplicates are ignored"
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(emailDomain2) >> null

        when: "RcnAdmin: ignore null values for emailDomains"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(null)
        existingProvider.emailDomains.add(emailDomain)
        service.validateIdentityProviderForUpdateForRcnAdmin(identityProvider, existingProvider)

        then:
        0 * federatedIdentityService.getIdentityProviderByEmailDomain(_)

        when: "UserAdminOrUserManage: one email domain"
        identityProvider.emailDomains.emailDomain.clear()
        existingProvider.emailDomains.clear()
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(identityProvider, existingProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(emailDomain) >> null

        when: "UserAdminOrUserManage: existing IDP with same emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        existingProvider.emailDomains.add(emailDomain)
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(identityProvider, existingProvider)

        then:
        0 * federatedIdentityService.getIdentityProviderByEmailDomain(_)

        when: "UserAdminOrUserManage: replace IDP's emailDomains"
        identityProvider.emailDomains.emailDomain.clear()
        existingProvider.emailDomains.clear()
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(emailDomain2)
        existingProvider.emailDomains.add(emailDomain)
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(identityProvider, existingProvider)

        then: "Assert newly added emailDomain does not belong to another IDP"
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(emailDomain2) >> null

        when: "UserAdminOrUserManage: duplicate emailDomains (case insensitive)"
        identityProvider.emailDomains.emailDomain.clear()
        existingProvider.emailDomains.clear()
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(emailDomain2)
        identityProvider.emailDomains.emailDomain.add(emailDomain2)
        identityProvider.emailDomains.emailDomain.add(emailDomain2.toUpperCase())
        existingProvider.emailDomains.add(emailDomain.toUpperCase())
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(identityProvider, existingProvider)

        then: "Assert duplicates are ignored"
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(emailDomain2) >> null

        when: "UserAdminOrUserManager: ignore null values for emailDomains"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(null)
        existingProvider.emailDomains.add(emailDomain)
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(identityProvider, existingProvider)

        then:
        0 * federatedIdentityService.getIdentityProviderByEmailDomain(_)
    }

    def "ValidateIdentityProviderForUpdate: emailDomains error check"() {
        given:
        IdentityProvider identityProvider = new IdentityProvider().with {
            it.name = "name"
            it.authenticationUrl = "authenticationUrl"
            it.emailDomains = new EmailDomains().with {
                it.emailDomain = [].asList()
                it
            }
            it
        }
        com.rackspace.idm.domain.entity.IdentityProvider existingProvider = new com.rackspace.idm.domain.entity.IdentityProvider().with {
            it.name = "name"
            it.enabled = true
            it.authenticationUrl = "authenticationUrl"
            it.approvedDomainGroup = ApprovedDomainGroupEnum.GLOBAL
            it.federationType = IdentityProviderFederationTypeEnum.DOMAIN
            it.emailDomains = [].asList()
            it
        }
        def emailDomain = "emailDomain"
        def emailDomain2 = "emailDomain2"

        when: "Manager: exceeded max size"
        identityProvider.emailDomains.emailDomain.add(testUtils.getRandomUUIDOfLength(emailDomain, 256))
        service.validateIdentityProviderForUpdateForIdentityProviderManager(identityProvider, existingProvider)

        then:
        thrown(BadRequestException)

        when: "Manager: Empty emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add("")
        service.validateIdentityProviderForUpdateForIdentityProviderManager(identityProvider, existingProvider)

        then:
        thrown(BadRequestException)

        when: "Manager: Blank emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add("      ")
        service.validateIdentityProviderForUpdateForIdentityProviderManager(identityProvider, existingProvider)

        then:
        thrown(BadRequestException)

        when: "Manager: EmailDomain containing whitespace characters"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add("bad      ")
        service.validateIdentityProviderForUpdateForIdentityProviderManager(identityProvider, existingProvider)

        then:
        thrown(BadRequestException)

        when: "Manager: duplicate emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        existingProvider.emailDomains.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(emailDomain2)
        service.validateIdentityProviderForUpdateForIdentityProviderManager(identityProvider, existingProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(emailDomain2) >> new com.rackspace.idm.domain.entity.IdentityProvider()
        thrown(DuplicateException)

        when: "RcnAdmin: exceeded max size"
        identityProvider.emailDomains.emailDomain.clear()
        existingProvider.emailDomains.clear()
        identityProvider.emailDomains.emailDomain.add(testUtils.getRandomUUIDOfLength(emailDomain, 256))
        service.validateIdentityProviderForUpdateForRcnAdmin(identityProvider, existingProvider)

        then:
        thrown(BadRequestException)

        when: "RcnAdmin: Empty emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add("")
        service.validateIdentityProviderForUpdateForRcnAdmin(identityProvider, existingProvider)

        then:
        thrown(BadRequestException)

        when: "RcnAdmin: Blank emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add("      ")
        service.validateIdentityProviderForUpdateForRcnAdmin(identityProvider, existingProvider)

        then:
        thrown(BadRequestException)

        when: "RcnAdmin: EmailDomain containing whitespace characters"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add("bad      ")
        service.validateIdentityProviderForUpdateForRcnAdmin(identityProvider, existingProvider)

        then:
        thrown(BadRequestException)

        when: "RcnAdmin: duplicate emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        existingProvider.emailDomains.clear()
        existingProvider.emailDomains.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(emailDomain2)
        service.validateIdentityProviderForUpdateForRcnAdmin(identityProvider, existingProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(emailDomain2) >> new com.rackspace.idm.domain.entity.IdentityProvider()
        thrown(DuplicateException)

        when: "UserAdminOrUserManage: exceeded max size"
        identityProvider.emailDomains.emailDomain.clear()
        existingProvider.emailDomains.clear()
        identityProvider.emailDomains.emailDomain.add(testUtils.getRandomUUIDOfLength(emailDomain, 256))
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(identityProvider, existingProvider)

        then:
        thrown(BadRequestException)

        when: "UserAdminOrUserManage: Empty emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add("")
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(identityProvider, existingProvider)

        then:
        thrown(BadRequestException)

        when: "UserAdminOrUserManage: Blank emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add("      ")
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(identityProvider, existingProvider)

        then:
        thrown(BadRequestException)

        when: "UserAdminOrUserManage: EmailDomain containing whitespace characters"
        identityProvider.emailDomains.emailDomain.clear()
        identityProvider.emailDomains.emailDomain.add("bad      ")
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(identityProvider, existingProvider)

        then:
        thrown(BadRequestException)

        when: "UserAdminOrUserManage: duplicate emailDomain"
        identityProvider.emailDomains.emailDomain.clear()
        existingProvider.emailDomains.clear()
        existingProvider.emailDomains.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(emailDomain)
        identityProvider.emailDomains.emailDomain.add(emailDomain2)
        service.validateIdentityProviderForUpdateForUserAdminOrUserManage(identityProvider, existingProvider)

        then:
        1 * federatedIdentityService.getIdentityProviderByEmailDomain(emailDomain2) >> new com.rackspace.idm.domain.entity.IdentityProvider()
        thrown(DuplicateException)
    }

    def "validateIdentityProviderIssuerWithDupCheck - if IDP already exists with issuer, throws Dup exception"() {
        given:
        IdentityProvider webProvider = new IdentityProvider().with {
            it.issuer = "duplicate"
            it
        }
        when:
        service.validateIdentityProviderIssuerWithDupCheck(webProvider)

        then: "calls to check dup"
        1 * federatedIdentityService.getIdentityProviderByIssuer(webProvider.issuer) >> new com.rackspace.idm.domain.entity.IdentityProvider()

        and: "throws dup exception when IDP already exists with that issuer"
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, DuplicateException, ErrorCodes.ERROR_CODE_IDP_ISSUER_ALREADY_EXISTS, ErrorCodes.ERROR_CODE_IDP_ISSUER_ALREADY_EXISTS_MSG)
    }

    def "validateIdentityProviderIssuerWithDupCheck - if IDP does not exists with issuer, does not throw Dup exception"() {
        given:
        IdentityProvider webProvider = new IdentityProvider().with {
            it.issuer = "duplicate"
            it
        }
        when:
        service.validateIdentityProviderIssuerWithDupCheck(webProvider)

        then: "calls to check dup"
        1 * federatedIdentityService.getIdentityProviderByIssuer(webProvider.issuer) >> null

        and: "no error is thrown if dup check doesn't return an IDP"
        notThrown()
    }

    def "containsIgnoreCase - assert unique values"() {
        given:
        List<String> list = new ArrayList<>()
        list.add("foo")

        when:
        def result = service.containsIgnoreCase(list, "foo".toUpperCase())

        then:
        result

        when:
        result = service.containsIgnoreCase(list, "other")

        then:
        !result
    }

    @Unroll
    def "validateStringNonWhitespace - assert non whitespace character: value = #value"() {
        when:
        service.validateStringNonWhitespace("property", value)

        then:
        noExceptionThrown()

        where:
        value << ["good", "sp3ci@l"]
    }

    @Unroll
    def "validateStringNonWhitespace - assert BadRequestException: value = #value"() {
        when:
        service.validateStringNonWhitespace("property", value)

        then:
        thrown(BadRequestException)

        where:
        value << ["   bad", "bad   ", "bad bad", "bad\n", "\nbad"]
    }

    @Unroll
    def "validateStringIsNotBlank - assert not blank: value = #value"() {
        when:
        service.validateStringNotBlank("property", value)

        then:
        noExceptionThrown()

        where:
        value << ["good", "sp3ci@l"]
    }

    @Unroll
    def "validateStringIsNotBlank - assert BadRequestException: value = #value"() {
        when:
        service.validateStringNotBlank("property", value)

        then:
        thrown(BadRequestException)

        where:
        value << ["  ", "", null, "\n"]
    }

    def "validateItsNotUnverifiedUser - restrict unverified user"(){
        given:
        User unverifiedUser = entityFactory.createUnverifiedUser()
        User verifiedUser = entityFactory.createUser()

        when: "unverified users is passed as argument"
        Validator20.validateItsNotUnverifiedUser(unverifiedUser)

        then: "exception is thrown"
        thrown(ForbiddenException)

        when: "verified users is passed as argument"
        Validator20.validateItsNotUnverifiedUser(verifiedUser)

        then: "exception is not thrown"
        noExceptionThrown()
    }

    def "validatePasswordIsNotBlacklisted - when password is not blacklisted"() {
        given:
        identityConfig.getReloadableConfig().getDynamoDBPasswordBlacklistCountMaxAllowed() >> 12

        when:
        passwordBlacklistService.isPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD) >> false
        service.validatePasswordIsNotBlacklisted(Constants.BLACKLISTED_PASSWORD)

        then:
        noExceptionThrown()
    }

    def "validatePasswordIsNotBlacklisted - when password is blacklisted"() {
        given:
        identityConfig.getReloadableConfig().getDynamoDBPasswordBlacklistCountMaxAllowed() >> 12

        when:
        passwordBlacklistService.isPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD) >> true
        service.validatePasswordIsNotBlacklisted(Constants.BLACKLISTED_PASSWORD)

        then:
        BadRequestException ex = thrown()
        IdmExceptionAssert.assertException(ex, BadRequestException, ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD, ErrorCodes.ERROR_CODE_BLACKLISTED_PASSWORD_MSG)
    }

    def "test validatePasswordCredentialsForCreateOrUpdate method for password blacklist feature flag #passwordFeatureFlag"() {
        given:
        PasswordCredentialsBase passwordCredentialsBase = new PasswordCredentialsBase().with {
            it.username = "testUser"
            it.password = Constants.BLACKLISTED_PASSWORD
            it
        }

        when: "password blacklist feature if turn off"

        service.validatePasswordCredentialsForCreateOrUpdate(passwordCredentialsBase)

        then: "password blacklist service is not invoked"
        1 * identityConfig.getReloadableConfig().isPasswordBlacklistValidationEnabled() >> passwordFeatureFlag

        if (passwordFeatureFlag) {
            1 * passwordBlacklistService.isPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)
        } else {
            0 * passwordBlacklistService.isPasswordInBlacklist(Constants.BLACKLISTED_PASSWORD)
        }

        where:
        passwordFeatureFlag << [true, false]
    }

    def "validateDomainType - validate correct domain type"() {
        given:
        def domain = v1Factory.createDomain().with {
            it.type = "CLOUD_US"
            it
        }
        identityConfig.repositoryConfig.getDomainTypes() >> ["CLOUD_US"]

        when: "valid domain type"
        service.validateDomainType(domain)

        then:
        notThrown()

        when: "invalid domain type"
        domain.type = "BAD"
        service.validateDomainType(domain)

        then:
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, BadRequestException, ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST, "Invalid value for domain type. Acceptable values are: [CLOUD_US]")
    }

    @Unroll
    def "Test to check phone pin validation error on pin: #testPin"(){

        when:
        service.validatePhonePin(testPin)

        then: "Validation error is thrown"
        Exception ex = thrown()
        IdmExceptionAssert.assertException(ex, BadRequestException, ErrorCodes.ERROR_CODE_PHONE_PIN_BAD_REQUEST, ErrorCodes.ERROR_MESSAGE_PHONE_PIN_BAD_REQUEST)

        where:
        testPin << [
                "aseemtest",        // invalid characters
                "abc#??sng^&*(st",  // invalid characters
                "78612",            // less than 6 digits
                "7861249",          // more than 6 digits
                "666666",           // Same digit throughout
                "666697",           // Repeats > 3 at beginning of PIN
                "123456",           // Ascending sequence > 3 for whole PIN
                "123498",           // Ascending sequence > 3 at beginning of PIN
                "311234",           // Ascending sequence > 3 at end of PIN
                "654321",           // Descending sequence > 3 for whole PIN
                "654378",           // Descending sequence > 3 at beginning of PIN
                "319876",           // Descending sequence > 3 at end of PIN
                "-451934",          // Negative number where numeric part is 6 characters
                "-45193",           // Negative number where string is still 6 characters total
                "+451934",          // Positive number where numeric part is 6 characters
                "+45193",           // Positive number where string is still 6 characters total
                "453.12",           // Decimal where string is still 6 characters total
                "453.122",          // Decimal where string is still 6 characters total
        ]
    }

    def "Test happy path for phone pin validation for pin: #testPin"(){

        when:
        service.validatePhonePin(testPin)

        then: "It is ok and no exception is thrown"
        noExceptionThrown()

        where:
        testPin << [
                "333197", // 3 same numbers in row at beginning
                "123678", // 3 sequential numbers in row at beginning and end
                "987321", // 3 descending numbers in row both at beginning and end
                "456333", // 3 ascending AND 3 repeating
                "987333", // 3 descending and 3 repeating
                "121212"  // non sequential numbers having difference of 1
        ]
    }
}
