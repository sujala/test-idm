package com.rackspace.idm.validation.property;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.IdentityPropertyValueType;
import com.rackspace.idm.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PropertyValueTypeValidator implements TargetedIdentityPropertyValidator {

    public static final String PROPERTY_VALUE_TYPE_MISMATCH_ERROR_MSG = "Identity property '%s' must value a value type of %s.";

    @Autowired
    IdentityConfig identityConfig;

    @Override
    public boolean supports(IdentityProperty identityProperty) {
        return identityConfig.getPropertyValueType(identityProperty.getName()) != null;
    }

    @Override
    public void validate(IdentityProperty identityProperty) {
        if (identityProperty == null || identityProperty.getValue() == null) {
            return;
        }

        IdentityPropertyValueType valueType = identityConfig.getPropertyValueType(identityProperty.getName());
        if (valueType != null && valueType != IdentityPropertyValueType.getValueTypeByName(identityProperty.getValueType())) {
            throw new BadRequestException(String.format(PROPERTY_VALUE_TYPE_MISMATCH_ERROR_MSG, identityProperty.getName(), valueType.name()));
        }
    }

}
