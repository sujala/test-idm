package com.rackspace.idm.validation.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty;
import com.rackspace.idm.domain.entity.IdentityPropertyValueType;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Component;

@Component
public class IdentityPropertyIntValueTypeValidator implements IdentityPropertyValueTypeValidator {

    public static final String VALUE_REQUIRED_MSG = "Identity property value required.";
    public static final String VALUE_INVALID_INT_MSG = "Identity property value is an invalid integer value.";

    @Override
    public boolean supports(IdentityPropertyValueType valueType) {
        return IdentityPropertyValueType.INT == valueType;
    }

    @Override
    public void validateIdentityProperty(IdentityProperty identityProperty) {
        Validate.isTrue(IdentityPropertyValueType.INT ==
                IdentityPropertyValueType.getValueTypeByName(identityProperty.getValueType()));

        if (StringUtils.isEmpty(identityProperty.getValue())) {
            throw new BadRequestException(VALUE_REQUIRED_MSG);
        }

        try {
            Integer.parseInt(identityProperty.getValue());
        } catch (NumberFormatException e) {
            throw new BadRequestException(VALUE_INVALID_INT_MSG);
        }

    }

}
