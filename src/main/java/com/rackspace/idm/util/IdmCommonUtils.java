package com.rackspace.idm.util;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class IdmCommonUtils {

    public Boolean getBoolean(String value) {
        return StringUtils.isNotBlank(value)
                && (Boolean.TRUE.toString().equalsIgnoreCase(value))
                || (Boolean.FALSE.toString().equalsIgnoreCase(value)) ? Boolean.valueOf(value) : null;
    }
}
