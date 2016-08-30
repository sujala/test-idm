package com.rackspace.idm.domain.service;

import lombok.Getter;


@Getter
public class OpenstackType {

    private String name;

    public OpenstackType(String name) {
        this.name = name;
    }

}
