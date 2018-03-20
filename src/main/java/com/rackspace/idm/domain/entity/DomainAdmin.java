package com.rackspace.idm.domain.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents the status of a domain's admin user.
 */
@JsonRootName(value = "domain")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class DomainAdmin {

    private String id;
    private String userAdminDN = "";
    private String previousUserAdminDN = "";

    private static ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
    }

    public DomainAdmin (String domainId) {
        this.id = domainId;
    }

    public String toJson() throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

}
