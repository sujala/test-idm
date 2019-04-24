package com.rackspace.idm.domain.entity

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordCheckResultTypeEnum
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

class ValidatePasswordResultTest extends RootServiceTest {
    @Shared
    ValidatePasswordResult validatePasswordResult

    def setupSpec() {
        validatePasswordResult = new ValidatePasswordResult()
    }

    @Unroll
    def "verify nonPassingChecksValues has zero elements when check result are in PASSED or DISABLED for input: #compositionCheckInput and #blacklistCheckInput "() {
        given:
        validatePasswordResult.with {
            it.compositionCheck = compositionCheckInput
            it.blacklistCheck = blacklistCheckInput
            it
        }

        when:
        List<String> nonPassingCheckArray = validatePasswordResult.getNonPassingChecks()

        then:
        nonPassingCheckArray.size() == 0

        where:
        [compositionCheckInput, blacklistCheckInput] << [[PasswordCheckResultTypeEnum.PASSED, PasswordCheckResultTypeEnum.DISABLED], [PasswordCheckResultTypeEnum.DISABLED, PasswordCheckResultTypeEnum.PASSED]].combinations()
    }

    @Unroll
    def "verify nonPassingChecksValues has elements when checks are not in PASSED or DISABLED for values: #compositionCheckInput and #blacklistCheckInput"() {
        given:
        validatePasswordResult.with {
            it.compositionCheck = compositionCheckInput
            it.blacklistCheck = blacklistCheckInput
            it
        }

        when:
        List<String> nonPassingCheckArray = validatePasswordResult.getNonPassingChecks()

        then:
        nonPassingCheckArray.size() == expectedSize

        where:
        compositionCheckInput                | blacklistCheckInput                             | expectedSize
        PasswordCheckResultTypeEnum.FAILED   | PasswordCheckResultTypeEnum.FAILED              | 2
        PasswordCheckResultTypeEnum.PASSED   | PasswordCheckResultTypeEnum.FAILED              | 1
        PasswordCheckResultTypeEnum.DISABLED | PasswordCheckResultTypeEnum.FAILED              | 1
        PasswordCheckResultTypeEnum.SKIPPED  | PasswordCheckResultTypeEnum.FAILED              | 2

        PasswordCheckResultTypeEnum.FAILED   | PasswordCheckResultTypeEnum.PASSED              | 1
        PasswordCheckResultTypeEnum.PASSED   | PasswordCheckResultTypeEnum.PASSED              | 0
        PasswordCheckResultTypeEnum.DISABLED | PasswordCheckResultTypeEnum.PASSED              | 0
        PasswordCheckResultTypeEnum.SKIPPED  | PasswordCheckResultTypeEnum.PASSED              | 1

        PasswordCheckResultTypeEnum.FAILED   | PasswordCheckResultTypeEnum.DISABLED            | 1
        PasswordCheckResultTypeEnum.PASSED   | PasswordCheckResultTypeEnum.DISABLED            | 0
        PasswordCheckResultTypeEnum.DISABLED | PasswordCheckResultTypeEnum.DISABLED            | 0
        PasswordCheckResultTypeEnum.SKIPPED  | PasswordCheckResultTypeEnum.DISABLED            | 1

        PasswordCheckResultTypeEnum.FAILED   | PasswordCheckResultTypeEnum.SKIPPED             | 2
        PasswordCheckResultTypeEnum.PASSED   | PasswordCheckResultTypeEnum.SKIPPED             | 1
        PasswordCheckResultTypeEnum.DISABLED | PasswordCheckResultTypeEnum.SKIPPED             | 1
        PasswordCheckResultTypeEnum.SKIPPED  | PasswordCheckResultTypeEnum.SKIPPED             | 2

        PasswordCheckResultTypeEnum.FAILED   | PasswordCheckResultTypeEnum.INDETERMINATE_ERROR | 2
        PasswordCheckResultTypeEnum.PASSED   | PasswordCheckResultTypeEnum.INDETERMINATE_ERROR | 1
        PasswordCheckResultTypeEnum.DISABLED | PasswordCheckResultTypeEnum.INDETERMINATE_ERROR | 1
        PasswordCheckResultTypeEnum.SKIPPED  | PasswordCheckResultTypeEnum.INDETERMINATE_ERROR | 2

        PasswordCheckResultTypeEnum.FAILED   | PasswordCheckResultTypeEnum.INDETERMINATE_RETRY | 2
        PasswordCheckResultTypeEnum.PASSED   | PasswordCheckResultTypeEnum.INDETERMINATE_RETRY | 1
        PasswordCheckResultTypeEnum.DISABLED | PasswordCheckResultTypeEnum.INDETERMINATE_RETRY | 1
        PasswordCheckResultTypeEnum.SKIPPED  | PasswordCheckResultTypeEnum.INDETERMINATE_RETRY | 2

    }
}

