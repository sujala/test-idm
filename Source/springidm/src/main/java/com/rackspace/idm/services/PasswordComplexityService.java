package com.rackspace.idm.services;

import java.util.List;

import com.rackspace.idm.domain.entity.PasswordComplexityResult;
import com.rackspace.idm.domain.entity.PasswordRule;

public interface PasswordComplexityService {
    PasswordComplexityResult checkPassword(String password);
    
    List<PasswordRule> getRules();
}
