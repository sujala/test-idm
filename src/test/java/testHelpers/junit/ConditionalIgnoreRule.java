package testHelpers.junit;

import com.rackspace.idm.domain.config.RepositoryProfileResolver;
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.junit.rules.TestRule;
import org.junit.runner.Description;

import org.junit.Assume;
import org.junit.runners.model.Statement;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.util.StringUtils;

import java.util.Set;

public class ConditionalIgnoreRule implements TestRule {

    @Override
    public Statement apply(Statement statement, Description description) {
        ReturnValue returnValue = findIgnoreByAnnotation(description);
        SpringRepositoryProfileEnum activeRepositoryProfile;
        if(returnValue.ignoreByAnnotation != null) {
            if(!returnValue.ignoreByClass) {
                activeRepositoryProfile = RepositoryProfileResolver.getActiveRepositoryProfile();
            } else {
                activeRepositoryProfile = getRepositoryProfileFromSystemProperties();
            }
            if(activeRepositoryProfile == returnValue.ignoreByAnnotation.profile()) {
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

    private ReturnValue findIgnoreByAnnotation(Description description) {
        IgnoreByRepositoryProfile ignoreByAnnotation = description.getAnnotation(IgnoreByRepositoryProfile.class);
        if(ignoreByAnnotation == null && description.isTest()) {
            return new ReturnValue(description.getTestClass().getAnnotation(IgnoreByRepositoryProfile.class), true);
        }
        return new ReturnValue(ignoreByAnnotation, false);
    }

    private SpringRepositoryProfileEnum getRepositoryProfileFromSystemProperties() {
        String profilesString = System.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
        Set<String> profiles = StringUtils.commaDelimitedListToSet(profilesString);
        if(!CollectionUtils.intersection(profiles, SpringRepositoryProfileEnum.SQL.getProfileStrings()).isEmpty()) {
            return SpringRepositoryProfileEnum.SQL;
        } else {
            return SpringRepositoryProfileEnum.LDAP;
        }
    }

    @Data
    private class ReturnValue {
        private IgnoreByRepositoryProfile ignoreByAnnotation;
        private boolean ignoreByClass;

        public ReturnValue(IgnoreByRepositoryProfile ignoreByAnnotation, boolean ignoreByClass) {
            this.ignoreByAnnotation = ignoreByAnnotation;
            this.ignoreByClass = ignoreByClass;
        }
    }

}