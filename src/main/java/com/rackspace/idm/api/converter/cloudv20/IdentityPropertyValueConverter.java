package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.domain.entity.IdentityPropertyValueType;
import com.rackspace.idm.domain.entity.ReadableIdentityProperty;
import org.springframework.stereotype.Component;

@Component
public class IdentityPropertyValueConverter {

    /**
     * Converts the value of the IdentityProperty into the expected object types.
     * For example, value types of INT are converted to Integer objects
     *
     * @param identityProperty
     * @throws NumberFormatException - thrown if the property is an INT property but the value is not parseable to an int
     * @return
     */
    public Object convertPropertyValue(ReadableIdentityProperty identityProperty) {
        if (identityProperty == null || identityProperty.getValue() == null || identityProperty.getValueType() == null) {
            return null;
        }

        switch (IdentityPropertyValueType.getValueTypeByName(identityProperty.getValueType())) {
            case INT:
                return Integer.parseInt(identityProperty.getValueAsString());
            case BOOLEAN:
                return Boolean.parseBoolean(identityProperty.getValueAsString());
            default:
                return identityProperty.getValueAsString();
        }
    }
}
