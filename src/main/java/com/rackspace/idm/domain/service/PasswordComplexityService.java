package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.PasswordComplexityResult;
import com.rackspace.idm.domain.entity.PasswordRule;

import java.util.List;

public interface PasswordComplexityService {
    PasswordComplexityResult checkPassword(String password);
    
    List<PasswordRule> getRules();
}
