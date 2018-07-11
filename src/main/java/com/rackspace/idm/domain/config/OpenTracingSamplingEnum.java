package com.rackspace.idm.domain.config;

public enum OpenTracingSamplingEnum {
    CONST("const"), RATELIMITING("ratelimiting"), PROBABILISTIC("probabilistic");

    String name;
    OpenTracingSamplingEnum(String name) {
        this.name = name;
    }

    String getName() {
        return this.name;
    }
}