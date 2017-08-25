package com.rackspace.idm.validation.property;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.service.IdpPolicyFormatEnum;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.validation.Validator20;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IdentityProviderDefaultPolicyPropertyValidator implements TargetedIdentityPropertyValidator {

    @Autowired
    private Validator20 validator20;

    @Override
    public boolean supports(IdentityProperty identityProperty) {
        return IdentityConfig.FEDERATION_IDENTITY_PROVIDER_DEFAULT_POLICY_PROP.equals(identityProperty.getName());
    }

    @Override
    public void validate(IdentityProperty identityProperty) {
        if (identityProperty == null || identityProperty.getValue() == null) {
            return;
        }

        IdpPolicyFormatEnum idpPolicyFormatEnum;
        try {
            idpPolicyFormatEnum = IdpPolicyFormatEnum.valueOf(identityProperty.getValueType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid valueType for default mapping policy");
        }
        validator20.validateIdpPolicy(identityProperty.getValue(), idpPolicyFormatEnum);
    }
}
