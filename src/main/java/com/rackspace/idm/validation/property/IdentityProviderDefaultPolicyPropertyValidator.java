package com.rackspace.idm.validation.property;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.validation.Validator20;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.MediaType;

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

        // Default Policy is stored in JSON; Note: This will need to change once JSON support is removed in favor of YAML.
        validator20.validateIdpPolicy(identityProperty.getValue(), MediaType.APPLICATION_JSON_TYPE);
    }

}
