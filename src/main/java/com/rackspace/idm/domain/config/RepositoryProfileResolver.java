package com.rackspace.idm.domain.config;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;


@Component
public class RepositoryProfileResolver implements EnvironmentAware {

    private static Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public static SpringRepositoryProfileEnum getActiveRepositoryProfile() {

        for(String profileString : environment.getActiveProfiles()) {
            SpringRepositoryProfileEnum profileEnum = SpringRepositoryProfileEnum.getProfileEnumFromProfileString(profileString);
            if(profileEnum != null) {
                return profileEnum;
            }
        }
        return SpringRepositoryProfileEnum.LDAP;
    }

}
