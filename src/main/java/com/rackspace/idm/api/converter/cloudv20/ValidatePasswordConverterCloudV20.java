package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ValidatePassword;
import com.rackspace.idm.domain.entity.ValidatePasswordResult;
import org.dozer.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ValidatePasswordConverterCloudV20 {
    @Autowired
    private Mapper mapper;

    public ValidatePassword toResponse(ValidatePasswordResult validatePasswordResult) {
        ValidatePassword validatePasswordResponse = mapper.map(validatePasswordResult, ValidatePassword.class);
        validatePasswordResponse.setValid(validatePasswordResult.getValid());
        validatePasswordResponse.getNonPassingCheckNames().addAll(validatePasswordResult.getNonPassingChecks());
        return validatePasswordResponse;
    }
}
