package com.rackspace.idm.validation.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty;
import com.rackspace.idm.domain.entity.IdentityPropertyValueType;

public interface IdentityPropertyValueTypeValidator {

    /**
     * Returns true if the IdentityPropertyValueType is supported by this validator
     *
     * @param valueType
     * @return
     */
    boolean supports(IdentityPropertyValueType valueType);

    /**
     * Validates the IdentityProperty for the provided value type. Throws BadRequestException if the value on the
     * IdentityProperty is blank or null.
     *
     * @throws com.rackspace.idm.exception.BadRequestException when the supplied IdentityProperty does not pass validation
     * @param identityProperty
     */
    void validateIdentityProperty(IdentityProperty identityProperty);

}
