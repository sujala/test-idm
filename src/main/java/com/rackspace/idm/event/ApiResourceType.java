package com.rackspace.idm.event;

import lombok.Getter;

public enum ApiResourceType {
    /**
     * Any service whose primary function is to authenticate the user (e.g to return a token).
     */
    AUTH("Auth"),

    /**
     * Services which require authentication to consume (via supplied token of basic auth)
     */
    PRIVATE("Private"),

    /**
     * Services wide open that require no authentication
     */
    PUBLIC("Public");

    @Getter
    private String reportValue;

    ApiResourceType(String reportValue) {
        this.reportValue = reportValue;
    }
}
