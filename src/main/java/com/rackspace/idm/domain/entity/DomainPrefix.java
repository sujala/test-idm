package com.rackspace.idm.domain.entity;

public enum DomainPrefix {
    DEDICATED("dedicated:"),
    DATAPIPE("dp:");

    String name;

    DomainPrefix(String name){
        this.name = name;
    }

    public String getName() {
        return name.toLowerCase();
    }
}
