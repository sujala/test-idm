package com.rackspace.idm.domain.dozer.converters;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainMultiFactorEnforcementLevelEnum;
import com.rackspace.idm.GlobalConstants;
import org.dozer.DozerConverter;

public class DomainMultiFactorEnforcementLevelConverter extends DozerConverter<String, DomainMultiFactorEnforcementLevelEnum> {

    public DomainMultiFactorEnforcementLevelConverter() {
        super(String.class, DomainMultiFactorEnforcementLevelEnum.class);
    }

    @Override
    public DomainMultiFactorEnforcementLevelEnum convertTo(String source, DomainMultiFactorEnforcementLevelEnum destination) {
        if(source == null) {
            return null;
        }

        if(GlobalConstants.DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED.equals(source)) {
            return DomainMultiFactorEnforcementLevelEnum.REQUIRED;
        } else if(GlobalConstants.DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL.equals(source)) {
            return DomainMultiFactorEnforcementLevelEnum.OPTIONAL;
        } else {
            throw new IllegalStateException(String.format("Invalid value for multi-factor enforcement level on a domain: '%s'", source));
        }
    }

    @Override
    public String convertFrom(DomainMultiFactorEnforcementLevelEnum source, String destination) {
        return (source == null) ? null : source.toString();
    }

}
