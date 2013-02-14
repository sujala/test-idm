package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 5:51 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class UserForValidation {
    @Size(max = 100)
    protected String id;
    @Size(max = 100)
    protected String username;
    @Size(max = 100)
    protected String email;
    @Size(max = 100)
    protected String displayName;
    @Size(max = 100)
    protected String password;
}
