package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordCheckResultTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordValidityTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.ValidatePasswordResult
import groovy.json.JsonSlurper
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.openstack.docs.identity.api.v2.ItemNotFoundFault
import org.openstack.docs.identity.api.v2.UnauthorizedFault
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

class PasswordValidationServiceIntegrationTest extends RootIntegrationTest {


    def "Test feature flag works as expected"() {
        given: "feature flag is turned off"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_VALIDATION_SERVICES_PROP, false)

        when: "service is called"
        def response = cloud20.validatePassword(utils.getIdentityAdminToken(), v2Factory.createValidatePassword(Constants.EASY_PASSWORD))

        then: "503 is return"
        IdmAssert.assertOpenStackV2FaultResponse(response, ItemNotFoundFault, HttpStatus.SC_NOT_FOUND, Constants.ERROR_MSG_503_SERVICE_UNAVAILABE)
    }

    def "Verify all return HTTP status code for password validation service"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_VALIDATION_SERVICES_PROP, true)
        def userAdmin = utils.createCloudAccount()
        def userAdminToken = utils.getToken(userAdmin.username)

        def identityAdminToken = utils.getIdentityAdminToken()

        when: "token is invalid"
        def response = cloud20.validatePassword("inavlidtoken", v2Factory.createValidatePassword(Constants.EASY_PASSWORD))

        then: "401 is returned"
        IdmAssert.assertOpenStackV2FaultResponse(response, UnauthorizedFault, HttpStatus.SC_UNAUTHORIZED, Constants.ERROR_MSG_401_INVALID_TOKEN)

        when: "token of user less then admin is used"
        response = cloud20.validatePassword(userAdminToken, v2Factory.createValidatePassword(Constants.EASY_PASSWORD))

        then: "403 is returned"
        IdmAssert.assertOpenStackV2FaultResponse(response, ForbiddenFault, HttpStatus.SC_FORBIDDEN, Constants.ERROR_MSG_403_FORBIDDEN)

        when: "send invalid request object"
        response = cloud20.validatePassword(identityAdminToken, v2Factory.createPasswordReset(Constants.EASY_PASSWORD))

        then: "400 is returned"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, Constants.ERROR_MSG_400_INVALID_JSON)

        when: "Happy path when password is valid"
        response = cloud20.validatePassword(identityAdminToken, v2Factory.createValidatePassword(Constants.EASY_PASSWORD))

        then: "200 is returned"
        response.status == HttpStatus.SC_OK
    }

    def "Test Scenario when a strong password is validated "() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_VALIDATION_SERVICES_PROP, true)
        String adminToken = utils.getIdentityAdminToken()

        when: "blacklisted password is validated"
        def response = cloud20.validatePassword(adminToken, v2Factory.createValidatePassword(Constants.DEFAULT_PASSWORD))
        def validatePasswordResult = parseResponse(response)

        then:
        validatePasswordResult.valid == PasswordValidityTypeEnum.TRUE.toString()

        and: "Value of all checks must be passed"
        validatePasswordResult.compositionCheck == PasswordCheckResultTypeEnum.PASSED.toString()
        validatePasswordResult.blacklistCheck == PasswordCheckResultTypeEnum.PASSED.toString()

        and: "nonPassingChecks array must be empty"
        validatePasswordResult.nonPassingCheckNames.size() == 0

        and: "nonPassingChecks array must not contain"
        !validatePasswordResult.nonPassingCheckNames.contains(ValidatePasswordResult.CHECK_TYPES.COMPOSITION_CHECK.toString())
        !validatePasswordResult.nonPassingCheckNames.contains(ValidatePasswordResult.CHECK_TYPES.BLACKLIST_CHECK.toString())

        and: "Error message for checks should be "
        validatePasswordResult.compositionCheckMessage == null
        validatePasswordResult.blacklistCheckMessage == null
    }


    def "Test Scenario when a weak password is validated which fails to follow rackspace standards"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_VALIDATION_SERVICES_PROP, true)
        String adminToken = utils.getIdentityAdminToken()

        when: "password is invalid password - fails composition check :-("
        def response = cloud20.validatePassword(adminToken, v2Factory.createValidatePassword(Constants.EASY_PASSWORD))
        def validatePasswordResult = parseResponse(response)

        then:
        validatePasswordResult.valid == PasswordValidityTypeEnum.FALSE.toString()

        and: "Value of all check must not be passed"
        validatePasswordResult.compositionCheck == PasswordCheckResultTypeEnum.FAILED.toString()
        validatePasswordResult.blacklistCheck == PasswordCheckResultTypeEnum.SKIPPED.toString()

        and: "nonPassingChecks array must not be empty"
        validatePasswordResult.nonPassingCheckNames.size() == 2

        and: "nonPassingChecks array must contain"
        validatePasswordResult.nonPassingCheckNames.contains(ValidatePasswordResult.CHECK_TYPES.COMPOSITION_CHECK.toString())
        validatePasswordResult.nonPassingCheckNames.contains(ValidatePasswordResult.CHECK_TYPES.BLACKLIST_CHECK.toString())

        and: "Error message for checks should be "
        validatePasswordResult.compositionCheckMessage == ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_COMPOSITION_FAILED, ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_COMPOSITION_FAILED_MSG);
        validatePasswordResult.blacklistCheckMessage == ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_SKIPPED, ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_SKIPPED_MSG);

    }


    def "Test Scenario when a blacklisted password is validated "() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_VALIDATION_SERVICES_PROP, true)
        String adminToken = utils.getIdentityAdminToken()

        when: "blacklisted password is validated"
        def response = cloud20.validatePassword(adminToken, v2Factory.createValidatePassword(Constants.BLACKLISTED_PASSWORD))
        def validatePasswordResult = parseResponse(response)

        then:
        validatePasswordResult.valid == PasswordValidityTypeEnum.FALSE.toString()

        and: "Value of blacklist check must be FAILED"
        validatePasswordResult.compositionCheck == PasswordCheckResultTypeEnum.PASSED.toString()
        validatePasswordResult.blacklistCheck == PasswordCheckResultTypeEnum.FAILED.toString()

        and: "nonPassingChecks array must not be empty"
        validatePasswordResult.nonPassingCheckNames.size() == 1

        and: "nonPassingChecks array must only contain blacklistCheck"
        !validatePasswordResult.nonPassingCheckNames.contains(ValidatePasswordResult.CHECK_TYPES.COMPOSITION_CHECK.toString())
        validatePasswordResult.nonPassingCheckNames.contains(ValidatePasswordResult.CHECK_TYPES.BLACKLIST_CHECK.toString())

        and:
        validatePasswordResult.blacklistCheckMessage == ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_FAILED, ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_FAILED_MSG);
        validatePasswordResult.compositionCheckMessage == null
    }

    def "Test Scenario when a blacklisted feature is disabled and password is validated "() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_PASSWORD_VALIDATION_SERVICES_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLED_PASSWORD_BLACKLIST_PROP, false)
        String adminToken = utils.getIdentityAdminToken()

        when: "blacklisted password is validated"
        def response = cloud20.validatePassword(adminToken, v2Factory.createValidatePassword(Constants.BLACKLISTED_PASSWORD))
        def validatePasswordResult = parseResponse(response)

        then:
        validatePasswordResult.valid == PasswordValidityTypeEnum.TRUE.toString()

        and: "Value of blacklist check must be DISABLED"
        validatePasswordResult.compositionCheck == PasswordCheckResultTypeEnum.PASSED.toString()
        validatePasswordResult.blacklistCheck == PasswordCheckResultTypeEnum.DISABLED.toString()

        and: "nonPassingChecks array must not be empty"
        validatePasswordResult.nonPassingCheckNames.size() == 0

        and:
        validatePasswordResult.blacklistCheckMessage == ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_DISABLED, ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_DISABLED_MSG);
        validatePasswordResult.compositionCheckMessage == null
    }

    def parseResponse(response) {
        def body = response.getEntity(String.class)
        def slurper = new JsonSlurper().parseText(body)
        return slurper.'RAX-AUTH:validatePassword'
    }
}