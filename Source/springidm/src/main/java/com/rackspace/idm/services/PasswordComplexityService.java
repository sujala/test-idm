package com.rackspace.idm.services;

import java.util.List;

import com.rackspace.idm.entities.passwordcomplexity.PasswordComplexityResult;
import com.rackspace.idm.entities.passwordcomplexity.PasswordRule;

public interface PasswordComplexityService {
    PasswordComplexityResult checkPassword(String password);
    
    List<PasswordRule> getRules();
}
