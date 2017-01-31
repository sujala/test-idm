package com.rackspace.idm.validation.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty;
import com.rackspace.idm.domain.entity.IdentityPropertyValueType;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Component
public class IdentityPropertyStringValueTypeValidator implements IdentityPropertyValueTypeValidator {

    public static final int STRING_VALUE_MAX_LENGTH_KB = 512;

    public static final String VALUE_LENGTH_EXCEEDED_MSG = String.format("String value must be no more than %d kilobytes in UTF-8.", STRING_VALUE_MAX_LENGTH_KB);
    public static final String VALUE_REQUIRED_MSG = "Identity property value required.";

    @Override
    public boolean supports(IdentityPropertyValueType valueType) {
        return IdentityPropertyValueType.STRING == valueType;
    }

    @Override
    public void validateIdentityProperty(IdentityProperty identityProperty) {
        Validate.isTrue(IdentityPropertyValueType.STRING ==
                IdentityPropertyValueType.getValueTypeByName(identityProperty.getValueType()));

        if (StringUtils.isEmpty(identityProperty.getValue())) {
            throw new BadRequestException(VALUE_REQUIRED_MSG);
        }

        if ((double) identityProperty.getValue().getBytes(StandardCharsets.UTF_8).length / 1024 > STRING_VALUE_MAX_LENGTH_KB) {
            throw new BadRequestException(VALUE_LENGTH_EXCEEDED_MSG);
        }
    }

}
