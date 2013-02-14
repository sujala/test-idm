package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 3:37 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class SecretQAForValidation {
    @Size(max = 100)
    protected String username;
    @Size(max = 100)
    protected String id;
    @Size(max = 1000)
    protected String question;
    @Size(max = 100)
    protected String answer;
}
