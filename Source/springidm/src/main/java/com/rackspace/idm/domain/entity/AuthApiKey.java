package com.rackspace.idm.domain.entity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AuthApiKey {
    private String value;

    public AuthApiKey() {
        // Needed by JAXB
    }

    public AuthApiKey(String key) {
        this.value = key;
    }

    @XmlElement
    public String getValue() {
        return value;
    }

    public void setValue(String apiKey) {
        this.value = apiKey;
    }
}
