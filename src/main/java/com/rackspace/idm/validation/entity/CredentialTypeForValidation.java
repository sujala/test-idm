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
    protected String username;

    @Size(max = MAX)
    protected String password;

    @Size(max = MAX)
    protected String tokenKey;

    @Size(max = MAX)
    protected String key;

    @Size(max = MAX)
    protected String signature;

    @Size(max = MAX)
    protected String question;

    @Size(max = MAX)
    protected String answer;

    @Size(max = MAX)
    protected String apiKey;
}
