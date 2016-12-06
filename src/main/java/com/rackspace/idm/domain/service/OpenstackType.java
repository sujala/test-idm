package com.rackspace.idm.domain.service;

import lombok.Getter;
import org.apache.commons.lang.StringUtils;


@Getter
public class OpenstackType {

    private String name;

    private static final String EMPTY_OPENSTACKTYPE_NAME_ERROR_MSG = "OpenstackType name cannot be empty.";

    public OpenstackType(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException(EMPTY_OPENSTACKTYPE_NAME_ERROR_MSG);
        }
        this.name = name;
    }

}
