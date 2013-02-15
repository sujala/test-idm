package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.*;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class StringForValidation {

    @Size(max = MAX)
    private String value;
}
