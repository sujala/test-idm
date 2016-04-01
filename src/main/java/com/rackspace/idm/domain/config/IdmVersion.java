package com.rackspace.idm.domain.config;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IdmVersion {

    private final Version version;
    private final Build build;

    @Getter
    @AllArgsConstructor
    public static class Version {
        private final String value;
    }

    @Getter
    @AllArgsConstructor
    public static class Build {
        private final String value;
    }

}
