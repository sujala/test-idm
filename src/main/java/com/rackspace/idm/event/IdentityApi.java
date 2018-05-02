package com.rackspace.idm.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IdentityApi {
    ApiResourceType apiResourceType();

    /**
     * List of keywords to include for the request.
     *
     * Note - the DEPRECATED keyword will automatically be applied if the service in question is annotated with the {@link Deprecated}
     * annotation.
     *
     * @return
     */
    ApiKeyword[] keywords() default {};

    /**
     * The name of the API for easier reference. A required attribute.
     *
     * @return
     */
    String name();
}
