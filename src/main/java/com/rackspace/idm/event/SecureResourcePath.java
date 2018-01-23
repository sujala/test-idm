package com.rackspace.idm.event;

import java.lang.annotation.*;


/**
 * Each group matched by the {@link #regExPattern()} is secured by generating an HMAC using SHA-256, encoding
 * the resultant hash with web safe base64, and replacing the value in the request path with the base64 hash result.
 *
 * All groups included in the regex must match something. Otherwise the request path is not considered secure and will
 * not be reported.
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SecureResourcePath {
    /**
     * A regex pattern to apply against the request path of a REST resource (e.g. http://localhost:8083/idm/cloud/v2.0/tenants/-10/users/342341/roles)
     * to identify groups of strings to mask.
     *
     * @return
     */
    String regExPattern();
}
