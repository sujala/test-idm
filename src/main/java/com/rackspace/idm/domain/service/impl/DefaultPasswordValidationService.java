package com.rackspace.idm.domain.service.impl;

import com.newrelic.api.agent.Trace;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordCheckResultTypeEnum;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.ValidatePasswordResult;
import com.rackspace.idm.domain.entity.ValidatePasswordResultBuilder;
import com.rackspace.idm.domain.service.PasswordBlacklistService;
import com.rackspace.idm.domain.service.PasswordValidationService;
import com.rackspace.idm.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultPasswordValidationService implements PasswordValidationService {

    @Autowired
    IdentityConfig identityConfig;

    @Autowired
    PasswordBlacklistService passwordBlacklistService;

    @Autowired
    Validator validator;

    @Trace
    @Override
    public ValidatePasswordResult validatePassword(String password) {

        // Do password composition check.
        boolean compositionCheckResult = validator.validatePasswordComposition(password);

        PasswordCheckResultTypeEnum blacklistCheckResult = PasswordCheckResultTypeEnum.SKIPPED;

        // Do password blacklist check only if composition check passes .
        if (compositionCheckResult) {
            blacklistCheckResult = passwordBlacklistService.checkPasswordInBlacklist(password);
        }

        // Build and return ValidatePasswordResult object.
        return ValidatePasswordResultBuilder.builder()
                .withCompositionResult(compositionCheckResult)
                .withPasswordCheckResult(blacklistCheckResult)
                .build();
    }
}