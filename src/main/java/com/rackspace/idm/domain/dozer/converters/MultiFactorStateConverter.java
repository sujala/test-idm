package com.rackspace.idm.domain.dozer.converters;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorStateEnum;
import com.rackspace.idm.multifactor.service.BasicMultiFactorService;
import org.dozer.DozerConverter;

public class MultiFactorStateConverter extends DozerConverter<String, MultiFactorStateEnum> {

    public MultiFactorStateConverter() {
        super(String.class, MultiFactorStateEnum.class);
    }

    @Override
    public MultiFactorStateEnum convertTo(String source, MultiFactorStateEnum destination) {
        if(source == null) {
            return null;
        } else if(BasicMultiFactorService.MULTI_FACTOR_STATE_ACTIVE.equals(source)) {
            return MultiFactorStateEnum.ACTIVE;
        } else if(BasicMultiFactorService.MULTI_FACTOR_STATE_LOCKED.equals(source)) {
            return MultiFactorStateEnum.LOCKED;
        } else {
            throw new IllegalStateException("Invalid value for multi-factor state");
        }
    }

    @Override
    public String convertFrom(MultiFactorStateEnum source, String destination) {
        return (source == null) ? null : source.toString();
    }

}
