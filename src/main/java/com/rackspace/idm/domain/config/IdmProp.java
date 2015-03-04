package com.rackspace.idm.domain.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a configuration property that will be returned by the API.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface IdmProp {
    String key();
    String description() default "";
    String versionAdded() default "";
}
