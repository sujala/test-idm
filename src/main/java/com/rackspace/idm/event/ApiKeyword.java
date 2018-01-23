package com.rackspace.idm.event;

import lombok.Getter;

/**
 * Keywords describing APIs
 */
public enum ApiKeyword {
    DEPRECATED("Deprecated"), COSTLY("Costly");

    @Getter
    private String reportValue;

    ApiKeyword(String reportValue) {
        this.reportValue = reportValue;
    }

    }
