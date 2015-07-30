package testHelpers.junit

import org.junit.rules.TestRule
import org.junit.runner.Description;

import org.junit.Assume;
import org.junit.runners.model.Statement;

public class ConditionalIgnoreRule implements TestRule {

    @Override
    public Statement apply(Statement statement, Description description) {
        IgnoreByRepositoryProfile repositoryProfile = description.getAnnotation(IgnoreByRepositoryProfile)
        if(repositoryProfile != null) {
            SpringRepositoryProfileEnum activeRepositoryProfile = RepositoryProfileResolver.getActiveRepositoryProfile()
            if(activeRepositoryProfile == repositoryProfile.profile()) {
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

}