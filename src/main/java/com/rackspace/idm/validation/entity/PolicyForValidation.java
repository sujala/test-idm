package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.*;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 5:10 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class PolicyForValidation {
    @Size(max = MAX)
    protected String id;
    @Size(max = MAX)
    protected String name;
    @Size(max = MAX)
    protected String type;
    @Size(max = LONG_MAX)
    protected String description;
    @Size(max = MEGA_MAX)
    protected String blob;
}
