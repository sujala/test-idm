package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class StringForValidation {

    @Size(max = 100)
    private String value;
}
