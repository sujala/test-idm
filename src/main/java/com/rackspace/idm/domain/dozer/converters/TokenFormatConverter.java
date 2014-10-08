package com.rackspace.idm.domain.dozer.converters;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenFormatEnum;
import org.dozer.DozerConverter;

public class TokenFormatConverter extends DozerConverter<String, TokenFormatEnum> {

    public TokenFormatConverter() {
        super(String.class, TokenFormatEnum.class);
    }

    @Override
    public TokenFormatEnum convertTo(String source, TokenFormatEnum destination) {
        if(source == null) {
            return null;
        }
        try {
            return TokenFormatEnum.fromValue(source);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Invalid value for token format: '%s'", source));
        }
    }

    @Override
    public String convertFrom(TokenFormatEnum source, String destination) {
        return source == null ? null : source.value();
    }

}
