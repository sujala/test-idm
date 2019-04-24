package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordCheckResultTypeEnum;
import com.rackspace.idm.domain.entity.ValidatePasswordResult;

public interface PasswordBlacklistService {

    /**
     * Checks the password blacklist for the given password. A password
     * is considered to be in the blacklist when the password is in the
     * blacklist and the number of times the password has been
     * publicly compromised is greater than the provided count.
     *
     * @param password
     * @return
     */
    boolean isPasswordInBlacklist(String password);

    PasswordCheckResultTypeEnum checkPasswordInBlacklist(String password);
}
