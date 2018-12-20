package com.rackspace.idm.domain.dao.impl;

import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Concatenates the string parameters of the method and creates a SHA 256 hash of the resultant string. Can only support
 * methods for which *all* the parameters are strings.
 */
@Component
public class HashedStringKeyGenerator implements KeyGenerator {

    private static final String HASH_KEY = RandomStringUtils.randomAlphanumeric(10);

    public Object generate(Object target, Method method, Object... params) {
        StringBuilder builder = new StringBuilder();
        for (Object param : params) {
            if (param != null && !(param instanceof String)) {
                throw new IllegalStateException("Key generator does not support generating keys for non-string params");
            }
            if (param != null) {
                builder.append((String)param);
            }
        }

        String plainKey = builder.toString();
        String hashKey = HmacUtils.hmacSha256Hex(HASH_KEY, plainKey);
        return hashKey;
    }
}
