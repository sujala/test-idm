package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.MAX;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 9:22 AM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class TokenForAuthenticationRequestForValidation {
    @Size(max = MAX)
    private String id;

}
