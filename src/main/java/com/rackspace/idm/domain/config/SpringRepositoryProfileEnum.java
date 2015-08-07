package com.rackspace.idm.domain.config;


import java.util.Arrays;
import java.util.List;

public enum SpringRepositoryProfileEnum {
    LDAP(new String[]{"default", "LDAP"}),
    SQL(new String[]{"SQL"});

    private List<String> profileStrings;

    SpringRepositoryProfileEnum(String[] profileStrings) {
        this.profileStrings = Arrays.asList(profileStrings);
    }

    public static SpringRepositoryProfileEnum getProfileEnumFromProfileString(String profileString) {
        for(SpringRepositoryProfileEnum profileEnum : values()) {
            if(profileEnum.matchesProfileString(profileString)) {
                return profileEnum;
            }
        }
        return null;
    }

    public boolean matchesProfileString(String profile) {
        return profileStrings.contains(profile);
    }

}