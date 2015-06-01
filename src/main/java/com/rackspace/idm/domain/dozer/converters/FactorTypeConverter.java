package com.rackspace.idm.domain.dozer.converters;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.FactorTypeEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenFormatEnum;
import org.dozer.DozerConverter;

public class FactorTypeConverter extends DozerConverter<String, FactorTypeEnum> {

    public FactorTypeConverter() {
        super(String.class, FactorTypeEnum.class);
    }

    @Override
    public FactorTypeEnum convertTo(String source, FactorTypeEnum destination) {
        if(source == null) {
            return null;
        }
        try {
            return FactorTypeEnum.fromValue(source);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Invalid value for token format: '%s'", source));
        }
    }

    @Override
    public String convertFrom(FactorTypeEnum source, String destination) {
        return source == null ? null : source.value();
    }

}
