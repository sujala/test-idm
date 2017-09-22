package com.rackspace.test;

import org.springframework.context.annotation.Configuration;

import java.lang.annotation.*;

/**
 * This annotation is used to mark spring java bean configuration classes that should NOT be automatically loaded as part of standard spring application context (app-config.xml).
 * Instead these java based configs are used for single tests that load custom spring app contexts.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
public @interface SingleTestConfiguration {
}



