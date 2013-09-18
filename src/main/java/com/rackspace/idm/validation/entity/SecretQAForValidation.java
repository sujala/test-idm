package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.LONG_MAX;
import static com.rackspace.idm.validation.entity.Constants.MAX;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 3:37 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class SecretQAForValidation {
    @Size(max = MAX)
    private String username;
    @Size(max = MAX)
    private String id;
    @Size(max = MAX)
    private String answer;
    @Size(max = LONG_MAX)
    private String question;
}
