package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.ValidatePasswordResult;

public interface PasswordValidationService {

    /**
     * The service validate that the password follows the standard Identity password composition
     * and that the password is not blacklisted.
     *
     * @param password
     * @return ValidatePasswordResult
     */
    ValidatePasswordResult validatePassword(String password);
}
