package testHelpers.junit

import com.rackspace.idm.domain.config.RepositoryProfileResolver
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import org.apache.commons.collections.CollectionUtils
import org.junit.rules.TestRule
import org.junit.runner.Description;

import org.junit.Assume;
import org.junit.runners.model.Statement

import org.springframework.core.env.AbstractEnvironment
import org.springframework.util.StringUtils;

public class ConditionalIgnoreRule implements TestRule {

    @Override
    public Statement apply(Statement statement, Description description) {
        def (ignoreByAnnotation, ignoreByClass) = findIgnoreByAnnotation(description)
        SpringRepositoryProfileEnum activeRepositoryProfile
        if(ignoreByAnnotation != null) {
            if(!ignoreByClass) {
                activeRepositoryProfile = RepositoryProfileResolver.getActiveRepositoryProfile()
            } else {
                activeRepositoryProfile = getRepositoryProfileFromSystemProperties()
            }
            if(activeRepositoryProfile == ignoreByAnnotation.profile()) {
                return new IgnoreStatement();
            }
        }
        return statement;
    }

    public static class IgnoreStatement extends Statement {

        @Override
        public void evaluate() {
            Assume.assumeTrue(false);
        }

    }

    private static findIgnoreByAnnotation(Description description) {
        IgnoreByRepositoryProfile ignoreByAnnotation = description.getAnnotation(IgnoreByRepositoryProfile)
        if(ignoreByAnnotation == null && description.isTest()) {
            return [description.getTestClass().getAnnotation(IgnoreByRepositoryProfile), true]
        }
        return [ignoreByAnnotation, false]
    }

    private static SpringRepositoryProfileEnum getRepositoryProfileFromSystemProperties() {
        String profilesString = System.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME)
        Set<String> profiles = StringUtils.commaDelimitedListToSet(profilesString)
        if(!CollectionUtils.intersection(profiles, SpringRepositoryProfileEnum.SQL.getProfileStrings()).isEmpty()) {
            return SpringRepositoryProfileEnum.SQL
        } else {
            return SpringRepositoryProfileEnum.LDAP
        }
    }

}