package com.rackspace.idm.domain.entity;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class AuthenticationRequestForValidate extends ObjectMapper<AuthenticationRequestForValidate> {
    @Size(max = 100)
    protected String tenantId;

    public AuthenticationRequestForValidate convert() {
        return convert(AuthenticationRequestForValidate.class);
    }
}
