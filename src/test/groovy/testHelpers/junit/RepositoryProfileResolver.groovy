package testHelpers.junit

import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component


@Component
class RepositoryProfileResolver implements EnvironmentAware {

    private static Environment environment

    @Override
    void setEnvironment(Environment environment) {
        this.environment = environment
    }

    public static SpringRepositoryProfileEnum getActiveRepositoryProfile() {

        def profileEnum
        for(def profileString : environment.getActiveProfiles()) {
            profileEnum = SpringRepositoryProfileEnum.getProfileEnumFromProfileString(profileString)
            if(profileEnum != null) {
                return profileEnum
            }
        }

    }

}
