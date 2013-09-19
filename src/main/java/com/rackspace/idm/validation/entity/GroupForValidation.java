package com.rackspace.idm.validation.entity;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.LONG_MAX;
import static com.rackspace.idm.validation.entity.Constants.MAX;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 3:22 PM
 * To change this template use File | Settings | File Templates.
 */
@Getter
@Setter
public class GroupForValidation {
    @Size(max = MAX)
    private String id;
    @Size(max = MAX)
    private String name;
    @Size(max = LONG_MAX)
    private String description;
}
