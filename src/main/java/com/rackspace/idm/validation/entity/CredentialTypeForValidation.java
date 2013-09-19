package com.rackspace.idm.validation.entity;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.MAX;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */
@Getter
@Setter
public class CredentialTypeForValidation {
    @Size(max = MAX)
    private String username;

    @Size(max = MAX)
    private String password;

    @Size(max = MAX)
    private String tokenKey;

    @Size(max = MAX)
    private String key;

    @Size(max = MAX)
    private String signature;

    @Size(max = MAX)
    private String question;

    @Size(max = MAX)
    private String answer;

    @Size(max = MAX)
    private String apiKey;
}
