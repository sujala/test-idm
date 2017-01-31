package com.rackspace.idm.validation.property;


import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty;

/**
 * Classes that implement this interface are used to validate IdentityProperty objects that match
 * specific IdentityProperty instances. In other words, if a feature in Identity depends on
 * an IdentityProperty existing and that property must pass specific validation then
 * this interface can be implemented and used for that purpose.
 *
 * NOTE: Simply implementing this interface and marking the implementing class with @Component
 *  is sufficient to have the IdentityProperty CRUD APIs pick up this class for validation.
 */
public interface TargetedIdentityPropertyValidator {

    /**
     * Returns true if the provided IdentityProperty is supported for validation
     *
     * @param identityProperty
     * @return
     */
    boolean supports(IdentityProperty identityProperty);

    /**
     * Validates the provided IdentityProperty. Implementations of this method should only validate the value on
     * IdentityProperty if the value is not null.
     *
     * @throws com.rackspace.idm.exception.BadRequestException if the provided IdentityProperty does not pass validation
     */
    void validate(IdentityProperty identityProperty);
}
