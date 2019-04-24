package com.rackspace.idm.domain.dozer.converters;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordValidityTypeEnum;
import org.dozer.DozerConverter;

public class PasswordValidityTypeConverter extends DozerConverter<String, PasswordValidityTypeEnum> {

    public PasswordValidityTypeConverter() {
        super(String.class, PasswordValidityTypeEnum.class);
    }

    @Override
    public PasswordValidityTypeEnum convertTo(String source, PasswordValidityTypeEnum destination) {
        if (source == null) {
            return null;
        }

        if (PasswordValidityTypeEnum.TRUE.name().equals(source)) {
            return PasswordValidityTypeEnum.TRUE;
        } else if (PasswordValidityTypeEnum.FALSE.name().equals(source)) {
            return PasswordValidityTypeEnum.FALSE;
        } else if (PasswordValidityTypeEnum.INDETERMINATE.name().equals(source)) {
            return PasswordValidityTypeEnum.INDETERMINATE;
        } else {
            throw new IllegalStateException(String.format("Invalid value for Password validity type: '%s'", source));
        }
    }

    @Override
    public String convertFrom(PasswordValidityTypeEnum source, String destination) {
        return (source == null) ? null : source.name();
    }
}
