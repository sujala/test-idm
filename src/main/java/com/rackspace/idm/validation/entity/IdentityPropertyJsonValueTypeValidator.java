package com.rackspace.idm.validation.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty;
import com.rackspace.idm.domain.entity.IdentityPropertyValueType;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;

@Component
public class IdentityPropertyJsonValueTypeValidator implements IdentityPropertyValueTypeValidator {

    public static final int JSON_VALUE_MAX_LENGTH = 1024 * 1024;

    public static final String VALUE_LENGTH_EXCEEDED_MSG = String.format("JSON value type must be less than %d.", JSON_VALUE_MAX_LENGTH);
    public static final String VALUE_REQUIRED_MSG = "Identity property value required.";
    public static final String VALUE_INVALID_JSON_MSG = "Identity value is invalid json.";

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

        if (identityProperty.getValue().length() > JSON_VALUE_MAX_LENGTH) {
            throw new BadRequestException(VALUE_LENGTH_EXCEEDED_MSG);
        }

        try {
            JSONParser jsonParser = new JSONParser();
            jsonParser.parse(new StringReader(identityProperty.getValue()));
        } catch (ParseException | IOException ex) {
            throw new BadRequestException(VALUE_INVALID_JSON_MSG);
        }

    }

}
