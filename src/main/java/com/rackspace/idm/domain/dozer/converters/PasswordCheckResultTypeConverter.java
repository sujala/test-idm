package com.rackspace.idm.domain.dozer.converters;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasswordCheckResultTypeEnum;
import org.dozer.DozerConverter;

public class PasswordCheckResultTypeConverter extends DozerConverter<String, PasswordCheckResultTypeEnum> {

    public PasswordCheckResultTypeConverter() {
        super(String.class, PasswordCheckResultTypeEnum.class);
    }

    @Override
    public PasswordCheckResultTypeEnum convertTo(String source, PasswordCheckResultTypeEnum destination) {
        if (source == null) {
            return null;
        }

        if (PasswordCheckResultTypeEnum.PASSED.name().equals(source)) {
            return PasswordCheckResultTypeEnum.PASSED;
        } else if (PasswordCheckResultTypeEnum.FAILED.name().equals(source)) {
            return PasswordCheckResultTypeEnum.FAILED;
        } else if (PasswordCheckResultTypeEnum.SKIPPED.name().equals(source)) {
            return PasswordCheckResultTypeEnum.SKIPPED;
        } else if (PasswordCheckResultTypeEnum.DISABLED.name().equals(source)) {
            return PasswordCheckResultTypeEnum.DISABLED;
        } else if (PasswordCheckResultTypeEnum.INDETERMINATE_RETRY.name().equals(source)) {
            return PasswordCheckResultTypeEnum.INDETERMINATE_RETRY;
        } else if (PasswordCheckResultTypeEnum.INDETERMINATE_ERROR.name().equals(source)) {
            return PasswordCheckResultTypeEnum.INDETERMINATE_ERROR;
        } else {
            throw new IllegalStateException(String.format("Invalid value for Password check result type: '%s'", source));
        }
    }

    @Override
    public String convertFrom(PasswordCheckResultTypeEnum source, String destination) {
        return (source == null) ? null : source.name();
    }
}
