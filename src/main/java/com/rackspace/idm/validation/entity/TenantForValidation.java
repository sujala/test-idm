package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.constraints.Size;

import static com.rackspace.idm.validation.entity.Constants.LONG_MAX;
import static com.rackspace.idm.validation.entity.Constants.MAX;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 3:59 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class TenantForValidation {
    @Size(max = MAX)
    protected String id;
    @Size(max = MAX)
    protected String name;
    @Size(max = MAX)
    protected String displayName;
    @Size(max = LONG_MAX)
    protected String description;
}
