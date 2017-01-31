package com.rackspace.idm.validation;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty;
import com.rackspace.idm.domain.entity.IdentityPropertyValueType;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.validation.entity.IdentityPropertyValueTypeValidator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class IdentityPropertyValidator {

    private static final Logger logger = LoggerFactory.getLogger(IdentityPropertyValidator.class);

    public static final String IDENTITY_PROPERTY_REQUIRED = "Property %s is required.";
    public static final String IDENTITY_PROPERTY_NAME_REQUIRED = String.format(IDENTITY_PROPERTY_REQUIRED, "name");
    public static final String IDENTITY_PROPERTY_VALUE_REQUIRED = String.format(IDENTITY_PROPERTY_REQUIRED, "value");
    public static final String IDENTITY_PROPERTY_VALUE_TYPE_REQUIRED = String.format(IDENTITY_PROPERTY_REQUIRED, "valueType");
    public static final String IDENTITY_PROPERTY_DESCRIPTION_REQUIRED = String.format(IDENTITY_PROPERTY_REQUIRED, "description");
    public static final String IDENTITY_PROPERTY_IDENTITY_VERSION_REQUIRED = String.format(IDENTITY_PROPERTY_REQUIRED, "identity version");
    public static final String IDENTITY_PROPERTY_RELOADABLE_REQUIRED = String.format(IDENTITY_PROPERTY_REQUIRED, "reloadable");
    public static final String IDENTITY_PROPERTY_SEARCHABLE_REQUIRED = String.format(IDENTITY_PROPERTY_REQUIRED, "searchable");
    public static final String IDENTITY_PROPERTY_VALUE_TYPE_INVALID = "Identity property value type is invalid.";

    public static final int IDENTITY_PROPERTY_NAME_MAX_LENGTH = 255;
    public static final int IDENTITY_PROPERTY_DESCRIPTION_MAX_LENGTH = 255;
    public static final int IDENTITY_PROPERTY_VERSION_MAX_LENGTH = 15;

    public static final String IDENTITY_PROPERTY_NAME_LENGTH_EXCEEDED = "Property name must be no longer than " + IDENTITY_PROPERTY_NAME_MAX_LENGTH + " characters.";
    public static final String IDENTITY_PROPERTY_DESCRIPTION_LENGTH_EXCEEDED = "Property description must be no longer than " + IDENTITY_PROPERTY_DESCRIPTION_MAX_LENGTH + " characters.";
    public static final String IDENTITY_PROPERTY_VERSION_LENGTH_EXCEEDED = "Property identity version must be no longer than " + IDENTITY_PROPERTY_VERSION_MAX_LENGTH + " characters.";

    @Autowired
    Collection<IdentityPropertyValueTypeValidator> valueTypeValidators;

    /**
     * Validates the IdentityProperty for creation. This verifies that the required properties are set
     * and that all provided properties have valid values
     *
     * @param identityProperty
     */
    public void validateIdentityPropertyForCreate(IdentityProperty identityProperty) {
        validateRequiredPropertiesSet(identityProperty);
        validateName(identityProperty);
        validateDescription(identityProperty);
        validateIdmVersion(identityProperty);
        validateValueType(identityProperty);
        validateValue(identityProperty);
    }

    /**
     * Validates the IdentityProperty for updates. This ignores any properties that a user is not allowed
     * to update.
     *
     * @param identityProperty
     */
    public void validateIdentityPropertyForUpdate(IdentityProperty identityProperty) {
        validateDescription(identityProperty);
        validateIdmVersion(identityProperty);
        validateValue(identityProperty);
    }

    /**
     * Validates that the required properties for an IdentityProperty have a value set
     *
     * @param identityProperty
     */
    private void validateRequiredPropertiesSet(IdentityProperty identityProperty) {

        if (StringUtils.isBlank(identityProperty.getName())) {
            throw new BadRequestException(IDENTITY_PROPERTY_NAME_REQUIRED);
        }

        if (StringUtils.isBlank(identityProperty.getValueType())) {
            throw new BadRequestException(IDENTITY_PROPERTY_VALUE_TYPE_REQUIRED);
        }

        if (StringUtils.isBlank(identityProperty.getValue())) {
            throw new BadRequestException(IDENTITY_PROPERTY_VALUE_REQUIRED);
        }

        if (StringUtils.isBlank(identityProperty.getDescription())) {
            throw new BadRequestException(IDENTITY_PROPERTY_DESCRIPTION_REQUIRED);
        }

        if (StringUtils.isBlank(identityProperty.getIdmVersion())) {
            throw new BadRequestException(IDENTITY_PROPERTY_IDENTITY_VERSION_REQUIRED);
        }

        if (identityProperty.isReloadable() == null) {
            throw new BadRequestException(IDENTITY_PROPERTY_RELOADABLE_REQUIRED);
        }

        if (identityProperty.isSearchable() == null) {
            throw new BadRequestException(IDENTITY_PROPERTY_SEARCHABLE_REQUIRED);
        }

    }

    private void validateName(IdentityProperty identityProperty) {
        if (StringUtils.isNotEmpty(identityProperty.getName()) &&
                identityProperty.getName().length() > IDENTITY_PROPERTY_NAME_MAX_LENGTH) {
            throw new BadRequestException(IDENTITY_PROPERTY_NAME_LENGTH_EXCEEDED);
        }
    }

    private void validateDescription(IdentityProperty identityProperty) {
        if (StringUtils.isNotEmpty(identityProperty.getDescription()) &&
                identityProperty.getDescription().length() > IDENTITY_PROPERTY_DESCRIPTION_MAX_LENGTH) {
            throw new BadRequestException(IDENTITY_PROPERTY_DESCRIPTION_LENGTH_EXCEEDED);
        }
    }

    private void validateIdmVersion(IdentityProperty identityProperty) {
        if (StringUtils.isNotEmpty(identityProperty.getIdmVersion()) &&
                identityProperty.getIdmVersion().length() > IDENTITY_PROPERTY_VERSION_MAX_LENGTH) {
            throw new BadRequestException(IDENTITY_PROPERTY_VERSION_LENGTH_EXCEEDED);
        }
    }

    private void validateValueType(IdentityProperty identityProperty) {
        if (StringUtils.isNotEmpty(identityProperty.getValueType()) &&
                IdentityPropertyValueType.getValueTypeByName(identityProperty.getValueType()) == null) {
            throw new BadRequestException(IDENTITY_PROPERTY_VALUE_TYPE_INVALID);
        }
    }

    private void validateValue(IdentityProperty identityProperty) {
        IdentityPropertyValueType valueType = IdentityPropertyValueType.getValueTypeByName(identityProperty.getValueType());

        IdentityPropertyValueTypeValidator validator = null;
        for (IdentityPropertyValueTypeValidator v : valueTypeValidators) {
            if (v.supports(valueType)) {
                validator = v;
            }
        }

        // value type should have already been validated by this point but we do not want to support
        // value types that do not have a validator.
        if (validator == null) {
            logger.debug("Identity value validator not found for value type " + identityProperty.getValueType());
            throw new BadRequestException(IDENTITY_PROPERTY_VALUE_TYPE_INVALID);
        }

        if (StringUtils.isNotBlank(identityProperty.getValue())) {
            validator.validateIdentityProperty(identityProperty);
        }
    }

}
