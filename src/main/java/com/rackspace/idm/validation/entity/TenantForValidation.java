package com.rackspace.idm.validation.entity;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.LONG_MAX;
import static com.rackspace.idm.validation.entity.Constants.MAX;
import static com.rackspace.idm.validation.entity.Constants.MAX_64;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 3:59 PM
 * To change this template use File | Settings | File Templates.
 */
@Getter
@Setter
public class TenantForValidation {
    @Size(max = MAX)
    private String id;
    @Size(max = MAX_64)
    private String name;
    @Size(max = MAX)
    private String displayName;
    @Size(max = LONG_MAX)
    private String description;
}
