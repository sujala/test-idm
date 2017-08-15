package com.rackspace.idm.validation.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty;
import com.rackspace.idm.domain.entity.IdentityPropertyValueType;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.validation.JsonValidator;
import com.rackspace.idm.validation.Validator20;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IdentityPropertyJsonValueTypeValidator implements IdentityPropertyValueTypeValidator {

    public static final int JSON_VALUE_MAX_LENGTH_KB = 512;

    public static final String VALUE_LENGTH_EXCEEDED_MSG = String.format("JSON value must be no more than %d kilobytes in UTF-8.", JSON_VALUE_MAX_LENGTH_KB);
    public static final String VALUE_REQUIRED_MSG = "Identity property value required.";
    public static final String VALUE_INVALID_JSON_MSG = "Identity value is invalid json.";

    @Autowired
    private JsonValidator jsonValidator;

    @Autowired
    private Validator20 validator20;

    @Override
    public boolean supports(IdentityPropertyValueType valueType) {
        return IdentityPropertyValueType.JSON == valueType;
    }

    @Override
    public void validateIdentityProperty(IdentityProperty identityProperty) {
        Validate.isTrue(IdentityPropertyValueType.JSON ==
                IdentityPropertyValueType.getValueTypeByName(identityProperty.getValueType()));

        if (StringUtils.isEmpty(identityProperty.getValue())) {
            throw new BadRequestException(VALUE_REQUIRED_MSG);
        }

        if (!validator20.stringDoesNotExceedSize(identityProperty.getValue(), JSON_VALUE_MAX_LENGTH_KB)) {
            throw new BadRequestException(VALUE_LENGTH_EXCEEDED_MSG);
        }

        if (!jsonValidator.isValidJson(identityProperty.getValue())) {
            throw new BadRequestException(VALUE_INVALID_JSON_MSG);
        }

    }

}
