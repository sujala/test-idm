package com.rackspace.idm.validation.entity;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.MAX_TOKEN_LENGTH;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 9:22 AM
 * To change this template use File | Settings | File Templates.
 */
@Getter
@Setter
public class TokenForAuthenticationRequestForValidation {
    @Size(max = MAX_TOKEN_LENGTH)
    private String id;
}
