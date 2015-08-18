package testHelpers.junit.java;

import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface IgnoreByRepositoryProfile {
    SpringRepositoryProfileEnum profile();
}
