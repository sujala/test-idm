package com.rackspace.idm.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReportableQueryParams {

    /**
     * Single name of a query param, or a comma separated list of param names, to log the usage and value
     * used. The value must be secured (HMAC). Failure to appropriately secure the value will result in the value being
     * reported as "&lt;PRIVATE>"
     *
     * @return
     */
    String[] securedQueryParams() default {};

    /**
     * Single name of a query param, or a comma separated list of param names, to log the usage of, but NOT the value
     * used. This is useful when you want to indicate that a particular query param was used, but the value itself
     * must not be reported. Values will be reported as "&lt;HIDDEN>".
     *
     * Any params also included in {@link #securedQueryParams()} are ignored.
     *
     * @return
     */
    String[] includedQueryParams() default {};

    /**
     * Single name of a query param, or a comma separated list of param names, to log the usage of and the value used
     * without any security around the value (e.g. hashing)
     *
     * Any params also included in {@link #securedQueryParams()} or {@link #includedQueryParams()} are ignored.
     *
     * @return
     */
    String[] unsecuredQueryParams() default {};
}
