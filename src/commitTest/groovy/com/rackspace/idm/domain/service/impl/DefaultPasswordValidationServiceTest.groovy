package com.rackspace.idm.domain.service.impl

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordCheckResultTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordValidityTypeEnum
import com.rackspace.idm.Constants
import com.rackspace.idm.ErrorCodes
import com.rackspace.idm.domain.entity.ValidatePasswordResult
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

class DefaultPasswordValidationServiceTest extends RootServiceTest {


    @Shared
    DefaultPasswordValidationService service


    def setup() {
        service = new DefaultPasswordValidationService()
        mockIdentityConfig(service)
        mockPasswordBlacklistService(service)
        mockValidator(service)
    }

    def "Test happy path when all checks pass"() {

        given:
        service.validator.validatePasswordComposition(_) >> true
        service.passwordBlacklistService.checkPasswordInBlacklist(_) >> PasswordCheckResultTypeEnum.PASSED

        when: "password is validated"
        def checkResult = service.validatePassword(Constants.DEFAULT_PASSWORD)

        then: "over all result / valid property should be true"
        checkResult.valid == PasswordValidityTypeEnum.TRUE

        and: "value for all check should be passed"
        checkResult.blacklistCheck == PasswordCheckResultTypeEnum.PASSED
        checkResult.compositionCheck == PasswordCheckResultTypeEnum.PASSED
    }

    def "Test that password blacklist service should be skipped when composition check fails"() {

        given:
        service.validator.validatePasswordComposition(_) >> false

        when: "password is validated"
        def checkResult = service.validatePassword(Constants.DEFAULT_PASSWORD)

        then: "over all result / valid property should be false"
        checkResult.valid == PasswordValidityTypeEnum.FALSE

        and: "blacklist check should not get invoked"
        0 * service.passwordBlacklistService.checkPasswordInBlacklist(_)

        and: "value for blacklist check should be skipped"
        checkResult.blacklistCheck == PasswordCheckResultTypeEnum.SKIPPED
        checkResult.compositionCheck == PasswordCheckResultTypeEnum.FAILED

        and: "non passing array values should be both checks"
        checkResult.nonPassingChecks.contains(ValidatePasswordResult.CHECK_TYPES.COMPOSITION_CHECK.toString())
        checkResult.nonPassingChecks.contains(ValidatePasswordResult.CHECK_TYPES.BLACKLIST_CHECK.toString())

        and: "verify compositionCheckFailureMessage"
        String compositionCheckFailureMessage = ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_COMPOSITION_FAILED, ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_COMPOSITION_FAILED_MSG)
        checkResult.compositionCheckMessage.equalsIgnoreCase(compositionCheckFailureMessage)

        and: "verify blacklistCheckMessage"
        String blacklistCheckMessageMessage = ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_SKIPPED, ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_SKIPPED_MSG)
        checkResult.blacklistCheckMessage.equalsIgnoreCase(blacklistCheckMessageMessage)
    }


    @Unroll
    def "Test different combination of response based on check results"() {

        given:
        service.validator.validatePasswordComposition(_) >> compositionCheckInput
        service.passwordBlacklistService.checkPasswordInBlacklist(_) >> blackListCheckInput

        when: "password is validated"
        def checkResult = service.validatePassword(Constants.DEFAULT_PASSWORD)

        then: "over all result / valid property should be"
        checkResult.valid == validityExpectation

        and: "expected values for different checks"
        checkResult.blacklistCheck == blacklistCheckExpectation
        checkResult.compositionCheck == compositionCheckExpectation

        and:
        if (expectedCheckName != null) {
            checkResult.nonPassingChecks.contains(expectedCheckName.toString())
        }

        where: "different combination of checks are"
        expectedCheckName                                    | compositionCheckInput | blackListCheckInput                             | validityExpectation                    | blacklistCheckExpectation                       | compositionCheckExpectation
        ValidatePasswordResult.CHECK_TYPES.COMPOSITION_CHECK | false                 | PasswordCheckResultTypeEnum.SKIPPED             | PasswordValidityTypeEnum.FALSE         | PasswordCheckResultTypeEnum.SKIPPED             | PasswordCheckResultTypeEnum.FAILED
        ValidatePasswordResult.CHECK_TYPES.BLACKLIST_CHECK   | true                  | PasswordCheckResultTypeEnum.FAILED              | PasswordValidityTypeEnum.FALSE         | PasswordCheckResultTypeEnum.FAILED              | PasswordCheckResultTypeEnum.PASSED
        ValidatePasswordResult.CHECK_TYPES.BLACKLIST_CHECK   | true                  | PasswordCheckResultTypeEnum.DISABLED            | PasswordValidityTypeEnum.TRUE          | PasswordCheckResultTypeEnum.DISABLED            | PasswordCheckResultTypeEnum.PASSED
        ValidatePasswordResult.CHECK_TYPES.BLACKLIST_CHECK   | true                  | PasswordCheckResultTypeEnum.INDETERMINATE_RETRY | PasswordValidityTypeEnum.INDETERMINATE | PasswordCheckResultTypeEnum.INDETERMINATE_RETRY | PasswordCheckResultTypeEnum.PASSED
        ValidatePasswordResult.CHECK_TYPES.BLACKLIST_CHECK   | true                  | PasswordCheckResultTypeEnum.INDETERMINATE_ERROR | PasswordValidityTypeEnum.INDETERMINATE | PasswordCheckResultTypeEnum.INDETERMINATE_ERROR | PasswordCheckResultTypeEnum.PASSED
        null                                                 | true                  | PasswordCheckResultTypeEnum.PASSED              | PasswordValidityTypeEnum.TRUE          | PasswordCheckResultTypeEnum.PASSED              | PasswordCheckResultTypeEnum.PASSED

    }
}
