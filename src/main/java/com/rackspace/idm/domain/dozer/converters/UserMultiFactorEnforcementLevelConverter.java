package com.rackspace.idm.domain.dozer.converters;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorStateEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserMultiFactorEnforcementLevelEnum;
import com.rackspace.idm.GlobalConstants;
import org.dozer.DozerConverter;

public class UserMultiFactorEnforcementLevelConverter extends DozerConverter<String, UserMultiFactorEnforcementLevelEnum> {

    public UserMultiFactorEnforcementLevelConverter() {
        super(String.class, UserMultiFactorEnforcementLevelEnum.class);
    }

    @Override
    public UserMultiFactorEnforcementLevelEnum convertTo(String source, UserMultiFactorEnforcementLevelEnum destination) {
        if(source == null) {
            return null;
        } else if(GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT.equals(source)) {
            return UserMultiFactorEnforcementLevelEnum.DEFAULT;
        } else if(GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED.equals(source)) {
            return UserMultiFactorEnforcementLevelEnum.REQUIRED;
        } else if(GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL.equals(source)) {
            return UserMultiFactorEnforcementLevelEnum.OPTIONAL;
        } else {
            throw new IllegalStateException(String.format("Invalid value for multi-factor enforcement level on a user: '%s'", source));
        }
    }

    @Override
    public String convertFrom(UserMultiFactorEnforcementLevelEnum source, String destination) {
        return (source == null) ? null : source.toString();
    }

}
