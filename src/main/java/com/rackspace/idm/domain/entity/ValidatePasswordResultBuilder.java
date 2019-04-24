package com.rackspace.idm.domain.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordCheckResultTypeEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordValidityTypeEnum;
import com.rackspace.idm.ErrorCodes;

import java.util.ArrayList;

public class ValidatePasswordResultBuilder {

    private PasswordCheckResultTypeEnum blacklistCheckResult;
    private boolean compositionCheckResult;

    private ValidatePasswordResultBuilder() {
    }

    public static ValidatePasswordResultBuilder builder() {
        return new ValidatePasswordResultBuilder();
    }

    public ValidatePasswordResultBuilder withCompositionResult(boolean result) {
        this.compositionCheckResult = result;
        return this;

    }

    public ValidatePasswordResultBuilder withPasswordCheckResult(PasswordCheckResultTypeEnum result) {
        this.blacklistCheckResult = result;
        return this;
    }

    public ValidatePasswordResult build() {
        ValidatePasswordResult validatePasswordResult = new ValidatePasswordResult();
        if (blacklistCheckResult == null) {
            blacklistCheckResult = PasswordCheckResultTypeEnum.SKIPPED;
        }

        // Populate values for Composition check with message
        populateCompositionCheckResultAndMessage(validatePasswordResult, compositionCheckResult);

        // Populate values for Blacklist check with message
        populateBlacklistCheckResultAndMessage(validatePasswordResult, blacklistCheckResult);


        return validatePasswordResult;
    }


    private void populateCompositionCheckResultAndMessage(ValidatePasswordResult validatePasswordResult, boolean compositionCheckResult) {
        if (compositionCheckResult) {
            validatePasswordResult.setCompositionCheck(PasswordCheckResultTypeEnum.PASSED);
        } else {
            validatePasswordResult.setCompositionCheck(PasswordCheckResultTypeEnum.FAILED);
            String compositionCheckFailureMessage = ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_COMPOSITION_FAILED, ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_COMPOSITION_FAILED_MSG);
            validatePasswordResult.setCompositionCheckMessage(compositionCheckFailureMessage);
        }
    }

    private void populateBlacklistCheckResultAndMessage(ValidatePasswordResult validatePasswordResult, PasswordCheckResultTypeEnum blacklistCheckResult) {
        String errorMessage = ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_INDETERMINATE_ERROR, ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_INDETERMINATE_ERROR_MSG);

        if (blacklistCheckResult == PasswordCheckResultTypeEnum.PASSED) {
            validatePasswordResult.setBlacklistCheck(PasswordCheckResultTypeEnum.PASSED);
            // clear out the message when blacklist check passes.
            errorMessage = null;
        } else if (blacklistCheckResult == PasswordCheckResultTypeEnum.FAILED) {
            validatePasswordResult.setBlacklistCheck(PasswordCheckResultTypeEnum.FAILED);
            errorMessage = ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_FAILED, ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_FAILED_MSG);
        } else if (blacklistCheckResult == PasswordCheckResultTypeEnum.SKIPPED) {
            validatePasswordResult.setBlacklistCheck(PasswordCheckResultTypeEnum.SKIPPED);
            errorMessage = ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_SKIPPED, ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_SKIPPED_MSG);
        } else if (blacklistCheckResult == PasswordCheckResultTypeEnum.DISABLED) {
            validatePasswordResult.setBlacklistCheck(PasswordCheckResultTypeEnum.DISABLED);
            errorMessage = ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_DISABLED, ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_DISABLED_MSG);
        } else if (blacklistCheckResult == PasswordCheckResultTypeEnum.INDETERMINATE_RETRY) {
            validatePasswordResult.setBlacklistCheck(PasswordCheckResultTypeEnum.INDETERMINATE_RETRY);
            errorMessage = ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_INDETERMINATE_RETRY, ErrorCodes.ERROR_CODE_VALIDATE_PASSWORD_BLACKLIST_INDETERMINATE_RETRY_MSG);
        } else {
            validatePasswordResult.setBlacklistCheck(PasswordCheckResultTypeEnum.INDETERMINATE_ERROR);
        }

        validatePasswordResult.setBlacklistCheckMessage(errorMessage);
    }
}



